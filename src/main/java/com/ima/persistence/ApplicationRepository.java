package com.ima.persistence;

import com.ima.domain.Checks;
import com.ima.domain.Page;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;

public final class ApplicationRepository {
    private final HibernateTransactions transactions;

    public ApplicationRepository(HibernateTransactions transactions) {
        this.transactions = transactions;
    }

    public ApplicationEntity find(final String id) {
        Checks.identifier(id, "applicationId");
        return transactions.read(new HibernateTransactions.Work<ApplicationEntity>() {
            @Override
            public ApplicationEntity execute(Session session) {
                return session.get(ApplicationEntity.class, id);
            }
        });
    }

    public void create(final ApplicationEntity application) {
        validate(application);
        transactions.write(new HibernateTransactions.Work<Void>() {
            @Override
            public Void execute(Session session) {
                session.persist(application);
                return null;
            }
        });
    }

    public ApplicationEntity update(final ApplicationEntity application) {
        validate(application);
        return transactions.write(new HibernateTransactions.Work<ApplicationEntity>() {
            @Override
            public ApplicationEntity execute(Session session) {
                if (session.get(ApplicationEntity.class, application.getId()) == null) {
                    throw new IllegalArgumentException("application does not exist");
                }
                return (ApplicationEntity) session.merge(application);
            }
        });
    }

    public Page<ApplicationEntity> listByOwner(final String owner, final int offset, final int limit) {
        Checks.identifier(owner, "ownerReference");
        Checks.range(offset, "offset", 0, 1000000);
        Checks.range(limit, "limit", 1, 1000);
        return transactions.read(new HibernateTransactions.Work<Page<ApplicationEntity>>() {
            @Override
            public Page<ApplicationEntity> execute(Session session) {
                List<ApplicationEntity> items = session.createQuery(
                        "from ApplicationEntity where ownerReference = :owner order by id",
                        ApplicationEntity.class).setParameter("owner", owner)
                        .setFirstResult(offset).setMaxResults(limit + 1).list();
                boolean more = items.size() > limit;
                return new Page<ApplicationEntity>(new ArrayList<ApplicationEntity>(
                        items.subList(0, Math.min(items.size(), limit))), offset, limit, more);
            }
        });
    }

    private void validate(ApplicationEntity application) {
        Checks.required(application, "application");
        Checks.identifier(application.getId(), "applicationId");
        Checks.text(application.getDisplayName(), "displayName", 160);
        Checks.identifier(application.getOwnerReference(), "ownerReference");
        Checks.range(application.getRetentionDays(), "retentionDays", 1, 3650);
        Checks.positive(application.getSessionGapMillis(), "sessionGapMillis");
        Checks.nonnegative(application.getCreatedAt(), "createdAt");
        if (application.getUpdatedAt() < application.getCreatedAt()) {
            throw new IllegalArgumentException("updatedAt precedes createdAt");
        }
    }
}
