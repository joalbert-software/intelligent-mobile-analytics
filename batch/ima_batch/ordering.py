from __future__ import absolute_import

import heapq
import itertools
import os
import shutil
import tempfile

from .compat import open_text
from .errors import CapacityError, ConflictingEventError, UnsortedInputError
from .serialization import dumps, loads


def event_key(event):
    return event["applicationId"], event["eventId"]


def user_key(event):
    return event["applicationId"], event["userId"]


def time_key(event):
    return event["timestamp"], event["eventId"]


def session_key(event):
    return user_key(event) + time_key(event)


def payload(event):
    return dict((key, value) for key, value in event.items() if key != "receivedAt")


def unique(events, maximum=1000000):
    seen = {}
    for event in events:
        key = event_key(event)
        previous = seen.get(key)
        current = payload(event)
        if previous is not None:
            if previous != current:
                raise ConflictingEventError("an event identity has different payloads")
            continue
        if len(seen) >= maximum:
            raise CapacityError("deduplication capacity exceeded; use external deduplication")
        seen[key] = current
        yield event


def sorted_unique(events, maximum=1000000):
    return sorted(unique(events, maximum), key=session_key)


def assert_sorted(events, key=session_key):
    previous = None
    for event in events:
        current = key(event)
        if previous is not None and current < previous:
            raise UnsortedInputError("input is not ordered by the required key")
        previous = current
        yield event


def users(events):
    for key, grouped in itertools.groupby(assert_sorted(events), key=user_key):
        yield key, grouped


def _chunk_rows(path):
    with open_text(path) as stream:
        for line in stream:
            yield loads(line)


def _merge(paths, key):
    iterators = [iter(_chunk_rows(path)) for path in paths]
    heap = []
    for index, iterator in enumerate(iterators):
        try:
            value = next(iterator)
            heapq.heappush(heap, (key(value), index, value))
        except StopIteration:
            pass
    while heap:
        unused, index, value = heapq.heappop(heap)
        yield value
        try:
            following = next(iterators[index])
            heapq.heappush(heap, (key(following), index, following))
        except StopIteration:
            pass


def external_sort(events, key=session_key, chunk_size=50000, temporary_directory=None,
                  maximum_chunks=128):
    if chunk_size < 1 or maximum_chunks < 1:
        raise ValueError("sort limits must be positive")
    directory = tempfile.mkdtemp(prefix="ima-sort-", dir=temporary_directory)
    paths = []
    try:
        chunk = []
        for event in events:
            chunk.append(event)
            if len(chunk) >= chunk_size:
                paths.append(_write_chunk(directory, len(paths), chunk, key))
                chunk = []
                if len(paths) > maximum_chunks:
                    raise CapacityError("too many sort chunks; use Hadoop for larger input")
        if chunk:
            paths.append(_write_chunk(directory, len(paths), chunk, key))
        if len(paths) > maximum_chunks:
            raise CapacityError("too many sort chunks")
        for event in _merge(paths, key):
            yield event
    finally:
        shutil.rmtree(directory)


def _write_chunk(directory, index, events, key):
    path = os.path.join(directory, "chunk-{0:06d}.jsonl".format(index))
    with open_text(path, "w") as stream:
        for event in sorted(events, key=key):
            stream.write(dumps(event) + u"\n")
    return path


def external_unique(events, chunk_size=50000, temporary_directory=None):
    ordered = external_sort(events, key=event_key, chunk_size=chunk_size,
                            temporary_directory=temporary_directory)
    previous_key = None
    previous_payload = None
    for event in ordered:
        key = event_key(event)
        current = payload(event)
        if key == previous_key:
            if current != previous_payload:
                raise ConflictingEventError("an event identity has different payloads")
            continue
        previous_key = key
        previous_payload = current
        yield event
