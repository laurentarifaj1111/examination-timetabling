package org.fiek.models;

public enum Structure {
    SINGLE_EXAMS_MOVE(1, 1),
    TWO_EXAMS_MOVE(8, 2),
    THREE_EXAMS_MOVE(12, 3),
    SWAP_PERIODS(12, 2);


    public final int evalThreshold;
    public final int numOfExams;


    private Structure(int evalThreshold, int numOfExams) {
        this.evalThreshold = evalThreshold;
        this.numOfExams = numOfExams;
    }
}
