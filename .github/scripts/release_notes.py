#!/usr/bin/env python3
"""Merge this build's commits into the release notes for a fixed version.

The plugin version is permanent, so a release accumulates builds: each push prepends a
"Build N" section listing the commits since the previous build, and the previous sections
are preserved. The last built commit is remembered in an HTML comment in the release
body, which is also what makes the merge idempotent.
"""

from __future__ import annotations

import argparse
import datetime
import os
import subprocess
import sys

MARKER = "<!-- last-build-sha: {} -->"
HEADER = "## Changelog"


def run(*command: str) -> str:
    """Run a command, returning stdout (or an empty string when it fails)."""
    try:
        return subprocess.run(
            command, capture_output=True, text=True, check=True
        ).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as failure:
        print(f"[notes] {' '.join(command)} failed: {failure}", file=sys.stderr)
        return ""


def existing_body(version: str) -> str:
    """The current release body, or an empty string when the release does not exist yet."""
    body = run("gh", "release", "view", f"v{version}", "--json", "body", "-q", ".body")
    return body.strip()


def last_built_sha(body: str) -> str | None:
    """The commit the previous build recorded, if any."""
    for line in body.splitlines():
        if line.startswith("<!-- last-build-sha:"):
            return line.split(":", 1)[1].removesuffix("-->").strip() or None
    return None


def commits(since: str | None, limit: int = 30) -> list[str]:
    """Commit subjects to list, newest first.

    When the recorded sha can no longer be resolved — a rebase or force-push moved the
    history it pointed at — fall back to the newest commits rather than reporting that no
    details exist, because the build itself is still worth a changelog entry.
    """
    entries: list[str] = []

    if since:
        log = run("git", "log", "--no-merges", "--pretty=format:- %s (`%h`)", f"{since}..HEAD")
        entries = [line for line in log.splitlines() if line.strip()]
        if not entries:
            print(
                f"[notes] {since[:7]} is not in this history (rebased or force-pushed); "
                "listing the newest commits instead",
                file=sys.stderr,
            )

    if not entries:
        log = run(
            "git", "log", "--no-merges", f"--max-count={limit}", "--pretty=format:- %s (`%h`)", "HEAD"
        )
        entries = [line for line in log.splitlines() if line.strip()]

    return entries or ["- No commit details available for this build."]


def strip_old_header(body: str) -> str:
    """Drop our own changelog header and the previous sha marker from the old body."""
    lines = [
        line
        for line in body.splitlines()
        if not line.startswith("<!-- last-build-sha:")
        and not line.strip() == HEADER
    ]
    return "\n".join(lines).strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--build", required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--run-url", default="")
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    previous = existing_body(args.version)
    since = last_built_sha(previous)

    stamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    section = [
        f"### Build {args.build} — {stamp}",
        "",
    ]

    if args.run_url:
        section.append(f"Commit [`{args.sha[:7]}`]({args.run_url})")
    else:
        section.append(f"Commit `{args.sha[:7]}`")

    section.append("")
    section.extend(commits(since))
    section.append("")
    section.append(MARKER.format(args.sha))

    if since:
        print(f"[notes] changes since {since[:7]}", file=sys.stderr)
    else:
        print("[notes] first build recorded for this version", file=sys.stderr)

    older = strip_old_header(previous)

    parts = [HEADER, "", "\n".join(section)]

    if older:
        parts.extend(["", "---", "", older])

    body = "\n".join(parts).rstrip() + "\n"

    with open(args.out, "w", encoding="utf-8") as handle:
        handle.write(body)

    print(body)
    return 0


if __name__ == "__main__":
    sys.exit(main())
