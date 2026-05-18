package com.zavabank.ledger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class LedgerConnectionFactory {
    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("SQL Server JDBC driver not found", exception);
        }
    }

    private LedgerConnectionFactory() {
    }

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            LedgerConfig.getDbUrl(),
            LedgerConfig.getDbUser(),
            LedgerConfig.getDbPassword()
        );
    }
}
