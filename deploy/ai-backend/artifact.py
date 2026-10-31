"""Create/validate release metadata without executing any artifact-provided code."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

SHA = re.compile(r"[0-9a-f]{40}")
DIGEST = re.compile(r"[0-9a-f]{64}")


def checksum(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def verify(directory, revision):
    if not SHA.fullmatch(revision):
        raise ValueError("Expected a full lowercase Git SHA")
    data = json.loads((directory / "manifest.json").read_text())
    if data.get("revision") != revision or data.get("image") != f"ithome-ai:{revision}":
        raise ValueError("Artifact revision/image mismatch")
    for key in ("archive_sha256", "image_id"):
        if not isinstance(data.get(key), str) or not DIGEST.fullmatch(data[key]):
            raise ValueError(f"Invalid {key}")
    if checksum(directory / "image.tar.zst") != data["archive_sha256"]:
        raise ValueError("Archive checksum mismatch")
    return data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("create", "verify"))
    parser.add_argument("directory", type=Path)
    parser.add_argument("revision")
    args = parser.parse_args()
    if not SHA.fullmatch(args.revision):
        parser.error("revision must be a full lowercase Git SHA")
    if args.action == "create":
        image = f"ithome-ai:{args.revision}"
        image_id = subprocess.check_output(
            ["docker", "image", "inspect", "--format", "{{.Id}}", image], text=True
        ).strip().removeprefix("sha256:")
        manifest = dict(revision=args.revision, image=image, image_id=image_id,
                        archive_sha256=checksum(args.directory / "image.tar.zst"))
        (args.directory / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    data = verify(args.directory, args.revision)
    if args.action == "verify":
        print(f"REVISION={data['revision']}")
        print(f"ARCHIVE_SHA256={data['archive_sha256']}")
        print(f"IMAGE_ID={data['image_id']}")


if __name__ == "__main__":
    main()
