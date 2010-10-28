from __future__ import absolute_import, division

import re

from .aggregates import dimension_value
from .compat import string_types
from .ordering import sorted_unique, users
from .validation import identifier, integer

OPERATORS = set(("eq", "ne", "in", "not_in", "exists", "prefix", "gt", "gte", "lt", "lte"))


def validate_predicate(predicate):
    if not isinstance(predicate, dict):
        raise ValueError("predicate must be an object")
    if set(predicate) - set(("field", "operator", "value")):
        raise ValueError("unknown predicate fields")
    field = predicate.get("field")
    if not isinstance(field, string_types):
        raise ValueError("predicate field is required")
    if field not in ("eventName", "platform", "appVersion"):
        if not field.startswith("property:"):
            raise ValueError("unknown predicate field")
        identifier(field[9:], "property")
    operator = predicate.get("operator")
    if operator not in OPERATORS:
        raise ValueError("unknown predicate operator")
    value = predicate.get("value")
    if operator == "exists":
        if not isinstance(value, bool):
            raise ValueError("exists requires a boolean")
    elif operator in ("in", "not_in"):
        if not isinstance(value, list) or not 1 <= len(value) <= 100:
            raise ValueError("membership requires 1 to 100 values")
        if not all(isinstance(item, string_types) for item in value):
            raise ValueError("membership values must be strings")
    elif not isinstance(value, string_types):
        raise ValueError("predicate value must be a string")
    return predicate


def matches(event, predicate):
    value = dimension_value(event, predicate["field"])
    expected = predicate.get("value")
    operator = predicate["operator"]
    if operator == "exists":
        return (value is not None) == expected
    if value is None:
        return False
    if operator == "eq":
        return value == expected
    if operator == "ne":
        return value != expected
    if operator == "in":
        return value in expected
    if operator == "not_in":
        return value not in expected
    if operator == "prefix":
        return value.startswith(expected)
    try:
        from .statistics import finite
        actual_number = finite(value)
        expected_number = finite(expected)
    except (ValueError, TypeError):
        return False
    if operator == "gt":
        return actual_number > expected_number
    if operator == "gte":
        return actual_number >= expected_number
    if operator == "lt":
        return actual_number < expected_number
    if operator == "lte":
        return actual_number <= expected_number
    raise ValueError("unknown predicate operator")


def select_events(events, predicates, mode="all"):
    if mode not in ("all", "any"):
        raise ValueError("predicate mode must be all or any")
    if not isinstance(predicates, list) or not 1 <= len(predicates) <= 20:
        raise ValueError("segment requires 1 to 20 predicates")
    rules = [validate_predicate(rule) for rule in predicates]
    combine = all if mode == "all" else any
    for event in events:
        if combine(matches(event, rule) for rule in rules):
            yield event


def select_users(events, predicates, minimum_events=1, maximum_events=None, mode="all"):
    integer(minimum_events, "minimumEvents", 1, 1000000)
    if maximum_events is not None:
        integer(maximum_events, "maximumEvents", minimum_events, 1000000)
    rules = [validate_predicate(rule) for rule in predicates]
    if not 1 <= len(rules) <= 20 or mode not in ("all", "any"):
        raise ValueError("invalid segment definition")
    for key, grouped in users(sorted_unique(events)):
        count = sum(1 for unused in select_events(grouped, rules, mode))
        if count >= minimum_events and (maximum_events is None or count <= maximum_events):
            yield {"applicationId": key[0], "userId": key[1], "matchingEvents": count}


def compare_populations(first, second):
    first = set((row["applicationId"], row["userId"]) for row in first)
    second = set((row["applicationId"], row["userId"]) for row in second)
    overlap = first & second
    union = first | second
    return {
        "firstUsers": len(first),
        "secondUsers": len(second),
        "overlapUsers": len(overlap),
        "onlyFirst": len(first - second),
        "onlySecond": len(second - first),
        "jaccard": len(overlap) / len(union) if union else None,
    }
