from __future__ import absolute_import


class AnalyticsError(Exception):
    code = "analytics_error"


class ValidationError(AnalyticsError):
    code = "invalid_event"

    def __init__(self, field, message):
        self.field = field
        self.message = message
        super(ValidationError, self).__init__("{0}: {1}".format(field, message))


class ConflictingEventError(AnalyticsError):
    code = "event_identity_conflict"


class UnsortedInputError(AnalyticsError):
    code = "unsorted_input"


class CapacityError(AnalyticsError):
    code = "capacity_exceeded"


class CheckpointError(AnalyticsError):
    code = "checkpoint_error"


class TransportError(AnalyticsError):
    code = "transport_error"

    def __init__(self, message, retryable=False, retry_after=None):
        self.retryable = retryable
        self.retry_after = retry_after
        super(TransportError, self).__init__(message)
