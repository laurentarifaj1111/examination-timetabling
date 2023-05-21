package org.fiek;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fiek.helpers.JsonSerialization;
import org.fiek.helpers.LoadInstance;
import org.fiek.models.*;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.fiek.models.Structure.*;


public class Solver {

    private static List<Exam> generateRandomSolution(Instance instance) {
        List<Exam> exams = new ArrayList<>(instance.getCourses().size() * 2);
        Map<String, List<Integer>> roomsInPeriods = new HashMap<>();
        Map<String, List<Integer>> teacherAndPeriod = new HashMap<>();
        for (Room room : instance.getRooms()) {
            roomsInPeriods.put(room.getRoom(), new ArrayList<>());
        }
        for (String teacher : instance.getTeachers()) {
            teacherAndPeriod.put(teacher, new ArrayList<>());
        }

        instance.getCourses().forEach(course -> {
            List<Room> roomsForExam = getRandomRooms(instance.getRooms(), course.getRoomsRequested());
            List<Integer> notAvailablePeriods = new ArrayList<>();
            roomsForExam.forEach(room -> {
                notAvailablePeriods.addAll(roomsInPeriods.get(room.getRoom()));
            });

            int numOfPeriod = (int) (Math.random() * instance.getPeriods());
            while (notAvailablePeriods.contains(numOfPeriod) || teacherAndPeriod.get(course.getTeacher()).contains(numOfPeriod) || numOfPeriod == 0) {
                numOfPeriod = (int) (Math.random() * instance.getPeriods());
            }

            int finalNumOfPeriod1 = numOfPeriod;
            roomsForExam.forEach(room -> roomsInPeriods.get(room.getRoom()).add(finalNumOfPeriod1));
            teacherAndPeriod.get(course.getTeacher()).add(numOfPeriod);
            exams.add(new Exam(UUID.randomUUID().toString(), course, numOfPeriod, roomsForExam));
        });
        return exams;
    }

    public static SolutionBuilder initSolution(String name) throws IOException {
        Instance instance = LoadInstance.instance(name);
        List<Exam> exams = generateRandomSolution(instance);
        SolutionBuilder potentialSolution = new SolutionBuilder();
        potentialSolution.setInstance(instance);
        potentialSolution.setInstanceName(name);
        potentialSolution.setExams(exams);
        potentialSolution.evaluate();
        return potentialSolution;
    }

    private static SolutionBuilder saLocalSearch(SolutionBuilder initialSolution, SolutionBuilder bestSolution, SAConfigs saConfigs, long initTime) throws IOException {
        double temperature = saConfigs.getTemperature();
        int numOfSolutions = 0;
        while (temperature > 0 && initialSolution.eval != 0) {
            SolutionBuilder neighbourSolution = modifySolution(initialSolution);
            neighbourSolution.evaluate();
            numOfSolutions++;
            if (shouldAcceptSolution(initialSolution.eval, neighbourSolution.eval, temperature)) {
                System.out.println("Conflict: " + initialSolution.eval);
                initialSolution = neighbourSolution;
                if (neighbourSolution.eval < bestSolution.eval) {
                    bestSolution = neighbourSolution;
                }
                System.gc();
            }
            if (numOfSolutions == saConfigs.getNumOfSolutionBeforeDecrease()) {
                numOfSolutions = 0;
                temperature = temperature - saConfigs.getCoolingRate();
                System.out.println("Temp decreased to:" + temperature);
            }
        }
        return initialSolution;
    }

    public static void sa(String name) throws IOException {
        SolutionBuilder bestSolutionYet = initSolution(name);
        long initTime = Instant.now().getEpochSecond();
        SAConfigs saConfig = SAConfigs.generateConfig(0.9, 0.03, 500);
        SolutionBuilder solution = saLocalSearch(bestSolutionYet, bestSolutionYet, saConfig, initTime);
        mapAndWriteToFile(solution, initTime, "SA");

    }

    public static void vns(String name) throws IOException {
        SolutionBuilder bestSolutionYet = initSolution(name);
        long initTime = Instant.now().getEpochSecond();
        VNSConfigs vnsConfig = VNSConfigs.compute(20, 10, SINGLE_EXAMS_MOVE);
        SolutionBuilder solution = vnsLocalSearch(bestSolutionYet, vnsConfig);
        mapAndWriteToFile(solution, initTime, "VNS");

    }

