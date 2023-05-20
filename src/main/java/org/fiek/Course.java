package org.fiek;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Course {
    @JsonProperty("Course")
    String id;
    @JsonProperty("ExamType")
    String examType;
    @JsonProperty("NumberOfExams")
    Integer numberOfExams;
    @JsonProperty("MinimumDistanceBetweenExams")
    Integer minimumDistanceBetweenExams;
    @JsonProperty("RoomsRequested")
    RoomsRequested roomsRequested;
    @JsonProperty("Teacher")
    String teacher;
    @JsonProperty("WrittenOralSpecs")
    WrittenOralSpecs writtenOralSpecs;


}
