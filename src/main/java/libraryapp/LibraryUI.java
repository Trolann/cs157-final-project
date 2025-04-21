package libraryapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JavaFX UI for the Library Management System based on the provided mockup.
 * 
 * The UI is structured as follows:
 * - Left panel: Book search and results
 *   - Search field for books by author, category, etc.
 *   - Table showing search results
 *   - If the search box is blank and author is selected, shows all authors
 *   - Same for category, etc.
 *   - Selecting a book and clicking "Borrow" borrows it to the selected borrower
 *     (or errors out if no borrower selected)
 * 
 * - Middle panel: Action buttons for borrowing/returning
 *   - Borrow button: Moves selected book to the borrower
 *   - Return button: Returns selected book from borrower
 * 
 * - Right panel: Borrower information
 *   - Search field for borrowers
 *   - Selected borrower display
 *   - Toggle for viewing history
 *   - Table showing borrowed books
 *   - Only borrowed books are shown here
 *   - Selecting a book and clicking "Return" returns it
 *   - If view history is toggled, shows history below borrowed books
 *   - Historical books cannot be selected
 * 
 * - Bottom panel: Action buttons for creating new items
 *   - New Category, Author, Book, Borrower buttons
 *   - Edit Borrower button
 */
public class LibraryUI extends Application {
    
    private LibraryDatabase db;
    private TableView<BookData> booksTable;
    private TableView<BorrowerData> borrowersTable;
    private TableView<LoanData> borrowedBooksTable;
    private ComboBox<String> searchTypeComboBox;
    private TextField bookSearchField;
    private TextField borrowerSearchField;
    private Label selectedBorrowerLabel;
    private CheckBox viewHistoryToggle;
    private Label statusLabel;
    private BorrowerData selectedBorrower;
    
    @Override
    public void start(Stage primaryStage) {
        // Initialize database
        db = new LibraryDatabase();
        try {
            db.connect();
        } catch (SQLException e) {
            showError("Database Connection Error", "Failed to connect to the database: " + e.getMessage());
        }
        // Main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #999999; -fx-background-radius: 30;");
        
        // Create a 3-column layout
        HBox columnsLayout = new HBox(10);
        
        // Left side - Book search and results
        VBox leftPanel = createLeftPanel();
        
        // Middle - Borrow/Return buttons
        VBox middlePanel = createMiddlePanel();
        
        // Right side - Borrower search and results
        VBox rightPanel = createRightPanel();
        
        // Bottom panel - Status bar
        HBox bottomPanel = createBottomPanel();
        
        // Set up search listeners
        setupSearchListeners();
        
        // Add panels to columns layout
        columnsLayout.getChildren().addAll(leftPanel, middlePanel, rightPanel);
        
        // Add to main layout
        mainLayout.setCenter(columnsLayout);
        mainLayout.setBottom(bottomPanel);
        
        // Create scene and show stage
        Scene scene = new Scene(mainLayout, 900, 600);
        primaryStage.setTitle("Library Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Initialize database in background
        new Thread(() -> {
            try {
                db.initializeDatabase();
                Platform.runLater(() -> updateStatus("Database initialized successfully"));
                
                // Load initial data
                refreshBooksList();
                refreshBorrowersList();
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Database Error", "Failed to initialize database: " + e.getMessage()));
            }
        }).start();
    }
    
    private VBox createLeftPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(350);
        
        // Search type dropdown
        Label searchLabel = new Label("Search Type");
        searchTypeComboBox = new ComboBox<>();
        searchTypeComboBox.getItems().addAll("Books", "Authors", "Categories");
        searchTypeComboBox.setValue("Books");
        
        // Search field
        bookSearchField = new TextField();
        bookSearchField.setPromptText("Search box");
        
        // Search results area
        booksTable = new TableView<>();
        booksTable.setPrefHeight(300);
        
        // Create columns for books table
        TableColumn<BookData, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setPrefWidth(150);
        
        TableColumn<BookData, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("authors"));
        authorColumn.setPrefWidth(100);
        
