from __future__ import absolute_import, division

import math
from collections import defaultdict

from .ordering import sorted_unique, users
from .timeutil import DAY
from .validation import integer, revenue_properties


def touchpoints(events, conversion_time, lookback):
    return [event for event in events if event["name"] == "campaign_touch"
            and 0 <= conversion_time - event["timestamp"] <= lookback
            and event["properties"].get("campaign")]


def weights(touches, conversion_time, model, half_life):
    if not touches:
        return [("organic", 1.0)]
    if model == "first":
        return [(touches[0]["properties"]["campaign"], 1.0)]
    if model == "last":
        return [(touches[-1]["properties"]["campaign"], 1.0)]
    if model == "linear":
        raw = [1.0] * len(touches)
    elif model == "time_decay":
        latest = touches[-1]["timestamp"]
        raw = [math.exp(-math.log(2) * (latest - touch["timestamp"]) / half_life) for touch in touches]
    elif model == "position":
        if len(touches) <= 2:
            raw = [1.0] * len(touches)
        else:
            raw = [0.4] + [0.2 / (len(touches) - 2)] * (len(touches) - 2) + [0.4]
    else:
        raise ValueError("unknown attribution model")
    total = sum(raw)
    return [(touch["properties"]["campaign"], weight / total)
            for touch, weight in zip(touches, raw)]


def distribute_minor_units(amount, campaign_weights):
    from decimal import Decimal, localcontext, ROUND_FLOOR
    with localcontext() as context:
        context.prec = 60
        combined = defaultdict(Decimal)
        for campaign, weight in campaign_weights:
            number = Decimal(str(weight))
            if not number.is_finite() or number < 0:
                raise ValueError("attribution weights must be finite and nonnegative")
            combined[campaign] += number
        total = sum(combined.values())
        if total <= 0:
            raise ValueError("attribution weights require positive total weight")
        allocations = {}
        remainders = []
        for campaign, weight in combined.items():
            raw = Decimal(amount) * weight / total
            allocation = int(raw.to_integral_value(rounding=ROUND_FLOOR))
            allocations[campaign] = allocation
            remainders.append((-(raw - allocation), campaign))
        remaining = amount - sum(allocations.values())
        ordered = sorted(remainders)
        for index in range(remaining):
            allocations[ordered[index % len(ordered)][1]] += 1
        return allocations


def calculate(events, model="last", lookback=30 * DAY, half_life=7 * DAY):
    integer(lookback, "lookback", 1)
    integer(half_life, "halfLife", 1)
    if model not in ("first", "last", "linear", "time_decay", "position"):
        raise ValueError("unknown attribution model")
    totals = defaultdict(lambda: {"conversionCredit": 0.0, "revenueMinor": 0})
    for key, grouped in users(sorted_unique(events)):
        history = []
        for event in grouped:
            history = [touch for touch in history if event["timestamp"] - touch["timestamp"] <= lookback]
            if event["name"] == "campaign_touch":
                history.append(event)
            if event["name"] != "purchase":
                continue
            amount, currency = revenue_properties(event["properties"])
            eligible = touchpoints(history, event["timestamp"], lookback)
            credits = weights(eligible, event["timestamp"], model, half_life)
            money = distribute_minor_units(amount, credits)
            for campaign, weight in credits:
                totals[(key[0], campaign, currency)]["conversionCredit"] += weight
            for campaign, allocation in money.items():
                totals[(key[0], campaign, currency)]["revenueMinor"] += allocation
    result = []
    for key, row in sorted(totals.items()):
        row.update({"applicationId": key[0], "campaign": key[1], "currency": key[2], "model": model})
        result.append(row)
    return result
