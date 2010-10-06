from __future__ import absolute_import, division, print_function

import io
import sys

try:
    text_type = unicode
    integer_types = (int, long)
except NameError:
    text_type = str
    integer_types = (int,)

string_types = (text_type, str)


def is_integer(value):
    return isinstance(value, integer_types) and not isinstance(value, bool)


def open_text(path, mode="r"):
    return io.open(path, mode, encoding="utf-8", newline="")


def as_text(value):
    if isinstance(value, bytes):
        return value.decode("utf-8")
    if isinstance(value, text_type):
        return value
    raise TypeError("expected text or UTF-8 bytes")


def write_text(stream, value):
    value = as_text(value)
    try:
        stream.write(value)
    except UnicodeEncodeError:
        stream.write(value.encode("utf-8"))


def iteritems(mapping):
    return iter(mapping.items())


def stdout_text():
    if sys.version_info[0] >= 3:
        return sys.stdout
    return io.open(sys.stdout.fileno(), "w", encoding="utf-8", closefd=False)