        TableColumn<BookData, String> isbnColumn = new TableColumn<>("ISBN");
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnColumn.setPrefWidth(80);
        
        booksTable.getColumns().addAll(titleColumn, authorColumn, isbnColumn);
        
        // Create dynamic columns based on search type
        searchTypeComboBox.setOnAction(e -> {
            String searchType = searchTypeComboBox.getValue();
            bookSearchField.clear();
            
            switch(searchType) {
                case "Books":
                    setupBooksTable();
                    refreshBooksList();
                    break;
                case "Authors":
                    setupAuthorsTable();
                    refreshAuthorsList();
                    break;
                case "Categories":
                    setupCategoriesTable();
                    refreshCategoriesList();
                    break;
            }
        });
        
        // Action buttons for the left panel
        HBox actionButtonsBox = new HBox(10);
        actionButtonsBox.setPadding(new Insets(10, 0, 0, 0));
        actionButtonsBox.setAlignment(Pos.CENTER);
        
        Button newCategoryButton = new Button("New Category");
        Button newAuthorButton = new Button("New Author");
        Button newBookButton = new Button("New Book");
        
        actionButtonsBox.getChildren().addAll(
            newCategoryButton, newAuthorButton, newBookButton
        );
        
        // Add action handlers for the popup forms
        newCategoryButton.setOnAction(e -> showCategoryForm());
        newAuthorButton.setOnAction(e -> showAuthorForm());
        newBookButton.setOnAction(e -> showBookForm());
        
        panel.getChildren().addAll(
            searchLabel, searchTypeComboBox, bookSearchField, 
            booksTable, actionButtonsBox
        );
        
