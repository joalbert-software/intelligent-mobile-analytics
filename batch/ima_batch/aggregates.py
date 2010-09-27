from __future__ import absolute_import, division

from collections import defaultdict

from .ordering import unique, user_key
from .statistics import describe
from .timeutil import DAY, bucket, buckets, day
from .validation import integer, revenue_properties, time_range


def series(events, start, end, width=DAY):
    points = dict((timestamp, {"events": 0, "users": set()})
                  for timestamp in buckets(start, end, width))
    applications = {}
    for event in unique(events):
        if not start <= event["timestamp"] < end:
            continue
        app = event["applicationId"]
        if app not in applications:
            applications[app] = dict((timestamp, {"events": 0, "users": set()}) for timestamp in points)
        point = applications[app][bucket(event["timestamp"], width)]
        point["events"] += 1
        point["users"].add(event["userId"])
    result = []
    for app, application_points in sorted(applications.items()):
        for timestamp, point in sorted(application_points.items()):
            result.append({"applicationId": app, "timestamp": timestamp,
                           "events": point["events"], "users": len(point["users"])})
    return result


def dimension_value(event, dimension):
    if dimension == "eventName":
        return event["name"]
    if dimension in ("platform", "appVersion"):
        return event[dimension]
    if dimension.startswith("property:") and len(dimension) > 9:
        return event["properties"].get(dimension[9:])
    raise ValueError("unsupported dimension")


def breakdown(events, dimension, limit=20):
    integer(limit, "limit", 1, 1000)
    counts = defaultdict(int)
    active = defaultdict(set)
    for event in unique(events):
        key = (event["applicationId"], dimension_value(event, dimension))
        counts[key] += 1
        active[key].add(event["userId"])
    applications = defaultdict(list)
    for key, count in counts.items():
        app, value = key
        applications[app].append({"applicationId": app, "value": value,
                                  "missing": value is None, "events": count, "users": len(active[key])})
    result = []
    for app, rows in sorted(applications.items()):
        rows.sort(key=lambda row: (-row["events"], row["value"] is not None, row["value"] or ""))
        result.extend(rows[:limit])
    return result


def revenue(events):
    totals = {}
    for event in unique(events):
        if event["name"] not in ("purchase", "refund"):
            continue
        amount, currency = revenue_properties(event["properties"])
        key = (event["applicationId"], currency)
        if key not in totals:
            totals[key] = {"grossMinor": 0, "refundMinor": 0, "purchases": 0,
                           "refunds": 0, "purchasers": set()}
        total = totals[key]
        if event["name"] == "purchase":
            total["grossMinor"] += amount
            total["purchases"] += 1
            total["purchasers"].add(event["userId"])
        else:
            total["refundMinor"] += amount
            total["refunds"] += 1
    result = []
    for key, total in sorted(totals.items()):
        total["applicationId"], total["currency"] = key
        total["purchasers"] = len(total["purchasers"])
        total["netMinor"] = total["grossMinor"] - total["refundMinor"]
        total["averageOrderMinor"] = total["grossMinor"] / total["purchases"] if total["purchases"] else None
        result.append(total)
    return result


def engagement(events, start, end):
    time_range(start, end)
    active = defaultdict(set)
    for event in unique(events):
        if start <= event["timestamp"] < end:
            active[user_key(event)].add(day(event["timestamp"]))
    histogram = defaultdict(int)
    for key, days in active.items():
        histogram[(key[0], len(days))] += 1
    return [{"applicationId": key[0], "activeDays": key[1], "users": count}
            for key, count in sorted(histogram.items())]


def latency(events, property_name="duration_ms"):
    measurements = defaultdict(list)
    rejected = defaultdict(int)
    for event in unique(events):
        if property_name not in event["properties"]:
            continue
        key = (event["applicationId"], event["name"], event["platform"])
        raw = event["properties"][property_name]
        try:
            value = float(raw)
            if value < 0 or value > 86400000:
                raise ValueError("duration outside allowed range")
            from .statistics import finite
            measurements[key].append(finite(value))
        except (ValueError, TypeError):
            rejected[key] += 1
    result = []
    for key in sorted(set(measurements) | set(rejected)):
        row = describe(measurements[key])
        row.update({"applicationId": key[0], "eventName": key[1], "platform": key[2],
                    "invalidMeasurements": rejected[key]})
        result.append(row)
    return result


def crash_rates(sessions):
    values = {}
    for session in sessions:
        key = (session["applicationId"], session["appVersion"], session["platform"])
        if key not in values:
            values[key] = {"sessions": 0, "users": set(), "affectedUsers": set(),
                           "affectedSessions": 0, "crashes": 0}
        row = values[key]
        row["sessions"] += 1
        row["users"].add(session["userId"])
        crashes = sum(1 for event in session["events"] if event["name"] == "crash")
        row["crashes"] += crashes
        if crashes:
            row["affectedSessions"] += 1
            row["affectedUsers"].add(session["userId"])
    result = []
    for key, row in sorted(values.items()):
        row["users"] = len(row["users"])
        row["affectedUsers"] = len(row["affectedUsers"])
        row["crashFreeUserRate"] = 1 - row["affectedUsers"] / row["users"]
        row["crashFreeSessionRate"] = 1 - row["affectedSessions"] / row["sessions"]
        row.update({"applicationId": key[0], "appVersion": key[1], "platform": key[2]})
        result.append(row)
    return result
