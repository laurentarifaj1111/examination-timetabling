package org.fiek;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomAndPeriod {
    String room;
    String period;

    public RoomAndPeriod(String room, String period) {
        this.room = room;
        this.period = period;
    }

    public RoomAndPeriod() {
    }
}
