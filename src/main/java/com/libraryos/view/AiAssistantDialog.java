package com.libraryos.view;

import com.libraryos.app.AppContext;
import com.libraryos.model.catalog.Book;
import com.libraryos.service.AiAssistantService;
import com.libraryos.view.theme.ThemeManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modern ChatGPT / Claude-Style AI Library Assistant Modal.
 * Features:
 * - Real Markdown parsing (bold, italics, code pills, bullet lists) with zero raw syntax bugs
 * - Multi-row wrapped FlowPane for prompt chips (no horizontal scrollbar)
 * - Cohesive Light & Dark-Purple Theme synchronization
 * - Streamlined borderless header with top-right close button (redundant bottom button row removed)
 * - Interactive actionable book mini-cards with live stock, shelf locations, and 1-click issue
 */
public class AiAssistantDialog extends Dialog<Void> {

    private final VBox chatMessagesBox;
    private final ScrollPane chatScrollPane;
    private final TextField inputField;
    private final Button sendBtn;
    private final AiAssistantService aiService;

    // Typing Animation Indicator
    private Node typingIndicatorNode;
    private Timeline typingAnimation;

    // Action callbacks for quick actions
    private Consumer<Book> onIssueRequested;
    private Consumer<Book> onViewDetailsRequested;
    private Consumer<Book> onReserveRequested;

    public AiAssistantDialog(Window owner) {
        initOwner(owner);
        initModality(Modality.NONE); // Non-blocking floating window so user can interact with library while chatting
        setTitle("LibraryOS AI Assistant • BUBT CSE Edition");

        this.aiService = AppContext.getInstance().getAiAssistantService();

        DialogPane pane = getDialogPane();
        pane.getStyleClass().add("ai-dialog-pane");
        pane.getButtonTypes().add(ButtonType.CLOSE);

        // Hide default bottom button-bar so ONLY the top-right custom close button exists
        Platform.runLater(() -> {
            Node buttonBar = pane.lookup(".button-bar");
            if (buttonBar != null) {
                buttonBar.setVisible(false);
                buttonBar.setManaged(false);
            }
        });

        // Apply current active theme
        ThemeManager.getInstance().applyThemeToDialogPane(pane);

        VBox root = new VBox(14);
        root.setPadding(new Insets(18, 20, 20, 20));
        root.setPrefWidth(660);
        root.setPrefHeight(680);

        // Header with clean inline close button
        VBox headerBox = new VBox(4);
        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🤖 LibraryOS AI Assistant — BUBT CSE Edition");
        title.getStyleClass().add("ai-header-title");
        title.setStyle("-fx-font-size: 19px; -fx-font-weight: 800;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("btn-secondary");
        closeBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 4 10 4 10; -fx-background-radius: 14px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> close());

        topRow.getChildren().addAll(title, closeBtn);

        Label subtitle = new Label("Intelligent BUBT CSE academic advisor, live physical shelf finder, and instant circulation assistant.");
        subtitle.getStyleClass().add("ai-header-subtitle");
        subtitle.setStyle("-fx-font-size: 12px;");

        headerBox.getChildren().addAll(topRow, subtitle);

        // Chat message area
        chatMessagesBox = new VBox(16);
        chatMessagesBox.setPadding(new Insets(14));
        chatMessagesBox.getStyleClass().add("ai-chat-box");
        chatMessagesBox.setStyle("-fx-background-color: transparent;");

        chatScrollPane = new ScrollPane(chatMessagesBox);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setPrefHeight(420);
        chatScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: #4C1D95; -fx-border-radius: 12px;");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // Initial welcome message rendered with formatted markdown
        addAssistantMessage("Hello! I am your **LibraryOS AI Assistant** 🤖 *(BUBT CSE Edition)*.\n\n" +
                "I provide **real-time physical shelf locations, live stock verification**, and **BUBT CSE syllabus book recommendations**:\n" +
                "- 📚 *\"Recommend books for CSE 201 Data Structures\"*\n" +
                "- 🔍 *\"Is Clean Code available?\"*\n" +
                "- 📍 *\"Where is Silberschatz Operating Systems placed?\"*\n" +
                "- ৳ *\"What is the overdue fine rate?\"* / *\"দেরিতে জমা দিলে জরিমানা কত?\"*\n" +
                "- 📜 *\"How many books can a student borrow?\"*", null);

        // Multi-Row Wrapped FlowPane for Prompt Chips (No horizontal scrollbar!)
        FlowPane chipsPane = new FlowPane(8, 8);
        chipsPane.setAlignment(Pos.CENTER_LEFT);
        chipsPane.getChildren().addAll(
                createChip("🎓 CSE 201 (Data Structures)", "Recommend books for CSE 201 Data Structures"),
                createChip("🎓 CSE 209 (Algorithms)", "Recommend books for CSE 209 Algorithms"),
                createChip("🟢 Is Clean Code Available?", "Is Clean Code available?"),
                createChip("📍 OS Book Shelf Location", "Is Operating System Concepts available?"),
                createChip("৳ Overdue Fines", "What is the overdue fine rate?"),
                createChip("📜 Borrowing Rules", "How many books can a student borrow?"),
                createChip("🇧🇩 বাংলা সাহায্য", "দেরিতে বই ফেরত দিলে জরিমানা কত?")
        );

        // Modern Input Bar
        HBox inputBar = new HBox(10);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        inputField = new TextField();
        inputField.setPromptText("Ask anything (e.g., 'Books for CSE 303', 'Is Tanenbaum in stock?')...");
        inputField.getStyleClass().add("search-field");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        sendBtn = new Button("➤ Send / পাঠান");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setStyle("-fx-font-weight: 700;");

        inputBar.getChildren().addAll(inputField, sendBtn);

        root.getChildren().addAll(headerBox, chatScrollPane, chipsPane, inputBar);
        pane.setContent(root);

        // Action wiring
        sendBtn.setOnAction(e -> handleSendMessage());
        inputField.setOnAction(e -> handleSendMessage());
    }

