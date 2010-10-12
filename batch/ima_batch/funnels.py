from __future__ import absolute_import, division

from collections import defaultdict

from .ordering import sorted_unique, users
from .validation import identifier, integer


def validate_steps(steps, window):
    if not isinstance(steps, (list, tuple)) or not 2 <= len(steps) <= 20:
        raise ValueError("funnel requires 2 to 20 steps")
    for step in steps:
        identifier(step, "step")
    integer(window, "window", 1)


def user_progress(events, steps, window):
    latest = [None] * len(steps)
    best = -1
    first_completion = None
    for event in events:
        timestamp = event["timestamp"]
        for index in range(len(steps) - 1, 0, -1):
            if event["name"] != steps[index] or latest[index - 1] is None:
                continue
            start = latest[index - 1]
            if timestamp - start > window:
                continue
            latest[index] = start if latest[index] is None else max(start, latest[index])
            best = max(best, index)
            if index == len(steps) - 1 and first_completion is None:
                first_completion = timestamp - start
        if event["name"] == steps[0]:
            latest[0] = timestamp
            best = max(best, 0)
    return best, first_completion


def calculate(events, steps, window):
    validate_steps(steps, window)
    counts = defaultdict(lambda: [0] * len(steps))
    durations = defaultdict(list)
    for key, grouped in users(sorted_unique(events)):
        best, duration = user_progress(grouped, steps, window)
        application = key[0]
        application_counts = counts[application]
        for index in range(best + 1):
            application_counts[index] += 1
        if duration is not None:
            durations[application].append(duration)
    result = []
    for application, values in sorted(counts.items()):
        completed = durations[application]
        result.append({
            "applicationId": application,
            "steps": list(steps),
            "users": values,
            "conversionRate": values[-1] / values[0] if values[0] else 0.0,
            "meanTimeToConvert": sum(completed) / len(completed) if completed else None,
            "windowMillis": window,
        })
    return result


def dropoffs(funnel):
    counts = funnel["users"]
    steps = funnel["steps"]
    result = []
    for index in range(1, len(steps)):
        entering = counts[index - 1]
        progressing = counts[index]
        result.append({
            "from": steps[index - 1],
            "to": steps[index],
            "enteringUsers": entering,
            "progressingUsers": progressing,
            "lostUsers": entering - progressing,
            "stepConversionRate": progressing / entering if entering else None,
        })
    return result
