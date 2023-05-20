package org.fiek;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ResultsFormated {
    Long executionTimeSeconds;
    @JsonProperty("Assignments")
    List<Exam> Assignments;
    @JsonProperty("Cost")
    int Cost;

    @Getter
    @Setter
    public static class Exam {
        @JsonProperty("Course")
        String Course;
        @JsonProperty("Period")
        int Period;
        @JsonProperty("Room")
        String Room;
    }

    public static ResultsFormated fromPotentialSolution(PotentialSolution solution){
        ResultsFormated resultsFormated = new ResultsFormated();
        List<ResultsFormated.Exam> collect = solution.getExams()
                .stream().map(exam -> {
                    ResultsFormated.Exam exam1 = new ResultsFormated.Exam();
                    exam1.setPeriod(exam.period);
                    exam1.setCourse(exam.getCourse().id);
                    if (exam.getRooms() != null) {
                        exam1.setRoom(exam.getRooms().stream().map(Room::getRoom).collect(Collectors.joining(", ")));
                    } else {
                        exam1.setRoom("");
                    }
                    return exam1;
                }).toList();
        resultsFormated.setAssignments(collect);
        resultsFormated.setCost(solution.eval);
        return resultsFormated;
    }

}
