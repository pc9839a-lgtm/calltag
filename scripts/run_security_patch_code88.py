from pathlib import Path
import runpy
import subprocess

# Restore the canonical full regression workflow from the product branch before
# the staged patch script upgrades its version/contracts. This lets the existing
# workflow path act as a one-shot patch runner without leaving temporary CI code.
subprocess.run(["git", "fetch", "origin", "agent/play-internal-v0430-run2"], check=True)
base = subprocess.check_output([
    "git", "show",
    "origin/agent/play-internal-v0430-run2:.github/workflows/calltag-hotfix-build.yml",
], text=True)
Path(".github/workflows/calltag-hotfix-build.yml").write_text(base)
runpy.run_path("scripts/security_patch_code88.py", run_name="__main__")

for cleanup in [
    ".github/workflows/v04410-security-patch.yml",
    ".github/workflows/v04410-security-trigger.yml",
    "scripts/run_security_patch_code88.py",
]:
    path = Path(cleanup)
    if path.exists():
        path.unlink()
