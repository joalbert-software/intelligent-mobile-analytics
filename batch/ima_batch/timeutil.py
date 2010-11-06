from __future__ import absolute_import, division

import calendar
import datetime
import re

from .validation import integer, time_range

MINUTE = 60000
HOUR = 60 * MINUTE
DAY = 24 * HOUR
EPOCH = datetime.datetime(1970, 1, 1)


def bucket(timestamp, width=DAY):
    integer(timestamp, "timestamp")
    integer(width, "width", 1)
    return timestamp - timestamp % width


def day(timestamp):
    return bucket(timestamp, DAY)


def iso_date(timestamp):
    integer(timestamp, "timestamp")
    return (EPOCH + datetime.timedelta(milliseconds=timestamp)).strftime("%Y-%m-%d")


def parse_date(value):
    if not re.match(r"\A\d{4}-\d{2}-\d{2}\Z", value):
        raise ValueError("date must use YYYY-MM-DD")
    parsed = datetime.datetime.strptime(value, "%Y-%m-%d")
    result = calendar.timegm(parsed.timetuple()) * 1000
    integer(result, "date")
    return result


def parse_timestamp(value):
    match = re.match(r"\A(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,3}))?Z\Z", value)
    if not match:
        raise ValueError("timestamp must be an ISO UTC timestamp ending in Z")
    parsed = datetime.datetime.strptime(match.group(1), "%Y-%m-%dT%H:%M:%S")
    millis = int((match.group(2) or "0").ljust(3, "0"))
    result = calendar.timegm(parsed.timetuple()) * 1000 + millis
    integer(result, "timestamp")
    return result


def format_timestamp(timestamp):
    integer(timestamp, "timestamp")
    parsed = EPOCH + datetime.timedelta(milliseconds=timestamp)
    return parsed.strftime("%Y-%m-%dT%H:%M:%S") + ".{0:03d}Z".format(timestamp % 1000)


def buckets(start, end, width=DAY, maximum=10000):
    time_range(start, end)
    integer(width, "width", 1)
    first = bucket(start, width)
    last = bucket(end - 1, width)
    count = (last - first) // width + 1
    if count > maximum:
        raise ValueError("too many time buckets")
    for index in range(count):
        yield first + index * width


def month_start(timestamp):
    integer(timestamp, "timestamp")
    parsed = EPOCH + datetime.timedelta(milliseconds=timestamp)
    return parse_date("{0:04d}-{1:02d}-01".format(parsed.year, parsed.month))


def week_start(timestamp):
    parsed = EPOCH + datetime.timedelta(milliseconds=timestamp)
    result = day(timestamp) - parsed.weekday() * DAY
    return result


def complete_offsets(cohort_day, observed_until, maximum_day):
    integer(maximum_day, "maximumDay", 0, 365)
    complete = max(0, (observed_until - cohort_day) // DAY)
    return range(min(maximum_day + 1, complete))
