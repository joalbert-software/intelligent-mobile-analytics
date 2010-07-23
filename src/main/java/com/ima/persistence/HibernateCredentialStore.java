package com.ima.persistence;

import com.ima.security.ApiCredential;
import com.ima.security.Scope;
import com.ima.store.CredentialStore;
import java.time.Clock;
import java.util.EnumSet;
import org.hibernate.Session;

public final class HibernateCredentialStore implements CredentialStore {
    private final HibernateTransactions transactions;
    private final Clock clock;

    public HibernateCredentialStore(HibernateTransactions transactions, Clock clock) {
        this.transactions = transactions;
        this.clock = clock;
    }

    @Override
    public ApiCredential findByDigest(final String digest) {
        return transactions.read(new HibernateTransactions.Work<ApiCredential>() {
            @Override
            public ApiCredential execute(Session session) {
                CredentialEntity entity = session.createQuery(
                        "from CredentialEntity c where c.digest = :digest and exists "
                        + "(select a.id from ApplicationEntity a where a.id = c.applicationId and a.enabled = true)", CredentialEntity.class)
                        .setParameter("digest", digest).uniqueResult();
                if (entity == null) {
                    return null;
                }
                EnumSet<Scope> scopes = EnumSet.noneOf(Scope.class);
                for (String value : entity.getScopes().split(",")) {
                    scopes.add(Scope.valueOf(value));
                }
                return new ApiCredential(entity.getId(), entity.getApplicationId(),
                        entity.getDigest(), scopes, entity.getExpiresAt(), entity.isRevoked());
            }
        });
    }

    @Override
    public void save(final ApiCredential credential) {
        transactions.write(new HibernateTransactions.Work<Void>() {
            @Override
            public Void execute(Session session) {
                CredentialEntity entity = new CredentialEntity();
                entity.setId(credential.getId());
                entity.setApplicationId(credential.getApplicationId());
                entity.setDigest(credential.getDigest());
                StringBuilder scopes = new StringBuilder();
                for (Scope scope : credential.getScopes()) {
                    if (scopes.length() > 0) {
                        scopes.append(',');
                    }
                    scopes.append(scope.name());
                }
                entity.setScopes(scopes.toString());
                entity.setExpiresAt(credential.getExpiresAt());
                entity.setCreatedAt(clock.millis());
                entity.setRevoked(credential.isRevoked());
                session.persist(entity);
                return null;
            }
        });
    }

    @Override
    public boolean revoke(final String applicationId, final String keyId) {
        return transactions.write(new HibernateTransactions.Work<Boolean>() {
            @Override
            public Boolean execute(Session session) {
                return session.createQuery("update CredentialEntity set revoked = true "
                        + "where applicationId = :app and id = :id")
                        .setParameter("app", applicationId)
                        .setParameter("id", keyId).executeUpdate() == 1;
            }
        });
    }
}
