"""
Resolves known fork-vs-upstream conflicts in app/build.gradle.kts.

Rules:
- GITHUB_OWNER / GITHUB_REPO: always keep ours (fork updater target)
- versionCode / versionName: take upstream's numbers, append -fork to versionName
- Everything else: take upstream's version
"""
import re
import sys

TARGET = "app/build.gradle.kts"

with open(TARGET) as f:
    content = f.read()

if "<<<<<<< HEAD" not in content:
    print(f"No conflict markers in {TARGET}, nothing to do.")
    sys.exit(0)

def resolve_block(match):
    ours   = match.group(1)
    theirs = match.group(2)

    if "GITHUB_OWNER" in ours or "GITHUB_REPO" in ours:
        return ours

    if "versionCode" in theirs or "versionName" in theirs:
        resolved = re.sub(
            r'(versionName\s*=\s*")([^"]+)(")',
            lambda m: m.group(1) + m.group(2).rstrip("-fork") + "-fork" + m.group(3),
            theirs,
        )
        return resolved

    return theirs

pattern = re.compile(
    r"<<<<<<< HEAD\n(.*?)\n=======\n(.*?)\n>>>>>>> [^\n]+",
    re.DOTALL,
)
resolved = pattern.sub(resolve_block, content)

with open(TARGET, "w") as f:
    f.write(resolved)

print(f"Resolved {TARGET}")