    public void setOnIssueRequested(Consumer<Book> callback) {
        this.onIssueRequested = callback;
    }

    public void setOnViewDetailsRequested(Consumer<Book> callback) {
        this.onViewDetailsRequested = callback;
    }

    public void setOnReserveRequested(Consumer<Book> callback) {
        this.onReserveRequested = callback;
    }

    public void askQuestion(String question) {
        if (question != null && !question.isBlank()) {
            inputField.setText(question);
            handleSendMessage();
        }
    }

    private Button createChip(String label, String query) {
        Button chip = new Button(label);
        chip.getStyleClass().add("ai-prompt-chip");
        chip.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 16px; -fx-padding: 6 12 6 12; -fx-cursor: hand;");
        chip.setOnAction(e -> {
            inputField.setText(query);
            handleSendMessage();
        });
        return chip;
    }

    private void handleSendMessage() {
        String text = inputField.getText().trim();
        if (text.isBlank()) return;

        inputField.clear();
        addUserMessage(text);
        sendBtn.setDisable(true);
        showTypingIndicator();

        Task<AiAssistantService.AiResponse> task = new Task<>() {
            @Override
            protected AiAssistantService.AiResponse call() {
                return aiService.processQuery(text);
            }
        };

        task.setOnSucceeded(e -> {
            hideTypingIndicator();
            sendBtn.setDisable(false);
            AiAssistantService.AiResponse resp = task.getValue();
            addAssistantMessage(resp.replyMarkdown(), resp);
        });

        task.setOnFailed(e -> {
            hideTypingIndicator();
            sendBtn.setDisable(false);
            addAssistantMessage("Sorry, I encountered an issue processing your request: " + task.getException().getMessage(), null);
        });

        AppContext.getInstance().getExecutorService().submit(task);
    }

