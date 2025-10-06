package com.icars.bot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Properties;

public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private final HikariDataSource dataSource;

    public DatabaseConfig() {
        // ---- 1) читаем параметры (env > -D > дефолт) ----
        String dbUrl  = opt("DB_URL", null);              // jdbc:postgresql://host:port/db
        String dbUser = req("DB_USER", "postgres");
        String dbPass = req("DB_PASS", "postgres");

        // если DB_URL не задан, собираем из host/port/name
        if (isBlank(dbUrl)) {
            String host = opt("DB_HOST", "localhost");    // в контейнере лучше задавать db
            String port = opt("DB_PORT", "5432");
            String name = opt("DB_NAME", "icars");
            dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + name;
        }

        // ---- 2) настраиваем Hikari ----
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(dbUrl);
        cfg.setUsername(dbUser);
        cfg.setPassword(dbPass);

        cfg.setPoolName(opt("DB_HIKARI_POOL_NAME", "ICarsBotPool"));
        cfg.setMaximumPoolSize(parseInt(opt("DB_HIKARI_MAX_POOL_SIZE", "10"), 10));
        cfg.setMinimumIdle(parseInt(opt("DB_HIKARI_MIN_IDLE", "2"), 2));
        cfg.setConnectionTimeout(parseLong(opt("DB_HIKARI_CONNECTION_TIMEOUT", "30000"), 30_000L));
        cfg.setIdleTimeout(parseLong(opt("DB_HIKARI_IDLE_TIMEOUT", "600000"), 600_000L));
        cfg.setMaxLifetime(parseLong(opt("DB_HIKARI_MAX_LIFETIME", "1800000"), 1_800_000L));
        // опционально: помогает ловить подвисшие соединения
        cfg.setLeakDetectionThreshold(parseLong(opt("DB_HIKARI_LEAK_DETECTION_MS", "0"), 0L));

        // ---- 3) создаём пул ----
        try {
            log.info("Initializing HikariCP: url={}, user={}, poolName={}, maxPool={}",
                    safeUrl(dbUrl), dbUser, cfg.getPoolName(), cfg.getMaximumPoolSize());
            this.dataSource = new HikariDataSource(cfg);
            log.info("HikariCP started.");
        } catch (Exception e) {
            log.error("Failed to initialize datasource. url={}, user={}", safeUrl(dbUrl), dbUser, e);
            throw e;
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /* ================= helpers ================= */

    private static String req(String name, String def) {
        String v = sanitize(System.getenv(name));
        if (isBlank(v)) v = sanitize(System.getProperty(name)); // поддержка -DDB_USER=...
        if (isBlank(v)) v = def;
        if (isBlank(v)) {
            throw new IllegalStateException("Required DB env var " + name + " is not set.");
        }
        return v;
    }

    private static String opt(String name, String def) {
        String v = sanitize(System.getenv(name));
        if (isBlank(v)) v = sanitize(System.getProperty(name));
        return isBlank(v) ? def : v;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /** убрать BOM/пробелы/окаймляющие кавычки */
    private static String sanitize(String s) {
        if (s == null) return null;
        String t = s.replace("\uFEFF", "").trim();
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }

    /** маскируем пароль если вдруг окажется в строке; и чуть укорачиваем в логах */
    private static String safeUrl(String url) {
        if (url == null) return null;
        return url.replaceAll("(?i)(password=)[^&]+", "$1***");
    }
}
