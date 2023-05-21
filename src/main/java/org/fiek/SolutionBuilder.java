package org.fiek;

import lombok.Getter;
import lombok.Setter;
import org.fiek.models.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Setter
public class SolutionBuilder {
    String instanceName;
    Instance instance;
    List<Exam> exams;
    Set<Exam> examsWithHardConstraintConflictSet;
    Set<Exam> examsWithSoftConstraintConflictSet;
    Map<String, AtomicInteger> curriculasWithConflicts;
    int hardConflicts;
    int softConflicts;
    int hash;
    int eval;


    public SolutionBuilder() {
        hardConflicts = 0;
        softConflicts = 0;
        curriculasWithConflicts = new HashMap<>();
        examsWithHardConstraintConflictSet = new HashSet<>();
        examsWithSoftConstraintConflictSet = new HashSet<>();
    }


    public void evaluate() {
        hardConflicts = 0;
        softConflicts = 0;
        examsWithHardConstraintConflictSet = new HashSet<>();
        examsWithSoftConstraintConflictSet = new HashSet<>();
        evalHardConstraints();
        evalSoftConstraints();
        calculateEval();
        this.hash = generateHash();
    }

    public void calculateEval() {
        this.eval = hardConflicts * 1000;
        this.eval = this.eval + softConflicts;
    }

    public void evalSoftConstraints() {
        softConflictEval();
        primaryDistance();
        primarySecondaryDistance();
    }

    public void evalHardConstraints() {
        roomTypeAndNumCheck();
        roomOccupationCheck();
        hardConflictEval();
    }

    /**
     * RoomRequest check
     */
    private void roomTypeAndNumCheck() {
        for (Exam exam : this.getExams()) {
            List<String> rooms =
                    exam.getRooms().stream()
                            .map(Room::getType)
                            .toList();

            Optional<String> first = rooms.stream()
                    .filter(roomType -> !roomType.equals(exam.getCourse().getRoomsRequested().getType()))
                    .findFirst();
            if (first.isPresent() || exam.getRooms().size() != exam.getCourse().getRoomsRequested().getNumber()) {
                exam.setHasRoomRequestConflict(true);
                examsWithHardConstraintConflictSet.add(exam);
            }
        }
    }

    public int getHash() {
        return this.hash;
    }

    public int generateHash() {
        List<Integer> ints = new ArrayList<>();
        this.exams.forEach(exam -> {
            ints.add(exam.getHash());
        });
        return Objects.hash(ints.stream()
                .sorted()
                .collect(Collectors.toList()));
    }

    private void roomOccupationCheck() {
        Map<String, List<Exam>> examsGroupedByRoomOccupation = new HashMap<>();
        for (Exam exam : this.exams) {
            for (Room room : exam.getRooms()) {
                String roomAndPeriodKey = room.getRoom().trim() + "-" + exam.getPeriod();
                if (examsGroupedByRoomOccupation.containsKey(roomAndPeriodKey)) {
                    examsGroupedByRoomOccupation.get(roomAndPeriodKey).add(exam);
                } else {
                    ArrayList<Exam> arrayList = new ArrayList<>();
                    arrayList.add(exam);
                    examsGroupedByRoomOccupation.put(roomAndPeriodKey, arrayList);
                }
            }
        }
        examsGroupedByRoomOccupation.forEach((key, examsList) -> {
            if (examsList.size() > 1) {
                for (Exam exam : examsList) {
                    exam.setHasRoomRequestConflict(true);
                    hardConflicts++;
                    examsWithHardConstraintConflictSet.add(exam);
                }
            }
        });
    }

