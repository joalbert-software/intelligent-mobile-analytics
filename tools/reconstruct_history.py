"""Create a disclosed synthetic history in a NEW local repository only.

Contemporary Python 3 maintenance tool, excluded from implementation LOC.
It refuses to rewrite any pre-existing Git repository.
"""
import collections
import datetime as dt
import json
import os
from pathlib import Path
import random
import subprocess
import sys
import shutil
import hashlib
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
EXCLUDED = {".git", "target", "__pycache__", ".tools"}
INDEX = ROOT.parent / ".tools" / "ima-reconstruction-index"
METADATA = ROOT.parent / ".tools" / "ima-reconstructed-metadata"


def git(*arguments, environment=None, input=None):
    environment = dict(os.environ if environment is None else environment)
    environment["GIT_INDEX_FILE"] = str(INDEX)
    environment["GIT_DIR"] = str(METADATA)
    environment["GIT_WORK_TREE"] = str(ROOT)
    result = subprocess.run(["git"] + list(arguments), cwd=str(ROOT),
                            env=environment, capture_output=True, input=input)
    if result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", "replace").strip() or "Git operation failed")
    return result.stdout.decode("utf-8").strip()


def make_tree(entries, cache):
    directories = {}
    files = []
    for path, value in entries.items():
        if "/" in path:
            directory, remainder = path.split("/", 1)
            directories.setdefault(directory, {})[remainder] = value
        else:
            files.append((path, value[0], value[1]))
    for name, children in directories.items():
        files.append((name, "040000", make_tree(children, cache)))
    key = tuple(sorted(files))
    if key not in cache:
        rows = []
        ordered = sorted(key, key=lambda row: (row[0] + ("/" if row[1] == "040000" else "")).encode("utf-8"))
        for name, mode, oid in ordered:
            rows.append((mode.lstrip("0") + " " + name).encode("utf-8") + b"\0" + bytes.fromhex(oid))
        cache[key] = write_object("tree", b"".join(rows))
    return cache[key]


def write_object(kind, content):
    # Standard loose Git objects; direct writes avoid unsupported lock renames.
    raw = (kind + " " + str(len(content))).encode("ascii") + b"\0" + content
    oid = hashlib.sha1(raw).hexdigest()
    path = METADATA / "objects" / oid[:2] / oid[2:]
    path.parent.mkdir(exist_ok=True)
    if path.exists():
        if zlib.decompress(path.read_bytes()) != raw:
            raise RuntimeError("Existing Git object content does not match its identity")
        return oid
    path.write_bytes(zlib.compress(raw))
    return oid


def write_index(entries):
    # Git index v2: zero stat fields force content comparison on first checkout.
    data = bytearray(b"DIRC" + struct.pack("!II", 2, len(entries)))
    for path, (mode, oid) in sorted(entries.items()):
        name = path.encode("utf-8")
        entry = struct.pack("!10I", 0, 0, 0, 0, 0, 0, int(mode, 8), 0, 0,
                            (ROOT / path).stat().st_size)
        entry += bytes.fromhex(oid) + struct.pack("!H", min(len(name), 4095)) + name + b"\0"
        entry += b"\0" * ((-len(entry)) % 8)
        data.extend(entry)
    data.extend(hashlib.sha1(data).digest())
    (METADATA / "index").write_bytes(data)


def copy_metadata(source, destination):
    target = Path(destination)
    if target.exists() and Path(source).read_bytes() == target.read_bytes():
        return str(target)
    return shutil.copy2(source, destination)


def year_for(path):
    if path.startswith(("src/main/java/com/ima/domain/", "src/main/java/com/ima/analytics/")):
        return 2013
    if path.startswith(("src/main/java/com/ima/store/", "src/main/java/com/ima/persistence/", "sql/mysql/")):
        return 2014
    if path.startswith(tuple("src/main/java/com/ima/" + part + "/"
                             for part in ("security", "service", "catalog", "client"))):
        return 2015
    if path.startswith(("batch/ima_batch/", "sql/hive/", "src/main/java/com/ima/cache/", "ops/hadoop-")):
        return 2016
    return 2017


