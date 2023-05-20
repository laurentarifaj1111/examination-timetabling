package org.fiek;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomsRequested {

    @JsonProperty("Number")
    Integer number;
    @JsonProperty("Type")
    String type;
}