    private void hardConflictEval() {
        List<Curricula> curriculaList = this.getInstance().getCurricula();
        List<String> teachers = this.instance.getTeachers();
        Map<String, Map<Integer, List<Exam>>> examsOfCurriculaMap = new HashMap<>();
        Map<String, Map<Integer, List<Exam>>> examsOfTeacherMap = new HashMap<>(teachers.size());

        for (Curricula curricula : curriculaList) {
            examsOfCurriculaMap.put(curricula.getId(), new HashMap<>());
        }

        for (String teacher : teachers) {
            examsOfTeacherMap.put(teacher, new HashMap<>());
        }

        for (Exam exam : this.exams) {
            for (Curricula curricula : curriculaList) {
                if (curricula.getPrimaryCourses().contains(exam.getCourse().getId())) {
                    Map<Integer, List<Exam>> examsOfPeriodMap = examsOfCurriculaMap.get(curricula.getId());
                    if (examsOfPeriodMap.containsKey(exam.getPeriod())) {
                        examsOfPeriodMap.get(exam.getPeriod()).add(exam);
                    } else {
                        ArrayList<Exam> examsOfPeriod = new ArrayList<>();
                        examsOfPeriod.add(exam);
                        examsOfPeriodMap.put(exam.getPeriod(), examsOfPeriod);
                    }
                }
            }
            Map<Integer, List<Exam>> integerListMap = examsOfTeacherMap.get(exam.getCourse().getTeacher());
            if (integerListMap.containsKey(exam.getPeriod())) {
                integerListMap.get(exam.getPeriod()).add(exam);
            } else {
                List<Exam> examList = new ArrayList<>();
                examList.add(exam);
                integerListMap.put(exam.getPeriod(), examList);
            }
        }

        examsOfCurriculaMap.forEach((key, integerListMap) ->
                integerListMap.forEach((integer, examsOfPeriod) -> {
                    if (examsOfPeriod.size() > 1) {
                        for (Exam exam : examsOfPeriod) {
                            exam.setHasHardConflict(true);
                            hardConflicts++;
                            examsWithHardConstraintConflictSet.add(exam);
                        }
                    }
                }));
        examsOfTeacherMap.forEach((key, integerListMap) -> {
            integerListMap.forEach((integer, examsOfPeriod) -> {
                if (examsOfPeriod.size() > 1) {
                    for (Exam exam : examsOfPeriod) {
                        exam.setHasHardConflict(true);
                        hardConflicts++;
                        examsWithHardConstraintConflictSet.add(exam);
                    }
                }
            });
        });
    }

    private void softConflictEval() {
        List<Curricula> curriculaList = this.getInstance().getCurricula();
        Map<String, Map<Integer, List<Exam>>> examsOfCurriculaMap = new HashMap<>();
        for (Curricula curricula : curriculaList) {
            examsOfCurriculaMap.put(curricula.getId(), new HashMap<>());
        }
        for (Exam exam : this.exams) {
            for (Curricula curricula : curriculaList) {
                List<String> courses = new ArrayList<>();
                courses.addAll(curricula.getPrimaryCourses());
                courses.addAll(curricula.getSecondaryCourses());
                if (courses.contains(exam.getCourse().getId())) {
                    Map<Integer, List<Exam>> examsOfPeriodMap = examsOfCurriculaMap.get(curricula.getId());
                    if (examsOfPeriodMap.containsKey(exam.getPeriod())) {
                        examsOfPeriodMap.get(exam.getPeriod()).add(exam);
                    } else {
                        ArrayList<Exam> examsOfPeriod = new ArrayList<>();
                        examsOfPeriod.add(exam);
                        examsOfPeriodMap.put(exam.getPeriod(), examsOfPeriod);
                    }
                }
            }
        }

        examsOfCurriculaMap.forEach((key, integerListMap) ->
                integerListMap.forEach((integer, examsOfPeriod) -> {
                    if (examsOfPeriod.size() > 1) {
                        for (Exam exam : examsOfPeriod) {
                            exam.setHasSoftConflict( true);
                            softConflicts++;
                            examsWithSoftConstraintConflictSet.add(exam);
                        }
                    }
                }));
    }

