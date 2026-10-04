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


def run(*command: str) -> str | None:
    """Run a command, returning stdout, or None when the command itself failed."""
    try:
        return subprocess.run(
            command, capture_output=True, text=True, check=True
        ).stdout
    except (subprocess.CalledProcessError, FileNotFoundError) as failure:
        print(f"[notes] {' '.join(command)} failed: {failure}", file=sys.stderr)
        return None


def existing_body(version: str) -> str:
    """The current release body, or an empty string when the release does not exist yet.

    Requires GH_TOKEN: without it gh fails and the previous body reads as empty, which
    silently drops every earlier build section and rewrites the notes as a first build.
    """
    body = run("gh", "release", "view", f"v{version}", "--json", "body", "-q", ".body")

    if body is None:
        print(
            "[notes] could not read the existing release body (is GH_TOKEN set?); "
            "the notes for this build will not be merged with the previous ones",
            file=sys.stderr,
        )
        return ""

    return body.strip()


def last_built_sha(body: str) -> str | None:
    """The commit the previous build recorded, if any."""
    for line in body.splitlines():
        if line.startswith("<!-- last-build-sha:"):
            return line.split(":", 1)[1].removesuffix("-->").strip() or None
    return None


def commits(since: str | None, limit: int = 30) -> list[str]:
    """Commit subjects to list, newest first.

    Three cases matter, and they are not the same:
      - the recorded sha is unresolvable (a rebase or force-push moved the history):
        fall back to the newest commits, because the build still deserves a changelog;
      - the range resolved but is empty (a re-run for a commit already released):
        say so, rather than dumping the whole history as if it were new;
      - no recorded sha at all (first build of this version): list the newest commits.
    """
    if since:
        log = run("git", "log", "--no-merges", "--pretty=format:- %s (`%h`)", f"{since}..HEAD")

        if log is None:
            print(
                f"[notes] {since[:7]} is not in this history (rebased or force-pushed); "
                "listing the newest commits instead",
                file=sys.stderr,
            )
        else:
            entries = [line for line in log.splitlines() if line.strip()]
            if entries:
                return entries
            print(f"[notes] no commits since {since[:7]}; this is a re-run", file=sys.stderr)
            return ["- No new commits: this build re-runs the commit already released."]

    log = run(
        "git", "log", "--no-merges", f"--max-count={limit}", "--pretty=format:- %s (`%h`)", "HEAD"
    )
    entries = [line for line in (log or "").splitlines() if line.strip()]

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
