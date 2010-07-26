package com.ima.persistence;

import com.ima.domain.Checks;
import java.util.List;
import org.hibernate.LockMode;
import org.hibernate.Session;

/** Lease transitions happen under a row lock; ownership fences completion. */
public final class JobRepository {
    private final HibernateTransactions transactions;

    public JobRepository(HibernateTransactions transactions) {
        this.transactions = transactions;
    }

    public JobEntity find(final String applicationId, final String jobId) {
        Checks.identifier(applicationId, "applicationId");
        Checks.identifier(jobId, "jobId");
        return transactions.read(new HibernateTransactions.Work<JobEntity>() {
            @Override
            public JobEntity execute(Session session) {
                return session.createQuery("from JobEntity where applicationId = :app and id = :id", JobEntity.class)
                        .setParameter("app", applicationId).setParameter("id", jobId).uniqueResult();
            }
        });
    }

    public com.ima.domain.Page<JobEntity> list(final String applicationId, final int offset, final int limit) {
        Checks.identifier(applicationId, "applicationId");
        Checks.range(offset, "offset", 0, 1000000);
        Checks.range(limit, "limit", 1, 1000);
        return transactions.read(new HibernateTransactions.Work<com.ima.domain.Page<JobEntity>>() {
            @Override
            public com.ima.domain.Page<JobEntity> execute(Session session) {
                List<JobEntity> rows = session.createQuery("from JobEntity where applicationId = :app order by createdAt, id",
                        JobEntity.class).setParameter("app", applicationId)
                        .setFirstResult(offset).setMaxResults(limit + 1).list();
                return new com.ima.domain.Page<JobEntity>(new java.util.ArrayList<JobEntity>(
                        rows.subList(0, Math.min(limit, rows.size()))), offset, limit, rows.size() > limit);
            }
        });
    }

    public boolean cancel(final String applicationId, final String jobId, final long now) {
        return transactions.write(new HibernateTransactions.Work<Boolean>() {
            @Override
            public Boolean execute(Session session) {
                return session.createQuery("update JobEntity set state = 'CANCELLED', updatedAt = :now, "
                        + "leaseUntil = 0, revision = revision + 1 where applicationId = :app and id = :id "
                        + "and state in ('QUEUED', 'RUNNING', 'FAILED')")
                        .setParameter("now", now).setParameter("app", applicationId)
                        .setParameter("id", jobId).executeUpdate() == 1;
            }
        });
    }

    public boolean heartbeat(final String jobId, final String worker, final int attempt,
                              final long now, final long leaseMillis) {
        Checks.identifier(jobId, "jobId");
        Checks.identifier(worker, "worker");
        Checks.positive(leaseMillis, "leaseMillis");
        if (now < 0 || now > Long.MAX_VALUE - leaseMillis) {
            throw new IllegalArgumentException("invalid lease interval");
        }
        return transactions.write(new HibernateTransactions.Work<Boolean>() {
            @Override
            public Boolean execute(Session session) {
                return session.createQuery("update JobEntity set leaseUntil = :until, updatedAt = :now, "
                        + "revision = revision + 1 where id = :id and leaseOwner = :worker "
                        + "and attempts = :attempt and state = 'RUNNING' and leaseUntil > :now")
                        .setParameter("until", now + leaseMillis).setParameter("now", now)
                        .setParameter("id", jobId).setParameter("worker", worker)
                        .setParameter("attempt", attempt).executeUpdate() == 1;
            }
        });
    }

    public int failExhausted(final long now, final int maximumAttempts) {
        Checks.range(maximumAttempts, "maximumAttempts", 1, 100);
        return transactions.write(new HibernateTransactions.Work<Integer>() {
            @Override
            public Integer execute(Session session) {
                return session.createQuery("update JobEntity set state = 'FAILED', failureCode = 'attempts_exhausted', "
                        + "updatedAt = :now, revision = revision + 1 where attempts >= :maximum "
                        + "and (state = 'QUEUED' or (state = 'RUNNING' and leaseUntil <= :now))")
                        .setParameter("now", now).setParameter("maximum", maximumAttempts).executeUpdate();
            }
        });
    }

