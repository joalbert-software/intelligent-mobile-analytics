package com.ima.store;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bson.Document;

public final class MongoEventStore implements EventStore {
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    private final MongoEventCodec codec = new MongoEventCodec();

    public MongoEventStore(MongoDatabase database) {
        this.database = Checks.required(database, "database");
        this.collection = database.getCollection("events")
                .withWriteConcern(WriteConcern.MAJORITY.withJournal(true));
    }

    public void ensureIndexes() {
        collection.createIndex(Indexes.ascending("applicationId", "eventId"),
                new IndexOptions().unique(true).name("event_identity"));
        collection.createIndex(Indexes.ascending("applicationId", "timestamp", "eventId"));
        collection.createIndex(Indexes.ascending("applicationId", "userId", "timestamp"));
        collection.createIndex(Indexes.ascending("applicationId", "name", "timestamp"));
    }

    @Override
    public SaveResult save(Event event) {
        try {
            collection.insertOne(codec.encode(event));
            return SaveResult.CREATED;
        } catch (MongoWriteException exception) {
            if (exception.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                Event stored = find(event.getApplicationId(), event.getEventId());
                if (stored == null) {
                    throw new StoreUnavailableException("duplicate event disappeared during read");
                }
                return stored.samePayload(event) ? SaveResult.DUPLICATE : SaveResult.CONFLICT;
            }
            throw new StoreUnavailableException("event write failed", exception);
        } catch (MongoException exception) {
            throw new StoreUnavailableException("event write failed", exception);
        }
    }

    @Override
    public Event find(String applicationId, String eventId) {
        Checks.identifier(applicationId, "applicationId");
        Checks.identifier(eventId, "eventId");
        try {
            Document value = collection.find(new Document("applicationId", applicationId)
                    .append("eventId", eventId)).maxTime(5, TimeUnit.SECONDS).first();
            return value == null ? null : codec.decode(value);
        } catch (MongoException exception) {
            throw new StoreUnavailableException("event lookup failed", exception);
        }
    }

    @Override
    public List<Event> query(EventQuery query) {
        Document filter = new Document("applicationId", query.getApplicationId())
                .append("timestamp", new Document("$gte", query.getRange().getStart())
                        .append("$lt", query.getRange().getEnd()));
        if (query.getEventName() != null) {
            filter.append("name", query.getEventName());
        }
        if (query.getUserId() != null) {
            filter.append("userId", query.getUserId());
        }
        if (query.getPlatform() != null) {
            filter.append("platform", query.getPlatform().name());
        }
        List<Event> events = new ArrayList<Event>();
        try (MongoCursor<Document> cursor = collection.find(filter)
                .sort(new Document("timestamp", 1).append("eventId", 1))
                .limit(query.getLimit()).maxTime(10, TimeUnit.SECONDS).iterator()) {
            while (cursor.hasNext()) {
                events.add(codec.decode(cursor.next()));
            }
        } catch (MongoException exception) {
            throw new StoreUnavailableException("event query failed", exception);
        }
        return events;
    }

    @Override
    public long deleteUser(String applicationId, String userId) {
        Checks.identifier(applicationId, "applicationId");
        Checks.identifier(userId, "userId");
        try {
            return collection.deleteMany(new Document("applicationId", applicationId)
                    .append("userId", userId)).getDeletedCount();
        } catch (MongoException exception) {
            throw new StoreUnavailableException("user deletion failed", exception);
        }
    }

    @Override
    public long deleteBefore(String applicationId, long timestamp) {
        Checks.identifier(applicationId, "applicationId");
        Checks.nonnegative(timestamp, "timestamp");
        try {
            return collection.deleteMany(new Document("applicationId", applicationId)
                    .append("timestamp", new Document("$lt", timestamp))).getDeletedCount();
        } catch (MongoException exception) {
            throw new StoreUnavailableException("retention deletion failed", exception);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (MongoException exception) {
            return false;
        }
    }
}
