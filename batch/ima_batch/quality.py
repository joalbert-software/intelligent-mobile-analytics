from __future__ import absolute_import, division

from collections import defaultdict

from .errors import ValidationError
from .ordering import event_key, payload
from .statistics import Moments
from .validation import event


class QualityReport(object):
    def __init__(self, maximum_identities=1000000):
        if maximum_identities < 1:
            raise ValueError("identity capacity must be positive")
        self.maximum_identities = maximum_identities
        self.rows = 0
        self.valid = 0
        self.invalid = 0
        self.duplicates = 0
        self.conflicts = 0
        self.errors = defaultdict(int)
        self.applications = defaultdict(int)
        self.platforms = defaultdict(int)
        self.identities = {}
        self.lag = Moments()
        self.first_timestamp = None
        self.last_timestamp = None
        self.negative_lag = 0

    def add(self, raw):
        self.rows += 1
        try:
            value = event(raw)
        except ValidationError as failure:
            self.invalid += 1
            self.errors[failure.field] += 1
            return None
        self.valid += 1
        key = event_key(value)
        current = payload(value)
        if key in self.identities:
            if self.identities[key] == current:
                self.duplicates += 1
            else:
                self.conflicts += 1
            return value
        if len(self.identities) >= self.maximum_identities:
            raise ValueError("quality identity capacity exceeded")
        self.identities[key] = current
        self.applications[value["applicationId"]] += 1
        self.platforms[value["platform"]] += 1
        lag = value["receivedAt"] - value["timestamp"]
        self.lag.add(lag)
        self.negative_lag += int(lag < 0)
        timestamp = value["timestamp"]
        self.first_timestamp = timestamp if self.first_timestamp is None else min(timestamp, self.first_timestamp)
        self.last_timestamp = timestamp if self.last_timestamp is None else max(timestamp, self.last_timestamp)
        return value

    def result(self):
        return {
            "rows": self.rows,
            "validRows": self.valid,
            "invalidRows": self.invalid,
            "uniqueEvents": len(self.identities),
            "duplicateRows": self.duplicates,
            "conflictingRows": self.conflicts,
            "invalidRate": self.invalid / self.rows if self.rows else None,
            "errorsByField": dict(self.errors),
            "eventsByApplication": dict(self.applications),
            "eventsByPlatform": dict(self.platforms),
            "firstTimestamp": self.first_timestamp,
            "lastTimestamp": self.last_timestamp,
            "ingestionLagMillis": self.lag.result(),
            "negativeLagEvents": self.negative_lag,
        }


def evaluate(rows, maximum_invalid_rate=0.01, allow_conflicts=False):
    if not 0 <= maximum_invalid_rate <= 1:
        raise ValueError("maximum invalid rate must be in [0, 1]")
    report = QualityReport()
    for row in rows:
        report.add(row)
    result = report.result()
    failures = []
    if not report.rows:
        failures.append("empty_input")
    if report.rows and report.invalid / report.rows > maximum_invalid_rate:
        failures.append("invalid_rate_exceeded")
    if report.conflicts and not allow_conflicts:
        failures.append("conflicting_event_identities")
    result["passed"] = not failures
    result["failures"] = failures
    return result