    public static void hybrid(String name) throws IOException {
        SolutionBuilder bestSolutionYet = initSolution(name);
        SolutionBuilder iterationBestSolution = copySolution(bestSolutionYet);
        long initTime = Instant.now().getEpochSecond();
        int solutionNotImprovedNumOfTimes = 0;
        int numOfIterations = 0;
        int maxNumOfIterations = 4;
        int bestEval = iterationBestSolution.getEval();
        VNSConfigs vnsConfig = VNSConfigs.compute(20, 10, SINGLE_EXAMS_MOVE, TWO_EXAMS_MOVE);
        while (numOfIterations < maxNumOfIterations + 1) {
            iterationBestSolution = vnsLocalSearch(iterationBestSolution, vnsConfig);
            if (iterationBestSolution.eval <= bestSolutionYet.eval) {
                bestSolutionYet = iterationBestSolution;
            }
            if (bestSolutionYet.eval < bestEval) {
                bestEval = bestSolutionYet.eval;
            } else {
                solutionNotImprovedNumOfTimes++;
            }
            if (solutionNotImprovedNumOfTimes == 2) {
                iterationBestSolution = shakeSolution(iterationBestSolution);
                continue;
            }
            if (numOfIterations == maxNumOfIterations) {
                break;
            }
            if (bestSolutionYet.eval == 0) {
                break;
            }
            System.out.println("Switching to Simulated Annealing");
            SolutionBuilder localOptimumSolution = copySolution(iterationBestSolution);
            SAConfigs saConfig = SAConfigs.generateConfig(0.4, 0.03, 200);
            iterationBestSolution = saLocalSearch(localOptimumSolution, bestSolutionYet, saConfig, initTime);
            if (iterationBestSolution.eval <= bestSolutionYet.eval) {
                bestSolutionYet = iterationBestSolution;
            }
            if (bestSolutionYet.eval == 0) {
                break;
            }
            System.out.println("Switching to VNS");
            numOfIterations++;
        }
        Solution solution = bestSolutionYet.build();
        solution.setExecutionTimeSeconds(Instant.now().getEpochSecond() - initTime);
        writeOutputToAFileFile(solution, iterationBestSolution.instanceName, "hybrid");
    }

