from __future__ import absolute_import, division

import os
import tempfile

from . import aggregates, attribution, funnels, retention, sessions
from .checkpoint import Checkpoint, FileLock
from .compat import open_text
from .ordering import external_sort, external_unique
from .serialization import dumps, read_json_lines
from .validation import event


def read_events(path, application_id=None):
    with open_text(path) as stream:
        for unused, value in read_json_lines(stream):
            yield event(value, application_id)


def compute(operation, events, options):
    if operation == "sessions":
        for row in sessions.calculate(events, options.get("gap", 1800000)):
            del row["events"]
            yield row
    elif operation == "session-summary":
        for row in sessions.summaries(sessions.calculate(events, options.get("gap", 1800000))):
            yield row
    elif operation == "funnel":
        for row in funnels.calculate(events, options["steps"], options["window"]):
            yield row
    elif operation == "retention":
        for row in retention.calculate(events, options["observedUntil"],
                                        options.get("maximumDay", 30), options.get("mode", "exact")):
            yield row
    elif operation == "revenue":
        for row in aggregates.revenue(events):
            yield row
    elif operation == "series":
        for row in aggregates.series(events, options["start"], options["end"], options.get("width", 86400000)):
            yield row
    elif operation == "breakdown":
        for row in aggregates.breakdown(events, options["dimension"], options.get("limit", 20)):
            yield row
    elif operation == "attribution":
        for row in attribution.calculate(events, options.get("model", "last"),
                                          options.get("lookback", 2592000000)):
            yield row
    elif operation == "latency":
        for row in aggregates.latency(events, options.get("property", "duration_ms")):
            yield row
    elif operation == "crashes":
        for row in aggregates.crash_rates(sessions.calculate(events, options.get("gap", 1800000))):
            yield row
    elif operation == "paths":
        for row in sessions.paths(sessions.calculate(events, options.get("gap", 1800000)),
                                   options.get("collapseRepeats", False)):
            yield row
    elif operation == "engagement":
        for row in aggregates.engagement(events, options["start"], options["end"]):
            yield row
    else:
        raise ValueError("unknown report operation")


def atomic_report(input_path, output_path, operation, options, checkpoint_path=None):
    source = os.path.realpath(input_path)
    destination = os.path.realpath(output_path)
    if source == destination:
        raise ValueError("output must not overwrite input")
    checkpoint_path = checkpoint_path or output_path + ".checkpoint.json"
    if os.path.realpath(checkpoint_path) in (source, destination):
        raise ValueError("checkpoint must be separate from input and output")
    with FileLock(checkpoint_path + ".lock"):
        checkpoint = Checkpoint(checkpoint_path, operation, input_path, options)
        checkpoint.load()
        if checkpoint.state["state"] == "completed":
            if checkpoint.verify_output() and checkpoint.state["outputPath"] == os.path.abspath(output_path):
                return checkpoint.state
            raise ValueError("completed output has changed; use a new checkpoint")
        checkpoint.start()
        parent = os.path.dirname(os.path.abspath(output_path))
        descriptor, temporary = tempfile.mkstemp(prefix=".ima-report-", dir=parent)
        os.close(descriptor)
        try:
            rows = 0
            with open_text(temporary, "w") as stream:
                for row in compute(operation, read_events(input_path), options):
                    stream.write(dumps(row) + u"\n")
                    rows += 1
                stream.flush()
                os.fsync(stream.fileno())
            checkpoint.progress(rows)
            if hasattr(os, "replace"):
                os.replace(temporary, output_path)
            else:
                os.rename(temporary, output_path)
            checkpoint.complete(output_path)
            return checkpoint.state
        except Exception as failure:
            checkpoint.fail(type(failure).__name__)
            raise
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)


def normalized_events(input_path, chunk_size=50000, temporary_directory=None):
    unique = external_unique(read_events(input_path), chunk_size, temporary_directory)
    for event_row in external_sort(unique, chunk_size=chunk_size, temporary_directory=temporary_directory):
        yield event_row
