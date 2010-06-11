package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EventPredicate {
    private final Dimension dimension;
    private final PredicateOperator operator;
    private final List<String> values;

    public EventPredicate(Dimension dimension, PredicateOperator operator, List<String> values) {
        this.dimension = Checks.required(dimension, "dimension");
        this.operator = Checks.required(operator, "operator");
        Checks.required(values, "values");
        if (operator == PredicateOperator.EXISTS || operator == PredicateOperator.MISSING) {
            Checks.range(values.size(), "values", 0, 0);
        } else if (operator == PredicateOperator.IN || operator == PredicateOperator.NOT_IN) {
            Checks.range(values.size(), "values", 1, 100);
        } else {
            Checks.range(values.size(), "values", 1, 1);
        }
        for (String value : values) {
            Checks.required(value, "predicate value");
            if (value.length() > 512) {
                throw new IllegalArgumentException("predicate value too long");
            }
        }
        this.values = Collections.unmodifiableList(new ArrayList<String>(values));
        if (numericOperator()) {
            parseDecimal(values.get(0));
        }
    }

    public Dimension getDimension() {
        return dimension;
    }

    public PredicateOperator getOperator() {
        return operator;
    }

    public List<String> getValues() {
        return values;
    }

    public boolean matches(Event event) {
        String actual = dimension.extract(event);
        if (operator == PredicateOperator.EXISTS) {
            return actual != null;
        }
        if (operator == PredicateOperator.MISSING) {
            return actual == null;
        }
        if (actual == null) {
            return false;
        }
        switch (operator) {
            case EQUAL:
                return actual.equals(values.get(0));
            case NOT_EQUAL:
                return !actual.equals(values.get(0));
            case IN:
                return values.contains(actual);
            case NOT_IN:
                return !values.contains(actual);
            case PREFIX:
                return actual.startsWith(values.get(0));
            default:
                return numericMatch(actual);
        }
    }

    private boolean numericOperator() {
        return operator == PredicateOperator.GREATER_THAN || operator == PredicateOperator.GREATER_OR_EQUAL
                || operator == PredicateOperator.LESS_THAN || operator == PredicateOperator.LESS_OR_EQUAL;
    }

    private BigDecimal parseDecimal(String value) {
        try {
            BigDecimal number = new BigDecimal(value);
            if (Math.abs((long) number.scale()) > 1000 || number.precision() > 512) {
                throw new IllegalArgumentException("numeric predicate exceeds precision limits");
            }
            return number;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("numeric predicate requires decimal values");
        }
    }

    private boolean numericMatch(String actual) {
        int order;
        try {
            order = parseDecimal(actual).compareTo(parseDecimal(values.get(0)));
        } catch (IllegalArgumentException failure) {
            return false;
        }
        switch (operator) {
            case GREATER_THAN:
                return order > 0;
            case GREATER_OR_EQUAL:
                return order >= 0;
            case LESS_THAN:
                return order < 0;
            case LESS_OR_EQUAL:
                return order <= 0;
            default:
                throw new IllegalStateException("unsupported predicate operator");
        }
    }
}