    public static SolutionBuilder vnsLocalSearch(SolutionBuilder potentialSolution, VNSConfigs vnsConfigs) throws IOException {
        int latestEvalValue = potentialSolution.getEval();
        int evalRepeated = 0;
        int numOfSolutionsPerStructGenerated = 0;
        Set<Integer> hashesChoosen = new HashSet<>();
        while (!(potentialSolution.eval == 0 || evalRepeated == vnsConfigs.getMaxNumOfSameEval())) {
            List<SolutionBuilder> potentialSolutionList = new ArrayList<>(100);
            int numOfCollisions;
            if (!potentialSolution.getExamsWithHardConstraintConflictSet().isEmpty()) {
                numOfCollisions = potentialSolution.examsWithHardConstraintConflictSet.size();
            } else {
                numOfCollisions = potentialSolution.examsWithSoftConstraintConflictSet.size();
            }
            int maxNeighbourhoodSize = numOfCollisions * potentialSolution.getInstance().getPeriods() - 2;
            int neighbourhoodSize = Math.min(maxNeighbourhoodSize, vnsConfigs.getMaxNeighbourhoodSize());
            Set<Integer> hashes = new HashSet<>(neighbourhoodSize);
            int numOfExamsWithConflict;
            if (potentialSolution.getExamsWithHardConstraintConflictSet().size() > 0) {
                numOfExamsWithConflict = potentialSolution.getExamsWithHardConstraintConflictSet().size();
            } else {
                numOfExamsWithConflict = potentialSolution.getExamsWithSoftConstraintConflictSet().size();
            }

            int neighbourhoodSizePerStructure = Math.min(maxNeighbourhoodSize, vnsConfigs.getMaxNeighbourhoodSize());
            for (Structure structure : vnsConfigs.getStructures()) {
                if (structure.evalThreshold < numOfExamsWithConflict) {
                    int sameSolutionRepeated = 0;
                    while (numOfSolutionsPerStructGenerated < neighbourhoodSizePerStructure) {
                        SolutionBuilder neighbourSolution = copySolution(potentialSolution);
                        List<Exam> examsWithConflict;
                        if (potentialSolution.getExamsWithHardConstraintConflictSet().size() > 0) {
                            examsWithConflict = getExamsRefsWithConflictsOfType(neighbourSolution, ConflictTypes.HARD);
                        } else {
                            examsWithConflict = getExamsRefsWithConflictsOfType(neighbourSolution, ConflictTypes.SOFT);
                        }
                        applyStructUpdateToExams(examsWithConflict, neighbourSolution.getInstance().getPeriods(), structure);
                        neighbourSolution.evaluate();
                        if (hashes.add(neighbourSolution.getHash()) && !hashesChoosen.contains(neighbourSolution.getHash())) {
                            potentialSolutionList.add(neighbourSolution);
                            numOfSolutionsPerStructGenerated++;
                        } else {
                            if (neighbourSolution.eval < potentialSolution.getEval()) {
                                potentialSolutionList.add(neighbourSolution);
                            }
                            if (sameSolutionRepeated == 10) {
                                break;
                            }
                            sameSolutionRepeated++;
                        }
                    }
                }
                numOfSolutionsPerStructGenerated = 0;
            }
            Optional<SolutionBuilder> optionalNeighbourSolution = potentialSolutionList.stream()
                    .min(Comparator.comparingInt(SolutionBuilder::getEval));

            SolutionBuilder neighbourSolution;
            if (optionalNeighbourSolution.isEmpty()) {
                break;
            } else {
                neighbourSolution = optionalNeighbourSolution.get();
            }
            if (neighbourSolution.eval <= potentialSolution.eval) {
                if (latestEvalValue == neighbourSolution.eval) {
                    evalRepeated++;
                } else {
                    latestEvalValue = neighbourSolution.getEval();
                    evalRepeated = 0;
                }
                System.out.println("Conflict: " + potentialSolution.eval);
                potentialSolution = neighbourSolution;
                hashesChoosen.add(neighbourSolution.getHash());
            }
        }
        return potentialSolution;
    }

    private static boolean shouldAcceptSolution(int currentCost, int newCost, double temperature) {
        if (newCost < currentCost) {
            return true;
        }
        Random random = new Random();
        return Math.exp((currentCost - newCost) / temperature) > random.nextDouble();
    }

    private static SolutionBuilder modifySolution(SolutionBuilder potentialSolution) throws IOException {
        String s = JsonSerialization.writeValueAsString(potentialSolution);
        SolutionBuilder potentialSolution1 = JsonSerialization.readValue(s, SolutionBuilder.class);
        if (potentialSolution1.examsWithHardConstraintConflictSet.size() > 0) {
            ArrayList<Exam> examsToUpdate = new ArrayList<>();
            for (Exam exam : potentialSolution1.getExams()) {
                if (potentialSolution.getExamsWithHardConstraintConflictSet().contains(exam)) {
                    examsToUpdate.add(exam);
                }
            }
            int index = (int) Math.floor((examsToUpdate.size() * Math.random()));
            Exam exam1 = examsToUpdate.get(index);
            exam1.setHasRoomOccupationConflict(false);
            exam1.setHasRoomConstraintConflict(false);
            exam1.setHasRoomRequestConflict(false);
            int numOfPeriod = (int) (Math.random() * potentialSolution.instance.getPeriods());
            while (numOfPeriod == 0) {
                numOfPeriod = (int) (Math.random() * potentialSolution.instance.getPeriods());
            }
            exam1.setPeriod(numOfPeriod);
        } else {
            ArrayList<Exam> examsToUpdate = new ArrayList<>();
            for (Exam exam : potentialSolution1.getExams()) {
                if (potentialSolution.getExamsWithSoftConstraintConflictSet().contains(exam)) {
                    examsToUpdate.add(exam);
                }
            }
            int index = (int) Math.floor((examsToUpdate.size() * Math.random()));
            Exam exam1 = examsToUpdate.get(index);
            exam1.setHasSoftConflict(false);
            int numOfPeriod = (int) (Math.random() * potentialSolution.instance.getPeriods());
            while (numOfPeriod == 0) {
                numOfPeriod = (int) (Math.random() * potentialSolution.instance.getPeriods());
            }
            exam1.setPeriod(numOfPeriod);
        }
        return potentialSolution1;
    }

