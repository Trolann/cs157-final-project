package libraryapp;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

/**
 * A reusable dialog class for creating forms for Categories, Authors, Books, and Borrowers
 * as mentioned in the mockup.
 */
public class FormDialog extends Dialog<Boolean> {
    
    private final GridPane formGrid = new GridPane();
    
    public FormDialog(String title) {
        setTitle(title);
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);
        
        // Configure the dialog
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        formGrid.setPadding(new Insets(20, 150, 10, 10));
        
        // We'll let the calling code add the appropriate fields
        // instead of adding default fields here to avoid duplication
        
        getDialogPane().setContent(formGrid);
        
        // Request focus on the first field
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setOnShown(e -> {
            if (!formGrid.getChildren().isEmpty() && formGrid.getChildren().get(1) instanceof TextField) {
                ((TextField) formGrid.getChildren().get(1)).requestFocus();
            }
        });
        
        // Convert the result to Boolean when the save button is clicked
        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return true;
            }
            return false;
        });
    }
    
    /**
     * Add a text field to the form
     * 
     * @param label The label for the field
     * @param row The row in the grid
     * @return The created text field
     */
    public TextField addTextField(String label, int row) {
        Label fieldLabel = new Label(label);
        TextField textField = new TextField();
        formGrid.add(fieldLabel, 0, row);
        formGrid.add(textField, 1, row);
        return textField;
    }
    
    /**
     * Add a combo box to the form
     * 
     * @param label The label for the field
     * @param row The row in the grid
     * @return The created combo box
     */
    public ComboBox<String> addComboBox(String label, int row) {
        Label fieldLabel = new Label(label);
        ComboBox<String> comboBox = new ComboBox<>();
        formGrid.add(fieldLabel, 0, row);
        formGrid.add(comboBox, 1, row);
        return comboBox;
    }
    
    /**
     * Add a node to the form grid.
     * 
     * @param label The label for the node
     * @param node The node to add
     * @param row The row to add the node to
     * @return The node that was added
     */
    public <T extends javafx.scene.Node> T addNode(String label, T node, int row) {
        Label fieldLabel = new Label(label);
        formGrid.add(fieldLabel, 0, row);
        formGrid.add(node, 1, row);
        return node;
    }
}
