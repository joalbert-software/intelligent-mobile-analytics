package com.ima.store;

import com.ima.domain.Event;
import com.ima.domain.Platform;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;

public final class MongoEventCodec {
    public Document encode(Event event) {
        Document properties = new Document();
        properties.putAll(event.getProperties());
        return new Document("applicationId", event.getApplicationId())
                .append("eventId", event.getEventId())
                .append("userId", event.getUserId())
                .append("name", event.getName())
                .append("timestamp", event.getTimestamp())
                .append("receivedAt", event.getReceivedAt())
                .append("platform", event.getPlatform().name())
                .append("appVersion", event.getAppVersion())
                .append("properties", properties);
    }

    public Event decode(Document document) {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        Document stored = (Document) document.get("properties");
        if (stored != null) {
            for (Map.Entry<String, Object> entry : stored.entrySet()) {
                if (!(entry.getValue() instanceof String)) {
                    throw new StoreUnavailableException("invalid stored property type");
                }
                properties.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return new Event(document.getString("applicationId"), document.getString("eventId"),
                document.getString("userId"), document.getString("name"),
                ((Number) document.get("timestamp")).longValue(),
                ((Number) document.get("receivedAt")).longValue(),
                Platform.parse(document.getString("platform")),
                document.getString("appVersion"), properties);
    }
}
