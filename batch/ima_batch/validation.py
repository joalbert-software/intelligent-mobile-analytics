from __future__ import absolute_import, division

import re
import unicodedata

from .compat import is_integer, string_types
from .errors import ValidationError

IDENTIFIER = re.compile(r"\A[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}\Z")
MAX_TIMESTAMP = 253402300799999
MAX_AMOUNT = 9223372036854775807


def identifier(value, field):
    if not isinstance(value, string_types) or not IDENTIFIER.match(value):
        raise ValidationError(field, "expected an opaque identifier of 1 to 128 characters")
    return value


def integer(value, field, minimum=0, maximum=MAX_TIMESTAMP):
    if not is_integer(value):
        raise ValidationError(field, "expected an integer")
    if value < minimum or value > maximum:
        raise ValidationError(field, "value is outside the allowed range")
    return value


def text(value, field, maximum=512, allow_empty=False):
    if not isinstance(value, string_types):
        raise ValidationError(field, "expected text")
    if len(value) > maximum or (not allow_empty and not value.strip()):
        raise ValidationError(field, "invalid text length")
    if any(unicodedata.category(char) == "Cc" for char in value):
        raise ValidationError(field, "control characters are not allowed")
    return value


def time_range(start, end):
    integer(start, "start")
    integer(end, "end")
    if end <= start:
        raise ValidationError("end", "must be after start")
    return start, end


def properties(value):
    if not isinstance(value, dict):
        raise ValidationError("properties", "expected an object")
    if len(value) > 32:
        raise ValidationError("properties", "at most 32 properties are allowed")
    result = {}
    for key, item in value.items():
        identifier(key, "property key")
        if "." in key:
            raise ValidationError("property key", "dots are not allowed")
        if not isinstance(item, string_types) or len(item) > 512:
            raise ValidationError("property value", "expected at most 512 characters")
        result[key] = item
    return result


def event(value, default_application=None):
    if not isinstance(value, dict):
        raise ValidationError("event", "expected an object")
    allowed = set(("applicationId", "eventId", "userId", "name", "timestamp",
                   "receivedAt", "platform", "appVersion", "properties"))
    if set(value) - allowed:
        raise ValidationError("event", "unknown fields")
    application = value.get("applicationId", default_application)
    if default_application is not None and application != default_application:
        raise ValidationError("applicationId", "event belongs to another application")
    platform = value.get("platform")
    if not isinstance(platform, string_types) or platform.upper() not in (
            "ANDROID", "IOS", "WEB", "OTHER"):
        raise ValidationError("platform", "unsupported platform")
    timestamp = integer(value.get("timestamp"), "timestamp")
    result = {
        "applicationId": identifier(application, "applicationId"),
        "eventId": identifier(value.get("eventId"), "eventId"),
        "userId": identifier(value.get("userId"), "userId"),
        "name": identifier(value.get("name"), "name"),
        "timestamp": timestamp,
        "receivedAt": integer(value.get("receivedAt", timestamp), "receivedAt"),
        "platform": platform.upper(),
        "appVersion": text(value.get("appVersion"), "appVersion", 64),
        "properties": properties(value.get("properties", {})),
    }
    if result["name"] in ("purchase", "refund"):
        revenue_properties(result["properties"])
    return result


def revenue_properties(value):
    amount = value.get("amount_minor")
    currency = value.get("currency")
    if not isinstance(amount, string_types) or not re.match(r"\A[0-9]{1,18}\Z", amount):
        raise ValidationError("amount_minor", "expected a nonnegative integer string")
    if not isinstance(currency, string_types) or not re.match(r"\A[A-Z]{3}\Z", currency):
        raise ValidationError("currency", "expected a three-letter uppercase currency code")
    return int(amount), currency


def require_application(events, application_id):
    identifier(application_id, "applicationId")
    for item in events:
        if item["applicationId"] != application_id:
            raise ValidationError("applicationId", "mixed applications in report input")
        yield item
