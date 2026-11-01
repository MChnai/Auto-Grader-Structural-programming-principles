#!/usr/bin/env python3
"""Check P00.1 repository hygiene without external Python dependencies."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def git(*args: str) -> subprocess.CompletedProcess[bytes]:
    return subprocess.run(
        ["git", "-C", str(ROOT), *args],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def main() -> int:
    errors: list[str] = []
    ignore_file = ROOT / ".gitignore"
    if not ignore_file.is_file():
        errors.append("Missing .gitignore")
    else:
        contents = ignore_file.read_text(encoding="utf-8")
        if any(marker in contents for marker in ("<<<<<<<", "======= HEAD", ">>>>>>>")):
            errors.append("Git conflict marker detected in .gitignore")
        for required in ("target/", "*.class", "*.jar"):
            if required not in contents:
                errors.append(f"Missing ignore rule: {required}")

    checked = git("ls-files", "-z")
    if checked.returncode != 0:
        errors.append("Not a git repository or git ls-files failed: " + checked.stderr.decode(errors="replace"))
        tracked: list[str] = []
    else:
        tracked = [x.decode("utf-8", errors="replace") for x in checked.stdout.split(b"\x00") if x]
    for filename in tracked:
        p = Path(filename)
        if "target" in p.parts or p.suffix.lower() in {".class", ".jar", ".war", ".ear"}:
            errors.append("Generated artifact tracked: " + filename)
        if filename == "dependency-reduced-pom.xml":
            errors.append("Generated dependency-reduced-pom.xml tracked")

    if not any(f.startswith("src/main/java/") and f.endswith(".java") for f in tracked):
        errors.append("No Java source file tracked")
    if not any(f.startswith("src/test/java/") and f.endswith(".java") for f in tracked):
        errors.append("No Java test source file tracked")
    for required in ("README.md", "CHANGELOG.md", "docs/architecture/README.md", "docs/research/README.md", "experiments/README.md"):
        if not (ROOT / required).is_file():
            errors.append("Missing required P00.1 file: " + required)

    for result in ("target/", "generated.class", "build.jar", "dependency-reduced-pom.xml"):
        if git("check-ignore", "-q", result).returncode != 0:
            errors.append("Ignore rule not effective for: " + result)
    for fixture in ("test_data/testcases/original.zip", "test_data/testcases/tc1.out"):
        if git("check-ignore", "-q", fixture).returncode == 0:
            errors.append("Tracked fixture incorrectly ignored: " + fixture)

    if errors:
        print("P00.1 hygiene: FAIL")
        for issue in errors:
            print("  -", issue)
        return 1
    print(f"P00.1 hygiene: PASS ({len(tracked)} tracked files, no generated binary artifacts)")
    print("Note: this checks repository hygiene, not Java build/test or GitHub remote sync.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
