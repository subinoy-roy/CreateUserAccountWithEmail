package org.roy.utils;

import com.amazonaws.services.lambda.runtime.Context;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for MySQL database connection.
 */
public class MySqlUtil {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test_db";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // Method to connect to MySQL
    public Connection connect(String username, String password, Context context) {
        try {
            Class.forName(DRIVER);
            return DriverManager.getConnection(DB_URL, username, password);
        } catch (SQLException | ClassNotFoundException e) {
            context.getLogger().log("Error connecting to MySQL: " + e.getMessage());
            return null;
        }
    }

    // Method to close MySQL connection
    public void closeConnection(Connection connection, Context context) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                context.getLogger().log("Error closing MySQL connection: " + e.getMessage());
            }
        }
    }
}