    private void showTypingIndicator() {
        hideTypingIndicator();

        HBox indicator = new HBox(8);
        indicator.setAlignment(Pos.CENTER_LEFT);
        indicator.getStyleClass().add("ai-assistant-bubble");
        indicator.setPadding(new Insets(10, 16, 10, 16));
        indicator.setMaxWidth(260);

        boolean isDark = ThemeManager.getInstance().isDarkMode();
        String bgColor = isDark ? "#241D35" : "#FFFFFF";
        String borderColor = isDark ? "#362B4E" : "#E2E8F0";
        String dotColor = isDark ? "#C084FC" : "#2563EB";

        indicator.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor +
                "; -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        Label botIcon = new Label("🤖");
        botIcon.setStyle("-fx-font-size: 14px;");

        Label dot1 = new Label("●");
        Label dot2 = new Label("●");
        Label dot3 = new Label("●");
        String baseDotStyle = "-fx-font-size: 13px; -fx-text-fill: " + dotColor + ";";
        dot1.setStyle(baseDotStyle);
        dot2.setStyle(baseDotStyle);
        dot3.setStyle(baseDotStyle);

        HBox dotsBox = new HBox(4, dot1, dot2, dot3);
        dotsBox.setAlignment(Pos.CENTER);

        Label thinkingLabel = new Label("Thinking...");
        thinkingLabel.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: " + (isDark ? "#A78BFA" : "#64748B") + ";");

        indicator.getChildren().addAll(botIcon, dotsBox, thinkingLabel);

        typingAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(dot1.opacityProperty(), 0.3),
                        new KeyValue(dot2.opacityProperty(), 0.3),
                        new KeyValue(dot3.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(dot1.opacityProperty(), 1.0),
                        new KeyValue(dot2.opacityProperty(), 0.3),
                        new KeyValue(dot3.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(dot1.opacityProperty(), 0.3),
                        new KeyValue(dot2.opacityProperty(), 1.0),
                        new KeyValue(dot3.opacityProperty(), 0.3)),
                new KeyFrame(Duration.millis(900),
                        new KeyValue(dot1.opacityProperty(), 0.3),
                        new KeyValue(dot2.opacityProperty(), 0.3),
                        new KeyValue(dot3.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(dot1.opacityProperty(), 0.3),
                        new KeyValue(dot2.opacityProperty(), 0.3),
                        new KeyValue(dot3.opacityProperty(), 0.3))
        );
        typingAnimation.setCycleCount(Animation.INDEFINITE);
        typingAnimation.play();

        typingIndicatorNode = indicator;
        chatMessagesBox.getChildren().add(typingIndicatorNode);
        scrollToBottom();
    }

    private void hideTypingIndicator() {
        if (typingAnimation != null) {
            typingAnimation.stop();
            typingAnimation = null;
        }
        if (typingIndicatorNode != null) {
            chatMessagesBox.getChildren().remove(typingIndicatorNode);
            typingIndicatorNode = null;
        }
    }

    private void addUserMessage(String message) {
        VBox bubble = new VBox(4);
        bubble.setAlignment(Pos.CENTER_RIGHT);

        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(480);
        label.getStyleClass().add("ai-user-bubble");
        label.setStyle("-fx-background-color: linear-gradient(to right, #7C3AED, #9333EA); -fx-text-fill: #FFFFFF; -fx-padding: 10 16 10 16; -fx-background-radius: 16px; -fx-font-size: 13.5px; -fx-font-weight: 500; -fx-effect: dropshadow(three-pass-box, rgba(124,58,237,0.4), 8, 0, 0, 3);");

        bubble.getChildren().add(label);
        chatMessagesBox.getChildren().add(bubble);
        scrollToBottom();
    }

    private void addAssistantMessage(String markdown, AiAssistantService.AiResponse resp) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(600);
        card.getStyleClass().add("ai-assistant-bubble");

        boolean isDark = ThemeManager.getInstance().isDarkMode();
        String bgColor = isDark ? "#1A1432" : "#FFFFFF";
        String borderColor = isDark ? "#3B1D6E" : "#E2E8F0";

        card.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor + "; -fx-border-radius: 14px; -fx-background-radius: 14px; -fx-padding: 14 18 14 18; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");

        // Parse and render formatted Markdown nodes (Headers, Bullets, Bold, Italics)
        Node formattedContent = renderMarkdownToNodes(markdown, isDark);
        card.getChildren().add(formattedContent);

        // If response includes interactive book stock items, render interactive actionable cards
        if (resp != null && resp.bookStockDetails() != null && !resp.bookStockDetails().isEmpty()) {
            VBox bookCardsBox = new VBox(10);
            bookCardsBox.setPadding(new Insets(6, 0, 4, 0));

            for (AiAssistantService.BookStockInfo stock : resp.bookStockDetails()) {
                Node actionCard = createInteractiveBookCard(stock, isDark);
                bookCardsBox.getChildren().add(actionCard);
            }
            card.getChildren().add(bookCardsBox);
        }

