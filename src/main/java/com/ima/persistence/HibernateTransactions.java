package com.ima.persistence;

import com.ima.domain.Checks;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/** Every operation has a closed session and an explicit transaction boundary. */
public final class HibernateTransactions {
    public interface Work<T> {
        T execute(Session session);
    }

    private final SessionFactory sessions;

    public HibernateTransactions(SessionFactory sessions) {
        this.sessions = Checks.required(sessions, "sessions");
    }

    public <T> T read(Work<T> work) {
        return execute(work, true);
    }

    public <T> T write(Work<T> work) {
        return execute(work, false);
    }

    private <T> T execute(Work<T> work, boolean readOnly) {
        try (Session session = sessions.openSession()) {
            session.setDefaultReadOnly(readOnly);
            Transaction transaction = session.beginTransaction();
            try {
                T result = work.execute(session);
                transaction.commit();
                return result;
            } catch (RuntimeException failure) {
                if (transaction.isActive()) {
                    try {
                        transaction.rollback();
                    } catch (RuntimeException rollbackFailure) {
                        failure.addSuppressed(rollbackFailure);
                    }
                }
                throw failure;
            }
        }
    }
}