    private static List<Exam> getExamsRefsWithConflictsOfType(SolutionBuilder potentialSolution, ConflictTypes conflictTypes) throws IOException {
        Set<Exam> examsWithConflicts;
        if (conflictTypes == ConflictTypes.HARD) {
            examsWithConflicts = potentialSolution.getExamsWithHardConstraintConflictSet();
        } else {
            examsWithConflicts = potentialSolution.getExamsWithSoftConstraintConflictSet();
        }
        ArrayList<Exam> examsToUpdate = new ArrayList<>();
        for (Exam exam : potentialSolution.getExams()) {
            if (examsWithConflicts.contains(exam)) {
                examsToUpdate.add(exam);
            }
        }
        return examsToUpdate;
    }

    public static void swapPeriodsOfNExams(List<Exam> examsToUpdate, Structure structure) {
        if (examsToUpdate.size() > structure.numOfExams) {
            List<Exam> examsSelected = new ArrayList<>();
            List<Integer> indexGenerated = new ArrayList<>();
            while (indexGenerated.size() < structure.numOfExams) {
                int indexOfExam = (int) Math.floor((examsToUpdate.size() * Math.random()));
                while (indexGenerated.contains(indexOfExam)) {
                    indexOfExam = (int) Math.floor((examsToUpdate.size() * Math.random()));
                }
                examsSelected.add(examsToUpdate.get(indexOfExam));
                indexGenerated.add(indexOfExam);
            }
            for (int i = 0; i < examsSelected.size(); i = i + 2) {
                Exam exam1 = examsSelected.get(i);
                Exam exam2 = examsSelected.get(i + 1);
                int period = exam1.getPeriod();
                exam1.setPeriod(exam2.getPeriod());
                exam2.setPeriod(period);
            }
        }
    }

    private static void applyStructUpdateToExams(List<Exam> examsToUpdate, int numOfPeriods, Structure structure) {
        if (structure.equals(SWAP_PERIODS)) {
            swapPeriodsOfNExams(examsToUpdate, structure);
        } else {
            changePeriodsOfNExams(examsToUpdate, numOfPeriods, structure.numOfExams);
        }
    }

    private static void changePeriodsOfNExams(List<Exam> examsToUpdate, int numOfPeriods, int numOfExams) {
        if (examsToUpdate.size() >= numOfExams) {
            List<Integer> alreadyChoosenPeriod = new ArrayList<>();
            List<Integer> choosenIndex = new ArrayList<>();
            int examsUpdated = 0;
            while (examsUpdated < numOfExams) {
                int index = (int) Math.floor((examsToUpdate.size() * Math.random()));
                while (choosenIndex.contains(index)) {
                    index = (int) Math.floor((examsToUpdate.size() * Math.random()));
                }
                choosenIndex.add(index);
                Exam exam1 = examsToUpdate.get(index);
                exam1.setHasSoftConflict(false);
                int numOfPeriod = (int) (Math.random() * numOfPeriods);
                while (numOfPeriod == 0 || alreadyChoosenPeriod.contains(numOfPeriod)) {
                    numOfPeriod = (int) (Math.random() * numOfPeriods);
                }
                alreadyChoosenPeriod.add(numOfPeriod);
                exam1.setPeriod(numOfPeriod);
                examsUpdated++;
            }
        }
    }

    private static SolutionBuilder shakeSolution(SolutionBuilder potentialSolution) throws IOException {
        String s = JsonSerialization.writeValueAsString(potentialSolution);
        SolutionBuilder potentialSolution1 = JsonSerialization.readValue(s, SolutionBuilder.class);

        Optional<Map.Entry<String, AtomicInteger>> maxEntryOptional = potentialSolution1.curriculasWithConflicts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue(Comparator.comparingInt(AtomicInteger::get)));

