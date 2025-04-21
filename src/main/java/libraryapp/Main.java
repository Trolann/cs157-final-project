package libraryapp;

import java.sql.SQLException;
import java.util.Map;

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
            
            // Add a random user
            db.execute(
                    "INSERT INTO users (name, email) VALUES (?, ?)",
                    "John Doe",
                    "john@doe.com"
            );

            // Example: Query the database
            Map<String, Object> user = db.querySingle(
                    "SELECT * FROM users WHERE email LIKE ?",
                    "%john%"
            );
            
            if (user != null) {
                for (Map.Entry<String, Object> entry : user.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
            } else {
                System.out.println("No user found");
            }
            
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
