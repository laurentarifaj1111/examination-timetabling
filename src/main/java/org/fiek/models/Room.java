package org.fiek.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Room {
    @JsonProperty("Members")
    List<String> members;
    @JsonProperty("Room")
    String room;
    @JsonProperty("Type")
    String type;

}
