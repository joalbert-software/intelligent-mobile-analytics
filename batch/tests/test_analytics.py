from __future__ import absolute_import, division

import unittest
from ima_batch import attribution, funnels, retention, sessions
from ima_batch.ordering import unique
from ima_batch.errors import ConflictingEventError
from ima_batch.timeutil import DAY


def event(identity, timestamp, name="open", app="app"):
    return {"applicationId": app, "eventId": identity, "userId": "user",
            "name": name, "timestamp": timestamp, "receivedAt": timestamp,
            "platform": "ANDROID", "appVersion": "1.0", "properties": {}}


class AnalyticsTests(unittest.TestCase):
    def test_reception_time_does_not_create_conflict(self):
        first = event("one", 1)
        self.assertEqual(1, len(list(unique([first, dict(first, receivedAt=2)]))))

    def test_conflicting_duplicate_is_rejected(self):
        with self.assertRaises(ConflictingEventError):
            list(unique([event("one", 1), event("one", 2)]))

    def test_applications_have_separate_sessions(self):
        self.assertEqual(2, len(sessions.calculate([event("one", 1), event("one", 1, app="other")])))

    def test_session_gap_is_strict(self):
        self.assertEqual(1, len(sessions.calculate([event("a", 0), event("b", 100)], gap=100)))
        self.assertEqual(2, len(sessions.calculate([event("a", 0), event("b", 101)], gap=100)))

    def test_repeated_steps_require_distinct_events(self):
        self.assertEqual([1, 0], funnels.calculate([event("a", 1, "tap")], ["tap", "tap"], 100)[0]["users"])

    def test_retention_omits_unobserved_offsets(self):
        result = retention.calculate([event("a", 1), event("b", DAY + 1)], 2 * DAY, 3)
        self.assertEqual({"0": 1, "1": 1}, result[0]["retained"])

    def test_attribution_conserves_large_integer_amounts(self):
        amount = 999999999999999999
        allocations = attribution.distribute_minor_units(amount, [("a", 0.1), ("b", 0.2), ("c", 0.7)])
        self.assertEqual(amount, sum(allocations.values()))
        self.assertTrue(all(value >= 0 for value in allocations.values()))


if __name__ == "__main__":
    unittest.main()
