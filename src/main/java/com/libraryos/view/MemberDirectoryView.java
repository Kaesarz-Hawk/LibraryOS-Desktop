package com.libraryos.view;

import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.member.Member;
import com.libraryos.model.member.MemberType;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Modern JavaFX Member Directory screen with student roll/ID lookup, Bangladesh phone validation,
 * and status management.
 */
public class MemberDirectoryView extends BorderPane {

    private final TextField searchField;
    private final ComboBox<MemberType> typeFilter;
    private final ComboBox<String> statusFilter;
    private final Button resetButton;
    private final Button addMemberButton;
    private final TableView<Member> memberTable;
    private final Label countLabel;
    private final ProgressIndicator loadingSpinner;

    public MemberDirectoryView() {
        getStyleClass().add("content-area");
        setPadding(new Insets(24));

        // Top Section
        VBox topBox = new VBox(16);

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Members Directory / সদস্যবৃন্দ");
        title.getStyleClass().add("page-title");

        countLabel = new Label("Loading registered members...");
        countLabel.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        loadingSpinner = new ProgressIndicator();
        loadingSpinner.setMaxSize(20, 20);
        loadingSpinner.setVisible(false);

        addMemberButton = new Button("+ Register Member / নতুন সদস্য");
        addMemberButton.getStyleClass().add("btn-primary");

        headerRow.getChildren().addAll(titleBox, spacer, loadingSpinner, addMemberButton);

        // Filter Toolbar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().addAll("card-panel", "filter-container");

        searchField = new TextField();
        searchField.setPromptText("Search by Name, Roll/ID, Phone (01...), or Card #...");
        searchField.getStyleClass().add("form-input");
        searchField.setPrefWidth(340);

        typeFilter = new ComboBox<>();
        typeFilter.setPromptText("All Member Types");
        typeFilter.getStyleClass().add("form-input");
        typeFilter.setPrefWidth(200);

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("ALL", "ACTIVE", "SUSPENDED", "EXPIRED");
        statusFilter.setValue("ALL");
        statusFilter.getStyleClass().add("form-input");
        statusFilter.setPrefWidth(150);

        resetButton = new Button("Reset");
        resetButton.getStyleClass().add("btn-secondary");

        filterBar.getChildren().addAll(searchField, typeFilter, statusFilter, resetButton);
        topBox.getChildren().addAll(headerRow, filterBar);
        setTop(topBox);

        // Center Table View
        memberTable = new TableView<>();
        memberTable.getStyleClass().add("table-view");
        memberTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Member, Number> idCol = new TableColumn<>("#");
        idCol.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        idCol.setMaxWidth(60);

        TableColumn<Member, String> cardCol = new TableColumn<>("Card Number");
        cardCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMembershipNumber()));
        cardCol.setMinWidth(130);

        TableColumn<Member, String> nameCol = new TableColumn<>("Full Name / নাম");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayName()));
        nameCol.setMinWidth(220);

        TableColumn<Member, String> typeCol = new TableColumn<>("Type / শ্রেণি");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getMemberTypeName() != null ? data.getValue().getMemberTypeName() : "General"));
        typeCol.setMinWidth(140);

        TableColumn<Member, String> rollCol = new TableColumn<>("Institutional ID / Roll");
        rollCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getInstitutionalId() != null ? data.getValue().getInstitutionalId() : "—"));
        rollCol.setMinWidth(150);

        TableColumn<Member, String> deptCol = new TableColumn<>("Department");
        deptCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDepartment() != null ? data.getValue().getDepartment() : "—"));
        deptCol.setMinWidth(160);

        TableColumn<Member, String> phoneCol = new TableColumn<>("Phone (BD)");
        phoneCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhone()));
        phoneCol.setMinWidth(120);

        TableColumn<Member, String> expCol = new TableColumn<>("Expiry Date");
        expCol.setCellValueFactory(data -> new SimpleStringProperty(DateTimeUtil.formatDate(data.getValue().getMembershipExpiryDate())));
        expCol.setMinWidth(110);

        TableColumn<Member, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().name()));
        statusCol.setMinWidth(120);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    switch (item) {
                        case "ACTIVE" -> badge.getStyleClass().add("badge-active");
                        case "SUSPENDED" -> badge.getStyleClass().add("badge-suspended");
                        default -> badge.getStyleClass().add("badge-overdue");
                    }
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        memberTable.getColumns().addAll(idCol, cardCol, nameCol, typeCol, rollCol, deptCol, phoneCol, expCol, statusCol);
        memberTable.setPlaceholder(new Label("No registered members found."));

        VBox centerBox = new VBox(memberTable);
        centerBox.setPadding(new Insets(16, 0, 0, 0));
        VBox.setVgrow(memberTable, Priority.ALWAYS);
        setCenter(centerBox);
    }

    public TextField getSearchField() {
        return searchField;
    }

    public ComboBox<MemberType> getTypeFilter() {
        return typeFilter;
    }

    public ComboBox<String> getStatusFilter() {
        return statusFilter;
    }

    public Button getResetButton() {
        return resetButton;
    }

    public Button getAddMemberButton() {
        return addMemberButton;
    }

    public TableView<Member> getMemberTable() {
        return memberTable;
    }

    public Label getCountLabel() {
        return countLabel;
    }

    public ProgressIndicator getLoadingSpinner() {
        return loadingSpinner;
    }
}