        chatMessagesBox.getChildren().add(card);
        scrollToBottom();
    }

    /**
     * Parses markdown text into rich JavaFX labels, bullets, and TextFlow nodes with zero raw syntax artifacts.
     */
    private Node renderMarkdownToNodes(String markdown, boolean isDark) {
        VBox flowBox = new VBox(6);
        if (markdown == null || markdown.isBlank()) return flowBox;

        String[] lines = markdown.split("\\r?\\n");
        String textColor = isDark ? "#F1F5F9" : "#0F172A";
        String accentColor = isDark ? "#A78BFA" : "#2563EB";
        String headerColor = isDark ? "#FFFFFF" : "#1E3A8A";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                Region spacer = new Region();
                spacer.setPrefHeight(4);
                flowBox.getChildren().add(spacer);
                continue;
            }

            // Heading: ### Heading or ## Heading
            if (trimmed.startsWith("#")) {
                String headerText = trimmed.replaceFirst("^#+\\s*", "");
                TextFlow headerFlow = parseInlineMarkdown(headerText, isDark);
                for (Node n : headerFlow.getChildren()) {
                    if (n instanceof Text t) {
                        t.setStyle(t.getStyle() + "; -fx-font-size: 15.5px; -fx-font-weight: 800; -fx-fill: " + headerColor + ";");
                    }
                }
                flowBox.getChildren().add(headerFlow);
                continue;
            }

            // Bullet List: - item or * item
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                String bulletText = trimmed.substring(2);
                HBox bulletRow = new HBox(8);
                bulletRow.setAlignment(Pos.TOP_LEFT);

                Label dot = new Label("•");
                dot.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: " + accentColor + ";");

                TextFlow textFlow = parseInlineMarkdown(bulletText, isDark);
                HBox.setHgrow(textFlow, Priority.ALWAYS);

                bulletRow.getChildren().addAll(dot, textFlow);
                flowBox.getChildren().add(bulletRow);
                continue;
            }

            // Numbered List: 1. item
            if (trimmed.matches("^\\d+\\.\\s+.*")) {
                int dotIdx = trimmed.indexOf('.');
                String numStr = trimmed.substring(0, dotIdx + 1);
                String bodyStr = trimmed.substring(dotIdx + 1).trim();

                HBox numRow = new HBox(8);
                numRow.setAlignment(Pos.TOP_LEFT);

                Label numLbl = new Label(numStr);
                numLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + accentColor + ";");

                TextFlow textFlow = parseInlineMarkdown(bodyStr, isDark);
                HBox.setHgrow(textFlow, Priority.ALWAYS);

                numRow.getChildren().addAll(numLbl, textFlow);
                flowBox.getChildren().add(numRow);
                continue;
            }

            // Standard Paragraph
            TextFlow paragraphFlow = parseInlineMarkdown(trimmed, isDark);
            flowBox.getChildren().add(paragraphFlow);
        }

        return flowBox;
    }

    private TextFlow parseInlineMarkdown(String line, boolean isDark) {
        TextFlow flow = new TextFlow();
        flow.setMaxWidth(560);
        flow.setLineSpacing(3);

        String textColor = isDark ? "#F8FAFC" : "#0F172A";
        String boldColor = isDark ? "#FFFFFF" : "#0F172A";
        String accentColor = isDark ? "#C084FC" : "#2563EB";

        Pattern p = Pattern.compile("(\\*{3}[^*]+\\*{3})|(\\*{2}[^*]+\\*{2})|(\\*[^*]+\\*)|(`[^`]+`)|([^*`]+)");
        Matcher m = p.matcher(line);

        while (m.find()) {
            String token = m.group();
            if (token.startsWith("***") && token.endsWith("***") && token.length() >= 6) {
                Text t = new Text(token.substring(3, token.length() - 3));
                t.setStyle("-fx-font-weight: bold; -fx-font-style: italic; -fx-font-size: 13.5px; -fx-fill: " + boldColor + ";");
                flow.getChildren().add(t);
            } else if (token.startsWith("**") && token.endsWith("**") && token.length() >= 4) {
                Text t = new Text(token.substring(2, token.length() - 2));
                t.setStyle("-fx-font-weight: bold; -fx-font-size: 13.5px; -fx-fill: " + boldColor + ";");
                flow.getChildren().add(t);
            } else if (token.startsWith("*") && token.endsWith("*") && token.length() >= 2) {
                Text t = new Text(token.substring(1, token.length() - 1));
                t.setStyle("-fx-font-style: italic; -fx-font-size: 13.5px; -fx-fill: " + textColor + ";");
                flow.getChildren().add(t);
            } else if (token.startsWith("`") && token.endsWith("`") && token.length() >= 2) {
                Text t = new Text(" " + token.substring(1, token.length() - 1) + " ");
                t.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: " + accentColor + ";");
                flow.getChildren().add(t);
            } else {
                String clean = token.replace("**", "").replace("*", "");
                Text t = new Text(clean);
                t.setStyle("-fx-font-size: 13.5px; -fx-fill: " + textColor + ";");
                flow.getChildren().add(t);
            }
        }

        return flow;
    }

    private Node createInteractiveBookCard(AiAssistantService.BookStockInfo stock, boolean isDark) {
        Book b = stock.book();

        VBox card = new VBox(8);
        card.getStyleClass().add("ai-book-card");
        card.setPadding(new Insets(12));

        // Row 1: Title & Stock Pill
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(b.getTitle());
        titleLbl.getStyleClass().add("ai-book-title");
        titleLbl.setStyle("-fx-font-weight: 800; -fx-font-size: 14.5px;");
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label stockPill = new Label();
        if (stock.inStock()) {
            stockPill.setText(String.format("🟢 IN STOCK (%d of %d)", stock.availableCopies(), stock.totalCopies()));
            stockPill.getStyleClass().add("ai-stock-pill-in");
        } else {
            stockPill.setText(String.format("🔴 OUT OF STOCK (%d total)", stock.totalCopies()));
            stockPill.getStyleClass().add("ai-stock-pill-out");
        }

        titleRow.getChildren().addAll(titleLbl, stockPill);

        // Row 2: Author, Course tag, and Shelf location
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        String authorStr = (b.getAuthors() != null && !b.getAuthors().isEmpty()) ?
                b.getAuthors().get(0).getNameEn() : "Academic Reference";
        Label authorLbl = new Label("✍ " + authorStr);
        authorLbl.getStyleClass().add("ai-book-meta");
        authorLbl.setStyle("-fx-font-size: 12px;");

        Label shelfPill = new Label("📍 " + stock.shelfLocation());
        shelfPill.getStyleClass().add("ai-shelf-pill");

        metaRow.getChildren().addAll(authorLbl, shelfPill);

        if (stock.courseCodeTag() != null) {
            Label coursePill = new Label("🎓 " + stock.courseCodeTag());
            coursePill.setStyle(isDark ?
                    "-fx-background-color: #312E81; -fx-text-fill: #C7D2FE; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 10px;" :
                    "-fx-background-color: #E0E7FF; -fx-text-fill: #3730A3; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 10px;");
            metaRow.getChildren().add(coursePill);
        }

        // Row 3: Quick Action Buttons
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        actionsRow.setPadding(new Insets(4, 0, 0, 0));

        if (stock.inStock()) {
            Button issueBtn = new Button("⇄ Issue Book / ধার দিন");
            issueBtn.getStyleClass().add("btn-primary");
            issueBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 14 6 14;");
            issueBtn.setOnAction(e -> {
                if (onIssueRequested != null) {
                    onIssueRequested.accept(b);
                }
            });
            actionsRow.getChildren().add(issueBtn);
        } else {
            Button reserveBtn = new Button("⏳ Place Hold / সংরক্ষণ");
            reserveBtn.getStyleClass().add("btn-secondary");
            reserveBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 14 6 14;");
            reserveBtn.setOnAction(e -> {
                if (onReserveRequested != null) {
                    onReserveRequested.accept(b);
                }
            });
            actionsRow.getChildren().add(reserveBtn);
        }

        Button detailsBtn = new Button("🔍 View in Catalog");
        detailsBtn.getStyleClass().add("btn-secondary");
        detailsBtn.setStyle("-fx-font-size: 12px; -fx-padding: 6 14 6 14;");
        detailsBtn.setOnAction(e -> {
            if (onViewDetailsRequested != null) {
                onViewDetailsRequested.accept(b);
            }
        });

        actionsRow.getChildren().add(detailsBtn);

        card.getChildren().addAll(titleRow, metaRow, actionsRow);
        return card;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
    }
}
