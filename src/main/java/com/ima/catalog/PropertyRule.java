package com.ima.catalog;

import com.ima.domain.Checks;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PropertyRule {
    private final String name;
    private final PropertyType type;
    private final boolean required;
    private final boolean sensitive;
    private final Set<String> allowedValues;

    public PropertyRule(String name, PropertyType type, boolean required,
                         boolean sensitive, Set<String> allowedValues) {
        this.name = Checks.identifier(name, "property name");
        if (name.indexOf('.') >= 0) {
            throw new IllegalArgumentException("property name must not contain dots");
        }
        this.type = Checks.required(type, "type");
        this.required = required;
        this.sensitive = sensitive;
        Checks.bounded(allowedValues, "allowedValues", 0, 100);
        if (type == PropertyType.ENUMERATION && allowedValues.isEmpty()) {
            throw new IllegalArgumentException("enumeration requires allowed values");
        }
        this.allowedValues = Collections.unmodifiableSet(new HashSet<String>(allowedValues));
    }

    public String getName() {
        return name;
    }

    public PropertyType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public Set<String> getAllowedValues() {
        return allowedValues;
    }

    public void validate(String value) {
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("missing required property: " + name);
            }
            return;
        }
        if (value.length() > 512) {
            throw new IllegalArgumentException("property exceeds size limit: " + name);
        }
        try {
            switch (type) {
                case INTEGER:
                    if (!value.matches("-?[0-9]+")) {
                        throw new IllegalArgumentException();
                    }
                    Long.parseLong(value);
                    break;
                case DECIMAL:
                    BigDecimal decimal = new BigDecimal(value);
                    if (Math.abs((long) decimal.scale()) > 1000) {
                        throw new IllegalArgumentException();
                    }
                    break;
                case BOOLEAN:
                    if (!("true".equals(value) || "false".equals(value))) {
                        throw new IllegalArgumentException();
                    }
                    break;
                case IDENTIFIER:
                    Checks.identifier(value, "identifier");
                    break;
                case ENUMERATION:
                    if (!allowedValues.contains(value)) {
                        throw new IllegalArgumentException();
                    }
                    break;
                default:
                    break;
            }
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("property does not match its declared type: " + name);
        }
    }
}
