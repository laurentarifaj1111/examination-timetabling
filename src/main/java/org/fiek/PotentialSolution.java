package org.fiek;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Setter
public class PotentialSolution {
    String instanceName;
    Instance instance;
    List<Exam> exams;
    int eval;
    List<Entry> listOfData;
    Set<Exam> examsWithHardConstraintConflictSet;
    Set<Exam> examsWithSoftConstraintConflictSet;
    Map<String, AtomicInteger> curriculasWithConflicts;
    int hardConflicts;
    int softConflicts;

    int hash;

    public PotentialSolution() {
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
        this.eval = hardConflicts * 1000;
        this.eval = this.eval + softConflicts;
        this.hash = generateHash();
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
                exam.hasRoomRequestConflict = true;
                examsWithHardConstraintConflictSet.add(exam);
            }
        }
    }

    public int getHash() {
        return this.hash;
//        List<Integer> ints = new ArrayList<>();
//        this.exams.forEach(exam -> {
//            ints.add(exam.getHash());
//        });
//        return Objects.hash(ints.stream()
//                .sorted()
//                .collect(Collectors.toList()));
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


    /**
     * RoomOccupation check
     */
    private void roomOccupationCheck() {
        Map<String, List<Exam>> examsGroupedByRoomOccupation = new HashMap<>();
        for (Exam exam : this.exams) {
            for (Room room : exam.getRooms()) {
                String roomAndPeriodKey = room.getRoom().trim() + "-" + exam.period;
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
                    exam.hasRoomRequestConflict = true;
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
                            exam.hasHardConflict = true;
                            hardConflicts++;
                            examsWithHardConstraintConflictSet.add(exam);
                        }
                    }
                }));
        examsOfTeacherMap.forEach((key, integerListMap) -> {
            integerListMap.forEach((integer, examsOfPeriod) -> {
                if (examsOfPeriod.size() > 1) {
                    for (Exam exam : examsOfPeriod) {
                        exam.hasHardConflict = true;
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
                            exam.hasSoftConflict = true;
                            softConflicts++;
                            examsWithSoftConstraintConflictSet.add(exam);
                        }
                    }
                }));
    }

    public void primaryDistance() {
        if (this.instance.primaryPrimaryDistance == null) {
            return;
        }
        List<Curricula> curriculaList = this.getInstance().curricula;
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
                    if (Math.abs(curriculaExams.get(i).getPeriod() - curriculaExams.get(j).getPeriod()) < this.getInstance().primaryPrimaryDistance) {
                        curriculaExams.get(i).setHasPrimaryPrimaryConflict(true);
                        curriculaExams.get(j).setHasPrimaryPrimaryConflict(true);
                        curriculaExams.get(i).numOfConflicts++;
                        curriculaExams.get(j).numOfConflicts++;
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
        if (this.instance.primarySecondaryDistance == null) {
            return;
        }
        for (int i = 0; i < this.exams.size(); i++) {
            for (int j = i + 1; j < this.exams.size(); j++) {
                String course1 = this.getExams().get(i).course.id;
                String course2 = this.getExams().get(j).course.id;
                for (Curricula curricula : this.instance.curricula) {
                    if ((curricula.primaryCourses.contains(course1) && curricula.getSecondaryCourses().contains(course2)) ||
                            (curricula.primaryCourses.contains(course2) && curricula.getSecondaryCourses().contains(course1))) {
                        if (Math.abs(this.getExams().get(i).getPeriod() - this.getExams().get(j).getPeriod()) < this.getInstance().primarySecondaryDistance) {
                            this.examsWithSoftConstraintConflictSet.add(this.exams.get(i));
                            this.examsWithSoftConstraintConflictSet.add(this.exams.get(j));
                            if (curriculasWithConflicts.containsKey(curricula.getId())) {
                                curriculasWithConflicts.get(curricula.getId()).getAndIncrement();
                            } else {
                                curriculasWithConflicts.put(curricula.id, new AtomicInteger(1));
                            }
                            softConflicts++;
                        }
                    }
                }
            }

        }
    }


//    @Override
//    public int compareTo(PotentialSolution o1) {
//        if (o1.eval != this.getEval()) {
//            return -Integer.compare(this.getEval(), o1.getEval());
//        }
//        List<Map.Entry<Integer, Long>> entryList1 = o1.getExams().stream()
//                .filter(exam -> exam.getNumOfConflicts() > 0)
//                .sorted(Comparator.comparing(exam -> exam.numOfConflicts))
//                .collect(Collectors.groupingBy(exam -> exam.numOfConflicts, Collectors.counting()))
//                .entrySet().stream()
//                .sorted(Map.Entry.<Integer, Long>comparingByKey().reversed())
//                .toList();
//
//
//        List<Map.Entry<Integer, Long>> entryList2 = this.getExams().stream()
//                .filter(exam -> exam.getNumOfConflicts() > 0)
//                .sorted(Comparator.comparing(exam -> exam.numOfConflicts))
//                .collect(Collectors.groupingBy(exam -> exam.numOfConflicts, Collectors.counting()))
//                .entrySet().stream()
//                .sorted(Map.Entry.<Integer, Long>comparingByKey().reversed())
//                .toList();
//        if (!entryList1.isEmpty() && !entryList2.isEmpty()) {
//            if (entryList1.get(0) != entryList2.get(0)) {
//                return Integer.compare(entryList2.get(0).getKey(), entryList1.get(0).getKey());
//            }
//        }
//        return Integer.compare(entryList2.size(), entryList1.size());
//    }

}
