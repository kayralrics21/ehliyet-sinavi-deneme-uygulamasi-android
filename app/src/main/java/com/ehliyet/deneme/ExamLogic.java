package com.ehliyet.deneme;

final class ExamLogic {
    private ExamLogic() { }

    static boolean canSelect(int previousAnswer) {
        return previousAnswer == -1;
    }

    static int[] grade(int[] answers, int[] correctAnswers) {
        if (answers == null || correctAnswers == null || answers.length != correctAnswers.length) {
            throw new IllegalArgumentException("Question/answer mismatch");
        }
        int right = 0;
        int wrong = 0;
        int blank = 0;
        for (int i = 0; i < answers.length; i++) {
            if (correctAnswers[i] < 0 || correctAnswers[i] > 3 || answers[i] < -1 || answers[i] > 3) {
                throw new IllegalArgumentException("Invalid answer index");
            }
            if (answers[i] < 0) blank++;
            else if (answers[i] == correctAnswers[i]) right++;
            else wrong++;
        }
        return new int[]{right, wrong, blank};
    }

    static boolean passed(int correct) {
        return correct >= 35;
    }
}
