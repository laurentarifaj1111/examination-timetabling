package org.fiek;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Instance {

    @JsonProperty("Periods")
    Integer periods;
    @JsonProperty("PrimaryPrimaryDistance")
    Integer primaryPrimaryDistance;
    @JsonProperty("PrimarySecondaryDistance")
    Integer primarySecondaryDistance;
    @JsonProperty("SlotsPerDay")
    Integer slotsPerDay;
    @JsonProperty("Teachers")
    List<String> teachers;
    @JsonProperty("Constraints")
    List<Constraint> Constraints;

    @JsonProperty("Courses")
    List<Course> courses;

    @JsonProperty("Curricula")
    List<Curricula> curricula;

    @JsonProperty("Rooms")
    List<Room> rooms;

    public Curricula getCurriculaWithId(String id) {
        return this.getCurricula().stream().filter(curricula1 -> curricula1.getId().equals(id))
                .findFirst().orElse(null);
    }

}
