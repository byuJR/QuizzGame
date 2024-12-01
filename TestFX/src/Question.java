
import java.util.List; 

public class Question {
    private String questionText;
    private String[] options;
    private int correctAnswer;
    private int timer;

    public Question(String questionText, String[] options, int correctAnswer, int timer) {
        this.questionText = questionText;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.timer = timer;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getOptions() {
        return options;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public int getTimer() {
        return timer;
    }
}