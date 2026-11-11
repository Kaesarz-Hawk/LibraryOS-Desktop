package com.libraryos.core.transaction;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional transaction coordinator for atomic multi-DAO operations.
 */
public final class TransactionManager {

    private static final Logger log = LoggerFactory.getLogger(TransactionManager.class);

    private TransactionManager() {
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction(Connection connection) throws Exception;
    }

    @FunctionalInterface
    public interface VoidTransactionCallback {
        void doInTransaction(Connection connection) throws Exception;
    }

    public static <T> T execute(TransactionCallback<T> callback) {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            T result = callback.doInTransaction(conn);

            conn.commit();
            return result;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    log.warn("Rolling back transaction due to error: {}", e.getMessage());
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction", rollbackEx);
                }
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new DatabaseException("Transaction execution failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    log.error("Failed to close transactional connection", e);
                }
            }
        }
    }

    public static void executeVoid(VoidTransactionCallback callback) {
        execute(conn -> {
            callback.doInTransaction(conn);
            return null;
        });
    }
}

