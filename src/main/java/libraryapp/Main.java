package libraryapp;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Main application class that demonstrates the use of the Library Database.
 */
public class Main {
    public static void main(String[] args) {
        // Create an instance of the Library Database
        LibraryDatabase db = new LibraryDatabase();
        
        try {
            // Initialize the library database
            db.initializeDatabase();
            
            // Example: Add a sample author
            db.execute(
                "INSERT OR IGNORE INTO authors (name, birth_year, country) VALUES (?, ?, ?)",
                "J.K. Rowling",
                1965,
                "United Kingdom"
            );
            
            // Example: Add a sample book
            db.execute(
                "INSERT OR IGNORE INTO books (title, author_id, publication_year, isbn, genre) " +
                "VALUES (?, (SELECT id FROM authors WHERE name = ?), ?, ?, ?)",
                "Harry Potter and the Philosopher's Stone",
                "J.K. Rowling",
                1997,
                "9780747532743",
                "Fantasy"
            );
            
            // Example: Query the database for books
            List<Map<String, Object>> books = db.queryMultiple(
                "SELECT b.title, a.name as author, b.publication_year, b.genre " +
                "FROM books b JOIN authors a ON b.author_id = a.id"
            );
            
            System.out.println("\nLibrary Books:");
            for (Map<String, Object> book : books) {
                System.out.println("Title: " + book.get("title"));
                System.out.println("Author: " + book.get("author"));
                System.out.println("Year: " + book.get("publication_year"));
                System.out.println("Genre: " + book.get("genre"));
                System.out.println("-------------------");
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
