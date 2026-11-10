import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/** Central MySQL configuration. Values come from config.properties, JVM properties, or environment variables. */
public final class DbConfig {
    private static final Properties FILE_PROPERTIES = new Properties();
    private static boolean loaded;

    private DbConfig() { }

    private static synchronized void loadProperties() {
        if (loaded) return;
        Path path = Path.of("config.properties");
        if (Files.isRegularFile(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                FILE_PROPERTIES.load(input);
            } catch (IOException ex) {
                System.err.println("Could not read config.properties: " + ex.getMessage());
            }
        }
        loaded = true;
    }

    private static String setting(String key, String systemProperty, String env, String fallback) {
        loadProperties();
        String value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) value = System.getenv(env);
        if (value == null || value.isBlank()) value = FILE_PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) value = fallback;
        return value == null ? "" : value.trim();
    }

    public static Connection getConnection() throws SQLException {
        String url = setting("db.url", "inventory.db.url", "INVENTORY_DB_URL",
                "jdbc:mysql://localhost:3306/Inventory?useSSL=false&serverTimezone=Asia/Kolkata");
        String user = setting("db.user", "inventory.db.user", "INVENTORY_DB_USER", "root");
        String password = setting("db.password", "inventory.db.password", "INVENTORY_DB_PASSWORD", "");
        if (password.isBlank()) {
            throw new SQLException("MySQL password is not configured. Copy config.properties.example to config.properties and set db.password, or set INVENTORY_DB_PASSWORD.");
        }
        return DriverManager.getConnection(url, user, password);
    }

    /** Created/upgraded on demand so legacy databases retain their bill records. Call before opening a transaction. */
    public static void ensureBillStockMovementTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS bill_stock_movements ("
                + "bill_id INT NOT NULL, product_id INT NOT NULL, quantity INT NOT NULL, "
                + "inventory_cost DECIMAL(14,2) NULL, is_reversed TINYINT(1) NOT NULL DEFAULT 0, "
                + "PRIMARY KEY (bill_id, product_id)) ENGINE=InnoDB";
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        if (!hasColumn(connection, "bill_stock_movements", "inventory_cost")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE bill_stock_movements ADD COLUMN inventory_cost DECIMAL(14,2) NULL");
            }
        }
        if (!hasColumn(connection, "bill_stock_movements", "is_reversed")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE bill_stock_movements ADD COLUMN is_reversed TINYINT(1) NOT NULL DEFAULT 0");
            }
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (java.sql.ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, table, null)) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) return true;
            }
        }
        return false;
    }
}
