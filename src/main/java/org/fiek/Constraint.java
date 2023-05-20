package org.fiek;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Constraint {
    @JsonProperty("Level")
    String level;

    @JsonProperty("Period")
    Integer period;
    @JsonProperty("Room")
    String room;

    @JsonProperty("Type")
    String type;
    @JsonProperty("Course")
    String course;
    @JsonProperty("Exam")
    String exam;
    @JsonProperty("Part")
    String part;
}
