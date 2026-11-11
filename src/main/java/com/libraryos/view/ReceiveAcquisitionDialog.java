package com.libraryos.view;

import com.libraryos.model.acquisition.Acquisition;
import com.libraryos.model.inventory.Shelf;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.util.List;

public class ReceiveAcquisitionDialog extends Dialog<Long> {

    private final ComboBox<Shelf> shelfComboBox;

    public ReceiveAcquisitionDialog(Window owner, Acquisition acquisition, List<Shelf> shelves) {
        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Receive Acquisition Order / পণ্য গ্রহণ ও ইনভেন্টরিতে যুক্ত");
        setHeaderText("Receive physical shipment and auto-generate inventory copies with barcodes.");

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("card-panel");
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) pane.lookupButton(ButtonType.OK);
        okBtn.setText("Receive & Generate Copies / গ্রহণ ও কপি তৈরি");
        okBtn.getStyleClass().add("btn-primary");

        VBox content = new VBox(14);
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Acquisition PO #:"), 0, 0);
        Label poLabel = new Label(acquisition.getAcquisitionNumber());
        poLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #1E3A8A;");
        grid.add(poLabel, 1, 0);

        grid.add(new Label("Supplier / সরবরাহকারী:"), 0, 1);
        grid.add(new Label(acquisition.getSupplierName()), 1, 1);

        grid.add(new Label("Total Quantity / মোট কপি:"), 0, 2);
        Label qtyLabel = new Label(acquisition.getTotalQuantity() + " physical copies to generate");
        qtyLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #16A34A;");
        grid.add(qtyLabel, 1, 2);

        grid.add(new Label("Target Placement Shelf / সেলফ:"), 0, 3);
        shelfComboBox = new ComboBox<>();
        shelfComboBox.getItems().addAll(shelves);
        shelfComboBox.setPromptText("Choose initial shelf (optional)...");
        shelfComboBox.setPrefWidth(260);
        grid.add(shelfComboBox, 1, 3);

        Label notice = new Label("Notice: Upon receiving, unique Accession Numbers (ACC-...) and Code-128 Barcodes will be automatically assigned to each physical book unit.");
        notice.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        notice.setWrapText(true);

        content.getChildren().addAll(grid, notice);
        pane.setContent(content);

        setResultConverter(btnType -> {
            if (btnType == ButtonType.OK) {
                Shelf selShelf = shelfComboBox.getValue();
                return selShelf != null ? selShelf.getId() : null;
            }
            return null;
        });
    }
}

