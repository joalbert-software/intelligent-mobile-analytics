from __future__ import absolute_import, division, print_function

import argparse
import os
import sys

from .compat import open_text, stdout_text
from .pipeline import read_events
from .serialization import dumps
from .spool import DiskSpool
from .transport import Client
from .validation import identifier, integer


def batches(values, size):
    integer(size, "batchSize", 1, 500)
    current = []
    for value in values:
        current.append(value)
        if len(current) == size:
            yield current
            current = []
    if current:
        yield current


def client_from_environment(application_id, environment=None):
    environment = os.environ if environment is None else environment
    endpoint = environment.get("IMA_ENDPOINT")
    token = environment.get("IMA_API_KEY")
    if not endpoint or not token:
        raise ValueError("IMA_ENDPOINT and IMA_API_KEY must be set")
    return Client(endpoint, application_id, token)


def send_file(client, path, batch_size, outcomes_stream):
    totals = {"accepted": 0, "rejected": 0, "retryable": 0, "batches": 0}
    for group in batches(read_events(path, client.application_id), batch_size):
        outcomes = client.ingest(group)
        totals["batches"] += 1
        for outcome in outcomes:
            status = outcome["status"]
            if status in ("CREATED", "DUPLICATE"):
                totals["accepted"] += 1
            elif status == "UNAVAILABLE":
                totals["retryable"] += 1
            else:
                totals["rejected"] += 1
            outcomes_stream.write(dumps(outcome) + u"\n")
        outcomes_stream.flush()
    return totals


def enqueue_file(spool, path):
    count = 0
    for value in read_events(path, spool.application_id):
        spool.enqueue(value)
        count += 1
    result = spool.inventory()
    result["enqueued"] = count
    return result


def drain_spool(spool, client, batch_size, maximum_batches):
    integer(maximum_batches, "maximumBatches", 1, 10000)
    totals = {"accepted": 0, "rejected": 0, "retryable": 0, "batches": 0}
    for unused in range(maximum_batches):
        if spool.inventory()["pendingEvents"] == 0:
            break
        result = spool.drain(client, batch_size)
        totals["batches"] += 1
        for key in ("accepted", "rejected", "retryable"):
            totals[key] += result[key]
        if result["retryable"]:
            break
    totals["remaining"] = spool.inventory()["pendingEvents"]
    return totals


def argument_parser():
    parser = argparse.ArgumentParser(description="Deliver event files using stable client IDs")
    parser.add_argument("--application", required=True)
    commands = parser.add_subparsers(dest="command")
    send = commands.add_parser("send")
    send.add_argument("input")
    send.add_argument("outcomes")
    send.add_argument("--batch-size", type=int, default=100)
    enqueue = commands.add_parser("enqueue")
    enqueue.add_argument("input")
    enqueue.add_argument("spool")
    drain = commands.add_parser("drain")
    drain.add_argument("spool")
    drain.add_argument("--batch-size", type=int, default=100)
    drain.add_argument("--maximum-batches", type=int, default=10)
    inventory = commands.add_parser("inventory")
    inventory.add_argument("spool")
    return parser


def main(arguments=None):
    parser = argument_parser()
    options = parser.parse_args(arguments)
    if options.command is None:
        parser.print_help()
        return 2
    identifier(options.application, "application")
    if options.command == "send":
        if os.path.exists(options.outcomes):
            raise ValueError("outcomes file already exists")
        if os.path.realpath(options.input) == os.path.realpath(options.outcomes):
            raise ValueError("outcomes must not overwrite input")
        from .checkpoint import FileLock
        with FileLock(options.outcomes + ".lock"):
            client = client_from_environment(options.application)
            with open_text(options.outcomes, "w") as stream:
                result = send_file(client, options.input, options.batch_size, stream)
    elif options.command == "enqueue":
        result = enqueue_file(DiskSpool(options.spool, options.application), options.input)
    elif options.command == "drain":
        result = drain_spool(DiskSpool(options.spool, options.application),
                             client_from_environment(options.application),
                             options.batch_size, options.maximum_batches)
    else:
        result = DiskSpool(options.spool, options.application).inventory()
    stdout_text().write(dumps(result) + u"\n")
    return 1 if result.get("rejected", 0) or result.get("retryable", 0) else 0


if __name__ == "__main__":
    sys.exit(main())
