package com.ima.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.domain.Event;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

public final class EventExporter {
    private final ObjectMapper mapper;

    public EventExporter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public long csv(Collection<Event> events, Writer output) throws IOException {
        CsvWriter csv = new CsvWriter(output);
        csv.row(Arrays.asList("applicationId", "eventId", "userId", "name", "timestamp",
                "receivedAt", "platform", "appVersion", "properties"));
        long count = 0;
        for (Event event : events) {
            csv.row(Arrays.asList(event.getApplicationId(), event.getEventId(), event.getUserId(),
                    event.getName(), event.getTimestamp(), event.getReceivedAt(), event.getPlatform().name(),
                    event.getAppVersion(), mapper.writeValueAsString(event.getProperties())));
            count++;
        }
        csv.flush();
        return count;
    }

    public long jsonLines(Collection<Event> events, Writer output) throws IOException {
        long count = 0;
        for (Event event : events) {
            output.write(mapper.writeValueAsString(event));
            output.write('\n');
            count++;
        }
        output.flush();
        return count;
    }
}
