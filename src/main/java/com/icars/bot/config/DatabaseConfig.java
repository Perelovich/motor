package com.icars.bot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Properties;

public class DatabaseConfig {

    private final HikariDataSource dataSource;

    public DatabaseConfig() {
        Properties props = new Properties();
        props.setProperty("dataSourceClassName", "org.postgresql.ds.PGSimpleDataSource");
        props.setProperty("dataSource.user", getEnv("DB_USER", "postgres"));
        props.setProperty("dataSource.password", getEnv("DB_PASS", "postgres"));
        props.setProperty("dataSource.databaseName", getEnv("DB_NAME", "icars"));
        props.setProperty("dataSource.serverName", getEnv("DB_HOST", "localhost"));
        props.setProperty("dataSource.portNumber", getEnv("DB_PORT", "5432"));

        HikariConfig config = new HikariConfig(props);
        config.setPoolName(getEnv("DB_HIKARI_POOL_NAME", "ICarsBotPool"));
        config.setMaximumPoolSize(Integer.parseInt(getEnv("DB_HIKARI_MAX_POOL_SIZE", "10")));
        config.setMinimumIdle(Integer.parseInt(getEnv("DB_HIKARI_MIN_IDLE", "2")));
        config.setConnectionTimeout(Long.parseLong(getEnv("DB_HIKARI_CONNECTION_TIMEOUT", "30000")));
        config.setIdleTimeout(Long.parseLong(getEnv("DB_HIKARI_IDLE_TIMEOUT", "600000")));
        config.setMaxLifetime(Long.parseLong(getEnv("DB_HIKARI_MAX_LIFETIME", "1800000")));

        this.dataSource = new HikariDataSource(config);
    }

    private String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
