package com.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A wrapper class for SQLite database operations.
 */
public class SQLiteWrapper {
    private String dbPath;
    private Connection connection;

    /**
     * Initialize the SQLite wrapper with a database path.
     * 
     * @param dbPath Path to the SQLite database file
     */
    public SQLiteWrapper(String dbPath) {
        this.dbPath = dbPath;
    }

    /**
     * Establish a connection to the SQLite database.
     * 
     * @throws SQLException if a database access error occurs
     */
    public void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        }
    }

    /**
     * Close the connection to the SQLite database.
     * 
     * @throws SQLException if a database access error occurs
     */
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Execute a SQL query with no return value.
     * 
     * @param sql SQL query string
     * @param params Parameters for the query
     * @throws SQLException if a database access error occurs
     */
    public void execute(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(sql, params)) {
            stmt.executeUpdate();
        }
    }

    /**
     * Execute a SQL query and return a single result.
     * 
     * @param sql SQL query string
     * @param params Parameters for the query
     * @return A map representing a single row of results
     * @throws SQLException if a database access error occurs
     */
    public Map<String, Object> querySingle(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = prepareStatement(sql, params);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return resultSetRowToMap(rs);
            }
            return null;
        }
    }

    /**
     * Execute a SQL query and return multiple results.
     * 
     * @param sql SQL query string
     * @param params Parameters for the query
     * @return A list of maps, each representing a row of results
     * @throws SQLException if a database access error occurs
     */
    public List<Map<String, Object>> queryMultiple(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (PreparedStatement stmt = prepareStatement(sql, params);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                results.add(resultSetRowToMap(rs));
            }
            return results;
        }
    }

    /**
     * Create a prepared statement with the given parameters.
     * 
     * @param sql SQL query string
     * @param params Parameters for the query
     * @return A prepared statement
     * @throws SQLException if a database access error occurs
     */
    private PreparedStatement prepareStatement(String sql, Object... params) throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        
        PreparedStatement stmt = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt;
    }

    /**
     * Convert a result set row to a map.
     * 
     * @param rs Result set
     * @return A map representing the current row
     * @throws SQLException if a database access error occurs
     */
    private Map<String, Object> resultSetRowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
        int columnCount = rs.getMetaData().getColumnCount();
        
        for (int i = 1; i <= columnCount; i++) {
            String columnName = rs.getMetaData().getColumnName(i);
            Object value = rs.getObject(i);
            row.put(columnName, value);
        }
        
        return row;
    }
}
