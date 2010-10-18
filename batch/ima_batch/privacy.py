from __future__ import absolute_import, division

import hashlib
import hmac

from .compat import as_text
from .validation import identifier, integer


def pseudonym(identifier_value, secret, namespace="ima"):
    identifier(identifier_value, "identifier")
    identifier(namespace, "namespace")
    if len(secret) < 32:
        raise ValueError("pseudonymization secret must be at least 32 bytes")
    secret = secret.encode("utf-8") if not isinstance(secret, bytes) else secret
    message = (namespace + ":" + identifier_value).encode("utf-8")
    return hmac.new(secret, message, hashlib.sha256).hexdigest()


def redact_properties(events, allowed_properties):
    allowed = set(allowed_properties)
    for key in allowed:
        identifier(key, "allowed property")
    for event in events:
        result = dict(event)
        result["properties"] = dict((key, value) for key, value in event["properties"].items() if key in allowed)
        yield result


def remove_users(events, application_id, user_ids):
    identifier(application_id, "applicationId")
    blocked = set(user_ids)
    for user in blocked:
        identifier(user, "userId")
    for event in events:
        if event["applicationId"] == application_id and event["userId"] in blocked:
            continue
        yield event


def retain_after(events, cutoffs):
    for application, cutoff in cutoffs.items():
        identifier(application, "applicationId")
        integer(cutoff, "cutoff")
    for event in events:
        cutoff = cutoffs.get(event["applicationId"])
        if cutoff is None or event["timestamp"] >= cutoff:
            yield event


def pseudonymize(events, secret, namespace="ima"):
    for event in events:
        result = dict(event)
        scope = namespace + ":" + event["applicationId"]
        result["userId"] = pseudonym(event["userId"], secret, scope)
        result["properties"] = dict(event["properties"])
        yield result


def suppress_small_groups(rows, count_field="users", minimum=5):
    integer(minimum, "minimum", 1, 1000000)
    for row in rows:
        count = row.get(count_field)
        integer(count, count_field, 0, 9223372036854775807)
        if count >= minimum:
            yield dict(row)