    public void enqueue(final JobEntity job) {
        Checks.identifier(job.getId(), "jobId");
        Checks.identifier(job.getApplicationId(), "applicationId");
        Checks.identifier(job.getType(), "type");
        if (job.getRangeEnd() <= job.getRangeStart() || job.getRangeStart() < 0) {
            throw new IllegalArgumentException("invalid job time range");
        }
        job.setState("QUEUED");
        job.setAttempts(0);
        job.setLeaseOwner(null);
        job.setLeaseUntil(0);
        transactions.write(new HibernateTransactions.Work<Void>() {
            @Override
            public Void execute(Session session) {
                session.persist(job);
                return null;
            }
        });
    }

    public JobEntity claim(final String worker, final long now, final long leaseMillis,
                           final int maximumAttempts) {
        Checks.identifier(worker, "worker");
        Checks.nonnegative(now, "now");
        Checks.positive(leaseMillis, "leaseMillis");
        Checks.range(maximumAttempts, "maximumAttempts", 1, 100);
        if (now > Long.MAX_VALUE - leaseMillis) {
            throw new IllegalArgumentException("lease overflows clock");
        }
        return transactions.write(new HibernateTransactions.Work<JobEntity>() {
            @Override
            public JobEntity execute(Session session) {
                List<JobEntity> candidates = session.createQuery("from JobEntity j where "
                        + "(state = 'QUEUED' or (state = 'RUNNING' and leaseUntil <= :now)) "
                        + "and attempts < :max order by createdAt, id", JobEntity.class)
                        .setParameter("now", now).setParameter("max", maximumAttempts)
                        .setMaxResults(1).setLockMode("j", LockMode.PESSIMISTIC_WRITE).list();
                if (candidates.isEmpty()) {
                    return null;
                }
                JobEntity job = candidates.get(0);
                job.setState("RUNNING");
                job.setLeaseOwner(worker);
                job.setLeaseUntil(now + leaseMillis);
                job.setAttempts(job.getAttempts() + 1);
                job.setUpdatedAt(now);
                job.setFailureCode(null);
                return job;
            }
        });
    }

    public boolean complete(final String jobId, final String worker, final int attempt, final long now) {
        return finish(jobId, worker, attempt, now, "SUCCEEDED", null);
    }

    public boolean fail(final String jobId, final String worker, final int attempt,
                        final long now, final String failureCode) {
        Checks.identifier(failureCode, "failureCode");
        return finish(jobId, worker, attempt, now, "FAILED", failureCode);
    }

    private boolean finish(final String id, final String worker, final int attempt,
                           final long now, final String state, final String failure) {
        return transactions.write(new HibernateTransactions.Work<Boolean>() {
            @Override
            public Boolean execute(Session session) {
                return session.createQuery("update JobEntity set state = :state, failureCode = :failure, "
                        + "leaseUntil = 0, updatedAt = :now, revision = revision + 1 "
                        + "where id = :id and state = 'RUNNING' and leaseOwner = :worker "
                        + "and attempts = :attempt and leaseUntil > :now")
                        .setParameter("state", state).setParameter("failure", failure)
                        .setParameter("now", now).setParameter("id", id)
                        .setParameter("worker", worker).setParameter("attempt", attempt)
                        .executeUpdate() == 1;
            }
        });
    }

    public boolean retry(final String applicationId, final String jobId, final long now) {
        return transactions.write(new HibernateTransactions.Work<Boolean>() {
            @Override
            public Boolean execute(Session session) {
                return session.createQuery("update JobEntity set state = 'QUEUED', updatedAt = :now, "
                        + "revision = revision + 1 where applicationId = :app and id = :id and state = 'FAILED'")
                        .setParameter("now", now).setParameter("app", applicationId)
                        .setParameter("id", jobId).executeUpdate() == 1;
            }
        });
    }
}
