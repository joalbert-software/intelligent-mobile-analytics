from __future__ import absolute_import, division, print_function

import argparse
import os
import sys

from .compat import open_text, stdout_text
from .errors import AnalyticsError
from .pipeline import atomic_report, normalized_events, read_events
from .quality import evaluate
from .serialization import dumps, loads, read_json_lines, write_json_lines

OPERATIONS = (
    "sessions", "session-summary", "funnel", "retention", "revenue", "series",
    "breakdown", "attribution", "latency", "crashes", "paths", "engagement",
)


def parser():
    result = argparse.ArgumentParser(description="Intelligent Mobile Analytics offline tools")
    result.add_argument("--version", action="version", version="IMA 1.0.0")
    commands = result.add_subparsers(dest="command")
    report = commands.add_parser("report", help="write a checkpointed JSONL report")
    report.add_argument("operation", choices=OPERATIONS)
    report.add_argument("input")
    report.add_argument("output")
    report.add_argument("--options", help="JSON object with operation parameters", default="{}")
    report.add_argument("--checkpoint")
    quality = commands.add_parser("quality", help="inspect JSONL validation and duplicates")
    quality.add_argument("input")
    quality.add_argument("--maximum-invalid-rate", type=float, default=0.01)
    quality.add_argument("--allow-conflicts", action="store_true")
    normalize = commands.add_parser("normalize", help="external deduplication and chronological user sorting")
    normalize.add_argument("input")
    normalize.add_argument("output")
    normalize.add_argument("--chunk-size", type=int, default=50000)
    normalize.add_argument("--temporary-directory")
    validate = commands.add_parser("validate", help="validate without creating an output dataset")
    validate.add_argument("input")
    return result


def main(arguments=None):
    argument_parser = parser()
    options = argument_parser.parse_args(arguments)
    if options.command is None:
        argument_parser.print_help()
        return 2
    try:
        if options.command == "report":
            parameters = loads(options.options)
            if not isinstance(parameters, dict):
                raise ValueError("options must be a JSON object")
            state = atomic_report(options.input, options.output, options.operation, parameters, options.checkpoint)
            stdout_text().write(dumps({"state": state["state"], "rows": state["processedRows"]}) + u"\n")
        elif options.command == "quality":
            with open_text(options.input) as stream:
                rows = (value for unused, value in read_json_lines(stream))
                report = evaluate(rows, options.maximum_invalid_rate, options.allow_conflicts)
            stdout_text().write(dumps(report) + u"\n")
            return 0 if report["passed"] else 1
        elif options.command == "normalize":
            if os.path.realpath(options.input) == os.path.realpath(options.output):
                raise ValueError("normalization output must not overwrite input")
            if os.path.exists(options.output):
                raise ValueError("normalization output already exists")
            from .checkpoint import FileLock
            with FileLock(options.output + ".lock"):
                temporary = options.output + ".partial"
                if os.path.exists(temporary):
                    raise ValueError("partial output exists; inspect it before retrying")
                try:
                    with open_text(temporary, "w") as stream:
                        count = write_json_lines(stream, normalized_events(options.input, options.chunk_size,
                                                                          options.temporary_directory))
                        stream.flush()
                        os.fsync(stream.fileno())
                    os.rename(temporary, options.output)
                except Exception:
                    if os.path.exists(temporary):
                        os.unlink(temporary)
                    raise
            stdout_text().write(dumps({"uniqueEvents": count}) + u"\n")
        elif options.command == "validate":
            count = sum(1 for unused in read_events(options.input))
            stdout_text().write(dumps({"validEvents": count}) + u"\n")
    except (AnalyticsError, ValueError, KeyError, IOError) as failure:
        sys.stderr.write("IMA command failed: {0}\n".format(str(failure)))
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