        String curriculaKey = maxEntryOptional.get().getKey();
        Curricula curriculaWithId = potentialSolution.getInstance().getCurriculaWithId(curriculaKey);
        List<Exam> curriculaExams = new ArrayList<>();
        for (Exam exam : potentialSolution1.getExams()) {
            List<String> courseIds = new ArrayList<>(curriculaWithId.getPrimaryCourses());
            if (courseIds.contains(exam.getCourse().getId())) {
                curriculaExams.add(exam);
            }

        }

        int firstPeriod = (int) (Math.random() * potentialSolution.instance.getPeriods());
        int modulus = firstPeriod % potentialSolution1.getInstance().getPrimaryPrimaryDistance();

        for (Exam exam : curriculaExams) {
            exam.setHasSoftConflict(false);
            exam.setPeriod(firstPeriod);

            firstPeriod = (int) (Math.random() * potentialSolution.instance.getPeriods());
            while (firstPeriod % potentialSolution1.getInstance().getPrimaryPrimaryDistance() == modulus && firstPeriod == 0)
                firstPeriod = (int) (Math.random() * potentialSolution.instance.getPeriods());

        }
        curriculaExams.forEach(exam -> {
            exam.setHasSoftConflict(false);
            int numOfPeriod = (int) (Math.random() * potentialSolution.instance.getPeriods());
            exam.setPeriod(numOfPeriod);
        });
        return potentialSolution1;
    }

    private static List<Room> getRandomRooms(List<Room> rooms, RoomsRequested roomsRequested) {
        List<Room> roomsForExam = new ArrayList<>();
        int numOfRooms = roomsRequested.getNumber();
        List<Room> validRooms = rooms.stream().filter(room -> room.getType().equals(roomsRequested.getType()))
                .collect(Collectors.toList());
        while (numOfRooms != 0) {
            double numOfRoom = Math.random() * validRooms.size();
            Room room = validRooms.get((int) numOfRoom);
            roomsForExam.add(room);
            validRooms.remove(room);
            numOfRooms--;
        }
        return roomsForExam;
    }


    public static void mapAndWriteToFile(SolutionBuilder solutionBuilder, long initTime, String algorithm) throws IOException {
        Solution solution = solutionBuilder.build();
        solution.setExecutionTimeSeconds(Instant.now().getEpochSecond() - initTime);
        writeOutputToAFileFile(solution, solutionBuilder.instanceName, algorithm);
    }

    private static SolutionBuilder copySolution(SolutionBuilder potentialSolution) throws IOException {
        String solutionString = JsonSerialization.writeValueAsString(potentialSolution);
        return JsonSerialization.readValue(solutionString, SolutionBuilder.class);
    }

    private static void writeOutputToAFileFile(Solution solution, String name, String algorithm) throws IOException {
        String path = "src/main/resources/results/";
        String fileNameFinal = getFileName(path, name, algorithm);
        String pathWithName = path + fileNameFinal;
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(pathWithName), solution);
    }

    private static String getFileName(String path, String name, String algorithm) {
        StringBuilder instanceName = new StringBuilder();
        if (name.contains(".json")) {
            instanceName.append(name.split("\\.")[0]);
        } else {
            instanceName.append(name);
        }
        instanceName.append("-").append(algorithm).append("-").append("output");
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        List<String> strings = Arrays.stream(listOfFiles).map(File::getName).toList();

        List<String> fileResults = strings.stream().filter(fileName -> fileName.contains(instanceName.toString()))
                .toList();

        if (fileResults.isEmpty()) {
            return instanceName.append("-1").append(".").append("json").toString();
        } else {
            Integer max = fileResults.stream()
                    .map(fileName -> {
                        String nameWithoutExtension = fileName.split("\\.")[0];
                        return Integer.valueOf(nameWithoutExtension.replace(instanceName.toString() + "-", ""));
                    })
                    .max(Integer::compareTo).get();
            return instanceName.append("-").append(max + 1).append(".").append("json").toString();
        }
    }
}
