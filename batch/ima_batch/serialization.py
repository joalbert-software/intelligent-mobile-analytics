from __future__ import absolute_import, division

import csv
import hashlib
import io
import json
import os
import tempfile

from .compat import as_text, open_text, text_type
from .errors import ValidationError


def reject_constant(value):
    raise ValueError("non-finite JSON number")


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate JSON object key")
        result[key] = value
    return result


def loads(value):
    return json.loads(value, parse_constant=reject_constant, object_pairs_hook=unique_object)


def dumps(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"),
                      ensure_ascii=True, allow_nan=False)


def read_json_lines(stream, maximum_line_bytes=1048576):
    line_number = 0
    while True:
        raw = stream.readline(maximum_line_bytes + 1)
        if not raw:
            return
        line_number += 1
        if len(raw) > maximum_line_bytes:
            raise ValidationError("line", "line {0} exceeds size limit".format(line_number))
        try:
            line = as_text(raw)
            if len(line.encode("utf-8")) > maximum_line_bytes:
                raise ValueError("oversized UTF-8 line")
            if not line.strip():
                continue
            value = loads(line)
        except (ValueError, UnicodeError):
            raise ValidationError("line", "invalid JSON at line {0}".format(line_number))
        yield line_number, value


def write_json_lines(stream, rows):
    count = 0
    for row in rows:
        stream.write(text_type(dumps(row)) + u"\n")
        count += 1
    return count


def atomic_json(path, value):
    parent = os.path.dirname(os.path.abspath(path))
    descriptor, temporary = tempfile.mkstemp(prefix=".ima-", dir=parent)
    try:
        with os.fdopen(descriptor, "wb") as stream:
            payload = (dumps(value) + "\n").encode("utf-8")
            stream.write(payload)
            stream.flush()
            os.fsync(stream.fileno())
        if hasattr(os, "replace"):
            os.replace(temporary, path)
        else:
            # POSIX rename replaces atomically; the legacy batch target is Linux.
            os.rename(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def file_digest(path, block_size=1048576):
    digest = hashlib.sha256()
    with open(path, "rb") as stream:
        while True:
            block = stream.read(block_size)
            if not block:
                break
            digest.update(block)
    return digest.hexdigest()


def csv_cell(value):
    if value is None:
        return ""
    result = text_type(value)
    if result.startswith(("=", "+", "-", "@", "\t", "\r")):
        result = "'" + result
    return result


def csv_line(values):
    escaped = []
    for value in values:
        cell = csv_cell(value)
        if any(char in cell for char in (",", '"', "\r", "\n")):
            cell = '"' + cell.replace('"', '""') + '"'
        escaped.append(cell)
    return ",".join(escaped) + "\r\n"


def write_csv(stream, columns, rows):
    if len(set(columns)) != len(columns):
        raise ValueError("duplicate CSV column")
    stream.write(csv_line(columns))
    count = 0
    for row in rows:
        stream.write(csv_line([row.get(column) for column in columns]))
        count += 1
    return count
