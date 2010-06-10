package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;

public final class Dimension {
    private final String name;

    public Dimension(String name) {
        this.name = Checks.text(name, "dimension", 128);
        if (!("platform".equals(name) || "appVersion".equals(name)
                || "eventName".equals(name) || name.startsWith("property:"))) {
            throw new IllegalArgumentException("unknown dimension");
        }
        if (name.startsWith("property:")) {
            Checks.identifier(name.substring(9), "property dimension");
        }
    }

    public String getName() {
        return name;
    }

    public String extract(Event event) {
        if ("platform".equals(name)) {
            return event.getPlatform().name();
        }
        if ("appVersion".equals(name)) {
            return event.getAppVersion();
        }
        if ("eventName".equals(name)) {
            return event.getName();
        }
        return event.property(name.substring(9));
    }
}
