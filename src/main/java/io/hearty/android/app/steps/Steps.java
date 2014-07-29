package io.hearty.android.app.steps;

/**
 * Created by ejf3 on 7/27/14.
 */
public class Steps {
    private static int lastSteps = 0;
    private final int steps;

    public Steps(int steps) {
        lastSteps = steps;
        this.steps = steps;
    }

    public static int getLastSteps() {
        return lastSteps;
    }

    public int getSteps() {
        return steps;
    }
}
