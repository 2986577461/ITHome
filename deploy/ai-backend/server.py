#!/usr/bin/env python3
"""Root-owned, fixed-scope deployment entry point. Never accept shell commands/configs.

Forced SSH command: python3 /opt/apps/ithome-ai/bin/server.py
Normal root session: python3 /opt/apps/ithome-ai/bin/server.py status|rollback|recover
"""

from contextlib import contextmanager
from datetime import datetime, timezone
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
from urllib.error import HTTPError
from urllib.request import ProxyHandler, Request, build_opener

ROOT = Path("/opt/apps/ithome-ai")
MAX_ARCHIVE = 3 * 1024**3
MIN_FREE = 2 * 1024**3
SHA = re.compile(r"[0-9a-f]{40}")
DIGEST = re.compile(r"[0-9a-f]{64}")
CONTAINER = "chatbot-backend"


def run(args, timeout=180):
    result = subprocess.run(args, capture_output=True, text=True, timeout=timeout)
    if result.returncode:
        # Do not echo container environment or exception bodies into CI logs.
        raise RuntimeError(f"{args[0]} {args[1]} failed (exit {result.returncode})")
    return result.stdout.strip()


def atomic_json(path, value):
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n")
    temporary.chmod(0o600)
    temporary.replace(path)


def directory_size(path):
    return sum(p.stat().st_size for p in path.rglob("*") if p.is_file() and not p.is_symlink())


def parse_command(command):
    # Do not shlex/eval or interpret caller-controlled options.
    parts = command.split()
    if parts == ["status"]:
        return parts
    if (len(parts) == 4 and parts[0] == "deploy" and SHA.fullmatch(parts[1])
            and DIGEST.fullmatch(parts[2]) and DIGEST.fullmatch(parts[3])):
        return parts
    raise ValueError("Allowed: status | deploy <40-char SHA> <archive SHA256> <image ID>")


