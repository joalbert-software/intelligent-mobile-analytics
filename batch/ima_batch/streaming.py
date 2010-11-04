from __future__ import absolute_import, division, print_function

import argparse
import itertools
import sys

from .compat import as_text, stdout_text
from .errors import UnsortedInputError
from .serialization import dumps, loads, read_json_lines
from .sessions import stream_sessions
from .timeutil import DAY, day
from .validation import event, integer


def counter(stream, name, amount=1):
    if not name.replace("_", "").isalnum():
        raise ValueError("invalid Hadoop counter name")
    stream.write("reporter:counter:IMA,{0},{1}\n".format(name, amount))


def encode_key(parts):
    encoded = []
    for part in parts:
        value = str(part)
        if "\t" in value or "\n" in value or "\r" in value:
            raise ValueError("invalid streaming key")
        encoded.append(value)
    return "\t".join(encoded)


def emit(stream, parts, value):
    stream.write(encode_key(parts) + "\t" + dumps(value) + u"\n")


def map_deduplicate(rows, output, diagnostics):
    for unused, raw in read_json_lines(rows):
        try:
            value = event(raw)
        except ValueError:
            counter(diagnostics, "invalid_events")
            raise
        emit(output, (value["applicationId"], value["eventId"]), value)
        counter(diagnostics, "mapped_events")


def map_sessions(rows, output, diagnostics):
    for unused, raw in read_json_lines(rows):
        value = event(raw)
        parts = (value["applicationId"], value["userId"],
                 "{0:015d}".format(value["timestamp"]), value["eventId"])
        emit(output, parts, value)
        counter(diagnostics, "mapped_events")


def map_daily(rows, output, diagnostics):
    for unused, raw in read_json_lines(rows):
        value = event(raw)
        parts = (value["applicationId"], "{0:015d}".format(day(value["timestamp"])), value["userId"])
        emit(output, parts, {"events": 1})
        counter(diagnostics, "mapped_events")


def keyed_rows(stream, key_fields):
    previous = None
    for line in stream:
        line = as_text(line)
        fields = line.rstrip("\n").split("\t", key_fields)
        if len(fields) != key_fields + 1:
            raise ValueError("malformed streaming row")
        key = tuple(fields[:key_fields])
        if previous is not None and key < previous:
            raise UnsortedInputError("Hadoop reducer input is not sorted")
        previous = key
        yield key, loads(fields[-1])


def reduce_deduplicate(rows, output, diagnostics):
    from .ordering import payload
    for key, grouped in itertools.groupby(keyed_rows(rows, 2), key=lambda row: row[0]):
        first = None
        first_payload = None
        for unused, raw in grouped:
            value = event(raw)
            if (value["applicationId"], value["eventId"]) != key:
                raise ValueError("deduplication key does not match payload")
            current = payload(value)
            if first is None:
                first = value
                first_payload = current
            elif current != first_payload:
                counter(diagnostics, "identity_conflicts")
                raise ValueError("conflicting duplicate event")
            else:
                counter(diagnostics, "duplicate_events")
        output.write(dumps(first) + u"\n")
        counter(diagnostics, "unique_events")


def reduce_sessions(rows, output, diagnostics, gap=1800000):
    def values():
        for key, raw in keyed_rows(rows, 4):
            value = event(raw)
            expected = (value["applicationId"], value["userId"],
                        "{0:015d}".format(value["timestamp"]), value["eventId"])
            if key != expected:
                raise ValueError("session key does not match payload")
            yield value
    for session in stream_sessions(values(), gap):
        del session["events"]
        output.write(dumps(session) + u"\n")
        counter(diagnostics, "sessions")


def reduce_daily(rows, output, diagnostics):
    grouped = itertools.groupby(keyed_rows(rows, 3), key=lambda row: row[0][:2])
    for key, daily_rows in grouped:
        total_events = 0
        total_users = 0
        previous_user = None
        for full_key, value in daily_rows:
            count = integer(value.get("events"), "events", 1, 9223372036854775807)
            total_events += count
            if full_key[2] != previous_user:
                total_users += 1
                previous_user = full_key[2]
        output.write(dumps({"applicationId": key[0], "day": int(key[1]),
                            "events": total_events, "users": total_users}) + u"\n")
        counter(diagnostics, "daily_rows")


def main(arguments=None):
    parser = argparse.ArgumentParser(description="Hadoop streaming transformations")
    parser.add_argument("operation", choices=("map-deduplicate", "reduce-deduplicate",
                                              "map-sessions", "reduce-sessions", "map-daily", "reduce-daily"))
    parser.add_argument("--gap", type=int, default=1800000)
    options = parser.parse_args(arguments)
    operations = {
        "map-deduplicate": map_deduplicate,
        "reduce-deduplicate": reduce_deduplicate,
        "map-sessions": map_sessions,
        "reduce-sessions": lambda rows, output, diagnostics: reduce_sessions(rows, output, diagnostics, options.gap),
        "map-daily": map_daily,
        "reduce-daily": reduce_daily,
    }
    try:
        operations[options.operation](sys.stdin, stdout_text(), sys.stderr)
    except (ValueError, KeyError) as failure:
        sys.stderr.write("streaming job rejected invalid input: {0}\n".format(type(failure).__name__))
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
