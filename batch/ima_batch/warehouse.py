from __future__ import absolute_import, division

import argparse
import os
import sys
import tempfile
import uuid

from .checkpoint import FileLock
from .compat import open_text, stdout_text
from .ordering import external_unique
from .pipeline import read_events
from .serialization import atomic_json, dumps, file_digest, loads, read_json_lines
from .timeutil import iso_date
from .validation import event

FIELDS = {
    "applicationId": "application_id",
    "eventId": "event_id",
    "userId": "user_id",
    "name": "event_name",
    "timestamp": "event_timestamp",
    "receivedAt": "received_at",
    "platform": "platform",
    "appVersion": "app_version",
    "properties": "properties",
}


def to_warehouse(value):
    normalized = event(value)
    return dict((target, normalized[source]) for source, target in FIELDS.items())


def from_warehouse(value):
    if not isinstance(value, dict):
        raise ValueError("warehouse row must be an object")
    source = dict((original, value.get(warehouse)) for original, warehouse in FIELDS.items())
    return event(source)


class PartitionWriter(object):
    def __init__(self, root, maximum_open=32):
        if maximum_open < 1:
            raise ValueError("open partition limit must be positive")
        self.root = os.path.abspath(root)
        self.maximum_open = maximum_open
        self.streams = {}
        self.counts = {}
        self.paths = {}
        self.sequence = []

    def _stream(self, date):
        if date in self.streams:
            self.sequence.remove(date)
            self.sequence.append(date)
            return self.streams[date]
        if len(self.streams) >= self.maximum_open:
            oldest = self.sequence.pop(0)
            self.streams.pop(oldest).close()
        directory = os.path.join(self.root, "event_date=" + date)
        if not os.path.isdir(directory):
            os.makedirs(directory)
        path = os.path.join(directory, "part-00000.jsonl")
        stream = open_text(path, "a")
        self.paths[date] = path
        self.streams[date] = stream
        self.sequence.append(date)
        return stream

    def add(self, value):
        row = to_warehouse(value)
        date = iso_date(row["event_timestamp"])
        self._stream(date).write(dumps(row) + u"\n")
        self.counts[date] = self.counts.get(date, 0) + 1

    def close(self):
        for stream in self.streams.values():
            stream.flush()
            os.fsync(stream.fileno())
            stream.close()
        self.streams.clear()
        self.sequence = []

    def manifest(self):
        if self.streams:
            raise ValueError("close partition streams before creating the manifest")
        return [{"date": date, "rows": self.counts[date],
                 "relativePath": os.path.relpath(path, self.root).replace(os.sep, "/"),
                 "sha256": file_digest(path)} for date, path in sorted(self.paths.items())]


def publish(input_path, output_directory, chunk_size=50000):
    output_directory = os.path.abspath(output_directory)
    if os.path.exists(output_directory):
        raise ValueError("warehouse output directory already exists")
    parent = os.path.dirname(output_directory)
    with FileLock(output_directory + ".lock"):
        temporary = tempfile.mkdtemp(prefix=".ima-warehouse-", dir=parent)
        writer = PartitionWriter(temporary)
        try:
            events = external_unique(read_events(input_path), chunk_size, parent)
            for value in events:
                writer.add(value)
            writer.close()
            manifest = {"formatVersion": 1, "runId": str(uuid.uuid4()),
                        "inputSha256": file_digest(input_path), "partitions": writer.manifest()}
            atomic_json(os.path.join(temporary, "_manifest.json"), manifest)
            os.rename(temporary, output_directory)
            return manifest
        finally:
            writer.close()
            if os.path.exists(temporary):
                import shutil
                shutil.rmtree(temporary)


def verify(directory):
    root = os.path.realpath(directory)
    with open_text(os.path.join(root, "_manifest.json")) as stream:
        manifest = loads(stream.read())
    if manifest.get("formatVersion") != 1:
        raise ValueError("unsupported warehouse manifest version")
    results = []
    for partition in manifest["partitions"]:
        path = os.path.realpath(os.path.join(root, partition["relativePath"]))
        if not path.startswith(root + os.sep):
            raise ValueError("partition path escapes warehouse root")
        digest_matches = os.path.isfile(path) and file_digest(path) == partition["sha256"]
        rows = 0
        if digest_matches:
            with open_text(path) as stream:
                for unused, value in read_json_lines(stream):
                    normalized = from_warehouse(value)
                    if iso_date(normalized["timestamp"]) != partition["date"]:
                        raise ValueError("row belongs to another date partition")
                    rows += 1
        results.append({"date": partition["date"], "passed": digest_matches and rows == partition["rows"],
                        "rows": rows})
    return {"passed": all(row["passed"] for row in results), "partitions": results}


def main(arguments=None):
    parser = argparse.ArgumentParser(description="Partition event JSONL for the Hive external table")
    commands = parser.add_subparsers(dest="command")
    create = commands.add_parser("publish")
    create.add_argument("input")
    create.add_argument("output")
    check = commands.add_parser("verify")
    check.add_argument("directory")
    options = parser.parse_args(arguments)
    if options.command == "publish":
        result = publish(options.input, options.output)
    elif options.command == "verify":
        result = verify(options.directory)
    else:
        parser.print_help()
        return 2
    stdout_text().write(dumps(result) + u"\n")
    return 0 if result.get("passed", True) else 1


if __name__ == "__main__":
    sys.exit(main())
