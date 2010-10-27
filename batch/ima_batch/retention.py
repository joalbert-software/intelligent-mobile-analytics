from __future__ import absolute_import, division

from collections import defaultdict

from .ordering import sorted_unique, users
from .timeutil import DAY, complete_offsets, day
from .validation import integer


def calculate(events, observed_until, maximum_day=30, mode="exact"):
    integer(observed_until, "observedUntil", 1)
    integer(maximum_day, "maximumDay", 0, 365)
    if mode not in ("exact", "rolling"):
        raise ValueError("retention mode must be exact or rolling")
    observed = (item for item in events if item["timestamp"] < observed_until)
    cohorts = defaultdict(list)
    for key, grouped in users(sorted_unique(observed)):
        active = sorted(set(day(item["timestamp"]) for item in grouped))
        first = active[0]
        offsets = set((value - first) // DAY for value in active)
        cohorts[(key[0], first)].append(offsets)
    result = []
    for key, members in sorted(cohorts.items()):
        app, cohort = key
        retained = {}
        rates = {}
        for offset in complete_offsets(cohort, observed_until, maximum_day):
            if mode == "exact":
                count = sum(1 for active in members if offset in active)
            else:
                count = sum(1 for active in members if max(active) >= offset)
            retained[str(offset)] = count
            rates[str(offset)] = count / len(members)
        result.append({
            "applicationId": app,
            "cohortDay": cohort,
            "cohortUsers": len(members),
            "retained": retained,
            "rates": rates,
            "mode": mode,
            "observedUntil": observed_until,
        })
    return result


def weighted_curve(rows):
    totals = defaultdict(lambda: [0, 0])
    for row in rows:
        for offset, count in row["retained"].items():
            totals[(row["applicationId"], int(offset))][0] += count
            totals[(row["applicationId"], int(offset))][1] += row["cohortUsers"]
    result = []
    for key, value in sorted(totals.items()):
        retained, eligible = value
        result.append({
            "applicationId": key[0],
            "offset": key[1],
            "retainedUsers": retained,
            "eligibleUsers": eligible,
            "rate": retained / eligible if eligible else None,
        })
    return result


def lifecycle(events, start, end, lookback_days=30):
    from .validation import time_range
    time_range(start, end)
    integer(lookback_days, "lookbackDays", 1, 365)
    histories = defaultdict(list)
    for key, grouped in users(sorted_unique(events)):
        timestamps = [item["timestamp"] for item in grouped if item["timestamp"] < end]
        if not timestamps:
            continue
        current = any(start <= timestamp < end for timestamp in timestamps)
        previous = any(max(0, start - lookback_days * DAY) <= timestamp < start for timestamp in timestamps)
        if timestamps[0] >= start:
            state = "new"
        elif current and previous:
            state = "retained"
        elif current:
            state = "resurrected"
        elif previous:
            state = "dormant"
        else:
            state = "inactive"
        histories[key[0]].append(state)
    result = []
    for app, states in sorted(histories.items()):
        row = {"applicationId": app, "start": start, "end": end}
        for state in ("new", "retained", "resurrected", "dormant", "inactive"):
            row[state] = states.count(state)
        result.append(row)
    return result
