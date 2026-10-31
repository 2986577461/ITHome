"""Deployment contract tests: temp files and fake Docker only; no production access."""

import hashlib
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import artifact
import bootstrap
import server
import verify_run

REV = "a" * 40
HASH = "b" * 64
IMAGE = "sha256:" + "c" * 64
OLD = dict(revision="legacy-test", image=IMAGE, storage_version="1", legacy_prompt=True)
NEW = dict(revision=REV, image="sha256:" + "d" * 64, storage_version="1", legacy_prompt=False)


class ArtifactTests(unittest.TestCase):
    def test_archive_and_revision_must_match(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "image.tar.zst").write_bytes(b"synthetic archive")
            data = dict(revision=REV, image=f"ithome-ai:{REV}", image_id=HASH,
                        archive_sha256=artifact.checksum(root / "image.tar.zst"))
            (root / "manifest.json").write_text(json.dumps(data))
            self.assertEqual(artifact.verify(root, REV), data)
            with self.assertRaises(ValueError):
                artifact.verify(root, "e" * 40)
            (root / "image.tar.zst").write_bytes(b"tampered")
            with self.assertRaisesRegex(ValueError, "checksum"):
                artifact.verify(root, REV)

    def test_manifest_cannot_inject_github_environment(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "manifest.json").write_text(json.dumps(dict(
                revision=REV, image=f"ithome-ai:{REV}", image_id=HASH + "\nEVIL=1",
                archive_sha256=HASH)))
            with self.assertRaisesRegex(ValueError, "image_id"):
                artifact.verify(root, REV)


class WorkflowTrustTests(unittest.TestCase):
    def valid(self):
        return dict(status="completed", conclusion="success", head_sha=REV,
                    path=".github/workflows/ai-backend-ci.yml", event="push", head_branch="dev",
                    head_repository={"full_name": "owner/repo"}, repository={"full_name": "owner/repo"})

    def test_successful_own_branch_run(self):
        verify_run.validate(self.valid(), "owner/repo", REV)

    def test_reject_pr_fork_wrong_workflow_commit_branch_or_failure(self):
        for key, value in (("event", "pull_request"), ("head_repository", {"full_name": "fork/repo"}),
                           ("repository", {}), ("path", "other.yml"), ("head_sha", "e" * 40),
                           ("head_branch", "untrusted"), ("conclusion", "failure"), ("status", "in_progress")):
            with self.subTest(key=key):
                run = self.valid()
                run[key] = value
                with self.assertRaises(ValueError):
                    verify_run.validate(run, "owner/repo", REV)


class CommandTests(unittest.TestCase):
    def test_only_fixed_ssh_grammar(self):
        self.assertEqual(server.parse_command(f"deploy {REV} {HASH} {HASH}"), ["deploy", REV, HASH, HASH])
        self.assertEqual(server.parse_command("status"), ["status"])
        for command in ("", "bash", "rollback", "recover", "status; id", "scp -t /root/x", "deploy ../../x",
                        f"deploy {REV} {HASH} {HASH}; id", f"deploy {'A'*40} {HASH} {HASH}"):
            with self.subTest(command=command), self.assertRaises(ValueError):
                server.parse_command(command)


class DeploymentTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.deployment = server.Deployment(self.root)
        server.atomic_json(self.root / "state.json", OLD)
        (self.root / "shared/data").mkdir(parents=True)
        (self.root / "shared/data/synthetic").write_text("keep this")
        (self.root / "shared/runtime.env").write_text("FAKE_KEY=not-a-real-secret\n")
        self.deployment.free_space = Mock()

    def test_lock_excludes_second_deployment(self):
        with self.deployment.lock():
            with self.assertRaises(BlockingIOError), self.deployment.lock():
                self.fail("Lock was not exclusive")

    def test_receive_checks_hash_and_removes_bad_upload(self):
        payload = b"synthetic compressed image"
        checksum = hashlib.sha256(payload).hexdigest()
        path = self.deployment.receive(REV, checksum, io.BytesIO(payload))
        self.assertEqual(path.read_bytes(), payload)
        with self.assertRaisesRegex(ValueError, "checksum"):
            self.deployment.receive(REV, HASH, io.BytesIO(payload))
        self.assertFalse(path.exists())

    def test_receive_enforces_size_and_free_space_before_stop(self):
        with patch.object(server, "MAX_ARCHIVE", 2), self.assertRaisesRegex(ValueError, "too large"):
            self.deployment.receive(REV, HASH, io.BytesIO(b"123"))
        self.deployment.free_space.side_effect = RuntimeError("disk full")
        with self.assertRaisesRegex(RuntimeError, "disk full"):
            self.deployment.receive(REV, HASH, io.BytesIO(b"123"))

    def test_success_order_and_pinned_previous_state(self):
        events = []
        self.deployment.compose = Mock(side_effect=lambda *a: events.append("stop"))
        self.deployment.backup = Mock(side_effect=lambda *a: events.append("backup") or "snapshot")
        self.deployment.activate = Mock(side_effect=lambda *a: events.append("activate"))
        self.deployment.replace(OLD, NEW)
        self.assertEqual(events, ["stop", "backup", "activate"])
        self.assertEqual(self.deployment.read("state.json"), NEW)
        self.assertEqual(self.deployment.read("previous.json"), OLD)
        self.assertFalse((self.root / "pending.json").exists())

    def test_failed_release_rolls_back_code_without_restoring_data(self):
        self.deployment.compose = Mock()
        self.deployment.backup = Mock(return_value="snapshot")
        self.deployment.activate = Mock(side_effect=[RuntimeError("unhealthy"), None])
        with self.assertRaisesRegex(RuntimeError, "unhealthy"):
            self.deployment.replace(OLD, NEW)
        self.assertEqual([call.args[0] for call in self.deployment.activate.call_args_list], [NEW, OLD])
        self.assertEqual(self.deployment.read("state.json"), OLD)
        self.assertEqual((self.root / "shared/data/synthetic").read_text(), "keep this")
        self.assertFalse((self.root / "pending.json").exists())

    def test_backup_failure_restarts_current_without_starting_target(self):
        self.deployment.compose = Mock()
        self.deployment.backup = Mock(side_effect=OSError("copy failed"))
        self.deployment.activate = Mock()
        with self.assertRaises(OSError):
            self.deployment.replace(OLD, NEW)
        self.deployment.activate.assert_called_once_with(OLD)

    def test_failed_rollback_retains_recovery_journal(self):
        self.deployment.compose = Mock()
        self.deployment.backup = Mock(return_value="snapshot")
        self.deployment.activate = Mock(side_effect=RuntimeError("unhealthy"))
        with self.assertRaises(RuntimeError):
            self.deployment.replace(OLD, NEW)
        self.assertEqual(self.deployment.read("pending.json")["previous"], OLD)
        with self.assertRaisesRegex(RuntimeError, "Unfinished"):
            self.deployment.deploy(REV, HASH, HASH, io.BytesIO())

    def test_different_storage_version_never_stops_current(self):
        self.deployment.compose = Mock()
        with self.assertRaisesRegex(ValueError, "storage"):
            self.deployment.replace(OLD, dict(NEW, storage_version="2"))
        self.deployment.compose.assert_not_called()

    def test_frozen_backup_contains_separate_copy_and_private_env(self):
        path = Path(self.deployment.backup(OLD))
        self.assertEqual((path / "data/synthetic").read_text(), "keep this")
        (self.root / "shared/data/synthetic").write_text("new write")
        self.assertEqual((path / "data/synthetic").read_text(), "keep this")
        self.assertEqual(json.loads((path / "state.json").read_text()), OLD)
        self.assertEqual((path / "state.json").stat().st_mode & 0o777, 0o600)

    def test_recover_activates_journal_previous_not_target(self):
        server.atomic_json(self.root / "pending.json", {"previous": OLD, "target": NEW})
        self.deployment.activate = Mock()
        self.deployment.recover()
        self.deployment.activate.assert_called_once_with(OLD)
        self.assertFalse((self.root / "pending.json").exists())

    def test_compose_rejects_mutable_image_tag(self):
        with self.assertRaisesRegex(ValueError, "pin"):
            self.deployment.compose(dict(OLD, image="ithome-ai:latest"), "up")

    def test_compose_is_ai_only_and_legacy_prompt_is_versioned(self):
        with patch.object(server, "run", return_value="") as command:
            self.deployment.compose(OLD, "stop", server.CONTAINER)
            args = command.call_args.args[0]
            self.assertIn(str(self.root / "legacy.override.yaml"), args)
            self.assertEqual(args[-2:], ["stop", "chatbot-backend"])
            self.deployment.compose(NEW, "up", "-d", "--no-deps", server.CONTAINER)
            self.assertNotIn(str(self.root / "legacy.override.yaml"), command.call_args.args[0])

    def test_preflight_is_offline_without_production_credentials_or_volumes(self):
        image = dict(Id=IMAGE, Architecture="amd64", Os="linux", Config={"Labels": {
            "org.opencontainers.image.revision": REV, "org.ithome.storage-version": "1"}})
        with patch.object(server, "run", side_effect=["loaded", json.dumps([image]), "ok"]) as command:
            result = self.deployment.load_and_check(Path("image.zst"), REV, "c" * 64, "1")
            self.assertEqual(result["image"], IMAGE)
            args = command.call_args.args[0]
            self.assertEqual(args[args.index("--network") + 1], "none")
            for forbidden in ("--volume", "-v", "--mount", "--env-file", "--privileged"):
                self.assertNotIn(forbidden, args)

    def test_mismatched_image_metadata_never_reaches_preflight(self):
        for change in (dict(Id="sha256:" + HASH), dict(Architecture="arm64"),
                       dict(Config={"Labels": {"org.opencontainers.image.revision": REV,
                                                "org.ithome.storage-version": "2"}})):
            image = dict(Id=IMAGE, Architecture="amd64", Os="linux", Config={"Labels": {
                "org.opencontainers.image.revision": REV, "org.ithome.storage-version": "1"}})
            image.update(change)
            with patch.object(server, "run", side_effect=["loaded", json.dumps([image])]) as command:
                with self.assertRaises(ValueError):
                    self.deployment.load_and_check(Path("image.zst"), REV, "c" * 64, "1")
                self.assertEqual(command.call_count, 2)


class BootstrapTests(unittest.TestCase):
    def test_plan_does_not_stop_or_copy(self):
        original = dict(Image=IMAGE, State={"Status": "running"}, Mounts=[{"Destination": "/app/db"},
                        {"Destination": "/app/SystemPrompt.md"}], NetworkSettings={"Networks": {"ithome_net": {}}})
        with tempfile.TemporaryDirectory() as directory, patch.object(bootstrap, "run", return_value=json.dumps([original])) as command:
            bootstrap.migrate(Path(directory), apply=False)
            self.assertEqual(command.call_count, 1)
            self.assertEqual(list(Path(directory).iterdir()), [])

    def test_sqlite_backup_requires_all_three_databases(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaisesRegex(RuntimeError, "Missing expected"):
                bootstrap.check_sqlite(Path(directory))


if __name__ == "__main__":
    unittest.main()
