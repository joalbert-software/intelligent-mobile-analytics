from __future__ import absolute_import, division

from collections import defaultdict

from .validation import identifier, integer


def index_rows(rows, identity_fields, metric_fields):
    indexed = {}
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError("reconciliation rows must be objects")
        if any(field not in row for field in identity_fields + metric_fields):
            raise ValueError("reconciliation row is missing a required field")
        key = tuple(row[field] for field in identity_fields)
        if key in indexed:
            raise ValueError("duplicate reconciliation identity")
        metrics = {}
        for field in metric_fields:
            metrics[field] = integer(row[field], field, -9223372036854775808, 9223372036854775807)
        indexed[key] = metrics
    return indexed


def compare(expected, actual, identity_fields, metric_fields, tolerance=0):
    integer(tolerance, "tolerance", 0, 9223372036854775807)
    if not identity_fields or not metric_fields:
        raise ValueError("reconciliation requires identity and metric fields")
    identity_fields = tuple(identity_fields)
    metric_fields = tuple(metric_fields)
    if len(set(identity_fields + metric_fields)) != len(identity_fields + metric_fields):
        raise ValueError("reconciliation fields must be distinct")
    first = index_rows(expected, identity_fields, metric_fields)
    second = index_rows(actual, identity_fields, metric_fields)
    differences = []
    totals = dict((field, {"expected": 0, "actual": 0}) for field in metric_fields)
    keys = set(first) | set(second)
    for key in sorted(keys):
        left = first.get(key)
        right = second.get(key)
        identity = dict(zip(identity_fields, key))
        if left is None:
            differences.append({"identity": identity, "kind": "unexpected_row"})
        elif right is None:
            differences.append({"identity": identity, "kind": "missing_row"})
        for field in metric_fields:
            expected_value = 0 if left is None else left[field]
            actual_value = 0 if right is None else right[field]
            totals[field]["expected"] += expected_value
            totals[field]["actual"] += actual_value
            delta = actual_value - expected_value
            if left is not None and right is not None and abs(delta) > tolerance:
                differences.append({"identity": identity, "kind": "metric_mismatch", "metric": field,
                                    "expected": expected_value, "actual": actual_value, "delta": delta})
    return {"passed": not differences, "expectedRows": len(first), "actualRows": len(second),
            "comparedIdentities": len(keys), "totals": totals, "differences": differences}


def aggregate(rows, identity_fields, additive_fields):
    grouped = defaultdict(lambda: dict((field, 0) for field in additive_fields))
    for row in rows:
        key = tuple(row[field] for field in identity_fields)
        target = grouped[key]
        for field in additive_fields:
            target[field] += integer(row[field], field, -9223372036854775808, 9223372036854775807)
    result = []
    for key, metrics in sorted(grouped.items()):
        row = dict(zip(identity_fields, key))
        row.update(metrics)
        result.append(row)
    return result
