package com.ima.persistence;

import com.ima.domain.Checks;
import com.ima.domain.TimeRange;
import java.util.List;
import org.hibernate.Session;

public final class AuditRepository {
    private final HibernateTransactions transactions;

    public AuditRepository(HibernateTransactions transactions) {
        this.transactions = transactions;
    }

    public void append(final AuditEntity record) {
        Checks.identifier(record.getId(), "auditId");
        Checks.identifier(record.getApplicationId(), "applicationId");
        Checks.identifier(record.getActorKeyId(), "actorKeyId");
        Checks.identifier(record.getAction(), "action");
        Checks.text(record.getResource(), "resource", 160);
        Checks.identifier(record.getRequestId(), "requestId");
        transactions.write(new HibernateTransactions.Work<Void>() {
            @Override
            public Void execute(Session session) {
                session.persist(record);
                return null;
            }
        });
    }

    public List<AuditEntity> list(final String applicationId, final TimeRange range, final int limit) {
        Checks.identifier(applicationId, "applicationId");
        Checks.range(limit, "limit", 1, 1000);
        return transactions.read(new HibernateTransactions.Work<List<AuditEntity>>() {
            @Override
            public List<AuditEntity> execute(Session session) {
                return session.createQuery("from AuditEntity where applicationId = :app "
                        + "and occurredAt >= :start and occurredAt < :end order by occurredAt, id",
                        AuditEntity.class).setParameter("app", applicationId)
                        .setParameter("start", range.getStart()).setParameter("end", range.getEnd())
                        .setMaxResults(limit).list();
            }
        });
    }
}
