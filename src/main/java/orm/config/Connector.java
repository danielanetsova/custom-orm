package orm.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class Connector {
    private static Connection connection;
    private static final String URL_CONNECTION = "jdbc:mysql://localhost:3306/%s";
    public static void createConnection(String username, String password, String dbName) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);

        connection = DriverManager.getConnection(
                String.format(URL_CONNECTION, dbName),
                properties);
    }
    public static Connection getConnection() {
        return connection;
    }
}