def dates_for(year, count, rng):
    mondays = []
    date = dt.date(year, 1, 14)
    while date.weekday() != 0:
        date += dt.timedelta(days=1)
    while date < dt.date(year, 12, 16):
        mondays.append(date)
        date += dt.timedelta(days=7)
    active = rng.sample(mondays, min(len(mondays), max(1, (count + 1) // 2)))
    timestamps = set()
    while len(timestamps) < count:
        day = rng.choice(active) + dt.timedelta(days=rng.randrange(5))
        timestamps.add(dt.datetime.combine(day, dt.time(rng.randrange(8, 20),
                                                        rng.randrange(60), rng.randrange(60))))
    return [stamp.isoformat() + "+00:00" for stamp in sorted(timestamps)]


def main():
    INDEX.parent.mkdir(exist_ok=True)
    resume_empty = sys.argv[1:] == ["--resume-empty"]
    if sys.argv[1:] and not resume_empty:
        raise SystemExit("Only --resume-empty is supported for recovery of this tool's empty repository.")
    if (ROOT / ".git").exists():
        head = subprocess.run(["git", "rev-parse", "--verify", "HEAD"], cwd=str(ROOT), capture_output=True)
        if not resume_empty or head.returncode == 0:
            raise SystemExit("Refusing to replace or rewrite an existing Git history.")
        marker = subprocess.run(["git", "config", "--local", "--get", "core.hooksPath"],
                                cwd=str(ROOT), capture_output=True, text=True)
        if marker.stdout.strip() != ".git/no-hooks":
            raise SystemExit("Repository was not initialized by this reconstruction tool.")
    if METADATA.exists():
        temporary_head = subprocess.run(["git", "--git-dir=" + str(METADATA), "rev-parse", "--verify", "HEAD"],
                                        cwd=str(ROOT), capture_output=True)
        if not resume_empty or temporary_head.returncode == 0:
            raise SystemExit("Refusing to overwrite existing temporary Git history.")
    paths = sorted(path.relative_to(ROOT).as_posix() for path in ROOT.rglob("*")
                   if path.is_file() and not any(part in EXCLUDED for part in path.relative_to(ROOT).parts))
    first_files = [".gitattributes", ".gitignore", "README.md", "HISTORY.md"]
    final_files = ["docs/history-manifest.json"]
    grouped = collections.defaultdict(list)
    for path in paths:
        if path not in first_files + final_files:
            grouped[year_for(path)].append(path)
    rng = random.Random(20132017)
    commits = [{"date": "2013-01-07T10:17:00+00:00", "files": first_files,
                "subject": "document source project and simulated history"}]
    for year in range(2013, 2018):
        for path, stamp in zip(grouped[year], dates_for(year, len(grouped[year]), rng)):
            commits.append({"date": stamp, "files": [path],
                            "subject": "add " + path})
    commits.append({"date": "2017-12-29T15:43:00+00:00", "files": final_files,
                    "subject": "record source-history schedule and weekly distribution"})
    weekly = collections.Counter()
    for commit in commits:
        date = dt.datetime.fromisoformat(commit["date"]).date()
        monday = date - dt.timedelta(days=date.weekday())
        weekly[monday.isoformat()] += 1
    weeks = []
    date = dt.date(2012, 12, 31)
    while date <= dt.date(2017, 12, 31):
        weeks.append({"week_start": date.isoformat(), "commits": weekly[date.isoformat()]})
        date += dt.timedelta(days=7)
    manifest = {
        "disclosure": "Synthetic source-assembly dates; not contemporaneous development history.",
        "actual_assembly_period": "October 2026",
        "reconstruction_recorded_date": "2026-10-12",
        "author": "Joalbert Guillermo",
        "email": "joalbert.guillermo33@outlook.com",
        "commit_count": len(commits),
        "active_weeks": sum(row["commits"] > 0 for row in weeks),
        "inactive_weeks": sum(row["commits"] == 0 for row in weeks),
        "maximum_commits_per_week": max(row["commits"] for row in weeks),
        "weekly_distribution": weeks,
        "commits": commits,
    }
    (ROOT / "docs/history-manifest.json").write_bytes((json.dumps(manifest, indent=2) + "\n").encode("utf-8"))
    git("init", "-b", "main")
    git("config", "user.name", manifest["author"])
    git("config", "user.email", manifest["email"])
    git("config", "core.autocrlf", "false")
    git("config", "commit.gpgsign", "false")
    git("config", "core.hooksPath", ".git/no-hooks")
    (METADATA / "no-hooks").mkdir(exist_ok=True)
    parent_commit = None
    entries = {}
    tree_cache = {}
    for index, commit in enumerate(commits):
        for path in commit["files"]:
            mode = "100755" if path.endswith(".sh") else "100644"
            oid = write_object("blob", (ROOT / path).read_bytes())
            entries[path] = (mode, oid)
        environment = dict(os.environ)
        environment["GIT_AUTHOR_DATE"] = commit["date"]
        environment["GIT_COMMITTER_DATE"] = commit["date"]
        tree = make_tree(entries, tree_cache)
        lines = ["tree " + tree]
        if parent_commit is not None:
            lines.append("parent " + parent_commit)
        timestamp = int(dt.datetime.fromisoformat(commit["date"]).timestamp())
        identity = manifest["author"] + " <" + manifest["email"] + "> " + str(timestamp) + " +0000"
        lines.extend(["author " + identity, "committer " + identity, "", commit["subject"], ""])
        parent_commit = write_object("commit", "\n".join(lines).encode("utf-8"))
        if (index + 1) % 25 == 0:
            print("Created {0}/{1} reconstructed commits".format(index + 1, len(commits)), flush=True)
    (METADATA / "refs/heads/main").write_bytes((parent_commit + "\n").encode("ascii"))
    write_index(entries)
    shutil.copytree(str(METADATA), str(ROOT / ".git"), dirs_exist_ok=True, copy_function=copy_metadata)
    print(json.dumps({key: manifest[key] for key in
                      ("commit_count", "active_weeks", "inactive_weeks", "maximum_commits_per_week")}, indent=2))


if __name__ == "__main__":
    main()
