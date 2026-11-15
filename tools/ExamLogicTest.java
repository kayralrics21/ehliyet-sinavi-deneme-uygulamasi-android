package com.ehliyet.deneme;

import java.util.Arrays;

public final class ExamLogicTest {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        check(ExamLogic.canSelect(-1), "unanswered selectable");
        for (int i = 0; i <= 3; i++) check(!ExamLogic.canSelect(i), "answer locks: " + i);

        int[] keys = new int[50];
        int[] picks = new int[50];
        Arrays.fill(picks, -1);
        check(Arrays.equals(ExamLogic.grade(picks, keys), new int[]{0, 0, 50}), "all blank");

        Arrays.fill(picks, 1);
        for (int i = 0; i < 34; i++) picks[i] = 0;
        check(Arrays.equals(ExamLogic.grade(picks, keys), new int[]{34, 16, 0}), "34 correct");
        check(!ExamLogic.passed(34), "34 must fail");
        picks[34] = 0;
        check(Arrays.equals(ExamLogic.grade(picks, keys), new int[]{35, 15, 0}), "35 correct");
        check(ExamLogic.passed(35), "35 must pass");
        Arrays.fill(picks, 0);
        check(Arrays.equals(ExamLogic.grade(picks, keys), new int[]{50, 0, 0}), "perfect score");
        check(ExamLogic.passed(50), "50 passes");
        System.out.println("PASS: answer locking, blank/wrong/right scoring, and 34/35 pass threshold.");
    }
}
