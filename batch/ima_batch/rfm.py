from __future__ import absolute_import, division

from collections import defaultdict

from .ordering import sorted_unique, users
from .timeutil import DAY
from .validation import integer, revenue_properties


def customer_values(events, observed_until):
    integer(observed_until, "observedUntil", 1)
    result = []
    for key, grouped in users(sorted_unique(events)):
        currencies = {}
        first_seen = None
        for event in grouped:
            if event["timestamp"] >= observed_until:
                continue
            first_seen = event["timestamp"] if first_seen is None else min(first_seen, event["timestamp"])
            if event["name"] not in ("purchase", "refund"):
                continue
            amount, currency = revenue_properties(event["properties"])
            if currency not in currencies:
                currencies[currency] = {"purchases": 0, "refunds": 0, "grossMinor": 0,
                                        "refundMinor": 0, "firstPurchase": None, "lastPurchase": None}
            row = currencies[currency]
            if event["name"] == "purchase":
                row["purchases"] += 1
                row["grossMinor"] += amount
                row["firstPurchase"] = event["timestamp"] if row["firstPurchase"] is None else min(row["firstPurchase"], event["timestamp"])
                row["lastPurchase"] = event["timestamp"]
            else:
                row["refunds"] += 1
                row["refundMinor"] += amount
        for currency, row in sorted(currencies.items()):
            row.update({"applicationId": key[0], "userId": key[1], "currency": currency,
                        "firstSeen": first_seen, "netMinor": row["grossMinor"] - row["refundMinor"]})
            row["recencyDays"] = ((observed_until - row["lastPurchase"]) // DAY
                                  if row["lastPurchase"] is not None else None)
            row["averageOrderMinor"] = row["grossMinor"] / row["purchases"] if row["purchases"] else None
            result.append(row)
    return result


def rank_scores(values, higher_is_better=True, buckets=5):
    integer(buckets, "buckets", 2, 10)
    distinct = sorted(set(values))
    if not distinct:
        return []
    if len(distinct) == 1:
        return [(buckets + 1) // 2] * len(values)
    ranks = dict((value, index) for index, value in enumerate(distinct))
    result = []
    for value in values:
        fraction = ranks[value] / (len(distinct) - 1)
        score = min(buckets, int(fraction * buckets) + 1)
        result.append(score if higher_is_better else buckets + 1 - score)
    return result


def score(customers, buckets=5):
    groups = defaultdict(list)
    for row in customers:
        if row["purchases"] > 0:
            groups[(row["applicationId"], row["currency"])].append(dict(row))
    result = []
    for unused, group in sorted(groups.items()):
        recency = rank_scores([row["recencyDays"] for row in group], False, buckets)
        frequency = rank_scores([row["purchases"] for row in group], True, buckets)
        monetary = rank_scores([row["netMinor"] for row in group], True, buckets)
        for row, r, f, m in zip(group, recency, frequency, monetary):
            row.update({"recencyScore": r, "frequencyScore": f, "monetaryScore": m,
                        "rfmScore": r + f + m, "scoreBuckets": buckets})
            result.append(row)
    return result


def lifetime_cohorts(customers):
    grouped = defaultdict(lambda: {"customers": 0, "netMinor": 0, "purchases": 0})
    for row in customers:
        cohort = row["firstSeen"] - row["firstSeen"] % DAY
        key = (row["applicationId"], row["currency"], cohort)
        total = grouped[key]
        total["customers"] += 1
        total["netMinor"] += row["netMinor"]
        total["purchases"] += row["purchases"]
    result = []
    for key, row in sorted(grouped.items()):
        row.update({"applicationId": key[0], "currency": key[1], "cohortDay": key[2],
                    "observedValuePerPurchaserMinor": row["netMinor"] / row["customers"]})
        result.append(row)
    return result
