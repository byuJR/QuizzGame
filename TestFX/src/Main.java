import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        QuizGame quizGame = new QuizGame(primaryStage);
        primaryStage.setTitle("Quiz Game");
        primaryStage.setScene(quizGame.getStartScene());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}