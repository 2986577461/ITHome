"""No paid calls, real user data, or production writes in these smoke checks."""

import argparse
import json
from pathlib import Path
import sys
import tempfile
from urllib.request import urlopen

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def live():
    with urlopen("http://127.0.0.1:8000/health", timeout=2) as response:
        assert response.status == 200
        assert json.load(response)["status"] == "ok"


def isolated(embedding=False, storage_version=None):
    # Run ONLY in an ephemeral container without production mounts or network.
    root = Path(__file__).resolve().parents[1]
    assert root == Path("/app"), "Isolated smoke must not run against a local development checkout"
    assert not (root / ".env").exists(), "Credentials were copied into the image"
    for relative in ("db", "agent/db", "common/db", "conversation/db", "kb/db"):
        directory = root / relative
        assert not directory.exists() or not any(directory.iterdir()), "Image/mount contains business data"
    if storage_version is not None:
        actual = (root / "storage-version").read_text().strip()
        assert actual == storage_version, "Storage-version file/label mismatch"
    from fastapi.testclient import TestClient
    from main import app

    with TestClient(app) as client:
        assert client.get("/health").json()["status"] == "ok"
        assert client.get("/api/conversations").status_code == 401
    if embedding:
        from common.vector_store import DIM, VectorStore

        with tempfile.TemporaryDirectory() as directory:
            store = VectorStore(str(Path(directory) / "milvus.db"))
            try:
                assert len(store.embed_one("部署自检")) == DIM
                assert store.insert_chunks(1, ["部署自检的合成资料"], "smoke") == 1
                assert store.search("部署自检", user_id="smoke")
                # Sharing vs owner-only visibility is a business test, not a CD assumption.
                try:
                    store.search("部署自检", user_id="")
                except ValueError:
                    pass
                else:
                    raise AssertionError("Unauthenticated vector search must be rejected")
            finally:
                store.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--live", action="store_true")
    parser.add_argument("--embedding", action="store_true")
    parser.add_argument("--storage-version")
    args = parser.parse_args()
    if args.live:
        live()
    else:
        isolated(args.embedding, args.storage_version)
