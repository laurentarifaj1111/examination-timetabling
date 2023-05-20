package org.fiek;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class Curricula {
    @JsonProperty("Curriculum")
    String id;
    @JsonProperty("PrimaryCourses")
    List<String> primaryCourses;
    @JsonProperty("SecondaryCourses")
    List<String> secondaryCourses;

}
