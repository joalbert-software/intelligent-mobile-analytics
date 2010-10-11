from __future__ import absolute_import, division

import hashlib
import math
from collections import defaultdict

from .ordering import sorted_unique, users
from .statistics import wilson
from .validation import identifier, integer


def assignment(application_id, experiment_id, user_id, variants):
    for field, value in (("applicationId", application_id), ("experimentId", experiment_id), ("userId", user_id)):
        identifier(value, field)
    if not variants or len(variants) > 20:
        raise ValueError("experiment requires 1 to 20 variants")
    if len(set(name for name, unused in variants)) != len(variants):
        raise ValueError("duplicate variant name")
    total = 0
    for name, weight in variants:
        identifier(name, "variant")
        integer(weight, "weight", 1, 1000000)
        total += weight
    digest = hashlib.sha256()
    for component in (application_id, experiment_id, user_id):
        encoded = component.encode("utf-8")
        digest.update(str(len(encoded)).encode("ascii") + b":" + encoded)
    slot = int(digest.hexdigest()[:16], 16) % total
    cumulative = 0
    for name, weight in variants:
        cumulative += weight
        if slot < cumulative:
            return name
    raise AssertionError("assignment slot was not covered")


def outcomes(events, experiment_id, conversion_event, window):
    identifier(experiment_id, "experimentId")
    identifier(conversion_event, "conversionEvent")
    integer(window, "window", 1)
    totals = defaultdict(lambda: {"exposedUsers": 0, "convertedUsers": 0})
    contaminated = defaultdict(int)
    for key, grouped in users(sorted_unique(events)):
        exposure = None
        variant = None
        converted = False
        conflict = False
        for event in grouped:
            if event["name"] == "experiment_exposure" and event["properties"].get("experiment") == experiment_id:
                name = event["properties"].get("variant")
                identifier(name, "variant")
                if variant is not None and variant != name:
                    conflict = True
                if exposure is None:
                    exposure = event["timestamp"]
                    variant = name
                continue
            if exposure is not None and event["name"] == conversion_event:
                if 0 <= event["timestamp"] - exposure <= window:
                    converted = True
        if exposure is None:
            continue
        if conflict:
            contaminated[key[0]] += 1
            continue
        row = totals[(key[0], variant)]
        row["exposedUsers"] += 1
        row["convertedUsers"] += int(converted)
    result = []
    for key, row in sorted(totals.items()):
        row["interval"] = wilson(row["convertedUsers"], row["exposedUsers"])
        row.update({"applicationId": key[0], "variant": key[1], "experimentId": experiment_id,
                    "excludedContaminatedUsers": contaminated[key[0]]})
        result.append(row)
    return result


def compare(control_successes, control_trials, treatment_successes, treatment_trials):
    control = wilson(control_successes, control_trials)
    treatment = wilson(treatment_successes, treatment_trials)
    if not control_trials or not treatment_trials:
        return {"absoluteLift": None, "relativeLift": None, "zScore": None, "pValue": None}
    difference = treatment["rate"] - control["rate"]
    pooled = (control_successes + treatment_successes) / (control_trials + treatment_trials)
    variance = pooled * (1 - pooled) * (1 / control_trials + 1 / treatment_trials)
    if variance == 0:
        score = 0.0 if difference == 0 else None
        p_value = 1.0 if difference == 0 else None
    else:
        score = difference / math.sqrt(variance)
        p_value = math.erfc(abs(score) / math.sqrt(2))
    return {
        "control": control,
        "treatment": treatment,
        "absoluteLift": difference,
        "relativeLift": difference / control["rate"] if control["rate"] else None,
        "zScore": score,
        "pValue": p_value,
        "method": "two-sided pooled normal approximation; no sequential or multiplicity adjustment",
    }
