"""Reject PR/fork/failed/different-workflow artifacts before requesting production access."""

import json
import os
import re
from urllib.request import Request, urlopen


def validate(run, repository, revision):
    required = {
        "status": "completed",
        "conclusion": "success",
        "head_sha": revision,
        "path": ".github/workflows/ai-backend-ci.yml",
    }
    for key, expected in required.items():
        if run.get(key) != expected:
            raise ValueError(f"CI run has unexpected {key}")
    if run.get("event") not in ("push", "workflow_dispatch"):
        raise ValueError("Only trusted branch CI runs may deploy")
    if run.get("head_branch") not in ("dev", "main"):
        raise ValueError("CI branch is not a release source")
    if (run.get("head_repository") or {}).get("full_name") != repository:
        raise ValueError("CI repository mismatch")
    if (run.get("repository") or {}).get("full_name") != repository:
        raise ValueError("Workflow repository mismatch")


def main():
    run_id = os.environ["CI_RUN_ID"]
    revision = os.environ["EXPECTED_REVISION"]
    if not re.fullmatch(r"[1-9][0-9]*", run_id) or not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("Invalid CI run ID or full Git SHA")
    repository = os.environ["GITHUB_REPOSITORY"]
    request = Request(
        f"https://api.github.com/repos/{repository}/actions/runs/{run_id}",
        headers={"Authorization": f"Bearer {os.environ['GH_TOKEN']}",
                 "Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28"},
    )
    with urlopen(request, timeout=30) as response:
        run = json.load(response)
    validate(run, repository, revision)
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"revision={revision}\nrun_id={run_id}\n")
    print(f"Verified successful trusted CI run {run_id} for {revision}")


if __name__ == "__main__":
    main()
