#!/usr/bin/env python3
"""One-time persistence migration, preserving the exact running image and prompt.

Run after installing compose.yaml, legacy.override.yaml, and bin/server.py.
Default is a read-only plan. --apply authorizes an AI-only maintenance window.
No user records or credentials are printed or transferred off the server.
"""

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import shutil
import sqlite3
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from server import CONTAINER, ROOT, Deployment, atomic_json, run

DATA = {
    "conversation": "/app/conversation/db",
    "agent": "/app/agent/db",
    "kb": "/app/kb/db",
    "vector": "/app/common/db",
    "legacy": "/app/db",
}


def check_sqlite(directory):
    # Only inspect frozen backup files, never live databases or user rows.
    for relative in ("conversation/app.db", "agent/conversations.db", "kb/kb.db"):
        path = directory / relative
        if not path.is_file():
            raise RuntimeError(f"Missing expected SQLite backup: {relative}")
        with sqlite3.connect(f"file:{path}?mode=ro", uri=True) as connection:
            if connection.execute("PRAGMA quick_check").fetchone()[0] != "ok":
                raise RuntimeError(f"SQLite backup check failed: {relative}")
    if not (directory / "vector/milvus.db").exists():
        raise RuntimeError("Missing Milvus backup")


def migrate(root, apply=False):
    deployment = Deployment(root)
    if (root / "state.json").exists():
        raise RuntimeError("Already initialized; do not repeat bootstrap")
    if (root / "bootstrap-pending.json").exists():
        raise RuntimeError("Previous bootstrap unfinished; root must inspect before retrying")
    original = json.loads(run(["docker", "inspect", CONTAINER]))[0]
    if original["State"]["Status"] != "running":
        raise RuntimeError("Expected the original AI container to be running")
    mounts = {item["Destination"] for item in original["Mounts"]}
    if mounts != {"/app/db", "/app/SystemPrompt.md"}:
        raise RuntimeError("Unexpected original mounts; review migration rather than omitting data")
    if "ithome_net" not in original["NetworkSettings"]["Networks"]:
        raise RuntimeError("Original container network differs from the reviewed deployment")
    print(f"Original image ID: {original['Image']}")
    print("Plan: stop AI only; snapshot all five data directories; verify SQLite; start SAME image with persistent mounts.")
    print("Java, Nginx, MySQL, Redis, and other projects will not be restarted.")
    if not apply:
        return
    if os.geteuid() != 0:
        raise PermissionError("Bootstrap must run as root")
    deployment.free_space(1024**3)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    legacy_name = f"chatbot-backend-legacy-{stamp}"
    backup = root / "backups" / f"bootstrap-{stamp}"
    backup.mkdir(parents=True, mode=0o700)
    atomic_json(backup / "container-inspect.json", original)  # Contains secrets: root-only.
    shared = root / "shared"
    shared.mkdir(mode=0o700, exist_ok=True)
    (shared / "logs").mkdir(mode=0o700, exist_ok=True)
    env = original["Config"]["Env"]
    if any("\n" in line or "\r" in line for line in env):
        raise ValueError("Multiline environment requires a separately reviewed encoding")
    (shared / "runtime.env").write_text("\n".join(env) + "\n")
    (shared / "runtime.env").chmod(0o600)
    baseline = {"revision": f"legacy-{stamp}", "image": original["Image"],
                "storage_version": "1", "legacy_prompt": True, "legacy_container": legacy_name}
    deployment.compose(baseline, "config", "--quiet")  # Validate raw env/Compose before stopping anything.
    atomic_json(root / "bootstrap-pending.json", {"original_id": original["Id"], "legacy_container": legacy_name})
    renamed = False
    try:
        run(["docker", "stop", "--time", "150", CONTAINER], timeout=180)
        (backup / "data").mkdir(mode=0o700)
        for name, source in DATA.items():
            run(["docker", "cp", "--archive", f"{CONTAINER}:{source}", str(backup / "data" / name)])
        check_sqlite(backup / "data")
        shutil.copytree(backup / "data", shared / "data", symlinks=True)
        run(["docker", "cp", f"{CONTAINER}:/app/SystemPrompt.md", str(shared / "legacy-SystemPrompt.md")])
        shutil.copy2(shared / "runtime.env", backup / "runtime.env")
        shutil.copy2(shared / "legacy-SystemPrompt.md", backup / "SystemPrompt.md")
        # Retain the original stopped container; it still holds its unmodified writable layer.
        run(["docker", "rename", CONTAINER, legacy_name])
        renamed = True
        deployment.activate(baseline)
        atomic_json(root / "state.json", baseline)
        atomic_json(backup / "state.json", baseline)
        (root / "bootstrap-pending.json").unlink()
        print(f"Persistence migration healthy. Frozen baseline backup: {backup}")
        print(f"Original stopped container retained: {legacy_name}")
    except BaseException:
        if renamed:
            # Only remove the replacement container, never the retained original.
            existing = run(["docker", "ps", "-aq", "--filter", f"name=^/{CONTAINER}$"])
            if existing:
                run(["docker", "rm", "--force", CONTAINER])
            run(["docker", "rename", legacy_name, CONTAINER])
        run(["docker", "start", CONTAINER])
        deployment.health()
        print("Bootstrap failed; original container restarted. Keep backup and inspect bootstrap-pending.json.", file=sys.stderr)
        raise


def main():
    os.umask(0o077)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    if not ROOT.is_dir():
        raise RuntimeError("Install the root-owned deployment files first")
    with Deployment().lock():
        migrate(ROOT, args.apply)


if __name__ == "__main__":
    main()
