package org.fiek;

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

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Room room1 = (Room) o;
//        return members.equals(room1.members) && room.equals(room1.room) && type.equals(room1.type);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(members, room, type);
//    }
}
