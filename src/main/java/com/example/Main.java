package com.example;

import com.example.db.SQLiteWrapper;
import java.sql.SQLException;

/**
 * Main application class that demonstrates the use of the SQLite wrapper.
 */
public class Main {
    public static void main(String[] args) {
        // Database file path
        String dbPath = "app_database.db";
        
        // Create an instance of the SQLite wrapper
        SQLiteWrapper db = new SQLiteWrapper(dbPath);
        
        try {
            // Connect to the database
            db.connect();
            
            // Example: Create a table
            db.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "email TEXT UNIQUE NOT NULL)"
            );
            
            System.out.println("Database initialized successfully.");
            
            // Later, we'll add business logic layer that wraps this wrapper
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Always close the connection when done
                db.disconnect();
            } catch (SQLException e) {
                System.err.println("Error closing database: " + e.getMessage());
            }
        }
    }
}
