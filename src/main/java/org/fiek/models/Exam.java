package org.fiek.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class Exam {
    String id;
    Course course;
    Integer period;
    List<Room> rooms;
    boolean hasRoomRequestConflict;
    boolean hasRoomOccupationConflict;
    boolean hasRoomConstraintConflict;
    boolean hasHardConflict;
    boolean hasSoftConflict;
    boolean hasPrimaryPrimaryConflict;
    boolean hasPrimarySecondaryConflict;
    int numOfConflicts;


    public Exam(String id, Course course, Integer period, List<Room> rooms) {
        this.id = id;
        this.course = course;
        this.period = period;
        this.rooms = rooms;
    }

    public Exam() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exam exam = (Exam) o;
        return id.equals(exam.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public int getHash() {
        return Objects.hash(this.id, this.period);
    }

    public void increaseNumOfConflicts(){
        this.numOfConflicts++;
    }

}