    public void primaryDistance() {
        if (this.instance.getPrimaryPrimaryDistance() == null) {
            return;
        }
        List<Curricula> curriculaList = this.getInstance().getCurricula();
        Map<String, List<String>> curriculaPrimaryCourses = new HashMap<>();
        Map<String, List<Exam>> curriculaPrimaryExamCourses = new HashMap<>();
        for (Curricula curricula : curriculaList) {
            curriculaPrimaryCourses.put(curricula.getId(), curricula.getPrimaryCourses());
            curriculaPrimaryExamCourses.put(curricula.getId(), new ArrayList<>());
        }
        for (Exam exam : this.exams) {
            curriculaPrimaryCourses.forEach((key, courses) -> {
                if (courses.contains(exam.getCourse().getId())) {
                    curriculaPrimaryExamCourses.get(key).add(exam);
                }
            });
        }

        curriculaPrimaryExamCourses.forEach((key, curriculaExams) -> {
            for (int i = 0; i < curriculaExams.size(); i++) {
                for (int j = i + 1; j < curriculaExams.size(); j++) {
                    if (Math.abs(curriculaExams.get(i).getPeriod() - curriculaExams.get(j).getPeriod()) < this.getInstance().getPrimaryPrimaryDistance()) {
                        curriculaExams.get(i).setHasPrimaryPrimaryConflict(true);
                        curriculaExams.get(j).setHasPrimaryPrimaryConflict(true);
                        curriculaExams.get(i).increaseNumOfConflicts(); ;
                        curriculaExams.get(j).increaseNumOfConflicts();
                        this.examsWithSoftConstraintConflictSet.add(curriculaExams.get(i));
                        this.examsWithSoftConstraintConflictSet.add(curriculaExams.get(j));
                        if (curriculasWithConflicts.containsKey(key)) {
                            curriculasWithConflicts.get(key).getAndIncrement();
                        } else {
                            curriculasWithConflicts.put(key, new AtomicInteger(1));
                        }
                        softConflicts++;
                    }
                }
            }
        });
    }

    public void primarySecondaryDistance() {
        if (this.instance.getPrimarySecondaryDistance() == null) {
            return;
        }
        for (int i = 0; i < this.exams.size(); i++) {
            for (int j = i + 1; j < this.exams.size(); j++) {
                String course1 = this.getExams().get(i).getCourse().getId();
                String course2 = this.getExams().get(j).getCourse().getId();
                for (Curricula curricula : this.instance.getCurricula()) {
                    if ((curricula.getPrimaryCourses().contains(course1) && curricula.getSecondaryCourses().contains(course2)) ||
                            (curricula.getPrimaryCourses().contains(course2) && curricula.getSecondaryCourses().contains(course1))) {
                        if (Math.abs(this.getExams().get(i).getPeriod() - this.getExams().get(j).getPeriod()) < this.getInstance().getPrimarySecondaryDistance()) {
                            this.examsWithSoftConstraintConflictSet.add(this.exams.get(i));
                            this.examsWithSoftConstraintConflictSet.add(this.exams.get(j));
                            if (curriculasWithConflicts.containsKey(curricula.getId())) {
                                curriculasWithConflicts.get(curricula.getId()).getAndIncrement();
                            } else {
                                curriculasWithConflicts.put(curricula.getId(), new AtomicInteger(1));
                            }
                            softConflicts++;
                        }
                    }
                }
            }

        }
    }

    public Solution build() {
        Solution resultsFormated = new Solution();
        List<Solution.Exam> collect = this.getExams()
                .stream().map(exam -> {
                    Solution.Exam exam1 = new Solution.Exam();
                    exam1.setPeriod(exam.getPeriod());
                    exam1.setCourse(exam.getCourse().getId());
                    if (exam.getRooms() != null) {
                        exam1.setRoom(exam.getRooms().stream().map(Room::getRoom).collect(Collectors.joining(", ")));
                    } else {
                        exam1.setRoom("");
                    }
                    return exam1;
                }).toList();
        resultsFormated.setAssignments(collect);
        resultsFormated.setCost(this.eval);
        return resultsFormated;
    }

}
