package com.libraryos.view;

import com.libraryos.model.catalog.Book;
import com.libraryos.model.member.Member;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class PlaceReservationDialog extends Dialog<PlaceReservationDialog.ReservationFormData> {

    public static class ReservationFormData {
        private final Long bookId;
        private final Long memberId;
        private final String notes;

        public ReservationFormData(Long bookId, Long memberId, String notes) {
            this.bookId = bookId;
            this.memberId = memberId;
            this.notes = notes;
        }

        public Long getBookId() { return bookId; }
        public Long getMemberId() { return memberId; }
        public String getNotes() { return notes; }
    }

    private final ComboBox<Book> bookCombo = new ComboBox<>();
    private final ComboBox<Member> memberCombo = new ComboBox<>();
    private final TextArea notesArea = new TextArea();
    private final Label errorLabel = new Label();

    public PlaceReservationDialog(List<Book> books, List<Member> members) {
        setTitle("Place Hold Reservation / সংরক্ষণ আবেদন");
        setHeaderText("Queue a member to receive the next returned copy of this book");

        ButtonType placeBtnType = new ButtonType("Place Reservation", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(placeBtnType, ButtonType.CANCEL);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(16, 20, 10, 20));
        layout.setPrefWidth(500);

        errorLabel.getStyleClass().add("banner-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        bookCombo.setItems(FXCollections.observableArrayList(books));
        bookCombo.setMaxWidth(Double.MAX_VALUE);
        bookCombo.getStyleClass().add("form-input");
        bookCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getTitleEn() + (item.getTitleBn() != null ? " (" + item.getTitleBn() + ")" : ""));
            }
        });
        bookCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getTitleEn() + (item.getTitleBn() != null ? " (" + item.getTitleBn() + ")" : ""));
            }
        });

        memberCombo.setItems(FXCollections.observableArrayList(members));
        memberCombo.setMaxWidth(Double.MAX_VALUE);
        memberCombo.getStyleClass().add("form-input");
        memberCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getMembershipNumber() + " — " + item.getDisplayName());
            }
        });
        memberCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Member item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getMembershipNumber() + " — " + item.getDisplayName());
            }
        });

        notesArea.setPromptText("Optional notes or reservation remarks");
        notesArea.setPrefRowCount(3);
        notesArea.getStyleClass().add("form-input");

        grid.add(new Label("Select Book / বই:"), 0, 0);
        grid.add(bookCombo, 1, 0);

        grid.add(new Label("Select Member / সদস্য:"), 0, 1);
        grid.add(memberCombo, 1, 1);

        grid.add(new Label("Notes / মন্তব্য:"), 0, 2);
        grid.add(notesArea, 1, 2);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        layout.getChildren().addAll(errorLabel, grid);
        getDialogPane().setContent(layout);

        Button placeBtn = (Button) getDialogPane().lookupButton(placeBtnType);
        placeBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (bookCombo.getValue() == null) {
                showError("Please select a book to reserve");
                event.consume();
                return;
            }
            if (memberCombo.getValue() == null) {
                showError("Please select a member");
                event.consume();
                return;
            }
        });

        setResultConverter(btn -> {
            if (btn == placeBtnType && bookCombo.getValue() != null && memberCombo.getValue() != null) {
                return new ReservationFormData(
                        bookCombo.getValue().getId(),
                        memberCombo.getValue().getId(),
                        notesArea.getText() != null ? notesArea.getText().trim() : null
                );
            }
            return null;
        });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}

