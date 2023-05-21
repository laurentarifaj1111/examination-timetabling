package org.fiek.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class Solution {
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


}
