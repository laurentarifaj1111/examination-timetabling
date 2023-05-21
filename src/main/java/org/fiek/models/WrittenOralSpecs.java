package org.fiek.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WrittenOralSpecs {

    @JsonProperty("MaxDistance")
    Integer maxDistance;
    @JsonProperty("MinDistance")
    Integer minDistance;

    @JsonProperty("RoomForOral")
    Boolean roomForOral;
    @JsonProperty("SameDay")
    Boolean sameDay;
}
