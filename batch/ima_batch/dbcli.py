from __future__ import absolute_import, division, print_function

import argparse
import sys

from .compat import open_text, stdout_text
from .mysql import AggregateLoader, MigrationRunner, connect
from .serialization import dumps, read_json_lines


def main(arguments=None):
    parser = argparse.ArgumentParser(description="MySQL metadata migrations and aggregate loading")
    commands = parser.add_subparsers(dest="command")
    migrate = commands.add_parser("migrate")
    migrate.add_argument("directory")
    load = commands.add_parser("load")
    load.add_argument("kind", choices=sorted(AggregateLoader.TABLES))
    load.add_argument("application_id")
    load.add_argument("job_id")
    load.add_argument("input")
    options = parser.parse_args(arguments)
    if options.command is None:
        parser.print_help()
        return 2
    connection = connect()
    try:
        if options.command == "migrate":
            result = {"applied": MigrationRunner(connection, options.directory).apply()}
        else:
            with open_text(options.input) as stream:
                rows = (value for unused, value in read_json_lines(stream))
                count = AggregateLoader(connection).upsert(options.kind, options.application_id, options.job_id, rows)
            result = {"loadedRows": count}
        stdout_text().write(dumps(result) + u"\n")
    finally:
        connection.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
