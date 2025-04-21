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
}