        return panel;
    }
    
    private VBox createMiddlePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(100);
        panel.setAlignment(Pos.CENTER);
        
        // Add vertical spacing to align buttons with tables
        Region spacer = new Region();
        spacer.setPrefHeight(100);
        
        // Borrow/Return buttons
        Button borrowButton = new Button("Borrow >>>");
        Button returnButton = new Button("<<< Return");
        
        // Add action handlers
        borrowButton.setOnAction(e -> borrowSelectedBook());
        returnButton.setOnAction(e -> returnSelectedBook());
        
        VBox buttonBox = new VBox(15, borrowButton, returnButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        panel.getChildren().addAll(spacer, buttonBox);
        
        return panel;
    }
    
    private VBox createRightPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(350);
        
        // Borrower search
        borrowerSearchField = new TextField();
        borrowerSearchField.setPromptText("Search for borrower (name, email, etc)");
        
        // Selected borrower
        HBox borrowerBox = new HBox(10);
        selectedBorrowerLabel = new Label("Selected Borrower (blank at start)");
        viewHistoryToggle = new CheckBox("View History");
        borrowerBox.getChildren().addAll(selectedBorrowerLabel, viewHistoryToggle);
        
        // Borrower table
        borrowersTable = new TableView<>();
        borrowersTable.setPrefHeight(150);
        
        // Set up borrowers table columns
        TableColumn<BorrowerData, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameColumn.setPrefWidth(150);
        
        TableColumn<BorrowerData, String> emailColumn = new TableColumn<>("Email");
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailColumn.setPrefWidth(180);
        
        borrowersTable.getColumns().addAll(nameColumn, emailColumn);
        
        // Borrower's books table
        borrowedBooksTable = new TableView<>();
        borrowedBooksTable.setPrefHeight(150);
        
        // Add table columns for borrowed books
        TableColumn<LoanData, String> bookTitleColumn = new TableColumn<>("Book Title");
        bookTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        bookTitleColumn.setPrefWidth(150);
        
        TableColumn<LoanData, String> isbnColumn = new TableColumn<>("ISBN");
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnColumn.setPrefWidth(80);
        
        TableColumn<LoanData, String> dueDateColumn = new TableColumn<>("Due Date");
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        dueDateColumn.setPrefWidth(100);
        
        borrowedBooksTable.getColumns().addAll(bookTitleColumn, isbnColumn, dueDateColumn);
        
        // Set up borrower selection listener
        borrowersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedBorrower = newSelection;
                selectedBorrowerLabel.setText(newSelection.getFullName());
                refreshBorrowedBooks();
            }
        });
        
        // Set up history toggle listener
        viewHistoryToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (selectedBorrower != null) {
                refreshBorrowedBooks();
            }
        });
        
        // Borrower action buttons
        HBox borrowerActionBox = new HBox(10);
        borrowerActionBox.setPadding(new Insets(10, 0, 0, 0));
        borrowerActionBox.setAlignment(Pos.CENTER);
        
        Button newBorrowerButton = new Button("New Borrower");
        Button editBorrowerButton = new Button("Edit Borrower");
        
        borrowerActionBox.getChildren().addAll(newBorrowerButton, editBorrowerButton);
        
        // Add action handlers
        newBorrowerButton.setOnAction(e -> showBorrowerForm(null));
        editBorrowerButton.setOnAction(e -> {
            if (selectedBorrower != null) {
                showBorrowerForm(selectedBorrower);
            } else {
                showError("No Selection", "Please select a borrower to edit");
            }
        });
        
        panel.getChildren().addAll(
            borrowerSearchField, borrowerBox, 
            borrowersTable, borrowedBooksTable, borrowerActionBox
        );
        
        return panel;
    }
    
    private HBox createBottomPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(20, 10, 10, 10));
        panel.setAlignment(Pos.CENTER);
        
        // Status label for the bottom panel
        statusLabel = new Label("Library Management System - Ready");
        statusLabel.setStyle("-fx-font-style: italic;");
        
        panel.getChildren().add(statusLabel);
        
        return panel;
    }
    
    private void setupSearchListeners() {
        // Book search listener
        bookSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchTypeComboBox.getValue().equals("Books")) {
                searchBooks(newValue);
            } else if (searchTypeComboBox.getValue().equals("Authors")) {
                searchAuthors(newValue);
            } else if (searchTypeComboBox.getValue().equals("Categories")) {
                searchCategories(newValue);
            }
        });
        
        // Borrower search listener
        borrowerSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchBorrowers(newValue);
        });
    }
    
    // ========== Table Setup Methods ==========
    
    private void setupBooksTable() {
        booksTable.getColumns().clear();
        
        TableColumn<BookData, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setPrefWidth(150);
        
        TableColumn<BookData, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("authors"));
        authorColumn.setPrefWidth(100);
        
        TableColumn<BookData, String> isbnColumn = new TableColumn<>("ISBN");
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnColumn.setPrefWidth(80);
        
        booksTable.getColumns().addAll(titleColumn, authorColumn, isbnColumn);
    }
    
    private void setupAuthorsTable() {
        booksTable.getColumns().clear();
        
        // Create a new TableView for authors with the correct type
        TableView<AuthorData> authorsTable = new TableView<>();
        
        TableColumn<AuthorData, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameColumn.setPrefWidth(150);
        
        TableColumn<AuthorData, Integer> birthYearColumn = new TableColumn<>("Birth Year");
        birthYearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));
        birthYearColumn.setPrefWidth(80);
        
        TableColumn<AuthorData, String> biographyColumn = new TableColumn<>("Biography");
        biographyColumn.setCellValueFactory(new PropertyValueFactory<>("biography"));
        biographyColumn.setPrefWidth(200);
        
        authorsTable.getColumns().addAll(nameColumn, birthYearColumn, biographyColumn);
        
        // Use unchecked cast - this is safe because we're only displaying the data
        @SuppressWarnings("unchecked")
        TableView<BookData> bookDataView = (TableView<BookData>)(TableView<?>)authorsTable;
        
        // Replace the books table with our authors table
        booksTable = bookDataView;
    }
    
    private void setupCategoriesTable() {
        booksTable.getColumns().clear();
        
        // Create a new TableView for categories with the correct type
        TableView<CategoryData> categoriesTable = new TableView<>();
        
        TableColumn<CategoryData, String> nameColumn = new TableColumn<>("Category Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(150);
        
        TableColumn<CategoryData, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setPrefWidth(200);
        
        categoriesTable.getColumns().addAll(nameColumn, descriptionColumn);
        
        // Use unchecked cast - this is safe because we're only displaying the data
        @SuppressWarnings("unchecked")
        TableView<BookData> bookDataView = (TableView<BookData>)(TableView<?>)categoriesTable;
        
        // Replace the books table with our categories table
        booksTable = bookDataView;
    }
    
    // ========== Data Refresh Methods ==========
    
    private void refreshBooksList() {
        try {
            List<Map<String, Object>> books = db.getAllBooks();
            ObservableList<BookData> booksList = FXCollections.observableArrayList();
            
            for (Map<String, Object> book : books) {
                booksList.add(new BookData(book));
            }
            
            booksTable.setItems(booksList);
            updateStatus("Loaded " + books.size() + " books");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load books: " + e.getMessage());
        }
    }
    
    private void refreshAuthorsList() {
        try {
            List<Map<String, Object>> authors = db.getAllAuthors();
            ObservableList<AuthorData> authorsList = FXCollections.observableArrayList();
            
            for (Map<String, Object> author : authors) {
                authorsList.add(new AuthorData(author));
            }
            
            @SuppressWarnings("unchecked")
            ObservableList<BookData> castedList = (ObservableList<BookData>)(ObservableList<?>)authorsList;
            booksTable.setItems(castedList);
            updateStatus("Loaded " + authors.size() + " authors");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load authors: " + e.getMessage());
        }
    }
    
    private void refreshCategoriesList() {
        try {
            List<Map<String, Object>> categories = db.getAllCategories();
            ObservableList<CategoryData> categoriesList = FXCollections.observableArrayList();
            
            for (Map<String, Object> category : categories) {
                categoriesList.add(new CategoryData(category));
            }
            
            @SuppressWarnings("unchecked")
            ObservableList<BookData> castedList = (ObservableList<BookData>)(ObservableList<?>)categoriesList;
            booksTable.setItems(castedList);
            updateStatus("Loaded " + categories.size() + " categories");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }
    
    private void refreshBorrowersList() {
        try {
            List<Map<String, Object>> borrowers = db.getAllBorrowers();
            ObservableList<BorrowerData> borrowersList = FXCollections.observableArrayList();
            
            for (Map<String, Object> borrower : borrowers) {
                borrowersList.add(new BorrowerData(borrower));
            }
            
            borrowersTable.setItems(borrowersList);
            updateStatus("Loaded " + borrowers.size() + " borrowers");
        } catch (SQLException e) {
            showError("Database Error", "Failed to load borrowers: " + e.getMessage());
        }
    }
    
    private void refreshBorrowedBooks() {
        if (selectedBorrower == null) {
            borrowedBooksTable.getItems().clear();
            return;
        }
        
        try {
            List<Map<String, Object>> loans;
            if (viewHistoryToggle.isSelected()) {
                loans = db.getBorrowerHistory(selectedBorrower.getCardNumber());
            } else {
                loans = db.getBorrowerActiveLoans(selectedBorrower.getCardNumber());
            }
            
            ObservableList<LoanData> loansList = FXCollections.observableArrayList();
            for (Map<String, Object> loan : loans) {
                loansList.add(new LoanData(loan));
            }
            
            borrowedBooksTable.setItems(loansList);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load borrowed books: " + e.getMessage());
        }
    }
    
    // ========== Search Methods ==========
    
    private void searchBooks(String searchTerm) {
        try {
            List<Map<String, Object>> books;
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                books = db.getAllBooks();
            } else {
                books = db.searchBooks(searchTerm);
            }
            
            ObservableList<BookData> booksList = FXCollections.observableArrayList();
            for (Map<String, Object> book : books) {
                booksList.add(new BookData(book));
            }
            
            booksTable.setItems(booksList);
            updateStatus("Found " + books.size() + " books matching '" + searchTerm + "'");
        } catch (SQLException e) {
            showError("Search Error", "Failed to search books: " + e.getMessage());
        }
    }
    
    private void searchAuthors(String searchTerm) {
        try {
            List<Map<String, Object>> authors;
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                authors = db.getAllAuthors();
            } else {
                authors = db.searchAuthors(searchTerm);
            }
            
            ObservableList<AuthorData> authorsList = FXCollections.observableArrayList();
            for (Map<String, Object> author : authors) {
                authorsList.add(new AuthorData(author));
            }
            
            @SuppressWarnings("unchecked")
            ObservableList<BookData> castedList = (ObservableList<BookData>)(ObservableList<?>)authorsList;
            booksTable.setItems(castedList);
            updateStatus("Found " + authors.size() + " authors matching '" + searchTerm + "'");
        } catch (SQLException e) {
            showError("Search Error", "Failed to search authors: " + e.getMessage());
        }
    }
    
    private void searchCategories(String searchTerm) {
        try {
            List<Map<String, Object>> categories;
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                categories = db.getAllCategories();
            } else {
                categories = db.searchCategories(searchTerm);
            }
            
            ObservableList<CategoryData> categoriesList = FXCollections.observableArrayList();
            for (Map<String, Object> category : categories) {
                categoriesList.add(new CategoryData(category));
            }
            
            @SuppressWarnings("unchecked")
            ObservableList<BookData> castedList = (ObservableList<BookData>)(ObservableList<?>)categoriesList;
            booksTable.setItems(castedList);
            updateStatus("Found " + categories.size() + " categories matching '" + searchTerm + "'");
        } catch (SQLException e) {
            showError("Search Error", "Failed to search categories: " + e.getMessage());
        }
    }
    
    private void searchBorrowers(String searchTerm) {
        try {
            List<Map<String, Object>> borrowers;
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                borrowers = db.getAllBorrowers();
            } else {
                borrowers = db.searchBorrowers(searchTerm);
            }
            
            ObservableList<BorrowerData> borrowersList = FXCollections.observableArrayList();
            for (Map<String, Object> borrower : borrowers) {
                borrowersList.add(new BorrowerData(borrower));
            }
            
            borrowersTable.setItems(borrowersList);
            updateStatus("Found " + borrowers.size() + " borrowers matching '" + searchTerm + "'");
        } catch (SQLException e) {
            showError("Search Error", "Failed to search borrowers: " + e.getMessage());
        }
    }
    
    // ========== Action Methods ==========
    
    private void borrowSelectedBook() {
        BookData selectedBook = booksTable.getSelectionModel().getSelectedItem();
        
        if (selectedBook == null) {
            showError("No Selection", "Please select a book to borrow");
            return;
        }
        
        if (selectedBorrower == null) {
            showError("No Borrower Selected", "Please select a borrower first");
            return;
        }
        
        // Calculate due date (30 days from now)
        LocalDate dueDate = LocalDate.now().plusDays(30);
        String dueDateStr = dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        try {
            boolean success = db.borrowBook(selectedBook.getBookId(), selectedBorrower.getCardNumber(), dueDateStr);
            
            if (success) {
                updateStatus("Book '" + selectedBook.getTitle() + "' borrowed by " + selectedBorrower.getFullName());
                refreshBooksList();
                refreshBorrowedBooks();
            } else {
                showError("Borrow Failed", "This book is not available for borrowing");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to borrow book: " + e.getMessage());
        }
    }
    
    private void returnSelectedBook() {
        LoanData selectedLoan = borrowedBooksTable.getSelectionModel().getSelectedItem();
        
        if (selectedLoan == null) {
            showError("No Selection", "Please select a book to return");
            return;
        }
        
        try {
            boolean success = db.returnBook(selectedLoan.getReservationId());
            
            if (success) {
                updateStatus("Book '" + selectedLoan.getTitle() + "' returned");
                refreshBooksList();
                refreshBorrowedBooks();
            } else {
                showError("Return Failed", "Failed to return the book");
            }
        } catch (SQLException e) {
            showError("Database Error", "Failed to return book: " + e.getMessage());
        }
    }
    
    // ========== Form Dialog Methods ==========
    
    private void showCategoryForm() {
        FormDialog dialog = new FormDialog("New Category");
        
        TextField nameField = dialog.addTextField("Category Name", 0);
        TextField descriptionField = dialog.addTextField("Description", 1);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    String name = nameField.getText();
                    String description = descriptionField.getText();
                    
                    if (name == null || name.trim().isEmpty()) {
                        showError("Validation Error", "Category name cannot be empty");
                        return false;
                    }
                    
                    db.addCategory(name, description);
                    updateStatus("Category '" + name + "' added successfully");
                    refreshCategoriesList();
                    return true;
                } catch (SQLException e) {
                    showError("Database Error", "Failed to add category: " + e.getMessage());
                    return false;
                }
            }
            return false;
        });
        
        dialog.showAndWait();
    }
    
    private void showAuthorForm() {
        FormDialog dialog = new FormDialog("New Author");
        
        TextField firstNameField = dialog.addTextField("First Name", 0);
        TextField lastNameField = dialog.addTextField("Last Name", 1);
        TextField birthYearField = dialog.addTextField("Birth Year", 2);
        TextField biographyField = dialog.addTextField("Biography", 3);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    String firstName = firstNameField.getText();
                    String lastName = lastNameField.getText();
                    String birthYearStr = birthYearField.getText();
                    String biography = biographyField.getText();
                    
                    if (firstName == null || firstName.trim().isEmpty() || 
                        lastName == null || lastName.trim().isEmpty()) {
                        showError("Validation Error", "First name and last name cannot be empty");
                        return false;
                    }
                    
                    int birthYear = 0;
                    if (birthYearStr != null && !birthYearStr.trim().isEmpty()) {
                        try {
                            birthYear = Integer.parseInt(birthYearStr);
                        } catch (NumberFormatException e) {
                            showError("Validation Error", "Birth year must be a number");
                            return false;
                        }
                    }
                    
                    db.addAuthor(firstName, lastName, birthYear, biography);
                    updateStatus("Author '" + firstName + " " + lastName + "' added successfully");
                    refreshAuthorsList();
                    return true;
                } catch (SQLException e) {
                    showError("Database Error", "Failed to add author: " + e.getMessage());
                    return false;
                }
            }
            return false;
        });
        
        dialog.showAndWait();
    }
    
    private void showBookForm() {
        FormDialog dialog = new FormDialog("New Book");
        
        TextField titleField = dialog.addTextField("Title", 0);
        TextField isbnField = dialog.addTextField("ISBN", 1);
        TextField yearField = dialog.addTextField("Publication Year", 2);
        TextField publisherField = dialog.addTextField("Publisher", 3);
        TextField copiesField = dialog.addTextField("Total Copies", 4);
        
        // Add category dropdown
        ComboBox<CategoryData> categoryComboBox = new ComboBox<>();
        try {
            List<Map<String, Object>> categories = db.getAllCategories();
            ObservableList<CategoryData> categoriesList = FXCollections.observableArrayList();
            for (Map<String, Object> category : categories) {
                categoriesList.add(new CategoryData(category));
            }
            categoryComboBox.setItems(categoriesList);
            
            // Set cell factory to display category name
            categoryComboBox.setCellFactory(new Callback<ListView<CategoryData>, ListCell<CategoryData>>() {
                @Override
                public ListCell<CategoryData> call(ListView<CategoryData> param) {
                    return new ListCell<CategoryData>() {
                        @Override
                        protected void updateItem(CategoryData item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setText(null);
                            } else {
                                setText(item.getName());
                            }
                        }
                    };
                }
            });
            
            // Set button cell to display category name
            categoryComboBox.setButtonCell(new ListCell<CategoryData>() {
                @Override
                protected void updateItem(CategoryData item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
            
            dialog.addNode("Category", categoryComboBox, 5);
        } catch (SQLException e) {
            showError("Database Error", "Failed to load categories: " + e.getMessage());
        }
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    String title = titleField.getText();
                    String isbn = isbnField.getText();
                    String yearStr = yearField.getText();
                    String publisher = publisherField.getText();
                    String copiesStr = copiesField.getText();
                    CategoryData category = categoryComboBox.getValue();
                    
                    if (title == null || title.trim().isEmpty() || 
                        isbn == null || isbn.trim().isEmpty()) {
                        showError("Validation Error", "Title and ISBN cannot be empty");
                        return false;
                    }
                    
                    if (category == null) {
                        showError("Validation Error", "Please select a category");
                        return false;
                    }
                    
                    int year = 0;
                    if (yearStr != null && !yearStr.trim().isEmpty()) {
                        try {
                            year = Integer.parseInt(yearStr);
                        } catch (NumberFormatException e) {
                            showError("Validation Error", "Publication year must be a number");
                            return false;
                        }
                    }
                    
                    int copies = 1;
                    if (copiesStr != null && !copiesStr.trim().isEmpty()) {
                        try {
                            copies = Integer.parseInt(copiesStr);
                            if (copies < 1) {
                                showError("Validation Error", "Total copies must be at least 1");
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            showError("Validation Error", "Total copies must be a number");
                            return false;
                        }
                    }
                    
                    // For simplicity, we're adding a book with a single author
                    // In a real application, you would have a multi-select for authors
                    List<Integer> authorIds = new ArrayList<>();
                    
                    db.addBook(title, isbn, year, publisher, copies, category.getCategoryId(), authorIds);
                    updateStatus("Book '" + title + "' added successfully");
                    refreshBooksList();
                    return true;
                } catch (SQLException e) {
                    showError("Database Error", "Failed to add book: " + e.getMessage());
                    return false;
                }
            }
            return false;
        });
        
        dialog.showAndWait();
    }
    
    private void showBorrowerForm(BorrowerData borrower) {
        FormDialog dialog = new FormDialog(borrower == null ? "New Borrower" : "Edit Borrower");
        
        TextField firstNameField = dialog.addTextField("First Name", 0);
        TextField lastNameField = dialog.addTextField("Last Name", 1);
        TextField addressField = dialog.addTextField("Address", 2);
        TextField phoneField = dialog.addTextField("Phone", 3);
        TextField emailField = dialog.addTextField("Email", 4);
        
        // If editing, populate fields
        if (borrower != null) {
            firstNameField.setText(borrower.getFirstName());
            lastNameField.setText(borrower.getLastName());
            addressField.setText(borrower.getAddress());
            phoneField.setText(borrower.getPhone());
            emailField.setText(borrower.getEmail());
        }
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    String firstName = firstNameField.getText();
                    String lastName = lastNameField.getText();
                    String address = addressField.getText();
                    String phone = phoneField.getText();
                    String email = emailField.getText();
                    
                    if (firstName == null || firstName.trim().isEmpty() || 
                        lastName == null || lastName.trim().isEmpty() ||
                        email == null || email.trim().isEmpty()) {
                        showError("Validation Error", "First name, last name, and email cannot be empty");
                        return false;
                    }
                    
                    if (borrower == null) {
                        // Add new borrower
                        db.addBorrower(firstName, lastName, address, phone, email);
                        updateStatus("Borrower '" + firstName + " " + lastName + "' added successfully");
                    } else {
                        // Update existing borrower
                        db.updateBorrower(borrower.getCardNumber(), firstName, lastName, address, phone, email);
                        updateStatus("Borrower '" + firstName + " " + lastName + "' updated successfully");
                    }
                    
                    refreshBorrowersList();
                    return true;
                } catch (SQLException e) {
                    showError("Database Error", "Failed to save borrower: " + e.getMessage());
                    return false;
                }
            }
            return false;
        });
        
        dialog.showAndWait();
    }
    
    // ========== Utility Methods ==========
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
    
    // ========== Data Model Classes ==========
    
    public static class BookData {
        private final int bookId;
        private final String title;
        private final String isbn;
        private final int publicationYear;
        private final String publisher;
        private final int availableCopies;
        private final int totalCopies;
        private final String category;
        private final String authors;
        
        public BookData(Map<String, Object> data) {
            this.bookId = ((Number) data.get("book_id")).intValue();
            this.title = (String) data.get("title");
            this.isbn = (String) data.get("isbn");
            this.publicationYear = data.get("publication_year") != null ? 
                    ((Number) data.get("publication_year")).intValue() : 0;
            this.publisher = (String) data.get("publisher");
            this.availableCopies = ((Number) data.get("available_copies")).intValue();
            this.totalCopies = ((Number) data.get("total_copies")).intValue();
            this.category = (String) data.get("category");
            this.authors = (String) data.get("authors");
        }
        
        public int getBookId() { return bookId; }
        public String getTitle() { return title; }
        public String getIsbn() { return isbn; }
        public int getPublicationYear() { return publicationYear; }
        public String getPublisher() { return publisher; }
        public int getAvailableCopies() { return availableCopies; }
        public int getTotalCopies() { return totalCopies; }
        public String getCategory() { return category; }
        public String getAuthors() { return authors; }
    }
    
    public static class AuthorData {
        private final int authorId;
        private final String firstName;
        private final String lastName;
        private final int birthYear;
        private final String biography;
        
        public AuthorData(Map<String, Object> data) {
            this.authorId = ((Number) data.get("author_id")).intValue();
            this.firstName = (String) data.get("first_name");
            this.lastName = (String) data.get("last_name");
            this.birthYear = data.get("birth_year") != null ? 
                    ((Number) data.get("birth_year")).intValue() : 0;
            this.biography = (String) data.get("biography");
        }
        
        public int getAuthorId() { return authorId; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return firstName + " " + lastName; }
        public int getBirthYear() { return birthYear; }
        public String getBiography() { return biography; }
    }
    
    public static class CategoryData {
        private final int categoryId;
        private final String name;
        private final String description;
        
        public CategoryData(Map<String, Object> data) {
            this.categoryId = ((Number) data.get("category_id")).intValue();
            this.name = (String) data.get("name");
            this.description = (String) data.get("description");
        }
        
        public int getCategoryId() { return categoryId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
    }
    
    public static class BorrowerData {
        private final int cardNumber;
        private final String firstName;
        private final String lastName;
        private final String address;
        private final String phone;
        private final String email;
        private final String registrationDate;
        
        public BorrowerData(Map<String, Object> data) {
            this.cardNumber = ((Number) data.get("card_number")).intValue();
            this.firstName = (String) data.get("first_name");
            this.lastName = (String) data.get("last_name");
            this.address = (String) data.get("address");
            this.phone = (String) data.get("phone");
            this.email = (String) data.get("email");
            this.registrationDate = (String) data.get("registration_date");
        }
        
        public int getCardNumber() { return cardNumber; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { return firstName + " " + lastName; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getRegistrationDate() { return registrationDate; }
    }
    
    public static class LoanData {
        private final int reservationId;
        private final int bookId;
        private final String title;
        private final String isbn;
        private final String authors;
        private final String checkoutDate;
        private final String dueDate;
        private final String returnDate;
        private final String status;
        
        public LoanData(Map<String, Object> data) {
            this.reservationId = ((Number) data.get("reservation_id")).intValue();
            this.bookId = ((Number) data.get("book_id")).intValue();
            this.title = (String) data.get("title");
            this.isbn = (String) data.get("isbn");
            this.authors = (String) data.get("authors");
            this.checkoutDate = (String) data.get("checkout_date");
            this.dueDate = (String) data.get("due_date");
            this.returnDate = (String) data.get("return_date");
            this.status = (String) data.get("status");
        }
        
        public int getReservationId() { return reservationId; }
        public int getBookId() { return bookId; }
        public String getTitle() { return title; }
        public String getIsbn() { return isbn; }
        public String getAuthors() { return authors; }
        public String getCheckoutDate() { return checkoutDate; }
        public String getDueDate() { return dueDate; }
        public String getReturnDate() { return returnDate; }
        public String getStatus() { return status; }
    }
}
