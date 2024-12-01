import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class QuizGame {
    private Stage stage;
    private String playerName;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private List<Question> questions;
    private Map<Integer, Boolean> results;
    private Timeline timer;
    private int timeLeft;
    private Label timerLabel;
    private static final String CSS_FILE = "style.css";

    public QuizGame(Stage stage) {
        this.stage = stage;
        this.results = new HashMap<>();
        loadQuestions();
    }

    private void loadQuestions() {
        questions = new ArrayList<>();
        File dataDir = new File("data/soal");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File[] files = dataDir.listFiles((dir, name) -> name.startsWith("soal_") && name.endsWith(".txt"));
        if (files != null) {
            Arrays.sort(files); // Mengurutkan file berdasarkan nama
            for (File file : files) {
                try {
                    List<String> lines = Files.readAllLines(file.toPath());
                    if (!lines.isEmpty()) {
                        String[] parts = lines.get(0).split("\\|");
                        String questionText = parts[0];
                        String[] options = parts[1].split(",");
                        int correctAnswer = Integer.parseInt(parts[2]);
                        int timer = Integer.parseInt(parts[3]);
                        questions.add(new Question(questionText, options, correctAnswer, timer));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveResult() {
        try {
            File resultDir = new File("data/hasil");
            if (!resultDir.exists()) {
                resultDir.mkdirs();
            }

            String fileName = "data/hasil/" + playerName + ".txt";
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            out.println("Nama: " + playerName);
            out.println("Skor Total: " + score + "/" + questions.size());
            out.println("\nDetail Jawaban:");
            for (int i = 0; i < questions.size(); i++) {
                out.println("Soal " + (i+1) + ": " + (results.get(i) ? "Benar" : "Salah"));
                out.println("Pertanyaan: " + questions.get(i).getQuestionText());
                out.println("Jawaban Benar: " + questions.get(i).getOptions()[questions.get(i).getCorrectAnswer()]);
                out.println("------------------------");
            }
            
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Scene getStartScene() {
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(20));
        mainLayout.getStyleClass().add("main-background");

        // Title
        Label titleLabel = new Label("JQuizz");
        titleLabel.getStyleClass().add("title-label");

        // Input Container
        VBox inputContainer = new VBox(15);
        inputContainer.setAlignment(Pos.CENTER);
        inputContainer.getStyleClass().add("input-container");
        inputContainer.setMaxWidth(400);
        inputContainer.setPadding(new Insets(20));

        // Input nama
        TextField nameInput = new TextField();
        nameInput.setPromptText("Masukkan Nama");
        nameInput.getStyleClass().add("name-input");

        // Buttons Container
        VBox buttonContainer = new VBox(10);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(10);

        Button startButton = new Button("Mulai Kuis");
        startButton.getStyleClass().add("action-button");
        
        Button manageButton = new Button("Kelola Soal");
        manageButton.getStyleClass().add("action-button");

        startButton.setOnAction(e -> {
            if (!nameInput.getText().trim().isEmpty()) {
                playerName = nameInput.getText().trim();
                stage.setScene(getQuizScene());
            } else {
                showAlert("Nama tidak boleh kosong!");
            }
        });

        manageButton.setOnAction(e -> {
            stage.setScene(getManageQuestionsScene());
        });

        inputContainer.getChildren().addAll(nameInput, startButton, manageButton);
        mainLayout.getChildren().addAll(titleLabel, inputContainer);

        Scene scene = new Scene(mainLayout, 800, 600);
        File cssFile = new File("src/style.css");
        scene.getStylesheets().add(cssFile.toURI().toString());
        
        return scene;
    }

    public Scene getManageQuestionsScene() {
        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);

        ListView<String> questionList = new ListView<>();
        updateQuestionList(questionList);

        Button addButton = new Button("Tambah Soal");
        Button deleteButton = new Button("Hapus Soal");
        Button backButton = new Button("Kembali");

        addButton.setOnAction(e -> showAddQuestionDialog(questionList));
        
        deleteButton.setOnAction(e -> {
            int selectedIdx = questionList.getSelectionModel().getSelectedIndex();
            if (selectedIdx >= 0) {
                File file = new File("data/soal/soal_" + (selectedIdx + 1) + ".txt");
                if (file.delete()) {
                    renameRemainingFiles(selectedIdx + 1);
                    loadQuestions();
                    updateQuestionList(questionList);
                }
            }
        });

        backButton.setOnAction(e -> stage.setScene(getStartScene()));

        layout.getChildren().addAll(questionList, addButton, deleteButton, backButton);
        return new Scene(layout, 400, 400);
    }

    private void showAddQuestionDialog(ListView<String> questionList) {
        Stage dialog = new Stage();
        dialog.setTitle("Tambah Soal");

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);

        TextField questionField = new TextField();
        questionField.setPromptText("Pertanyaan");

        TextField[] optionFields = new TextField[4];
        for (int i = 0; i < 4; i++) {
            optionFields[i] = new TextField();
            optionFields[i].setPromptText("Opsi " + (i + 1));
        }

        ComboBox<Integer> correctAnswerBox = new ComboBox<>();
        correctAnswerBox.getItems().addAll(0, 1, 2, 3);
        correctAnswerBox.setPromptText("Jawaban Benar (0-3)");

        TextField timerField = new TextField();
        timerField.setPromptText("Waktu (detik)");

        Button saveButton = new Button("Simpan");
        saveButton.setOnAction(e -> {
            if (isValidQuestionInput(questionField, optionFields, correctAnswerBox, timerField)) {
                try {
                    int timer = Integer.parseInt(timerField.getText());
                    saveNewQuestion(
                        questionField.getText(),
                        Arrays.stream(optionFields).map(TextField::getText).toArray(String[]::new),
                        correctAnswerBox.getValue(),
                        timer
                    );
                    loadQuestions();
                    updateQuestionList(questionList);
                    dialog.close();
                } catch (NumberFormatException ex) {
                    showAlert("Waktu harus berupa angka!");
                }
            }
        });

        layout.getChildren().addAll(
            questionField,
            new Label("Opsi Jawaban:"),
            new VBox(5, optionFields),
            correctAnswerBox,
            timerField,
            saveButton
        );

        dialog.setScene(new Scene(layout, 300, 400));
        dialog.show();
    }

    private boolean isValidQuestionInput(TextField questionField, TextField[] optionFields, 
                                       ComboBox<Integer> correctAnswerBox, TextField timerField) {
        if (questionField.getText().isEmpty()) {
            showAlert("Pertanyaan tidak boleh kosong!");
            return false;
        }
        
        for (TextField field : optionFields) {
            if (field.getText().isEmpty()) {
                showAlert("Semua opsi harus diisi!");
                return false;
            }
        }
        
        if (correctAnswerBox.getValue() == null) {
            showAlert("Pilih jawaban yang benar!");
            return false;
        }

        if (timerField.getText().isEmpty()) {
            showAlert("Waktu harus diisi!");
            return false;
        }

        try {
            int timer = Integer.parseInt(timerField.getText());
            if (timer <= 0) {
                showAlert("Waktu harus lebih dari 0 detik!");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Waktu harus berupa angka!");
            return false;
        }
        
        return true;
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }

    private void saveNewQuestion(String question, String[] options, int correctAnswer, int timer) {
        try {
            int newQuestionNumber = questions.size() + 1;
            String fileName = "data/soal/soal_" + newQuestionNumber + ".txt";
            
            FileWriter fw = new FileWriter(fileName);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            out.println(question + "|" + String.join(",", options) + "|" + correctAnswer + "|" + timer);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void renameRemainingFiles(int startIndex) {
        File folder = new File("data/soal");
        File[] files = folder.listFiles((dir, name) -> name.startsWith("soal_") && name.endsWith(".txt"));
        if (files != null) {
            Arrays.sort(files);
            for (int i = startIndex; i <= files.length; i++) {
                File oldFile = new File("data/soal/soal_" + (i + 1) + ".txt");
                File newFile = new File("data/soal/soal_" + i + ".txt");
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile);
                }
            }
        }
    }

    private void updateQuestionList(ListView<String> questionList) {
        questionList.getItems().clear();
        for (int i = 0; i < questions.size(); i++) {
            questionList.getItems().add("Soal " + (i + 1) + ": " + questions.get(i).getQuestionText());
        }
    }

    public Scene getQuizScene() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("main-background");

        Question currentQuestion = questions.get(currentQuestionIndex);
        
        timerLabel = new Label();
        timerLabel.getStyleClass().add("timer-label");
        timeLeft = currentQuestion.getTimer();
        updateTimerLabel();
        setupTimer();
        
        Label questionLabel = new Label("Question " + (currentQuestionIndex + 1) + ": " + 
                                     currentQuestion.getQuestionText());
        questionLabel.getStyleClass().add("question-label");
        
        GridPane optionsGrid = new GridPane();
        optionsGrid.setHgap(10);
        optionsGrid.setVgap(10);
        optionsGrid.setAlignment(Pos.CENTER);
        
        String[] options = currentQuestion.getOptions();
        for (int i = 0; i < options.length; i++) {
            Button optionButton = new Button(options[i]);
            optionButton.getStyleClass().add("option-button");
            optionButton.setMaxWidth(Double.MAX_VALUE);
            optionButton.setMinWidth(250);
            
            int finalI = i;
            optionButton.setOnAction(e -> {
                stopTimer();
                processAnswer(finalI);
            });
            
            optionsGrid.add(optionButton, i % 2, i / 2);
        }

        layout.getChildren().addAll(timerLabel, questionLabel, optionsGrid);
        
        Scene scene = new Scene(layout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        startTimer();
        
        return scene;
    }

    private void setupTimer() {
        timer = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                timeLeft--;
                updateTimerLabel();
                if (timeLeft <= 0) {
                    stopTimer();
                    processAnswer(-1);
                }
            })
        );
        timer.setCycleCount(Timeline.INDEFINITE);
    }

    private void updateTimerLabel() {
        timerLabel.setText(String.format("Waktu: %d detik", timeLeft));
        if (timeLeft <= 5) {
            timerLabel.setStyle("-fx-text-fill: red;");
        } else {
            timerLabel.setStyle("-fx-text-fill: black;");
        }
    }

    private void startTimer() {
        timer.play();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void processAnswer(int selectedAnswer) {
        stopTimer();
        
        Question currentQuestion = questions.get(currentQuestionIndex);
        boolean isCorrect = selectedAnswer == currentQuestion.getCorrectAnswer();
        results.put(currentQuestionIndex, isCorrect);
        
        if (isCorrect) {
            score++;
        }

        Platform.runLater(() -> {
            String message = selectedAnswer == -1 ? "Waktu habis!" : 
                            (isCorrect ? "Benar!" : "Salah!");
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Hasil");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();

            currentQuestionIndex++;
            if (currentQuestionIndex < questions.size()) {
                stage.setScene(getQuizScene());
            } else {
                saveResult();
                showResult();
            }
        });
    }

    private void showResult() {
        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        
        Label resultLabel = new Label(String.format("Congratulations %s!\nYour score: %d/%d", 
                playerName, score, questions.size()));
        Button restartButton = new Button("Play Again");
        
        restartButton.setOnAction(e -> {
            currentQuestionIndex = 0;
            score = 0;
            results.clear();
            stage.setScene(getStartScene());
        });
        
        layout.getChildren().addAll(resultLabel, restartButton);
        stage.setScene(new Scene(layout, 400, 300));
    }
}