class Deployment:
    def __init__(self, root=ROOT):
        self.root = root

    @contextmanager
    def lock(self):
        with (self.root / "deploy.lock").open("a") as lock:
            # No second upload/stop/backup can overlap an active deployment.
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            yield

    def read(self, name):
        return json.loads((self.root / name).read_text())

    def compose(self, state, *args):
        image = state["image"]
        if not re.fullmatch(r"sha256:[0-9a-f]{64}", image):
            raise ValueError("State must pin a Docker image ID")
        (self.root / "release.env").write_text(f"AI_IMAGE={image}\n")
        command = ["docker", "compose", "--project-name", "ithome-ai", "--project-directory",
                   str(self.root), "--env-file", str(self.root / "release.env"),
                   "-f", str(self.root / "compose.yaml")]
        if state.get("legacy_prompt"):
            command += ["-f", str(self.root / "legacy.override.yaml")]
        return run(command + list(args), timeout=240)

    def activate(self, state):
        self.compose(state, "up", "-d", "--no-deps", "--force-recreate", CONTAINER)
        actual_image = run(["docker", "inspect", "--format", "{{.Image}}", CONTAINER])
        if actual_image != state["image"]:
            raise RuntimeError("Started container image does not match the pinned release")
        self.health()

    def health(self):
        opener = build_opener(ProxyHandler({}))
        host = self.read("settings.json")["http_host"]
        for _ in range(60):
            state = json.loads(run(["docker", "inspect", "--format", "{{json .State}}", CONTAINER]))
            if state.get("Status") in ("exited", "dead"):
                break
            if state.get("Health", {}).get("Status") == "healthy":
                try:
                    req = Request("http://127.0.0.1/health", headers={"Host": host})
                    with opener.open(req, timeout=3) as response:
                        if response.status != 200 or json.load(response).get("status") != "ok":
                            raise ValueError("Unexpected proxy health")
                    # A missing identity must be rejected, not bypassed during deployment.
                    req = Request("http://127.0.0.1/api/conversations", headers={"Host": host})
                    try:
                        opener.open(req, timeout=3).close()
                    except HTTPError as exc:
                        if exc.code == 401:
                            return
                    raise ValueError("Unauthenticated endpoint was not rejected")
                except (OSError, ValueError):
                    pass
            time.sleep(2)
        raise RuntimeError("AI container/proxy/auth-boundary health check failed")

    def free_space(self, extra=0):
        if shutil.disk_usage(self.root).free < MIN_FREE + extra:
            raise RuntimeError("Insufficient disk headroom; no services changed")

    def receive(self, revision, expected_hash, stream):
        incoming = self.root / "incoming"
        incoming.mkdir(mode=0o700, exist_ok=True)
        path = incoming / f"{revision}.tar.zst"
        digest = hashlib.sha256()
        size = 0
        self.free_space()
        try:
            with path.open("wb") as target:
                while block := stream.read(1024 * 1024):
                    size += len(block)
                    if size > MAX_ARCHIVE:
                        raise ValueError("Image archive too large")
                    self.free_space(len(block))
                    target.write(block)
                    digest.update(block)
            if size == 0 or digest.hexdigest() != expected_hash:
                raise ValueError("Image archive checksum mismatch")
            return path
        except BaseException:
            path.unlink(missing_ok=True)
            raise

    def load_and_check(self, archive, revision, image_id, storage_version):
        # Docker detects zstd compression. No shell/pipeline or arbitrary filenames.
        run(["docker", "load", "--input", str(archive)], timeout=600)
        image = json.loads(run(["docker", "image", "inspect", f"ithome-ai:{revision}"]))[0]
        labels = image["Config"].get("Labels") or {}
        if image["Id"] != f"sha256:{image_id}":
            raise ValueError("Loaded image ID mismatch")
        if labels.get("org.opencontainers.image.revision") != revision:
            raise ValueError("Loaded image revision mismatch")
        if labels.get("org.ithome.storage-version") != storage_version:
            raise ValueError("Storage format changed: requires a separately reviewed migration")
        if image.get("Architecture") != "amd64" or image.get("Os") != "linux":
            raise ValueError("Expected a Linux amd64 image")
        # No production volumes, credentials or network in preflight.
        run(["docker", "run", "--rm", "--network", "none", "--memory", "1536m", "--cpus", "2",
             "--cap-drop", "ALL", "--security-opt", "no-new-privileges:true",
             "-e", "DEEPSEEK_API_KEY=smoke-placeholder", "-e", "TAVILY_API_KEY=smoke-placeholder",
             "--entrypoint", "python", image["Id"], "scripts/smoke.py", "--embedding"], timeout=240)
        return {"revision": revision, "image": image["Id"], "storage_version": storage_version,
                "legacy_prompt": False}

    def backup(self, current):
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
        backup = self.root / "backups" / stamp
        backup.mkdir(parents=True, mode=0o700)
        shutil.copytree(self.root / "shared/data", backup / "data", symlinks=True)
        atomic_json(backup / "state.json", current)
        shutil.copy2(self.root / "shared/runtime.env", backup / "runtime.env")
        return str(backup)

    def replace(self, current, target):
        """The storage-version contract requires backwards-compatible writes/migrations."""
        if current["storage_version"] != target["storage_version"]:
            raise ValueError("Cannot automatically roll across storage formats")
        self.free_space(directory_size(self.root / "shared/data"))
        pending = self.root / "pending.json"
        atomic_json(pending, {"previous": current, "target": target, "phase": "stopping"})
        try:
            self.compose(current, "stop", "--timeout", "150", CONTAINER)
            backup = self.backup(current)  # All writers have stopped. Never cp a live Milvus DB.
            atomic_json(pending, {"previous": current, "target": target, "phase": "starting", "backup": backup})
            self.activate(target)
        except BaseException:
            # Restore code ONLY. Do not erase writes by automatically restoring a DB snapshot.
            try:
                self.activate(current)
            except BaseException:
                print("CRITICAL: rollback failed; pending.json retained for root recovery", file=sys.stderr)
                raise
            pending.unlink(missing_ok=True)
            raise
        atomic_json(self.root / "previous.json", current)
        atomic_json(self.root / "state.json", target)
        pending.unlink()
        print(f"Healthy AI release: {target['revision']}; previous: {current['revision']}")

    def deploy(self, revision, archive_hash, image_id, stream):
        if (self.root / "pending.json").exists():
            raise RuntimeError("Unfinished deployment; root must inspect and run recover")
        current = self.read("state.json")
        archive = self.receive(revision, archive_hash, stream)
        try:
            target = self.load_and_check(archive, revision, image_id, current["storage_version"])
            self.replace(current, target)
        finally:
            archive.unlink(missing_ok=True)

    def rollback(self):
        if (self.root / "pending.json").exists():
            raise RuntimeError("Unfinished deployment; use recover")
        self.replace(self.read("state.json"), self.read("previous.json"))

    def recover(self):
        current = self.read("pending.json")["previous"]
        self.activate(current)
        atomic_json(self.root / "state.json", current)
        (self.root / "pending.json").unlink()
        print("Recovered previous image; database snapshot was NOT restored")

    def status(self):
        print(json.dumps(self.read("state.json"), indent=2))
        if (self.root / "pending.json").exists():
            print("WARNING: unfinished deployment; inspect pending.json")
        print(run(["docker", "inspect", "--format", "{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{end}}", CONTAINER]))


def main():
    os.umask(0o077)
    if os.geteuid() != 0:
        raise PermissionError("This fixed-scope entry point must be root-owned and run as root")
    if "SSH_ORIGINAL_COMMAND" in os.environ:
        args = parse_command(os.environ["SSH_ORIGINAL_COMMAND"])
    else:
        args = sys.argv[1:]
        if args not in (["status"], ["rollback"], ["recover"]):
            raise ValueError("Root session usage: server.py status|rollback|recover")
    deployment = Deployment()
    with deployment.lock():
        if args[0] == "deploy":
            deployment.deploy(*args[1:], sys.stdin.buffer)
        else:
            getattr(deployment, args[0])()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Deployment refused/failed: {exc}", file=sys.stderr)
        sys.exit(1)
