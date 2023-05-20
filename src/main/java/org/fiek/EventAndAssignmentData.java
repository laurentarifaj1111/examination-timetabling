package org.fiek;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EventAndAssignmentData {
    List<String> examIds;
    List<Integer> periods;

    public EventAndAssignmentData() {
        examIds = new ArrayList<>();
        periods = new ArrayList<>();
    }
}
