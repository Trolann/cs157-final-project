package libraryapp;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryDatabase extends SQLiteWrapper {

    public LibraryDatabase() {
        super("library.db");
    }

    public LibraryDatabase(String dbPath) {
        super(dbPath);
    }

    public void initializeDatabase() throws SQLException {
        connect();

        // Create categories table
        execute(
                "CREATE TABLE IF NOT EXISTS Categories (" +
                        "category_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "description TEXT)"
        );

        // Create authors table
        execute(
                "CREATE TABLE IF NOT EXISTS Authors (" +
                        "author_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "last_name TEXT NOT NULL, " +
                        "first_name TEXT NOT NULL, " +
                        "birth_year INTEGER, " +
                        "biography TEXT)"
        );

        // Create books table
        execute(
                "CREATE TABLE IF NOT EXISTS Books (" +
                        "book_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "title TEXT NOT NULL, " +
                        "isbn TEXT UNIQUE, " +
                        "publication_year INTEGER, " +
                        "publisher TEXT, " +
                        "total_copies INTEGER DEFAULT 1, " +
                        "available_copies INTEGER DEFAULT 1, " +
                        "category_id INTEGER, " +
                        "FOREIGN KEY (category_id) REFERENCES Categories(category_id))"
        );

        // Create book-authors relationship table
        execute(
                "CREATE TABLE IF NOT EXISTS BookAuthors (" +
                        "book_id INTEGER, " +
                        "author_id INTEGER, " +
                        "PRIMARY KEY (book_id, author_id), " +
                        "FOREIGN KEY (book_id) REFERENCES Books(book_id), " +
                        "FOREIGN KEY (author_id) REFERENCES Authors(author_id))"
        );

        // Create borrowers table
        execute(
                "CREATE TABLE IF NOT EXISTS Borrowers (" +
                        "card_number INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "last_name TEXT NOT NULL, " +
                        "first_name TEXT NOT NULL, " +
                        "address TEXT, " +
                        "phone TEXT, " +
                        "email TEXT UNIQUE, " +
                        "registration_date TEXT DEFAULT CURRENT_TIMESTAMP)"
        );

        // Create reservations table
        execute(
                "CREATE TABLE IF NOT EXISTS Reservations (" +
                        "reservation_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "book_id INTEGER, " +
                        "borrower_id INTEGER, " +
                        "checkout_date TEXT DEFAULT CURRENT_TIMESTAMP, " +
                        "due_date TEXT, " +
                        "return_date TEXT, " +
                        "status TEXT DEFAULT 'active', " +
                        "FOREIGN KEY (book_id) REFERENCES Books(book_id), " +
                        "FOREIGN KEY (borrower_id) REFERENCES Borrowers(card_number))"
        );

        System.out.println("Library database initialized successfully.");
    }

    public void seedDatabase() throws SQLException {
        // First check if data already exists to avoid duplicate entries
        if (isDataAlreadySeeded()) {
            System.out.println("Database already has seed data. Skipping seeding process.");
            return;
        }

        // Seed categories
        Map<String, Integer> categories = seedCategories();

        // Seed authors
        Map<String, Integer> authors = seedAuthors();

        // Seed books and book-author relationships
        seedBooks(categories, authors);

        // Seed borrowers
        seedBorrowers();

        System.out.println("Database seeded successfully.");
    }

    private boolean isDataAlreadySeeded() throws SQLException {
        List<Map<String, Object>> results = queryMultiple("SELECT COUNT(*) as count FROM Books");
        if (!results.isEmpty()) {
            Map<String, Object> row = results.get(0);
            return ((Number) row.get("count")).intValue() > 0;
        }
        return false;
    }

    private Map<String, Integer> seedCategories() throws SQLException {
        String[] categoryNames = {
                "Fiction", "Science Fiction", "Mystery", "Biography",
                "History", "Fantasy", "Romance", "Science",
                "Technology", "Self-Help", "Philosophy", "Poetry"
        };

        String[] descriptions = {
                "General fictional works", "Science-based speculative fiction",
                "Crime and detective stories", "Accounts of real people's lives",
                "Studies of past events", "Imaginative and magical content",
                "Love and relationship stories", "Studies of natural phenomena",
                "Studies of practical arts and applied sciences", "Personal improvement guides",
                "Theoretical studies of fundamental questions", "Literary works in verse"
        };

        Map<String, Integer> categoryMap = new HashMap<>();

        for (int i = 0; i < categoryNames.length; i++) {
            execute("INSERT INTO Categories (name, description) VALUES (?, ?)",
                    categoryNames[i], descriptions[i]);

            Map<String, Object> result = querySingle(
                    "SELECT category_id FROM Categories WHERE name = ?", categoryNames[i]);

            if (result != null) {
                categoryMap.put(categoryNames[i], ((Number) result.get("category_id")).intValue());
            }
        }

        return categoryMap;
    }

    private Map<String, Integer> seedAuthors() throws SQLException {
        String[][] authorData = {
                {"Rowling", "J.K.", "1965", "British author best known for the Harry Potter series."},
                {"Tolkien", "J.R.R.", "1892", "English writer, poet, and academic, best known for The Lord of the Rings."},
                {"Austen", "Jane", "1775", "English novelist known for works like Pride and Prejudice."},
                {"King", "Stephen", "1947", "American author of horror, supernatural fiction, and fantasy."},
                {"Christie", "Agatha", "1890", "English writer known for detective novels."},
                {"Orwell", "George", "1903", "English novelist and essayist."},
                {"Hemingway", "Ernest", "1899", "American novelist and short story writer."},
                {"García Márquez", "Gabriel", "1927", "Colombian novelist known for magical realism."},
                {"Dostoevsky", "Fyodor", "1821", "Russian novelist and philosopher."},
                {"Dickens", "Charles", "1812", "English writer of Victorian literature."},
                {"Woolf", "Virginia", "1882", "English writer and modernist."},
                {"Fitzgerald", "F. Scott", "1896", "American novelist known for The Great Gatsby."},
                {"Shelley", "Mary", "1797", "English novelist who wrote Frankenstein."},
                {"Wells", "H.G.", "1866", "English writer known for science fiction works."},
                {"Steinbeck", "John", "1902", "American author who won the Nobel Prize for Literature."},
                {"Morrison", "Toni", "1931", "American novelist and Nobel Prize winner."},
                {"Brontë", "Charlotte", "1816", "English novelist and poet, wrote Jane Eyre."},
                {"Stoker", "Bram", "1847", "Irish author known for the gothic novel Dracula."},
                {"Dumas", "Alexandre", "1802", "French writer known for historical novels."},
                {"Lee", "Harper", "1926", "American novelist known for To Kill a Mockingbird."},
                {"Tolstoy", "Leo", "1828", "Russian writer regarded as one of the greatest authors."},
                {"Twain", "Mark", "1835", "American writer, humorist, and lecturer."},
                {"Hugo", "Victor", "1802", "French poet, novelist, and dramatist."},
                {"Wilde", "Oscar", "1854", "Irish poet and playwright."},
                {"Bradbury", "Ray", "1920", "American author known for science fiction and fantasy."},
                {"Gaiman", "Neil", "1960", "English author of short fiction, novels, comic books, and films."},
                {"Pratchett", "Terry", "1948", "English humorist, satirist, and author of fantasy novels."}
        };

        Map<String, Integer> authorMap = new HashMap<>();

        for (String[] author : authorData) {
            execute("INSERT INTO Authors (last_name, first_name, birth_year, biography) VALUES (?, ?, ?, ?)",
                    author[0], author[1], Integer.parseInt(author[2]), author[3]);

            Map<String, Object> result = querySingle(
                    "SELECT author_id FROM Authors WHERE last_name = ? AND first_name = ?",
                    author[0], author[1]);

            if (result != null) {
                authorMap.put(author[1] + " " + author[0], ((Number) result.get("author_id")).intValue());
            }
        }

        return authorMap;
    }

    private void seedBooks(Map<String, Integer> categories, Map<String, Integer> authors) throws SQLException {
        Object[][] bookData = {
                {
                        "To Kill a Mockingbird", "9780061120084", 1960, "HarperCollins", 10, 10,
                        "Fiction", new String[]{"Harper Lee"}
                },
                {
                        "1984", "9780451524935", 1949, "Penguin Books", 8, 7,
                        "Fiction", new String[]{"George Orwell"}
                },
                {
                        "The Great Gatsby", "9780743273565", 1925, "Scribner", 6, 6,
                        "Fiction", new String[]{"F. Scott Fitzgerald"}
                },
                {
                        "One Hundred Years of Solitude", "9780060883287", 1967, "Harper & Row", 5, 4,
                        "Fiction", new String[]{"Gabriel García Márquez"}
                },
                {
                        "Harry Potter and the Philosopher's Stone", "9780747532743", 1997, "Bloomsbury", 12, 10,
                        "Fantasy", new String[]{"J.K. Rowling"}
                },
                {
                        "The Lord of the Rings", "9780618640157", 1954, "Allen & Unwin", 7, 6,
                        "Fantasy", new String[]{"J.R.R. Tolkien"}
                },
                {
                        "Pride and Prejudice", "9780141439518", 1813, "T. Egerton", 8, 7,
                        "Romance", new String[]{"Jane Austen"}
                },
                {
                        "The Shining", "9780307743657", 1977, "Doubleday", 6, 5,
                        "Fiction", new String[]{"Stephen King"}
                },
                {
                        "Murder on the Orient Express", "9780062693662", 1934, "Collins Crime Club", 9, 8,
                        "Mystery", new String[]{"Agatha Christie"}
                },
                {
                        "Frankenstein", "9780486282114", 1818, "Lackington, Hughes", 7, 7,
                        "Science Fiction", new String[]{"Mary Shelley"}
                },
                {
                        "The Old Man and the Sea", "9780684801223", 1952, "Charles Scribner's Sons", 8, 7,
                        "Fiction", new String[]{"Ernest Hemingway"}
                },
                {
                        "Crime and Punishment", "9780486415871", 1866, "The Russian Messenger", 5, 4,
                        "Fiction", new String[]{"Fyodor Dostoevsky"}
                },
                {
                        "Oliver Twist", "9780141439747", 1838, "Richard Bentley", 6, 5,
                        "Fiction", new String[]{"Charles Dickens"}
                },
                {
                        "The Time Machine", "9780451470702", 1895, "William Heinemann", 7, 6,
                        "Science Fiction", new String[]{"H.G. Wells"}
                },
                {
                        "Dracula", "9780486411095", 1897, "Archibald Constable and Company", 8, 7,
                        "Fiction", new String[]{"Bram Stoker"}
                },
                {
                        "The Three Musketeers", "9780140367470", 1844, "Baudry", 6, 5,
                        "Fiction", new String[]{"Alexandre Dumas"}
                },
                {
                        "War and Peace", "9781400079988", 1869, "The Russian Messenger", 4, 4,
                        "Fiction", new String[]{"Leo Tolstoy"}
                },
                {
                        "The Adventures of Tom Sawyer", "9780486400778", 1876, "American Publishing Company", 7, 7,
                        "Fiction", new String[]{"Mark Twain"}
                },
                {
                        "Les Misérables", "9780451419439", 1862, "A. Lacroix, Verboeckhoven & Cie", 5, 4,
                        "Fiction", new String[]{"Victor Hugo"}
                },
                {
                        "The Picture of Dorian Gray", "9780141439570", 1890, "Ward, Lock and Company", 6, 6,
                        "Fiction", new String[]{"Oscar Wilde"}
                },
                {
                        "Good Omens", "9780060853983", 1990, "Workman Publishing", 8, 7,
                        "Fantasy", new String[]{"Terry Pratchett", "Neil Gaiman"}
                },
                {
                        "American Gods", "9780062572110", 2001, "William Morrow", 7, 6,
                        "Fantasy", new String[]{"Neil Gaiman"}
                }
        };

        for (Object[] book : bookData) {
            String categoryName = (String) book[6];

            // Check if category exists, if not add it
            if (!categories.containsKey(categoryName)) {
                execute("INSERT INTO Categories (name, description) VALUES (?, ?)",
                        categoryName, categoryName + " books");

                Map<String, Object> result = querySingle(
                        "SELECT category_id FROM Categories WHERE name = ?", categoryName);

                if (result != null) {
                    categories.put(categoryName, ((Number) result.get("category_id")).intValue());
                }
            }

            int categoryId = categories.get(categoryName);

            // Insert book
            execute("INSERT INTO Books (title, isbn, publication_year, publisher, total_copies, available_copies, category_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    book[0], book[1], book[2], book[3], book[4], book[5], categoryId);

            // Get the book ID
            Map<String, Object> bookResult = querySingle(
                    "SELECT book_id FROM Books WHERE isbn = ?", book[1]);

            if (bookResult != null) {
                int bookId = ((Number) bookResult.get("book_id")).intValue();

                // Add book-author relationships
                String[] bookAuthors = (String[]) book[7];
                for (String authorName : bookAuthors) {
                    if (authors.containsKey(authorName)) {
                        int authorId = authors.get(authorName);
                        execute("INSERT INTO BookAuthors (book_id, author_id) VALUES (?, ?)",
                                bookId, authorId);
                    }
                }
            }
        }
    }

    private void seedBorrowers() throws SQLException {
        Object[][] borrowerData = {
                {"Smith", "John", "123 Main St, Anytown", "555-1234", "john.smith@email.com", "2023-01-15"},
                {"Johnson", "Emma", "456 Oak Ave, Somecity", "555-2345", "emma.j@email.com", "2023-02-20"},
                {"Williams", "Michael", "789 Pine Rd, Othertown", "555-3456", "m.williams@email.com", "2023-03-10"},
                {"Brown", "Sophia", "321 Elm St, Anytown", "555-4567", "sophia.b@email.com", "2023-04-05"},
                {"Jones", "William", "654 Maple Dr, Somecity", "555-5678", "w.jones@email.com", "2023-05-12"},
                {"Miller", "Olivia", "987 Cedar Ln, Othertown", "555-6789", "olivia.m@email.com", "2023-06-18"},
                {"Davis", "James", "741 Birch Rd, Anytown", "555-7890", "james.d@email.com", "2023-07-22"},
                {"García", "Sofia", "852 Walnut Ave, Somecity", "555-8901", "sofia.g@email.com", "2023-08-30"},
                {"Rodriguez", "Daniel", "963 Spruce St, Othertown", "555-9012", "daniel.r@email.com", "2023-09-14"},
                {"Wilson", "Ava", "159 Poplar Dr, Anytown", "555-0123", "ava.w@email.com", "2023-10-27"}
        };

        for (Object[] borrower : borrowerData) {
            execute("INSERT INTO Borrowers (last_name, first_name, address, phone, email, registration_date) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    borrower[0], borrower[1], borrower[2], borrower[3], borrower[4], borrower[5]);
        }
    }
    
    // ========== Book Operations ==========
    
    /**
     * Search for books based on a search term.
     * Searches in title, ISBN, and author name.
     * 
     * @param searchTerm The term to search for
     * @return List of books matching the search term
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> searchBooks(String searchTerm) throws SQLException {
        String sql = "SELECT b.book_id, b.title, b.isbn, b.publication_year, b.publisher, " +
                "b.available_copies, b.total_copies, c.name as category, " +
                "GROUP_CONCAT(a.first_name || ' ' || a.last_name, ', ') as authors " +
                "FROM Books b " +
                "LEFT JOIN Categories c ON b.category_id = c.category_id " +
                "LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id " +
                "LEFT JOIN Authors a ON ba.author_id = a.author_id " +
                "WHERE b.title LIKE ? OR b.isbn LIKE ? " +
                "OR EXISTS (SELECT 1 FROM Authors a2 JOIN BookAuthors ba2 ON a2.author_id = ba2.author_id " +
                "WHERE ba2.book_id = b.book_id AND (a2.first_name LIKE ? OR a2.last_name LIKE ?)) " +
                "GROUP BY b.book_id";
        
        String likePattern = "%" + searchTerm + "%";
        return queryMultiple(sql, likePattern, likePattern, likePattern, likePattern);
    }
    
    /**
     * Get all books in the library.
     * 
     * @return List of all books
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllBooks() throws SQLException {
        String sql = "SELECT b.book_id, b.title, b.isbn, b.publication_year, b.publisher, " +
                "b.available_copies, b.total_copies, c.name as category, " +
                "GROUP_CONCAT(a.first_name || ' ' || a.last_name, ', ') as authors " +
                "FROM Books b " +
                "LEFT JOIN Categories c ON b.category_id = c.category_id " +
                "LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id " +
                "LEFT JOIN Authors a ON ba.author_id = a.author_id " +
                "GROUP BY b.book_id";
        
        return queryMultiple(sql);
    }
    
    /**
     * Get a book by its ID.
     * 
     * @param bookId The ID of the book to retrieve
     * @return The book data or null if not found
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getBookById(int bookId) throws SQLException {
        String sql = "SELECT b.book_id, b.title, b.isbn, b.publication_year, b.publisher, " +
                "b.available_copies, b.total_copies, c.name as category, " +
                "GROUP_CONCAT(a.first_name || ' ' || a.last_name, ', ') as authors " +
                "FROM Books b " +
                "LEFT JOIN Categories c ON b.category_id = c.category_id " +
                "LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id " +
                "LEFT JOIN Authors a ON ba.author_id = a.author_id " +
                "WHERE b.book_id = ? " +
                "GROUP BY b.book_id";
        
        return querySingle(sql, bookId);
    }
    
    /**
     * Add a new book to the library.
     * 
     * @param title Book title
     * @param isbn ISBN number
     * @param publicationYear Year of publication
     * @param publisher Publisher name
     * @param totalCopies Total number of copies
     * @param categoryId Category ID
     * @param authorIds List of author IDs
     * @return The ID of the newly created book
     * @throws SQLException If a database error occurs
     */
    public int addBook(String title, String isbn, int publicationYear, String publisher, 
                      int totalCopies, int categoryId, List<Integer> authorIds) throws SQLException {
        // Insert the book
        execute("INSERT INTO Books (title, isbn, publication_year, publisher, total_copies, available_copies, category_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                title, isbn, publicationYear, publisher, totalCopies, totalCopies, categoryId);
        
        // Get the book ID
        Map<String, Object> result = querySingle("SELECT book_id FROM Books WHERE isbn = ?", isbn);
        int bookId = ((Number) result.get("book_id")).intValue();
        
        // Add book-author relationships
        for (int authorId : authorIds) {
            execute("INSERT INTO BookAuthors (book_id, author_id) VALUES (?, ?)", bookId, authorId);
        }
        
        return bookId;
    }
    
    /**
     * Update an existing book's information.
     * 
     * @param bookId Book ID
     * @param title Book title
     * @param isbn ISBN number
     * @param publicationYear Year of publication
     * @param publisher Publisher name
     * @param totalCopies Total number of copies
     * @param categoryId Category ID
     * @param authorIds List of author IDs
     * @throws SQLException If a database error occurs
     */
    public void updateBook(int bookId, String title, String isbn, int publicationYear, 
                          String publisher, int totalCopies, int categoryId, 
                          List<Integer> authorIds) throws SQLException {
        // Get current available copies to calculate the difference
        Map<String, Object> currentBook = querySingle(
                "SELECT total_copies, available_copies FROM Books WHERE book_id = ?", bookId);
        
        if (currentBook == null) {
            throw new SQLException("Book with ID " + bookId + " not found");
        }
        
        int currentTotal = ((Number) currentBook.get("total_copies")).intValue();
        int currentAvailable = ((Number) currentBook.get("available_copies")).intValue();
        
        // Calculate new available copies (maintain the same number of borrowed books)
        int borrowedCopies = currentTotal - currentAvailable;
        int newAvailable = Math.max(0, totalCopies - borrowedCopies);
        
        // Update the book
        execute("UPDATE Books SET title = ?, isbn = ?, publication_year = ?, " +
                "publisher = ?, total_copies = ?, available_copies = ?, category_id = ? " +
                "WHERE book_id = ?",
                title, isbn, publicationYear, publisher, totalCopies, newAvailable, categoryId, bookId);
        
        // Only update author relationships if authorIds is not empty
        if (!authorIds.isEmpty()) {
            // Update book-author relationships
            // First, remove all existing relationships
            execute("DELETE FROM BookAuthors WHERE book_id = ?", bookId);
            
            // Then add the new relationships
            for (int authorId : authorIds) {
                execute("INSERT INTO BookAuthors (book_id, author_id) VALUES (?, ?)", bookId, authorId);
            }
        }
    }
    
    // ========== Author Operations ==========
    
    /**
     * Search for authors based on a search term.
     * 
     * @param searchTerm The term to search for
     * @return List of authors matching the search term
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> searchAuthors(String searchTerm) throws SQLException {
        String sql = "SELECT author_id, first_name, last_name, birth_year, biography " +
                "FROM Authors " +
                "WHERE first_name LIKE ? OR last_name LIKE ?";
        
        String likePattern = "%" + searchTerm + "%";
        return queryMultiple(sql, likePattern, likePattern);
    }
    
    /**
     * Get all authors in the library.
     * 
     * @return List of all authors
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllAuthors() throws SQLException {
        String sql = "SELECT author_id, first_name, last_name, birth_year, biography FROM Authors";
        return queryMultiple(sql);
    }
    
    /**
     * Get an author by ID.
     * 
     * @param authorId The ID of the author to retrieve
     * @return The author data or null if not found
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getAuthorById(int authorId) throws SQLException {
        String sql = "SELECT author_id, first_name, last_name, birth_year, biography " +
                "FROM Authors WHERE author_id = ?";
        return querySingle(sql, authorId);
    }
    
    /**
     * Add a new author to the library.
     * 
     * @param firstName Author's first name
     * @param lastName Author's last name
     * @param birthYear Author's birth year
     * @param biography Author's biography
     * @return The ID of the newly created author
     * @throws SQLException If a database error occurs
     */
    public int addAuthor(String firstName, String lastName, int birthYear, String biography) throws SQLException {
        execute("INSERT INTO Authors (first_name, last_name, birth_year, biography) VALUES (?, ?, ?, ?)",
                firstName, lastName, birthYear, biography);
        
        Map<String, Object> result = querySingle(
                "SELECT author_id FROM Authors WHERE first_name = ? AND last_name = ?",
                firstName, lastName);
        
        return ((Number) result.get("author_id")).intValue();
    }
    
    /**
     * Update an existing author's information.
     * 
     * @param authorId Author's ID
     * @param firstName Author's first name
     * @param lastName Author's last name
     * @param birthYear Author's birth year
     * @param biography Author's biography
     * @throws SQLException If a database error occurs
     */
    public void updateAuthor(int authorId, String firstName, String lastName, 
                            int birthYear, String biography) throws SQLException {
        execute("UPDATE Authors SET first_name = ?, last_name = ?, birth_year = ?, biography = ? " +
                "WHERE author_id = ?",
                firstName, lastName, birthYear, biography, authorId);
    }
    
    // ========== Category Operations ==========
    
    /**
     * Search for categories based on a search term.
     * 
     * @param searchTerm The term to search for
     * @return List of categories matching the search term
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> searchCategories(String searchTerm) throws SQLException {
        String sql = "SELECT category_id, name, description FROM Categories WHERE name LIKE ?";
        return queryMultiple(sql, "%" + searchTerm + "%");
    }
    
    /**
     * Get all categories in the library.
     * 
     * @return List of all categories
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllCategories() throws SQLException {
        String sql = "SELECT category_id, name, description FROM Categories";
        return queryMultiple(sql);
    }
    
    /**
     * Get a category by ID.
     * 
     * @param categoryId The ID of the category to retrieve
     * @return The category data or null if not found
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getCategoryById(int categoryId) throws SQLException {
        String sql = "SELECT category_id, name, description FROM Categories WHERE category_id = ?";
        return querySingle(sql, categoryId);
    }
    
    /**
     * Add a new category to the library.
     * 
     * @param name Category name
     * @param description Category description
     * @return The ID of the newly created category
     * @throws SQLException If a database error occurs
     */
    public int addCategory(String name, String description) throws SQLException {
        execute("INSERT INTO Categories (name, description) VALUES (?, ?)", name, description);
        
        Map<String, Object> result = querySingle("SELECT category_id FROM Categories WHERE name = ?", name);
        return ((Number) result.get("category_id")).intValue();
    }
    
    /**
     * Update an existing category's information.
     * 
     * @param categoryId Category ID
     * @param name Category name
     * @param description Category description
     * @throws SQLException If a database error occurs
     */
    public void updateCategory(int categoryId, String name, String description) throws SQLException {
        execute("UPDATE Categories SET name = ?, description = ? WHERE category_id = ?",
                name, description, categoryId);
    }
    
    // ========== Borrower Operations ==========
    
    /**
     * Search for borrowers based on a search term.
     * 
     * @param searchTerm The term to search for
     * @return List of borrowers matching the search term
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> searchBorrowers(String searchTerm) throws SQLException {
        String sql = "SELECT card_number, first_name, last_name, address, phone, email, registration_date " +
                "FROM Borrowers " +
                "WHERE first_name LIKE ? OR last_name LIKE ? OR email LIKE ? OR phone LIKE ?";
        
        String likePattern = "%" + searchTerm + "%";
        return queryMultiple(sql, likePattern, likePattern, likePattern, likePattern);
    }
    
    /**
     * Get all borrowers in the library.
     * 
     * @return List of all borrowers
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getAllBorrowers() throws SQLException {
        String sql = "SELECT card_number, first_name, last_name, address, phone, email, registration_date FROM Borrowers";
        return queryMultiple(sql);
    }
    
    /**
     * Get a borrower by ID.
     * 
     * @param cardNumber The card number of the borrower to retrieve
     * @return The borrower data or null if not found
     * @throws SQLException If a database error occurs
     */
    public Map<String, Object> getBorrowerById(int cardNumber) throws SQLException {
        String sql = "SELECT card_number, first_name, last_name, address, phone, email, registration_date " +
                "FROM Borrowers WHERE card_number = ?";
        return querySingle(sql, cardNumber);
    }
    
    /**
     * Add a new borrower to the library.
     * 
     * @param firstName Borrower's first name
     * @param lastName Borrower's last name
     * @param address Borrower's address
     * @param phone Borrower's phone number
     * @param email Borrower's email
     * @return The card number of the newly created borrower
     * @throws SQLException If a database error occurs
     */
    public int addBorrower(String firstName, String lastName, String address, String phone, String email) throws SQLException {
        execute("INSERT INTO Borrowers (first_name, last_name, address, phone, email) " +
                "VALUES (?, ?, ?, ?, ?)",
                firstName, lastName, address, phone, email);
        
        Map<String, Object> result = querySingle("SELECT card_number FROM Borrowers WHERE email = ?", email);
        return ((Number) result.get("card_number")).intValue();
    }
    
    /**
     * Update an existing borrower's information.
     * 
     * @param cardNumber Borrower's card number
     * @param firstName Borrower's first name
     * @param lastName Borrower's last name
     * @param address Borrower's address
     * @param phone Borrower's phone number
     * @param email Borrower's email
     * @throws SQLException If a database error occurs
     */
    public void updateBorrower(int cardNumber, String firstName, String lastName, 
                              String address, String phone, String email) throws SQLException {
        execute("UPDATE Borrowers SET first_name = ?, last_name = ?, address = ?, phone = ?, email = ? " +
                "WHERE card_number = ?",
                firstName, lastName, address, phone, email, cardNumber);
    }
    
    // ========== Borrowing Operations ==========
    
    /**
     * Borrow a book for a borrower.
     * 
     * @param bookId The ID of the book to borrow
     * @param borrowerId The ID of the borrower
     * @param dueDate The due date for the book
     * @return true if successful, false if the book is not available
     * @throws SQLException If a database error occurs
     */
    public boolean borrowBook(int bookId, int borrowerId, String dueDate) throws SQLException {
        // Check if the book is available
        Map<String, Object> book = querySingle("SELECT available_copies FROM Books WHERE book_id = ?", bookId);
        if (book == null || ((Number) book.get("available_copies")).intValue() <= 0) {
            return false;
        }
        
        // Create a reservation
        execute("INSERT INTO Reservations (book_id, borrower_id, checkout_date, due_date, status) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, ?, 'active')",
                bookId, borrowerId, dueDate);
        
        // Update available copies
        execute("UPDATE Books SET available_copies = available_copies - 1 WHERE book_id = ?", bookId);
        
        return true;
    }
    
    /**
     * Return a borrowed book.
     * 
     * @param reservationId The ID of the reservation to return
     * @return true if successful, false if the reservation doesn't exist
     * @throws SQLException If a database error occurs
     */
    public boolean returnBook(int reservationId) throws SQLException {
        // Get the reservation
        Map<String, Object> reservation = querySingle(
                "SELECT book_id, status FROM Reservations WHERE reservation_id = ?", reservationId);
        
        if (reservation == null || !"active".equals(reservation.get("status"))) {
            return false;
        }
        
        int bookId = ((Number) reservation.get("book_id")).intValue();
        
        // Update the reservation
        execute("UPDATE Reservations SET return_date = CURRENT_TIMESTAMP, status = 'returned' " +
                "WHERE reservation_id = ?", reservationId);
        
        // Update available copies
        execute("UPDATE Books SET available_copies = available_copies + 1 WHERE book_id = ?", bookId);
        
        return true;
    }
    
    /**
     * Get all active loans for a borrower.
     * 
     * @param borrowerId The ID of the borrower
     * @return List of active loans
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getBorrowerActiveLoans(int borrowerId) throws SQLException {
        String sql = "SELECT r.reservation_id, r.checkout_date, r.due_date, " +
                "b.book_id, b.title, b.isbn, " +
                "GROUP_CONCAT(a.first_name || ' ' || a.last_name, ', ') as authors " +
                "FROM Reservations r " +
                "JOIN Books b ON r.book_id = b.book_id " +
                "LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id " +
                "LEFT JOIN Authors a ON ba.author_id = a.author_id " +
                "WHERE r.borrower_id = ? AND r.status = 'active' " +
                "GROUP BY r.reservation_id";
        
        return queryMultiple(sql, borrowerId);
    }
    
    /**
     * Get borrowing history for a borrower.
     * 
     * @param borrowerId The ID of the borrower
     * @return List of all loans (active and returned)
     * @throws SQLException If a database error occurs
     */
    public List<Map<String, Object>> getBorrowerHistory(int borrowerId) throws SQLException {
        String sql = "SELECT r.reservation_id, r.checkout_date, r.due_date, r.return_date, r.status, " +
                "b.book_id, b.title, b.isbn, " +
                "GROUP_CONCAT(a.first_name || ' ' || a.last_name, ', ') as authors " +
                "FROM Reservations r " +
                "JOIN Books b ON r.book_id = b.book_id " +
                "LEFT JOIN BookAuthors ba ON b.book_id = ba.book_id " +
                "LEFT JOIN Authors a ON ba.author_id = a.author_id " +
                "WHERE r.borrower_id = ? " +
                "GROUP BY r.reservation_id " +
                "ORDER BY r.checkout_date DESC";
        
        return queryMultiple(sql, borrowerId);
    }
    
    /**
     * Delete a book from the library.
     * Only allows deletion if no active loans exist for this book.
     * 
     * @param bookId The ID of the book to delete
     * @return true if successful, false if the book has active loans
     * @throws SQLException If a database error occurs
     */
    public boolean deleteBook(int bookId) throws SQLException {
        // Check if the book has any active loans
        Map<String, Object> activeLoans = querySingle(
                "SELECT COUNT(*) as count FROM Reservations WHERE book_id = ? AND status = 'active'", 
                bookId);
        
        if (activeLoans != null && ((Number) activeLoans.get("count")).intValue() > 0) {
            return false; // Cannot delete a book with active loans
        }
        
        // Delete book-author relationships
        execute("DELETE FROM BookAuthors WHERE book_id = ?", bookId);
        
        // Delete reservation history (if allowed by business rules)
        execute("DELETE FROM Reservations WHERE book_id = ?", bookId);
        
        // Delete the book
        execute("DELETE FROM Books WHERE book_id = ?", bookId);
        
        return true;
    }
    
    /**
     * Delete an author from the library.
     * Only allows deletion if no books are associated with this author.
     * 
     * @param authorId The ID of the author to delete
     * @return true if successful, false if the author has associated books
     * @throws SQLException If a database error occurs
     */
    public boolean deleteAuthor(int authorId) throws SQLException {
        // Check if the author has any books
        Map<String, Object> associatedBooks = querySingle(
                "SELECT COUNT(*) as count FROM BookAuthors WHERE author_id = ?", 
                authorId);
        
        if (associatedBooks != null && ((Number) associatedBooks.get("count")).intValue() > 0) {
            return false; // Cannot delete an author with associated books
        }
        
        // Delete the author
        execute("DELETE FROM Authors WHERE author_id = ?", authorId);
        
        return true;
    }
    
    /**
     * Delete a category from the library.
     * Only allows deletion if no books are in this category.
     * 
     * @param categoryId The ID of the category to delete
     * @return true if successful, false if the category has books
     * @throws SQLException If a database error occurs
     */
    public boolean deleteCategory(int categoryId) throws SQLException {
        // Check if the category has any books
        Map<String, Object> associatedBooks = querySingle(
                "SELECT COUNT(*) as count FROM Books WHERE category_id = ?", 
                categoryId);
        
        if (associatedBooks != null && ((Number) associatedBooks.get("count")).intValue() > 0) {
            return false; // Cannot delete a category with associated books
        }
        
        // Delete the category
        execute("DELETE FROM Categories WHERE category_id = ?", categoryId);
        
        return true;
    }
    
    /**
     * Delete a borrower from the library.
     * Only allows deletion if the borrower has no active loans.
     * 
     * @param borrowerId The ID of the borrower to delete
     * @return true if successful, false if the borrower has active loans
     * @throws SQLException If a database error occurs
     */
    public boolean deleteBorrower(int borrowerId) throws SQLException {
        // Check if the borrower has any active loans
        Map<String, Object> activeLoans = querySingle(
                "SELECT COUNT(*) as count FROM Reservations WHERE borrower_id = ? AND status = 'active'", 
                borrowerId);
        
        if (activeLoans != null && ((Number) activeLoans.get("count")).intValue() > 0) {
            return false; // Cannot delete a borrower with active loans
        }
        
        // Update reservation history to anonymize the borrower (if required by privacy rules)
        // Alternatively, you could delete the reservation history
        // This depends on your business requirements
        
        // Delete the borrower
        execute("DELETE FROM Borrowers WHERE card_number = ?", borrowerId);
        
        return true;
    }
}
