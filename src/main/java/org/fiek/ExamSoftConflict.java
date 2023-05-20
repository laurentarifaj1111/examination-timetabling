package org.fiek;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExamSoftConflict {
    public Exam fist;
    public Exam second;

    public ExamSoftConflict() {
    }

    public ExamSoftConflict(Exam fist, Exam second) {
        this.fist = fist;
        this.second = second;
    }
}
