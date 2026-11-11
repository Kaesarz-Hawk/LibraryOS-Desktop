package com.libraryos.view;

import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.reservation.Reservation;
import com.libraryos.model.reservation.ReservationStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ReservationsView extends BorderPane {

    private final Button placeReservationBtn = new Button("+ Place Hold / সংরক্ষণ আবেদন");
    private final Button processExpiredBtn = new Button("⏱ Process Expired Holds");
    private final Button refreshBtn = new Button("↻ Refresh");

    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final Button searchBtn = new Button("Filter");
    private final Button resetBtn = new Button("Reset");

    private final TableView<Reservation> reservationsTable = new TableView<>();
    private final Label countLabel = new Label("Showing 0 reservations");

    public ReservationsView() {
        getStyleClass().add("catalog-view");
        setPadding(new Insets(24));

        setTop(buildTopSection());
        setCenter(buildTableSection());
    }

    private VBox buildTopSection() {
        VBox topBox = new VBox(16);

        // Header
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Reservations Queue / সংরক্ষণ ব্যবস্থাপনা");
        title.getStyleClass().add("view-title");
        Label subtitle = new Label("FIFO hold queue for checked-out books, automated copy reservation on return, and hold expirations");
        subtitle.getStyleClass().add("view-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        placeReservationBtn.getStyleClass().add("btn-primary");
        processExpiredBtn.getStyleClass().add("btn-secondary");
        refreshBtn.getStyleClass().add("btn-secondary");

        headerRow.getChildren().addAll(titleBox, spacer, placeReservationBtn, processExpiredBtn, refreshBtn);

        // Filter Bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(12, 16, 12, 16));
        filterBar.getStyleClass().add("filter-container");

        searchField.setPromptText("Search by book title, member name, or card #...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(320);

        statusFilter.getItems().addAll("ALL", "WAITING", "READY_FOR_PICKUP", "FULFILLED", "CANCELLED", "EXPIRED");
        statusFilter.setValue("ALL");
        statusFilter.getStyleClass().add("form-input");
        statusFilter.setPrefWidth(170);

        searchBtn.getStyleClass().add("btn-primary");
        resetBtn.getStyleClass().add("btn-secondary");

        filterBar.getChildren().addAll(
                new Label("Search:"), searchField,
                new Label("Status:"), statusFilter,
                searchBtn, resetBtn
        );

        topBox.getChildren().addAll(headerRow, filterBar);
        return topBox;
    }

    private VBox buildTableSection() {
        VBox tableBox = new VBox(10);
        tableBox.setPadding(new Insets(12, 0, 0, 0));

        TableColumn<Reservation, String> colId = new TableColumn<>("#");
        colId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colId.setPrefWidth(50);

        TableColumn<Reservation, String> colBook = new TableColumn<>("Book Title / বই");
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookTitle() != null ? d.getValue().getBookTitle() : "—"));
        colBook.setPrefWidth(220);

        TableColumn<Reservation, String> colMember = new TableColumn<>("Member / সদস্য");
        colMember.setCellValueFactory(d -> new SimpleStringProperty(
                (d.getValue().getMemberName() != null ? d.getValue().getMemberName() : "—")
                + " (" + (d.getValue().getMembershipNumber() != null ? d.getValue().getMembershipNumber() : "—") + ")"
        ));
        colMember.setPrefWidth(180);

        TableColumn<Reservation, ReservationStatus> colStatus = new TableColumn<>("Status / অবস্থা");
        colStatus.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ReservationStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status.name());
                    switch (status) {
                        case WAITING -> badge.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case READY_FOR_PICKUP -> badge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case FULFILLED -> badge.setStyle("-fx-background-color: #F3E8FF; -fx-text-fill: #7E22CE; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                        case CANCELLED -> badge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-size: 11px;");
                        case EXPIRED -> badge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colStatus.setPrefWidth(140);

        TableColumn<Reservation, String> colBarcode = new TableColumn<>("Allocated Copy / বারকোড");
        colBarcode.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getAllocatedBarcode() != null ? d.getValue().getAllocatedBarcode() : "—"
        ));
        colBarcode.setPrefWidth(140);

        TableColumn<Reservation, String> colRequested = new TableColumn<>("Requested At");
        colRequested.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getRequestedAt() != null ? DateTimeUtil.formatMediumDateTime(d.getValue().getRequestedAt()) : "—"
        ));
        colRequested.setPrefWidth(150);

        TableColumn<Reservation, String> colExpires = new TableColumn<>("Pickup Expiration");
        colExpires.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getExpiresAt() != null ? DateTimeUtil.formatMediumDateTime(d.getValue().getExpiresAt()) : "—"
        ));
        colExpires.setPrefWidth(150);

        TableColumn<Reservation, String> colNotes = new TableColumn<>("Notes");
        colNotes.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNotes() != null ? d.getValue().getNotes() : ""));
        colNotes.setPrefWidth(150);

        reservationsTable.getColumns().addAll(colId, colBook, colMember, colStatus, colBarcode, colRequested, colExpires, colNotes);
        reservationsTable.setPlaceholder(new Label("No hold reservations found."));
        reservationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(reservationsTable, Priority.ALWAYS);

        HBox footer = new HBox(countLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(4, 0, 0, 0));
        countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        tableBox.getChildren().addAll(reservationsTable, footer);
        VBox.setVgrow(tableBox, Priority.ALWAYS);
        return tableBox;
    }

    public Button getPlaceReservationBtn() { return placeReservationBtn; }
    public Button getProcessExpiredBtn() { return processExpiredBtn; }
    public Button getRefreshBtn() { return refreshBtn; }
    public TextField getSearchField() { return searchField; }
    public ComboBox<String> getStatusFilter() { return statusFilter; }
    public Button getSearchBtn() { return searchBtn; }
    public Button getResetBtn() { return resetBtn; }
    public TableView<Reservation> getReservationsTable() { return reservationsTable; }
    public Label getCountLabel() { return countLabel; }
}
