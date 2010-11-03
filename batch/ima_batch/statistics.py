from __future__ import absolute_import, division

import math
import random


def finite(value):
    if isinstance(value, bool):
        raise ValueError("boolean is not a numeric observation")
    result = float(value)
    if math.isnan(result) or math.isinf(result):
        raise ValueError("observation must be finite")
    return result


class Moments(object):
    def __init__(self):
        self.count = 0
        self.mean = 0.0
        self.m2 = 0.0
        self.minimum = None
        self.maximum = None
        self.total = 0.0
        self.correction = 0.0

    def add(self, value):
        value = finite(value)
        self.count += 1
        delta = value - self.mean
        self.mean += delta / self.count
        self.m2 += delta * (value - self.mean)
        self.minimum = value if self.minimum is None else min(value, self.minimum)
        self.maximum = value if self.maximum is None else max(value, self.maximum)
        adjusted = value - self.correction
        following = self.total + adjusted
        self.correction = (following - self.total) - adjusted
        self.total = following
        return self

    def merge(self, other):
        if other.count == 0:
            return self
        if self.count == 0:
            self.count = other.count
            self.mean = other.mean
            self.m2 = other.m2
            self.minimum = other.minimum
            self.maximum = other.maximum
            self.total = other.total
            self.correction = other.correction
            return self
        count = self.count + other.count
        delta = other.mean - self.mean
        self.m2 += other.m2 + delta * delta * self.count * other.count / count
        self.mean += delta * other.count / count
        self.count = count
        self.minimum = min(self.minimum, other.minimum)
        self.maximum = max(self.maximum, other.maximum)
        self.total += other.total
        self.correction = 0.0
        return self

    def variance(self, sample=False):
        denominator = self.count - (1 if sample else 0)
        return max(0.0, self.m2 / denominator) if denominator > 0 else None

    def result(self):
        variance = self.variance()
        return {
            "count": self.count,
            "sum": self.total,
            "mean": self.mean if self.count else None,
            "minimum": self.minimum,
            "maximum": self.maximum,
            "populationVariance": variance,
            "sampleVariance": self.variance(sample=True),
            "standardDeviation": math.sqrt(variance) if variance is not None else None,
        }


def percentile(values, fraction):
    fraction = finite(fraction)
    if not 0 <= fraction <= 1:
        raise ValueError("fraction must be between zero and one")
    ordered = sorted(finite(value) for value in values)
    if not ordered:
        return None
    position = fraction * (len(ordered) - 1)
    lower = int(math.floor(position))
    upper = int(math.ceil(position))
    weight = position - lower
    return ordered[lower] * (1 - weight) + ordered[upper] * weight


def describe(values):
    numbers = list(finite(value) for value in values)
    moments = Moments()
    for number in numbers:
        moments.add(number)
    result = moments.result()
    for label, fraction in (("p50", 0.5), ("p75", 0.75), ("p90", 0.9), ("p95", 0.95), ("p99", 0.99)):
        result[label] = percentile(numbers, fraction)
    return result


def weighted_mean(values):
    numerator = 0.0
    denominator = 0.0
    for value, weight in values:
        value = finite(value)
        weight = finite(weight)
        if weight < 0:
            raise ValueError("weight must not be negative")
        numerator += value * weight
        denominator += weight
    return numerator / denominator if denominator else None


def pearson(pairs):
    points = [(finite(x), finite(y)) for x, y in pairs]
    if len(points) < 2:
        return None
    mean_x = sum(x for x, unused in points) / len(points)
    mean_y = sum(y for unused, y in points) / len(points)
    numerator = sum((x - mean_x) * (y - mean_y) for x, y in points)
    xx = sum((x - mean_x) ** 2 for x, unused in points)
    yy = sum((y - mean_y) ** 2 for unused, y in points)
    if xx == 0 or yy == 0:
        return None
    return max(-1.0, min(1.0, numerator / math.sqrt(xx * yy)))


def wilson(successes, trials, z=1.959963984540054):
    if not isinstance(successes, int) or not isinstance(trials, int):
        raise ValueError("binomial counts must be integers")
    if trials < 0 or successes < 0 or successes > trials:
        raise ValueError("invalid binomial counts")
    z = finite(z)
    if z <= 0:
        raise ValueError("z must be positive")
    if trials == 0:
        return {"rate": None, "lower": None, "upper": None}
    rate = successes / trials
    denominator = 1 + z * z / trials
    center = (rate + z * z / (2 * trials)) / denominator
    radius = z * math.sqrt(rate * (1 - rate) / trials + z * z / (4 * trials * trials)) / denominator
    return {"rate": rate, "lower": max(0.0, center - radius), "upper": min(1.0, center + radius)}


class Reservoir(object):
    def __init__(self, capacity, seed=0):
        if capacity < 1:
            raise ValueError("capacity must be positive")
        self.capacity = capacity
        self.seen = 0
        self.values = []
        self.random = random.Random(seed)

    def add(self, value):
        self.seen += 1
        if len(self.values) < self.capacity:
            self.values.append(value)
        else:
            index = self.random.randrange(self.seen)
            if index < self.capacity:
                self.values[index] = value

    def summary(self):
        result = describe(self.values)
        result["populationCount"] = self.seen
        result["sampled"] = self.seen > self.capacity
        return result
