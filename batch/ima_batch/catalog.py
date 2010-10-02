from __future__ import absolute_import, division

import decimal

from .compat import string_types
from .validation import identifier, integer

TYPES = set(("TEXT", "INTEGER", "DECIMAL", "BOOLEAN", "IDENTIFIER", "ENUMERATION"))


def validate_rule(rule):
    if not isinstance(rule, dict):
        raise ValueError("property rule must be an object")
    identifier(rule.get("name"), "property name")
    if "." in rule["name"]:
        raise ValueError("property names must not contain dots")
    if rule.get("type") not in TYPES:
        raise ValueError("unknown property type")
    for key in ("required", "sensitive"):
        if not isinstance(rule.get(key, False), bool):
            raise ValueError("rule flags must be boolean")
    allowed = rule.get("allowedValues", [])
    if not isinstance(allowed, list) or len(allowed) > 100:
        raise ValueError("allowedValues must be an array of at most 100 values")
    if not all(isinstance(value, string_types) and len(value) <= 512 for value in allowed):
        raise ValueError("allowedValues must contain bounded strings")
    if rule["type"] == "ENUMERATION" and not allowed:
        raise ValueError("enumeration requires allowed values")
    return dict(rule)


def validate_value(rule, value):
    if value is None:
        if rule.get("required", False):
            raise ValueError("required event property is missing")
        return
    if not isinstance(value, string_types) or len(value) > 512:
        raise ValueError("event property must be a bounded string")
    kind = rule["type"]
    if kind == "INTEGER":
        import re
        if not re.match(r"\A-?[0-9]+\Z", value):
            raise ValueError("integer property has invalid syntax")
        integer(int(value), "property", -9223372036854775808, 9223372036854775807)
    elif kind == "DECIMAL":
        try:
            number = decimal.Decimal(value)
        except decimal.InvalidOperation:
            raise ValueError("decimal property has invalid syntax")
        if not number.is_finite() or abs(number.as_tuple().exponent) > 1000:
            raise ValueError("decimal property exceeds supported range")
    elif kind == "BOOLEAN":
        if value not in ("true", "false"):
            raise ValueError("boolean property must be true or false")
    elif kind == "IDENTIFIER":
        identifier(value, "property")
    elif kind == "ENUMERATION":
        if value not in rule["allowedValues"]:
            raise ValueError("property is outside its enumeration")


class EventSchema(object):
    def __init__(self, definition):
        self.application_id = identifier(definition.get("applicationId"), "applicationId")
        self.event_name = identifier(definition.get("eventName"), "eventName")
        self.version = integer(definition.get("version"), "version", 1, 1000000)
        self.allow_unknown = definition.get("allowUnknown", False)
        if not isinstance(self.allow_unknown, bool):
            raise ValueError("allowUnknown must be boolean")
        rules = definition.get("properties", [])
        if not isinstance(rules, list) or len(rules) > 32:
            raise ValueError("schema requires at most 32 property rules")
        self.rules = {}
        for raw in rules:
            rule = validate_rule(raw)
            if rule["name"] in self.rules:
                raise ValueError("duplicate property rule")
            self.rules[rule["name"]] = rule

    def validate(self, event):
        if event["applicationId"] != self.application_id or event["name"] != self.event_name:
            raise ValueError("event does not match schema identity")
        for name, rule in self.rules.items():
            validate_value(rule, event["properties"].get(name))
        if not self.allow_unknown and set(event["properties"]) - set(self.rules):
            raise ValueError("event has an undeclared property")
        return event

    def public_properties(self, event):
        self.validate(event)
        return dict((name, value) for name, value in event["properties"].items()
                    if name in self.rules and not self.rules[name].get("sensitive", False))


class Catalog(object):
    def __init__(self, definitions, require_known=True):
        self.require_known = require_known
        self.schemas = {}
        for definition in definitions:
            schema = EventSchema(definition)
            key = schema.application_id, schema.event_name
            if key in self.schemas:
                raise ValueError("duplicate event schema")
            self.schemas[key] = schema

    def validate(self, events):
        for event in events:
            schema = self.schemas.get((event["applicationId"], event["name"]))
            if schema is None:
                if self.require_known:
                    raise ValueError("event schema is not registered")
            else:
                schema.validate(event)
            yield event
