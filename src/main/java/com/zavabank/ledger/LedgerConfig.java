package com.zavabank.ledger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class LedgerConfig {
    private static final Properties PROPERTIES = new Properties();

    static {
        try {
            InputStream inputStream = LedgerConfig.class.getClassLoader().getResourceAsStream("ledger.properties");
            if (inputStream != null) {
                PROPERTIES.load(inputStream);
                inputStream.close();
            }
        } catch (IOException ignored) {
        }
    }

    private LedgerConfig() {
    }

    public static String getDbUrl() {
        String host = read("DB_HOST", "db.host");
        String port = read("DB_PORT", "db.port");
        String name = read("DB_NAME", "db.name");
        return "jdbc:sqlserver://" + host + ":" + port + ";databaseName=" + name + ";encrypt=false;trustServerCertificate=true";
    }

    public static String getDbUser() {
        return read("DB_USER", "db.user");
    }

    public static String getDbPassword() {
        return read("DB_PASSWORD", "db.password");
    }

    private static String read(String envKey, String propertyKey) {
        String value = System.getenv(envKey);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return PROPERTIES.getProperty(propertyKey);
    }
}
