from __future__ import absolute_import, division

from .statistics import finite
from .validation import identifier, integer


class ThresholdRule(object):
    def __init__(self, rule_id, direction, threshold, minimum_samples=1,
                 consecutive=2, cooldown_millis=3600000):
        self.rule_id = identifier(rule_id, "ruleId")
        if direction not in ("above", "below"):
            raise ValueError("alert direction must be above or below")
        self.direction = direction
        self.threshold = finite(threshold)
        self.minimum_samples = integer(minimum_samples, "minimumSamples", 1, 1000000)
        self.consecutive = integer(consecutive, "consecutive", 1, 1000)
        self.cooldown_millis = integer(cooldown_millis, "cooldownMillis", 0)

    def breached(self, value):
        value = finite(value)
        return value > self.threshold if self.direction == "above" else value < self.threshold


class AlertState(object):
    def __init__(self, rule, application_id):
        self.rule = rule
        self.application_id = identifier(application_id, "applicationId")
        self.streak = 0
        self.firing = False
        self.last_timestamp = None
        self.last_notification = None
        self.sequence = 0

    def observe(self, timestamp, value, samples):
        integer(timestamp, "timestamp")
        integer(samples, "samples", 0, 9223372036854775807)
        value = finite(value)
        if self.last_timestamp is not None and timestamp <= self.last_timestamp:
            raise ValueError("alert observations must be strictly ordered")
        self.last_timestamp = timestamp
        if samples < self.rule.minimum_samples:
            self.streak = 0
            return None
        breached = self.rule.breached(value)
        if breached:
            self.streak += 1
        else:
            self.streak = 0
        transition = None
        if self.firing and not breached:
            self.firing = False
            transition = "resolved"
        elif not self.firing and self.streak >= self.rule.consecutive:
            self.firing = True
            transition = "firing"
        elif self.firing and self.last_notification is not None:
            if timestamp - self.last_notification >= self.rule.cooldown_millis:
                transition = "reminder"
        if transition is None:
            return None
        self.sequence += 1
        self.last_notification = timestamp
        return {
            "applicationId": self.application_id,
            "ruleId": self.rule.rule_id,
            "sequence": self.sequence,
            "timestamp": timestamp,
            "transition": transition,
            "value": value,
            "threshold": self.rule.threshold,
            "samples": samples,
        }

    def snapshot(self):
        return {"applicationId": self.application_id, "ruleId": self.rule.rule_id,
                "streak": self.streak, "firing": self.firing, "lastTimestamp": self.last_timestamp,
                "lastNotification": self.last_notification, "sequence": self.sequence}

    def restore(self, state):
        if state.get("applicationId") != self.application_id or state.get("ruleId") != self.rule.rule_id:
            raise ValueError("alert state identity mismatch")
        self.streak = integer(state.get("streak"), "streak", 0, 9223372036854775807)
        if not isinstance(state.get("firing"), bool):
            raise ValueError("firing flag must be boolean")
        self.firing = state["firing"]
        self.last_timestamp = state.get("lastTimestamp")
        self.last_notification = state.get("lastNotification")
        if self.last_timestamp is not None:
            integer(self.last_timestamp, "lastTimestamp")
        if self.last_notification is not None:
            integer(self.last_notification, "lastNotification")
            if self.last_timestamp is None or self.last_notification > self.last_timestamp:
                raise ValueError("notification occurs after the latest observation")
        self.sequence = integer(state.get("sequence"), "sequence", 0, 9223372036854775807)


def evaluate(rule, application_id, points):
    state = AlertState(rule, application_id)
    for point in points:
        notification = state.observe(point["timestamp"], point["value"], point["samples"])
        if notification is not None:
            yield notification
