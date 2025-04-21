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
        
        // Bottom panel - Action buttons
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
        
        // Search field
        Label searchLabel = new Label("Search Field (Author, category, etc)");
        TextField searchField = new TextField();
        searchField.setPromptText("Search box");
        
        // Search results area
        TableView<Object> resultsTable = new TableView<>();
        resultsTable.setPrefHeight(300);
        
        // Add table columns
        TableColumn<Object, String> titleColumn = new TableColumn<>("Title");
        TableColumn<Object, String> authorColumn = new TableColumn<>("Author");
        TableColumn<Object, String> isbnColumn = new TableColumn<>("ISBN");
        resultsTable.getColumns().addAll(titleColumn, authorColumn, isbnColumn);
        
        // Multi-select info as a comment in code
        // This is a multi-select with columns based on the Search Field
        
        panel.getChildren().addAll(
            searchLabel, searchField, 
            resultsTable
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
        ToggleButton viewHistoryToggle = new ToggleButton("View history toggle");
        borrowerBox.getChildren().addAll(selectedBorrowerLabel, viewHistoryToggle);
        
        // Borrower status and books
        TableView<Object> borrowerBooksTable = new TableView<>();
        borrowerBooksTable.setPrefHeight(300);
        
        // Add table columns
        TableColumn<Object, String> bookTitleColumn = new TableColumn<>("Book Title");
        TableColumn<Object, String> isbnColumn = new TableColumn<>("ISBN");
        TableColumn<Object, String> dueDateColumn = new TableColumn<>("Due Date");
        borrowerBooksTable.getColumns().addAll(bookTitleColumn, isbnColumn, dueDateColumn);
        
        // Edit selected item button
        Button editSelectedItemButton = new Button("Edit Selected Item");
        
        panel.getChildren().addAll(
            borrowerSearchField, borrowerBox, 
            borrowerBooksTable, editSelectedItemButton
        );
        
        return panel;
    }
    
    private HBox createBottomPanel() {
        HBox panel = new HBox(20);
        panel.setPadding(new Insets(20, 10, 10, 10));
        panel.setAlignment(Pos.CENTER);
        
        // Action buttons
        Button newCategoryButton = new Button("New Category");
        Button newAuthorButton = new Button("New Author");
        Button newBookButton = new Button("New Book");
        Button newBorrowerButton = new Button("New Borrower");
        Button editBorrowerButton = new Button("Edit Borrower");
        
        panel.getChildren().addAll(
            newCategoryButton, newAuthorButton, newBookButton, 
            newBorrowerButton, editBorrowerButton
        );
        
        // Add action handlers for the popup forms
        newCategoryButton.setOnAction(e -> showFormDialog("New Category"));
        newAuthorButton.setOnAction(e -> showFormDialog("New Author"));
        newBookButton.setOnAction(e -> showFormDialog("New Book"));
        newBorrowerButton.setOnAction(e -> showFormDialog("New Borrower"));
        editBorrowerButton.setOnAction(e -> showFormDialog("Edit Borrower"));
        
        return panel;
    }
    
    private void showFormDialog(String title) {
        FormDialog dialog = new FormDialog(title);
        dialog.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
