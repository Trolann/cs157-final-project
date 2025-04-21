package libraryapp;

import java.sql.SQLException;

/**
 * Library database manager that handles database initialization and operations.
 */
public class LibraryDatabase extends SQLiteWrapper {
    
    /**
     * Default constructor that uses the default database name.
     */
    public LibraryDatabase() {
        super("library.db");
    }
    
    /**
     * Constructor with custom database path.
     * 
     * @param dbPath Path to the database file
     */
    public LibraryDatabase(String dbPath) {
        super(dbPath);
    }
    
    /**
     * Initialize the library database with required tables if they don't exist.
     * 
     * @throws SQLException if a database access error occurs
     */
    public void initializeDatabase() throws SQLException {
        connect();
        
        // Create authors table
        execute(
            "CREATE TABLE IF NOT EXISTS authors (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "birth_year INTEGER, " +
            "country TEXT)"
        );
        
        // Create books table
        execute(
            "CREATE TABLE IF NOT EXISTS books (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "title TEXT NOT NULL, " +
            "author_id INTEGER, " +
            "publication_year INTEGER, " +
            "isbn TEXT UNIQUE, " +
            "genre TEXT, " +
            "available BOOLEAN DEFAULT 1, " +
            "FOREIGN KEY (author_id) REFERENCES authors(id))"
        );
        
        // Create borrowers table
        execute(
            "CREATE TABLE IF NOT EXISTS borrowers (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "name TEXT NOT NULL, " +
            "email TEXT UNIQUE, " +
            "phone TEXT, " +
            "registration_date TEXT DEFAULT CURRENT_TIMESTAMP)"
        );
        
        // Create loans table
        execute(
            "CREATE TABLE IF NOT EXISTS loans (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "book_id INTEGER, " +
            "borrower_id INTEGER, " +
            "checkout_date TEXT DEFAULT CURRENT_TIMESTAMP, " +
            "due_date TEXT, " +
            "return_date TEXT, " +
            "FOREIGN KEY (book_id) REFERENCES books(id), " +
            "FOREIGN KEY (borrower_id) REFERENCES borrowers(id))"
        );
        
        System.out.println("Library database initialized successfully.");
    }
}
