package libraryapp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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

    @Override
    public void start(Stage primaryStage) {
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
            Main.initializeDatabase();
        }).start();
    }
    
    private VBox createLeftPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(350);
        
        // Search type dropdown
        Label searchLabel = new Label("Search Type");
        ComboBox<String> searchTypeComboBox = new ComboBox<>();
        searchTypeComboBox.getItems().addAll("Books", "Authors", "Categories");
        searchTypeComboBox.setValue("Books");
        
        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search box");
        
        // Search results area
        TableView<Object> resultsTable = new TableView<>();
        resultsTable.setPrefHeight(300);
        
        // Create dynamic columns based on search type
        searchTypeComboBox.setOnAction(e -> {
            resultsTable.getColumns().clear();
            
            switch(searchTypeComboBox.getValue()) {
                case "Books":
                    TableColumn<Object, String> titleColumn = new TableColumn<>("Title");
                    TableColumn<Object, String> authorColumn = new TableColumn<>("Author");
                    TableColumn<Object, String> isbnColumn = new TableColumn<>("ISBN");
                    resultsTable.getColumns().addAll(titleColumn, authorColumn, isbnColumn);
                    break;
                case "Authors":
                    TableColumn<Object, String> nameColumn = new TableColumn<>("Name");
                    TableColumn<Object, String> birthYearColumn = new TableColumn<>("Birth Year");
                    TableColumn<Object, String> countryColumn = new TableColumn<>("Country");
                    resultsTable.getColumns().addAll(nameColumn, birthYearColumn, countryColumn);
                    break;
                case "Categories":
                    TableColumn<Object, String> categoryNameColumn = new TableColumn<>("Category Name");
                    TableColumn<Object, String> descriptionColumn = new TableColumn<>("Description");
                    resultsTable.getColumns().addAll(categoryNameColumn, descriptionColumn);
                    break;
            }
        });
        
        // Trigger initial column setup
        searchTypeComboBox.fireEvent(new javafx.event.ActionEvent());
        
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
        newCategoryButton.setOnAction(e -> showFormDialog("New Category"));
        newAuthorButton.setOnAction(e -> showFormDialog("New Author"));
        newBookButton.setOnAction(e -> showFormDialog("New Book"));
        
        panel.getChildren().addAll(
            searchLabel, searchTypeComboBox, searchField, 
            resultsTable, actionButtonsBox
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
        TextField borrowerSearchField = new TextField();
        borrowerSearchField.setPromptText("Search for borrower (name, email, etc)");
        
        // Selected borrower
        HBox borrowerBox = new HBox(10);
        Label selectedBorrowerLabel = new Label("Selected Borrower (blank at start)");
        CheckBox viewHistoryToggle = new CheckBox("View History");
        borrowerBox.getChildren().addAll(selectedBorrowerLabel, viewHistoryToggle);
        
        // Borrower status and books
        TableView<Object> borrowerBooksTable = new TableView<>();
        borrowerBooksTable.setPrefHeight(300);
        
        // Add table columns
        TableColumn<Object, String> bookTitleColumn = new TableColumn<>("Book Title");
        TableColumn<Object, String> isbnColumn = new TableColumn<>("ISBN");
        TableColumn<Object, String> dueDateColumn = new TableColumn<>("Due Date");
        borrowerBooksTable.getColumns().addAll(bookTitleColumn, isbnColumn, dueDateColumn);
        
        // Borrower action buttons
        HBox borrowerActionBox = new HBox(10);
        borrowerActionBox.setPadding(new Insets(10, 0, 0, 0));
        borrowerActionBox.setAlignment(Pos.CENTER);
        
        Button newBorrowerButton = new Button("New Borrower");
        Button editBorrowerButton = new Button("Edit Borrower");
        
        borrowerActionBox.getChildren().addAll(newBorrowerButton, editBorrowerButton);
        
        // Add action handlers
        newBorrowerButton.setOnAction(e -> showFormDialog("New Borrower"));
        editBorrowerButton.setOnAction(e -> showFormDialog("Edit Borrower"));
        
        panel.getChildren().addAll(
            borrowerSearchField, borrowerBox, 
            borrowerBooksTable, borrowerActionBox
        );
        
        return panel;
    }
    
    private HBox createBottomPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(20, 10, 10, 10));
        panel.setAlignment(Pos.CENTER);
        
        // Status label for the bottom panel
        Label statusLabel = new Label("Library Management System - Ready");
        statusLabel.setStyle("-fx-font-style: italic;");
        
        panel.getChildren().add(statusLabel);
        
        return panel;
    }
    
    private void showFormDialog(String title) {
        FormDialog dialog = new FormDialog(title);
        dialog.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
//        LibraryDatabase db = new LibraryDatabase();
//        try {
//            db.initializeDatabase();
//            db.seedDatabase();
//        } catch (Exception e) {
//            System.err.println("Error initializing database: " + e.getMessage());
//            e.printStackTrace();
//        }
    }
}
