from __future__ import absolute_import, division

from collections import defaultdict

from .ordering import assert_sorted, sorted_unique, user_key
from .timeutil import MINUTE
from .validation import integer


def stream_sessions(events, gap=30 * MINUTE, maximum_events=100000):
    integer(gap, "gap", 1)
    integer(maximum_events, "maximumEvents", 1)
    current = []
    current_user = None
    previous_time = None
    for event in assert_sorted(events):
        key = user_key(event)
        timestamp = event["timestamp"]
        boundary = current and (key != current_user or timestamp - previous_time > gap)
        if boundary:
            yield make_session(current)
            current = []
        if len(current) >= maximum_events:
            raise ValueError("session exceeds maximum event count")
        current.append(event)
        current_user = key
        previous_time = timestamp
    if current:
        yield make_session(current)


def make_session(events):
    if not events:
        raise ValueError("cannot construct an empty session")
    first = events[0]
    last = events[-1]
    return {
        "applicationId": first["applicationId"],
        "userId": first["userId"],
        "sessionId": first["eventId"],
        "start": first["timestamp"],
        "end": last["timestamp"],
        "duration": last["timestamp"] - first["timestamp"],
        "eventCount": len(events),
        "bounce": len(events) == 1,
        "platform": first["platform"],
        "appVersion": first["appVersion"],
        "events": list(events),
    }


def calculate(events, gap=30 * MINUTE):
    return list(stream_sessions(sorted_unique(events), gap))


def summaries(sessions):
    groups = {}
    for session in sessions:
        app = session["applicationId"]
        if app not in groups:
            groups[app] = {
                "sessions": 0,
                "users": set(),
                "bounces": 0,
                "events": 0,
                "totalDuration": 0,
                "minimumDuration": None,
                "maximumDuration": None,
            }
        summary = groups[app]
        summary["sessions"] += 1
        summary["users"].add(session["userId"])
        summary["bounces"] += int(session["bounce"])
        summary["events"] += session["eventCount"]
        summary["totalDuration"] += session["duration"]
        minimum = summary["minimumDuration"]
        maximum = summary["maximumDuration"]
        summary["minimumDuration"] = session["duration"] if minimum is None else min(minimum, session["duration"])
        summary["maximumDuration"] = session["duration"] if maximum is None else max(maximum, session["duration"])
    result = []
    for app, summary in sorted(groups.items()):
        summary["applicationId"] = app
        summary["users"] = len(summary["users"])
        summary["bounceRate"] = summary["bounces"] / summary["sessions"]
        summary["meanDuration"] = summary["totalDuration"] / summary["sessions"]
        summary["sessionsPerUser"] = summary["sessions"] / summary["users"]
        result.append(summary)
    return result


def paths(sessions, collapse_repeats=False):
    counts = defaultdict(int)
    for session in sessions:
        previous = None
        for event in session["events"]:
            name = event["name"]
            if previous is not None and (not collapse_repeats or previous != name):
                counts[(session["applicationId"], previous, name)] += 1
            previous = name
    return [
        {"applicationId": key[0], "from": key[1], "to": key[2], "transitions": count}
        for key, count in sorted(counts.items())
    ]


def entry_exit(sessions):
    counts = defaultdict(lambda: {"entries": 0, "exits": 0})
    for session in sessions:
        app = session["applicationId"]
        first = session["events"][0]["name"]
        last = session["events"][-1]["name"]
        counts[(app, first)]["entries"] += 1
        counts[(app, last)]["exits"] += 1
    result = []
    for key, counts_for_name in sorted(counts.items()):
        row = dict(counts_for_name)
        row.update({"applicationId": key[0], "eventName": key[1]})
        result.append(row)
    return result
