package libraryapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
 * JavaFX UI for the Library Management System
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
    
    // Add global error handler for uncaught exceptions in JavaFX thread
    static {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Unexpected Error");
                alert.setHeaderText("An unexpected error occurred");
                alert.setContentText(throwable.getMessage() != null ? throwable.getMessage() : throwable.toString());
                alert.showAndWait();
            });
        });
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Initialize database
        db = new LibraryDatabase();
        try {
            db.connect();
        } catch (SQLException e) {
            safeShowError("Database Connection Error", "Failed to connect to the database: " + e.getMessage());
        }
        // Main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(0));
        mainLayout.setStyle("-fx-background-color: linear-gradient(to bottom right, #f8fafc, #e0e7ef); -fx-background-radius: 20;");
        
        // Create a 3-column layout
        HBox columnsLayout = new HBox(30);
        columnsLayout.setPadding(new Insets(30, 40, 0, 40));
        columnsLayout.setAlignment(Pos.TOP_CENTER);
        
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
        Scene scene = new Scene(mainLayout, 1400, 850);
        primaryStage.setTitle("Library Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(750);
        primaryStage.show();
        
        // Initialize database in background
        new Thread(() -> {
            try {
                db.initializeDatabase();
                safeUpdateStatus("Database initialized successfully");
                // Load initial data
                runOnUiThread(this::refreshBooksList);
                runOnUiThread(this::refreshBorrowersList);
            } catch (SQLException e) {
                safeShowError("Database Error", "Failed to initialize database: " + e.getMessage());
            }
        }).start();
    }
    
    private VBox createLeftPanel() {
        VBox panel = new VBox(18);
        panel.setPadding(new Insets(24, 20, 24, 20));
        panel.setPrefWidth(540);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-effect: dropshadow(gaussian, #cfd8dc, 10, 0.2, 0, 2);");
        
        // Search type dropdown
        Label searchLabel = new Label("Search Books");
        searchLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2a2a2a;");
        searchTypeComboBox = new ComboBox<>();
        searchTypeComboBox.getItems().addAll("Books", "Authors", "Categories");
        searchTypeComboBox.setValue("Books");
        searchTypeComboBox.setStyle("-fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 6 12 6 12;");
        
        // Search field
        bookSearchField = new TextField();
        bookSearchField.setPromptText("Search for " + searchTypeComboBox.getValue());
        bookSearchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 6 12 6 12; -fx-border-color: #b0bec5; -fx-border-radius: 8;");
        
        // Search results area
        booksTable = new TableView<>();
        booksTable.setPrefHeight(300);
        booksTable.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-font-size: 13px;");
        booksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        booksTable.setPlaceholder(new Label("No books found."));
        
        // Create columns for books table
        TableColumn<BookData, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setPrefWidth(260);
        
        TableColumn<BookData, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("authors"));
        authorColumn.setPrefWidth(180);
        
        TableColumn<BookData, String> isbnColumn = new TableColumn<>("ISBN");
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnColumn.setPrefWidth(120);
        
        TableColumn<BookData, Integer> availableColumn = new TableColumn<>("# Available");
        availableColumn.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        availableColumn.setPrefWidth(100);
        
        TableColumn<BookData, Integer> totalColumn = new TableColumn<>("# Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        totalColumn.setPrefWidth(100);
        
        booksTable.getColumns().addAll(titleColumn, authorColumn, isbnColumn, availableColumn, totalColumn);
        booksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        
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
        actionButtonsBox.setPadding(new Insets(18, 0, 0, 0));
        actionButtonsBox.setAlignment(Pos.CENTER);
        actionButtonsBox.setStyle("-fx-background-color: transparent;");
        
        Button newItemButton = new Button("New Book");
        newItemButton.setStyle("-fx-background-color: #4f8cff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 6 18 6 18;");
        Button deleteBookButton = new Button("Delete Book");
        deleteBookButton.setPrefWidth(120);
        deleteBookButton.setOnAction(e -> deleteSelectedBooks());
        deleteBookButton.setStyle("-fx-background-color: #ff5e5b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 6 18 6 18;");
        
        actionButtonsBox.getChildren().addAll(newItemButton, deleteBookButton);
        
        // Update button text based on selected search type
        searchTypeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            newItemButton.setText("New " + newValue);
        });
        
        // We'll set up the double-click handlers in the specific table setup methods
        
        // Add action handlers for the popup forms
        newItemButton.setOnAction(e -> {
            String type = searchTypeComboBox.getValue();
            if ("Books".equals(type)) {
                showBookForm();
            } else if ("Authors".equals(type)) {
                showAuthorForm();
            } else if ("Categories".equals(type)) {
                showCategoryForm();
            }
        });
        
        panel.getChildren().addAll(
            searchLabel, searchTypeComboBox, bookSearchField, 
            booksTable, actionButtonsBox
        );
        
        return panel;
    }
    
    private VBox createMiddlePanel() {
        VBox panel = new VBox(48);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(120);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setStyle("-fx-background-color: transparent;");
        
        // Add vertical spacing to align buttons with tables
        Region spacer = new Region();
        spacer.setPrefHeight(180);
        
        // Borrow/Return buttons
        Button borrowButton = new Button("Borrow >>>>");
        borrowButton.setPrefWidth(140);
        borrowButton.setStyle("-fx-background-color: #43b581; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 8 0 8 0;");
        Button returnButton = new Button("<<<< Return");
        returnButton.setPrefWidth(140);
        returnButton.setStyle("-fx-background-color: #f5a623; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 8 0 8 0;");
        
        // Add action handlers
        borrowButton.setOnAction(e -> borrowSelectedBook());
        returnButton.setOnAction(e -> returnSelectedBook());
        
        // Initially enable borrow button only for Books view
        borrowButton.setDisable(false);
        
        // Add listener to searchTypeComboBox to enable/disable borrow button
        searchTypeComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            borrowButton.setDisable(!"Books".equals(newValue));
        });
        
        VBox buttonBox = new VBox(24, borrowButton, returnButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        panel.getChildren().addAll(spacer, buttonBox);
        
        return panel;
    }
    
    private VBox createRightPanel() {
        VBox panel = new VBox(18);
        panel.setPadding(new Insets(24, 20, 24, 20));
        panel.setPrefWidth(420);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 18; -fx-effect: dropshadow(gaussian, #cfd8dc, 10, 0.2, 0, 2);");
        
        // Borrower search
        borrowerSearchField = new TextField();
        borrowerSearchField.setPromptText("Search for borrower (name, email, etc)");
        borrowerSearchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 8; -fx-padding: 6 12 6 12; -fx-border-color: #b0bec5; -fx-border-radius: 8;");
        
        // Selected borrower
        HBox borrowerBox = new HBox(10);
        selectedBorrowerLabel = new Label("Selected A Borrower");
        selectedBorrowerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2a2a2a;");
        viewHistoryToggle = new CheckBox("View History");
        viewHistoryToggle.setStyle("-fx-font-size: 13px;");
        Button deleteBorrowerButton = new Button("Delete User");
        deleteBorrowerButton.setPrefWidth(120);
        deleteBorrowerButton.setOnAction(e -> deleteSelectedBorrowers());
        deleteBorrowerButton.setStyle("-fx-background-color: #ff5e5b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 6 18 6 18;");
        borrowerBox.getChildren().addAll(selectedBorrowerLabel, viewHistoryToggle, deleteBorrowerButton);
        
        // Borrower table
        borrowersTable = new TableView<>();
        borrowersTable.setPrefHeight(320);
        borrowersTable.setPrefWidth(400);
        borrowersTable.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-font-size: 13px;");
        borrowersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        borrowersTable.setPlaceholder(new Label("No users found."));
        
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
        borrowedBooksTable.setPrefHeight(320);
        borrowedBooksTable.setPrefWidth(400);
        borrowedBooksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); // Enable multiple selection
        
        // Make sure the selection model allows multiple selection
        MultipleSelectionModel<LoanData> selectionModel = borrowedBooksTable.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        
        // Set cell factory to style rows based on loan status
        borrowedBooksTable.setRowFactory(tv -> new TableRow<LoanData>() {
            @Override
            protected void updateItem(LoanData item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (!"active".equals(item.getStatus())) {
                    setStyle("-fx-text-fill: #888888;"); // Grey out returned books
                } else {
                    setStyle("-fx-font-weight: bold;"); // Bold active loans
                }
            }
        });
        
        // Add table columns for borrowed books
        TableColumn<LoanData, String> bookTitleColumn = new TableColumn<>("Book Title");
        bookTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        bookTitleColumn.setPrefWidth(170);
        
        // Set cell factory for each column to maintain styling
        bookTitleColumn.setCellFactory(column -> new TableCell<LoanData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                
                TableRow<LoanData> row = getTableRow();
                if (row != null && row.getItem() != null) {
                    LoanData loan = row.getItem();
                    if (!"active".equals(loan.getStatus())) {
                        setStyle("-fx-text-fill: #888888;"); // Grey out returned books
                    } else {
                        setStyle("-fx-font-weight: bold;"); // Bold active loans
                    }
                } else {
                    setStyle("");
                }
            }
        });
        
        TableColumn<LoanData, String> isbnColumn = new TableColumn<>("ISBN");
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnColumn.setPrefWidth(110);
        isbnColumn.setCellFactory(column -> new TableCell<LoanData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item);
                
                TableRow<LoanData> row = getTableRow();
                if (row != null && row.getItem() != null) {
                    LoanData loan = row.getItem();
                    if (!"active".equals(loan.getStatus())) {
                        setStyle("-fx-text-fill: #888888;"); // Grey out returned books
                    } else {
                        setStyle("-fx-font-weight: bold;"); // Bold active loans
                    }
                } else {
                    setStyle("");
                }
            }
        });
        
        TableColumn<LoanData, String> dueDateColumn = new TableColumn<>("Due Date");
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        dueDateColumn.setPrefWidth(100);
        dueDateColumn.setCellFactory(column -> {
            TableCell<LoanData, String> cell = new TableCell<LoanData, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? "" : item);
                    setStyle("-fx-alignment: CENTER; -fx-font-size: 12px;" + (empty ? "" : ""));
                    TableRow<LoanData> row = getTableRow();
                    if (row != null && row.getItem() != null) {
                        LoanData loan = row.getItem();
                        if (!"active".equals(loan.getStatus())) {
                            setStyle("-fx-alignment: CENTER; -fx-font-size: 12px; -fx-text-fill: #888888;");
                        } else {
                            setStyle("-fx-alignment: CENTER; -fx-font-size: 12px; -fx-font-weight: bold;");
                        }
                    } else {
                        setStyle("-fx-alignment: CENTER; -fx-font-size: 12px;");
                    }
                }
            };
            cell.setStyle("-fx-alignment: CENTER; -fx-font-size: 12px;");
            return cell;
        });
        
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
        newBorrowerButton.setStyle("-fx-background-color: #4f8cff; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 7; -fx-padding: 6 18 6 18;");
        
        borrowerActionBox.getChildren().add(newBorrowerButton);
        
        // Add action handlers
        newBorrowerButton.setOnAction(e -> showBorrowerForm(null));
        
        // Add double-click handler to open borrower edit form
        borrowersTable.setRowFactory(tv -> {
            TableRow<BorrowerData> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    BorrowerData borrower = row.getItem();
                    showBorrowerForm(borrower);
                }
            });
            return row;
        });
        
        panel.getChildren().addAll(
            borrowerSearchField, borrowerBox, 
            borrowersTable, borrowedBooksTable, borrowerActionBox
        );
        
        return panel;
    }
    
    private HBox createBottomPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(16, 10, 16, 10));
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: #f7fafd; -fx-background-radius: 0 0 18 18;");
        
        // Status label for the bottom panel
        statusLabel = new Label("Library Management System - Ready");
        statusLabel.setStyle("-fx-font-style: italic; -fx-font-size: 14px; -fx-text-fill: #4f8cff;");
        
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
        // Store the parent of the current table
        Parent parent = booksTable.getParent();
        VBox container = (VBox) parent;
        int index = container.getChildren().indexOf(booksTable);
        
        // Create a new TableView for books
        TableView<BookData> booksTableView = new TableView<>();
        booksTableView.setPrefHeight(booksTable.getPrefHeight());
        booksTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        TableColumn<BookData, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setPrefWidth(260);
        
        TableColumn<BookData, String> authorColumn = new TableColumn<>("Author");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("authors"));
        authorColumn.setPrefWidth(180);
        
        TableColumn<BookData, String> isbnColumn = new TableColumn<>("ISBN");
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnColumn.setPrefWidth(120);
        
        TableColumn<BookData, Integer> availableColumn = new TableColumn<>("# Available");
        availableColumn.setCellValueFactory(new PropertyValueFactory<>("availableCopies"));
        availableColumn.setPrefWidth(100);
        
        TableColumn<BookData, Integer> totalColumn = new TableColumn<>("# Total");
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalCopies"));
        totalColumn.setPrefWidth(100);
        
        booksTableView.getColumns().addAll(titleColumn, authorColumn, isbnColumn, availableColumn, totalColumn);
        
        // Add double-click handler for books
        booksTableView.setRowFactory(tv -> {
            TableRow<BookData> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    BookData book = row.getItem();
                    showBookForm(book);
                }
            });
            return row;
        });
        
        // Replace the books table in the UI
        container.getChildren().set(index, booksTableView);
        
        // Update the reference
        booksTable = booksTableView;
    }
    
    private void setupAuthorsTable() {
        // Store the parent of the current table
        Parent parent = booksTable.getParent();
        VBox container = (VBox) parent;
        int index = container.getChildren().indexOf(booksTable);
        
        // Create a new TableView for authors with the correct type
        TableView<AuthorData> authorsTable = new TableView<>();
        authorsTable.setPrefHeight(booksTable.getPrefHeight());
        
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
        
        // Add double-click handler for authors
        authorsTable.setRowFactory(tv -> {
            TableRow<AuthorData> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    AuthorData author = row.getItem();
                    showAuthorForm(author);
                }
            });
            return row;
        });
        
        // Replace the books table in the UI
        container.getChildren().set(index, authorsTable);
        
        // Update the reference
        booksTable = (TableView<BookData>)(TableView<?>)authorsTable;
    }
    
    private void setupCategoriesTable() {
        // Store the parent of the current table
        Parent parent = booksTable.getParent();
        VBox container = (VBox) parent;
        int index = container.getChildren().indexOf(booksTable);
        
        // Create a new TableView for categories with the correct type
        TableView<CategoryData> categoriesTable = new TableView<>();
        categoriesTable.setPrefHeight(booksTable.getPrefHeight());
        
        TableColumn<CategoryData, String> nameColumn = new TableColumn<>("Category Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(150);
        
        TableColumn<CategoryData, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descriptionColumn.setPrefWidth(200);
        
        categoriesTable.getColumns().addAll(nameColumn, descriptionColumn);
        
        // Add double-click handler for categories
        categoriesTable.setRowFactory(tv -> {
            TableRow<CategoryData> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    CategoryData category = row.getItem();
                    showCategoryForm(category);
                }
            });
            return row;
        });
        
        // Replace the books table in the UI
        container.getChildren().set(index, categoriesTable);
        
        // Update the reference
        booksTable = (TableView<BookData>)(TableView<?>)categoriesTable;
    }
    
    // ========== Data Refresh Methods ==========
    
    private void refreshBooksList() {
        try {
            List<Map<String, Object>> books = db.getAllBooks();
            ObservableList<BookData> booksList = FXCollections.observableArrayList();
            
            for (Map<String, Object> book : books) {
                booksList.add(new BookData(book));
            }
            
            runOnUiThread(() -> {
                booksTable.setItems(booksList);
                updateStatus("Loaded " + books.size() + " books");
            });
        } catch (SQLException e) {
            safeShowError("Database Error", "Failed to load books: " + e.getMessage());
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
            runOnUiThread(() -> {
                booksTable.setItems(castedList);
                updateStatus("Loaded " + authors.size() + " authors");
            });
        } catch (SQLException e) {
            safeShowError("Database Error", "Failed to load authors: " + e.getMessage());
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
            runOnUiThread(() -> {
                booksTable.setItems(castedList);
                updateStatus("Loaded " + categories.size() + " categories");
            });
        } catch (SQLException e) {
            safeShowError("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }
    
    private void refreshBorrowersList() {
        try {
            List<Map<String, Object>> borrowers = db.getAllBorrowers();
            ObservableList<BorrowerData> borrowersList = FXCollections.observableArrayList();
            
            for (Map<String, Object> borrower : borrowers) {
                borrowersList.add(new BorrowerData(borrower));
            }
            
            runOnUiThread(() -> {
                borrowersTable.setItems(borrowersList);
                updateStatus("Loaded " + borrowers.size() + " borrowers");
            });
        } catch (SQLException e) {
            safeShowError("Database Error", "Failed to load borrowers: " + e.getMessage());
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
                
                // When not in history view, all books should be active and bold
                for (Map<String, Object> loan : loans) {
                    loan.put("status", "active");
                }
            }
            
            ObservableList<LoanData> loansList = FXCollections.observableArrayList();
            
            // Sort loans: active first, then returned
            List<LoanData> activeLoans = new ArrayList<>();
            List<LoanData> returnedLoans = new ArrayList<>();
            
            for (Map<String, Object> loan : loans) {
                LoanData loanData = new LoanData(loan);
                if ("active".equals(loanData.getStatus())) {
                    activeLoans.add(loanData);
                } else {
                    returnedLoans.add(loanData);
                }
            }
            
            // Add active loans first, then returned loans
            loansList.addAll(activeLoans);
            loansList.addAll(returnedLoans);
            
            runOnUiThread(() -> {
                borrowedBooksTable.setItems(loansList);
            });
        } catch (SQLException e) {
            safeShowError("Database Error", "Failed to load borrowed books: " + e.getMessage());
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
            
            runOnUiThread(() -> {
                booksTable.setItems(booksList);
                updateStatus("Found " + books.size() + " books matching '" + searchTerm + "'");
            });
        } catch (SQLException e) {
            safeShowError("Search Error", "Failed to search books: " + e.getMessage());
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
            runOnUiThread(() -> {
                booksTable.setItems(castedList);
                updateStatus("Found " + authors.size() + " authors matching '" + searchTerm + "'");
            });
        } catch (SQLException e) {
            safeShowError("Search Error", "Failed to search authors: " + e.getMessage());
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
            runOnUiThread(() -> {
                booksTable.setItems(castedList);
                updateStatus("Found " + categories.size() + " categories matching '" + searchTerm + "'");
            });
        } catch (SQLException e) {
            safeShowError("Search Error", "Failed to search categories: " + e.getMessage());
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
            
            runOnUiThread(() -> {
                borrowersTable.setItems(borrowersList);
                updateStatus("Found " + borrowers.size() + " borrowers matching '" + searchTerm + "'");
            });
        } catch (SQLException e) {
            safeShowError("Search Error", "Failed to search borrowers: " + e.getMessage());
        }
    }
    
    // ========== Action Methods ==========
    
    private void borrowSelectedBook() {
        List<BookData> selectedBooks = new ArrayList<>(booksTable.getSelectionModel().getSelectedItems());
        
        if (selectedBooks.isEmpty()) {
            safeShowError("No Selection", "Please select at least one book to borrow");
            return;
        }
        
        if (selectedBorrower == null) {
            safeShowError("No Borrower Selected", "Please select a borrower first");
            return;
        }
        
        // Calculate due date (30 days from now)
        LocalDate dueDate = LocalDate.now().plusDays(30);
        String dueDateStr = dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        int successCount = 0;
        StringBuilder failedBooks = new StringBuilder();
        
        for (BookData book : selectedBooks) {
            try {
                boolean success = db.borrowBook(book.getBookId(), selectedBorrower.getCardNumber(), dueDateStr);
                
                if (success) {
                    successCount++;
                } else {
                    if (failedBooks.length() > 0) {
                        failedBooks.append(", ");
                    }
                    failedBooks.append(book.getTitle());
                }
            } catch (SQLException e) {
                safeShowError("Database Error", "Failed to borrow book: " + e.getMessage());
            }
        }
        
        if (successCount > 0) {
            safeUpdateStatus(successCount + " book(s) borrowed by " + selectedBorrower.getFullName());
            refreshBooksList();
            refreshBorrowedBooks();
        }
        
        if (failedBooks.length() > 0) {
            safeShowError("Some Books Not Available", 
                    "The following books are not available for borrowing: " + failedBooks.toString());
        }
    }

    private void returnSelectedBook() {
        // Get all selected loans
        List<LoanData> selectedLoans = new ArrayList<>(borrowedBooksTable.getSelectionModel().getSelectedItems());

        if (selectedLoans.isEmpty()) {
            safeShowError("No Selection", "Please select at least one book to return");
            return;
        }

        // Debug selected loans status
        for (LoanData loan : selectedLoans) {
            System.out.println("Selected loan: " + loan.getTitle() + ", Status: " + loan.getStatus());
        }

        // Return each selected book
        int successCount = 0;
        int failCount = 0;

        for (LoanData loan : selectedLoans) {
            try {
                // Try to return regardless of status check - might be a status reporting issue
                boolean success = db.returnBook(loan.getReservationId());
                if (success) {
                    successCount++;
                    System.out.println("Successfully returned: " + loan.getTitle());
                } else {
                    failCount++;
                    System.out.println("Failed to return: " + loan.getTitle());
                }
            } catch (SQLException e) {
                safeShowError("Database Error", "Failed to return book: " + e.getMessage());
                System.out.println("SQL error returning book: " + e.getMessage());
                failCount++;
            }
        }

        // Refresh the UI regardless of success/failure to ensure consistent state
        refreshBooksList();

        // Always refresh borrowed books view to show current state
        refreshBorrowedBooks();

        // Show appropriate message based on results
        if (successCount > 0) {
            safeUpdateStatus(successCount + " book(s) returned successfully");

            // Toggle history view if not already selected to show returned books
            if (!viewHistoryToggle.isSelected()) {
                viewHistoryToggle.setSelected(true);
                refreshBorrowedBooks(); // Refresh again with history view
            }
        } else if (failCount > 0) {
            safeShowError("Return Failed", "Failed to return " + failCount + " book(s)");
        } else {
            safeShowError("Return Failed", "Could not return any of the selected books. They may be unavailable or already returned.");
        }
    }
    
    // ========== Form Dialog Methods ==========
    
    private void showCategoryForm() {
        showCategoryForm(null);
    }
    
    private void showCategoryForm(CategoryData category) {
        FormDialog dialog = new FormDialog(category == null ? "New Category" : "Edit Category");
        
        TextField nameField = dialog.addTextField("Category Name", 0);
        TextField descriptionField = dialog.addTextField("Description", 1);
        
        // If editing, populate fields
        if (category != null) {
            nameField.setText(category.getName());
            descriptionField.setText(category.getDescription());
        }
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    String name = nameField.getText();
                    String description = descriptionField.getText();
                    
                    if (name == null || name.trim().isEmpty()) {
                        safeShowError("Validation Error", "Category name cannot be empty");
                        return false;
                    }
                    
                    if (category == null) {
                        // Add new category
                        db.addCategory(name, description);
                        safeUpdateStatus("Category '" + name + "' added successfully");
                    } else {
                        // Update existing category
                        db.updateCategory(category.getCategoryId(), name, description);
                        safeUpdateStatus("Category '" + name + "' updated successfully");
                    }
                    
                    refreshCategoriesList();
                    return true;
                } catch (SQLException e) {
                    safeShowError("Database Error", "Failed to save category: " + e.getMessage());
                    return false;
                }
            }
            return false;
        });
        
        dialog.showAndWait();
    }
    
    private void showAuthorForm() {
        showAuthorForm(null);
    }
    
    private void showAuthorForm(AuthorData author) {
        FormDialog dialog = new FormDialog(author == null ? "New Author" : "Edit Author");
        
        TextField firstNameField = dialog.addTextField("First Name", 0);
        TextField lastNameField = dialog.addTextField("Last Name", 1);
        TextField birthYearField = dialog.addTextField("Birth Year", 2);
        TextField biographyField = dialog.addTextField("Biography", 3);
        
        // If editing, populate fields
        if (author != null) {
            firstNameField.setText(author.getFirstName());
            lastNameField.setText(author.getLastName());
            birthYearField.setText(author.getBirthYear() > 0 ? String.valueOf(author.getBirthYear()) : "");
            biographyField.setText(author.getBiography());
        }
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                try {
                    String firstName = firstNameField.getText();
                    String lastName = lastNameField.getText();
                    String birthYearStr = birthYearField.getText();
                    String biography = biographyField.getText();
                    
                    if (firstName == null || firstName.trim().isEmpty() || 
                        lastName == null || lastName.trim().isEmpty()) {
                        safeShowError("Validation Error", "First name and last name cannot be empty");
                        return false;
                    }
                    
                    int birthYear = 0;
                    if (birthYearStr != null && !birthYearStr.trim().isEmpty()) {
                        try {
                            birthYear = Integer.parseInt(birthYearStr);
                        } catch (NumberFormatException e) {
                            safeShowError("Validation Error", "Birth year must be a number");
                            return false;
                        }
                    }
                    
                    if (author == null) {
                        // Add new author
                        db.addAuthor(firstName, lastName, birthYear, biography);
                        safeUpdateStatus("Author '" + firstName + " " + lastName + "' added successfully");
                    } else {
                        // Update existing author
                        db.updateAuthor(author.getAuthorId(), firstName, lastName, birthYear, biography);
                        safeUpdateStatus("Author '" + firstName + " " + lastName + "' updated successfully");
                    }
                    
                    refreshAuthorsList();
                    return true;
                } catch (SQLException e) {
                    safeShowError("Database Error", "Failed to save author: " + e.getMessage());
                    return false;
                }
            }
            return false;
        });
        
        dialog.showAndWait();
    }
    
    private void showBookForm() {
        showBookForm(null);
    }
    
    private void showBookForm(BookData book) {
        FormDialog dialog = new FormDialog(book == null ? "New Book" : "Edit Book");
        
        TextField titleField = dialog.addTextField("Title", 0);
        TextField isbnField = dialog.addTextField("ISBN", 1);
        TextField yearField = dialog.addTextField("Publication Year", 2);
        TextField publisherField = dialog.addTextField("Publisher", 3);
        TextField copiesField = dialog.addTextField("Total Copies", 4);
        
        // If editing, populate fields
        if (book != null) {
            titleField.setText(book.getTitle());
            isbnField.setText(book.getIsbn());
            yearField.setText(book.getPublicationYear() > 0 ? String.valueOf(book.getPublicationYear()) : "");
            publisherField.setText(book.getPublisher());
            copiesField.setText(String.valueOf(book.getTotalCopies()));
        }
        
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
            
            // If editing, select the current category
            if (book != null) {
                for (CategoryData category : categoriesList) {
                    if (category.getName().equals(book.getCategory())) {
                        categoryComboBox.setValue(category);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            safeShowError("Database Error", "Failed to load categories: " + e.getMessage());
        }
        
        // Add authors selection
        ListView<AuthorData> authorsListView = new ListView<>();
        authorsListView.setPrefHeight(100);
        authorsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        try {
            List<Map<String, Object>> allAuthors = db.getAllAuthors();
            ObservableList<AuthorData> authorsList = FXCollections.observableArrayList();
            for (Map<String, Object> author : allAuthors) {
                authorsList.add(new AuthorData(author));
            }
            
            authorsListView.setItems(authorsList);
            
            // Set cell factory to display author name
            authorsListView.setCellFactory(lv -> new ListCell<AuthorData>() {
                @Override
                protected void updateItem(AuthorData item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    } else {
                        setText(item.getFullName());
                    }
                }
            });
            
            dialog.addNode("Authors (select multiple)", authorsListView, 6);
            
            // If editing, select the current authors
            if (book != null && book.getAuthors() != null) {
                String[] bookAuthors = book.getAuthors().split(", ");
                for (String authorName : bookAuthors) {
                    for (AuthorData author : authorsList) {
                        if (author.getFullName().equals(authorName)) {
                            authorsListView.getSelectionModel().select(author);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            safeShowError("Database Error", "Failed to load authors: " + e.getMessage());
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
                        safeShowError("Validation Error", "Title and ISBN cannot be empty");
                        return false;
                    }
                    
                    if (category == null) {
                        safeShowError("Validation Error", "Please select a category");
                        return false;
                    }
                    
                    int year = 0;
                    if (yearStr != null && !yearStr.trim().isEmpty()) {
                        try {
                            year = Integer.parseInt(yearStr);
                        } catch (NumberFormatException e) {
                            safeShowError("Validation Error", "Publication year must be a number");
                            return false;
                        }
                    }
                    
                    int copies = 1;
                    if (copiesStr != null && !copiesStr.trim().isEmpty()) {
                        try {
                            copies = Integer.parseInt(copiesStr);
                            if (copies < 1) {
                                safeShowError("Validation Error", "Total copies must be at least 1");
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            safeShowError("Validation Error", "Total copies must be a number");
                            return false;
                        }
                    }
                    
                    // Get selected authors
                    List<Integer> authorIds = new ArrayList<>();
                    for (AuthorData author : authorsListView.getSelectionModel().getSelectedItems()) {
                        authorIds.add(author.getAuthorId());
                    }
                    
                    if (book == null) {
                        // Add new book
                        db.addBook(title, isbn, year, publisher, copies, category.getCategoryId(), authorIds);
                        safeUpdateStatus("Book '" + title + "' added successfully");
                    } else {
                        // Update existing book
                        db.updateBook(book.getBookId(), title, isbn, year, publisher, copies, 
                                     category.getCategoryId(), authorIds);
                        safeUpdateStatus("Book '" + title + "' updated successfully");
                    }
                    
                    refreshBooksList();
                    return true;
                } catch (SQLException e) {
                    safeShowError("Database Error", "Failed to save book: " + e.getMessage());
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
                        safeShowError("Validation Error", "First name, last name, and email cannot be empty");
                        return false;
                    }
                    
                    if (borrower == null) {
                        // Add new borrower
                        db.addBorrower(firstName, lastName, address, phone, email);
                        safeUpdateStatus("Borrower '" + firstName + " " + lastName + "' added successfully");
                    } else {
                        // Update existing borrower
                        db.updateBorrower(borrower.getCardNumber(), firstName, lastName, address, phone, email);
                        safeUpdateStatus("Borrower '" + firstName + " " + lastName + "' updated successfully");
                    }
                    
                    refreshBorrowersList();
                    return true;
                } catch (SQLException e) {
                    safeShowError("Database Error", "Failed to save borrower: " + e.getMessage());
                    return false;
                }
            }
            return false;
        });
        
        dialog.showAndWait();
    }
    
    // ========== Utility Methods ==========
    
    private void runOnUiThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
    
    private void safeShowError(String title, String message) {
        runOnUiThread(() -> showError(title, message));
    }

    private void safeUpdateStatus(String message) {
        runOnUiThread(() -> updateStatus(message));
    }
    
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
        LibraryDatabase db = new LibraryDatabase();
        try {
            db.initializeDatabase();
            db.seedDatabase();
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            return;
        }
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
    
    private void deleteSelectedBooks() {
        List<BookData> selectedBooks = new ArrayList<>(booksTable.getSelectionModel().getSelectedItems());
        if (selectedBooks.isEmpty()) {
            safeShowError("No Selection", "Please select at least one book to delete");
            return;
        }
        int successCount = 0;
        int failCount = 0;
        for (BookData book : selectedBooks) {
            try {
                boolean deleted = db.deleteBook(book.getBookId());
                if (deleted) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (SQLException e) {
                failCount++;
            }
        }
        refreshBooksList();
        if (successCount > 0) {
            safeUpdateStatus(successCount + " book(s) deleted successfully");
        }
        if (failCount > 0) {
            safeShowError("Delete Failed", "Failed to delete " + failCount + " book(s). They may have active loans.");
        }
    }
    
    private void deleteSelectedBorrowers() {
        List<BorrowerData> selectedBorrowers = new ArrayList<>(borrowersTable.getSelectionModel().getSelectedItems());
        if (selectedBorrowers.isEmpty()) {
            safeShowError("No Selection", "Please select at least one user to delete");
            return;
        }
        int successCount = 0;
        int failCount = 0;
        for (BorrowerData borrower : selectedBorrowers) {
            try {
                boolean deleted = db.deleteBorrower(borrower.getCardNumber());
                if (deleted) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (SQLException e) {
                failCount++;
            }
        }
        refreshBorrowersList();
        if (successCount > 0) {
            safeUpdateStatus(successCount + " user(s) deleted successfully");
        }
        if (failCount > 0) {
            safeShowError("Delete Failed", "Failed to delete " + failCount + " user(s). They may have active loans.");
        }
    }
}
