package com.ima.persistence;

import com.ima.domain.Checks;
import com.ima.domain.Page;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;

public final class ReportRepository {
    private final HibernateTransactions transactions;

    public ReportRepository(HibernateTransactions transactions) {
        this.transactions = transactions;
    }

    public ReportEntity find(final String applicationId, final String reportId) {
        Checks.identifier(applicationId, "applicationId");
        Checks.identifier(reportId, "reportId");
        return transactions.read(new HibernateTransactions.Work<ReportEntity>() {
            @Override
            public ReportEntity execute(Session session) {
                return session.createQuery("from ReportEntity where applicationId = :app and id = :id",
                        ReportEntity.class).setParameter("app", applicationId)
                        .setParameter("id", reportId).uniqueResult();
            }
        });
    }

    public void create(final ReportEntity report) {
        Checks.identifier(report.getId(), "reportId");
        Checks.identifier(report.getApplicationId(), "applicationId");
        Checks.text(report.getName(), "name", 160);
        Checks.identifier(report.getType(), "type");
        Checks.required(report.getDefinition(), "definition");
        if (report.getDefinition().length() > 16000) {
            throw new IllegalArgumentException("report definition is too large");
        }
        transactions.write(new HibernateTransactions.Work<Void>() {
            @Override
            public Void execute(Session session) {
                session.persist(report);
                return null;
            }
        });
    }

    public boolean delete(final String applicationId, final String reportId) {
        return transactions.write(new HibernateTransactions.Work<Boolean>() {
            @Override
            public Boolean execute(Session session) {
                return session.createQuery("delete from ReportEntity where applicationId = :app and id = :id")
                        .setParameter("app", applicationId).setParameter("id", reportId).executeUpdate() == 1;
            }
        });
    }

    public Page<ReportEntity> list(final String applicationId, final int offset, final int limit) {
        Checks.identifier(applicationId, "applicationId");
        Checks.range(offset, "offset", 0, 1000000);
        Checks.range(limit, "limit", 1, 1000);
        return transactions.read(new HibernateTransactions.Work<Page<ReportEntity>>() {
            @Override
            public Page<ReportEntity> execute(Session session) {
                List<ReportEntity> items = session.createQuery(
                        "from ReportEntity where applicationId = :app order by id", ReportEntity.class)
                        .setParameter("app", applicationId).setFirstResult(offset)
                        .setMaxResults(limit + 1).list();
                return new Page<ReportEntity>(new ArrayList<ReportEntity>(
                        items.subList(0, Math.min(items.size(), limit))), offset, limit, items.size() > limit);
            }
        });
    }
}
