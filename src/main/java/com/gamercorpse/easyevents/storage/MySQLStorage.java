package com.gamercorpse.easyevents.storage;

import com.gamercorpse.easyevents.EasyEvents;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MySQLStorage {

    private static final Pattern VALID_TABLE_NAME =
            Pattern.compile(
                    "^[A-Za-z0-9_]+$"
            );

    private final EasyEvents plugin;

    private Connection connection;

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String table;
    private boolean useSsl;

    public MySQLStorage(EasyEvents plugin) {

        this.plugin = plugin;

        loadSettings();
    }

    private void loadSettings() {

        host =
                plugin.getConfig().getString(
                        "storage.mysql.host",
                        "localhost"
                );

        port =
                plugin.getConfig().getInt(
                        "storage.mysql.port",
                        3306
                );

        database =
                plugin.getConfig().getString(
                        "storage.mysql.database",
                        "easyevents"
                );

        username =
                plugin.getConfig().getString(
                        "storage.mysql.username",
                        "root"
                );

        password =
                plugin.getConfig().getString(
                        "storage.mysql.password",
                        ""
                );

        table =
                plugin.getConfig().getString(
                        "storage.mysql.table",
                        "easy_events_data"
                );

        useSsl =
                plugin.getConfig().getBoolean(
                        "storage.mysql.use-ssl",
                        false
                );

        if (host == null || host.isBlank()) {
            host = "localhost";
        }

        if (database == null || database.isBlank()) {
            database = "easyevents";
        }

        if (username == null) {
            username = "";
        }

        if (password == null) {
            password = "";
        }

        if (table == null ||
                table.isBlank() ||
                !VALID_TABLE_NAME.matcher(table).matches()) {

            plugin.getLogger().warning(
                    "Invalid MySQL table name configured."
            );

            plugin.getLogger().warning(
                    "Using default table name: easy_events_data"
            );

            table =
                    "easy_events_data";
        }
    }

    public synchronized boolean connect() {

        close();

        loadSettings();

        String url =
                "jdbc:mysql://" +
                        host +
                        ":" +
                        port +
                        "/" +
                        database +
                        "?useSSL=" +
                        useSsl +
                        "&allowPublicKeyRetrieval=true" +
                        "&characterEncoding=utf8" +
                        "&useUnicode=true" +
                        "&serverTimezone=UTC";

        try {

            connection =
                    DriverManager.getConnection(
                            url,
                            username,
                            password
                    );

            createTable();

            return true;

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Could not connect to the MySQL database."
            );

            plugin.getLogger().severe(
                    "Host: " +
                            host +
                            ":" +
                            port
            );

            plugin.getLogger().severe(
                    "Database: " +
                            database
            );

            plugin.getLogger().severe(
                    "Reason: " +
                            exception.getMessage()
            );

            close();

            return false;
        }
    }

    private synchronized void createTable()
            throws SQLException {

        String sql =
                "CREATE TABLE IF NOT EXISTS `" +
                        table +
                        "` (" +
                        "`data_key` VARCHAR(255) NOT NULL," +
                        "`data_value` LONGTEXT NULL," +
                        "`updated_at` TIMESTAMP NOT NULL " +
                        "DEFAULT CURRENT_TIMESTAMP " +
                        "ON UPDATE CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (`data_key`)" +
                        ") ENGINE=InnoDB " +
                        "DEFAULT CHARSET=utf8mb4 " +
                        "COLLATE=utf8mb4_unicode_ci";

        try (Statement statement =
                     connection.createStatement()) {

            statement.executeUpdate(sql);
        }
    }

    public synchronized void set(
            String key,
            String value
    ) {

        if (!isValidKey(key)) {
            return;
        }

        if (!ensureConnection()) {
            return;
        }

        String sql =
                "INSERT INTO `" +
                        table +
                        "` (`data_key`, `data_value`) " +
                        "VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "`data_value` = VALUES(`data_value`)";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    key
            );

            statement.setString(
                    2,
                    value
            );

            statement.executeUpdate();

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Could not save MySQL value for key '" +
                            key +
                            "'."
            );

            plugin.getLogger().severe(
                    exception.getMessage()
            );
        }
    }

    public synchronized String get(
            String key
    ) {

        if (!isValidKey(key)) {
            return null;
        }

        if (!ensureConnection()) {
            return null;
        }

        String sql =
                "SELECT `data_value` " +
                        "FROM `" +
                        table +
                        "` " +
                        "WHERE `data_key` = ? " +
                        "LIMIT 1";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    key
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return resultSet.getString(
                            "data_value"
                    );
                }
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Could not load MySQL value for key '" +
                            key +
                            "'."
            );

            plugin.getLogger().severe(
                    exception.getMessage()
            );
        }

        return null;
    }

    public synchronized String get(
            String key,
            String defaultValue
    ) {

        String value =
                get(key);

        return value != null
                ? value
                : defaultValue;
    }

    public synchronized boolean contains(
            String key
    ) {

        if (!isValidKey(key)) {
            return false;
        }

        if (!ensureConnection()) {
            return false;
        }

        String sql =
                "SELECT 1 " +
                        "FROM `" +
                        table +
                        "` " +
                        "WHERE `data_key` = ? " +
                        "LIMIT 1";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    key
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                return resultSet.next();
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Could not check MySQL key '" +
                            key +
                            "'."
            );

            plugin.getLogger().severe(
                    exception.getMessage()
            );

            return false;
        }
    }

    public synchronized void remove(
            String key
    ) {

        if (!isValidKey(key)) {
            return;
        }

        if (!ensureConnection()) {
            return;
        }

        String sql =
                "DELETE FROM `" +
                        table +
                        "` " +
                        "WHERE `data_key` = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    key
            );

            statement.executeUpdate();

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Could not remove MySQL key '" +
                            key +
                            "'."
            );

            plugin.getLogger().severe(
                    exception.getMessage()
            );
        }
    }

    public synchronized Map<String, String> getAll() {

        if (!ensureConnection()) {
            return Collections.emptyMap();
        }

        String sql =
                "SELECT `data_key`, `data_value` " +
                        "FROM `" +
                        table +
                        "`";

        Map<String, String> values =
                new HashMap<>();

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql);

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            while (resultSet.next()) {

                values.put(
                        resultSet.getString(
                                "data_key"
                        ),
                        resultSet.getString(
                                "data_value"
                        )
                );
            }

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Could not load all MySQL storage values."
            );

            plugin.getLogger().severe(
                    exception.getMessage()
            );
        }

        return Collections.unmodifiableMap(
                values
        );
    }

    public synchronized void clear() {

        if (!ensureConnection()) {
            return;
        }

        String sql =
                "DELETE FROM `" +
                        table +
                        "`";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.executeUpdate();

        } catch (SQLException exception) {

            plugin.getLogger().severe(
                    "Could not clear MySQL storage."
            );

            plugin.getLogger().severe(
                    exception.getMessage()
            );
        }
    }

    public synchronized boolean isConnected() {

        if (connection == null) {
            return false;
        }

        try {

            return !connection.isClosed() &&
                    connection.isValid(2);

        } catch (SQLException exception) {

            return false;
        }
    }

    private synchronized boolean ensureConnection() {

        if (isConnected()) {
            return true;
        }

        plugin.getLogger().warning(
                "MySQL connection was lost. Attempting to reconnect."
        );

        return connect();
    }

    public synchronized void close() {

        if (connection == null) {
            return;
        }

        try {

            if (!connection.isClosed()) {
                connection.close();
            }

        } catch (SQLException exception) {

            plugin.getLogger().warning(
                    "An error occurred while closing the MySQL connection."
            );

            plugin.getLogger().warning(
                    exception.getMessage()
            );

        } finally {

            connection = null;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getTable() {
        return table;
    }

    private boolean isValidKey(
            String key
    ) {

        return key != null &&
                !key.isBlank();
    }
}