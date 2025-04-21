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
 */
public class LibraryUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));
        mainLayout.setStyle("-fx-background-color: #999999; -fx-background-radius: 30;");
        
        // Left side - Book search and results
        VBox leftPanel = createLeftPanel();
        
        // Right side - Borrower search and results
        VBox rightPanel = createRightPanel();
        
        // Bottom panel - Action buttons
        HBox bottomPanel = createBottomPanel();
        
        // Add panels to main layout
        mainLayout.setLeft(leftPanel);
        mainLayout.setRight(rightPanel);
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
        panel.setPrefWidth(400);
        
        // Search field
        TextField searchField = new TextField();
        searchField.setPromptText("Search box");
        Label searchLabel = new Label("Search Field (Author, category, etc)");
        
        // Search results area
        TableView<Object> resultsTable = new TableView<>();
        resultsTable.setPrefHeight(300);
        
        Label resultsLabel = new Label(
            "Display search results here. If the drop down selects " +
            "Author, show all authors if the search box is blank. Same " +
            "for category, etc. Selecting a book and clicking \"Borrow\" " +
            "borrows it to the selected borrower (or errors out if no " +
            "borrower selected)."
        );
        resultsLabel.setWrapText(true);
        
        // Multi-select info
        Label multiSelectInfo = new Label("This is a multi-select with columns based on the Search Field.");
        multiSelectInfo.setWrapText(true);
        
        // Borrow/Return buttons
        Button borrowButton = new Button("Borrow >>>");
        Button returnButton = new Button("<<< Return");
        
        HBox buttonBox = new HBox(10, borrowButton, returnButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        panel.getChildren().addAll(
            searchLabel, searchField, 
            resultsLabel, resultsTable, 
            multiSelectInfo, buttonBox
        );
        
        return panel;
    }
    
    private VBox createRightPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(400);
        
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
        
        Label borrowerStatusLabel = new Label(
            "Display borrower status. Only borrowed books are shown here. " +
            "Selecting a book and clicking \"Return\" returns it. If view " +
            "history is toggled, show the history below borrowed books. " +
            "Don't let any selection of historical books."
        );
        borrowerStatusLabel.setWrapText(true);
        
        // Multi-select info
        Label multiSelectInfo = new Label("This is a multi-select with columns for Current Book Title, ISBN, Due date.");
        multiSelectInfo.setWrapText(true);
        
        // Edit selected item button
        Button editSelectedItemButton = new Button("Edit Selected Item");
        
        panel.getChildren().addAll(
            borrowerSearchField, borrowerBox, 
            borrowerBooksTable, borrowerStatusLabel, 
            multiSelectInfo, editSelectedItemButton
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
