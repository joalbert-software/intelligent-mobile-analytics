from __future__ import absolute_import, division

import math

from .statistics import Moments, finite, percentile
from .validation import integer


def z_score(baseline, actual, minimum_samples=7, threshold=3.0):
    integer(minimum_samples, "minimumSamples", 2, 1000000)
    threshold = finite(threshold)
    actual = finite(actual)
    if threshold <= 0:
        raise ValueError("threshold must be positive")
    moments = Moments()
    for value in baseline:
        moments.add(value)
    result = {"actual": actual, "expected": moments.mean if moments.count else None,
              "score": None, "anomalous": False, "baselineCount": moments.count}
    if moments.count < minimum_samples:
        result["reason"] = "insufficient_history"
        return result
    deviation = math.sqrt(moments.variance(sample=True))
    if deviation == 0:
        result["score"] = 0.0 if actual == moments.mean else None
        result["anomalous"] = actual != moments.mean
        result["reason"] = "constant_baseline"
        return result
    result["score"] = (actual - moments.mean) / deviation
    result["anomalous"] = abs(result["score"]) > threshold
    result["reason"] = "evaluated"
    return result


def robust_score(baseline, actual, minimum_samples=7, threshold=3.5):
    values = [finite(value) for value in baseline]
    actual = finite(actual)
    integer(minimum_samples, "minimumSamples", 2, 1000000)
    threshold = finite(threshold)
    if threshold <= 0:
        raise ValueError("threshold must be positive")
    center = percentile(values, 0.5)
    result = {"actual": actual, "expected": center, "score": None,
              "anomalous": False, "baselineCount": len(values)}
    if len(values) < minimum_samples:
        result["reason"] = "insufficient_history"
        return result
    deviation = percentile([abs(value - center) for value in values], 0.5)
    if deviation == 0:
        result["score"] = 0.0 if actual == center else None
        result["anomalous"] = actual != center
        result["reason"] = "zero_median_deviation"
        return result
    result["score"] = 0.67448975 * (actual - center) / deviation
    result["anomalous"] = abs(result["score"]) > threshold
    result["reason"] = "evaluated"
    return result


def moving_average(values, window):
    integer(window, "window", 1, 100000)
    history = []
    total = 0.0
    for value in values:
        value = finite(value)
        history.append(value)
        total += value
        if len(history) > window:
            total -= history.pop(0)
        yield total / len(history)


def exponential_average(values, alpha=0.2):
    alpha = finite(alpha)
    if not 0 < alpha <= 1:
        raise ValueError("alpha must be in (0, 1]")
    estimate = None
    for value in values:
        value = finite(value)
        estimate = value if estimate is None else alpha * value + (1 - alpha) * estimate
        yield estimate


def detect_series(points, history_size=28, minimum_samples=7, threshold=3.5):
    integer(history_size, "historySize", minimum_samples, 100000)
    history = []
    previous = None
    for timestamp, value in points:
        integer(timestamp, "timestamp")
        if previous is not None and timestamp <= previous:
            raise ValueError("series timestamps must strictly increase")
        previous = timestamp
        result = robust_score(history, value, minimum_samples, threshold)
        result["timestamp"] = timestamp
        yield result
        history.append(finite(value))
        if len(history) > history_size:
            history.pop(0)


def seasonal_baseline(points, period, minimum_cycles=3):
    integer(period, "period", 1, 10000)
    integer(minimum_cycles, "minimumCycles", 1, 1000)
    slots = [[] for unused in range(period)]
    for index, value in enumerate(points):
        slots[index % period].append(finite(value))
    return [percentile(slot, 0.5) if len(slot) >= minimum_cycles else None for slot in slots]
