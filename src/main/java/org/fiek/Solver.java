package org.fiek;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.fiek.StructureType.*;


public class Solver {

    //    long startTime = System.currentTimeMillis();
    public static List<PotentialSolution> initialSolutions = new ArrayList<>();

    public List<Exam> generateRandomSolution(Instance instance) {
        List<Exam> exams = new ArrayList<>(instance.getCourses().size() * 2);
        Map<String, List<Integer>> roomsInPeriods = new HashMap<>();
        Map<String, List<Integer>> teacherAndPeriod = new HashMap<>();
        for (Room room : instance.getRooms()) {
            roomsInPeriods.put(room.room, new ArrayList<>());
        }
        for (String teacher : instance.getTeachers()) {
            teacherAndPeriod.put(teacher, new ArrayList<>());
        }

        instance.courses.forEach(course -> {
            List<Room> roomsForExam = getRandomRooms(instance.getRooms(), course.getRoomsRequested());
            List<Integer> notAvailablePeriods = new ArrayList<>();
            roomsForExam.forEach(room -> {
                notAvailablePeriods.addAll(roomsInPeriods.get(room.room));
            });

            int numOfPeriod = (int) (Math.random() * instance.periods);
            while (notAvailablePeriods.contains(numOfPeriod) || teacherAndPeriod.get(course.teacher).contains(numOfPeriod) || numOfPeriod == 0) {
                numOfPeriod = (int) (Math.random() * instance.periods);
            }

            int finalNumOfPeriod1 = numOfPeriod;
            roomsForExam.forEach(room -> roomsInPeriods.get(room.room).add(finalNumOfPeriod1));
            teacherAndPeriod.get(course.teacher).add(numOfPeriod);
            exams.add(new Exam(UUID.randomUUID().toString(), course, numOfPeriod, roomsForExam));
        });
        return exams;
    }

    public PotentialSolution saLocalSearch(PotentialSolution initialSolution, PotentialSolution bestSolution, SAConfigs saConfigs, long initTime) throws IOException {
        double temperature = saConfigs.temperature;
        int numOfSolutions = 0;
        List<Entry> listOfData = initialSolution.getListOfData();
        while (temperature > 0 && initialSolution.eval != 0) {
            PotentialSolution neighbourSolution = modifySolution(initialSolution);
            neighbourSolution.evaluate();
            numOfSolutions++;
            if (updateSolution(initialSolution.eval, neighbourSolution.eval, temperature)) {
                listOfData.add(new Entry(Instant.now().getEpochSecond() - initTime, neighbourSolution.eval));
                System.out.println("Conflict: " + initialSolution.eval);
                initialSolution = neighbourSolution;
                if (neighbourSolution.eval < bestSolution.eval) {
                    bestSolution = neighbourSolution;
                }
                System.gc();
            }
            if (numOfSolutions == saConfigs.numOfSolutionBeforeDecrease) {
                numOfSolutions = 0;
                temperature = temperature - saConfigs.coolingRate;
                System.out.println("Temp decreased to:" + temperature);
            }
        }
        initialSolution.listOfData = listOfData;
        return initialSolution;
    }

    public void writeFile(List<Entry> data, String name, String algorithm) {
        String eol = System.getProperty("line.separator");
        String fileNameFinal = getFileName("src/main/resources/eval/", name, algorithm, "eval-vns-2", "csv");
        String path = "src/main/resources/eval/" + fileNameFinal;
        try (Writer writer = new FileWriter(path)) {
            for (Entry entry : data) {
                writer.append(entry.getTime().toString())
                        .append(',')
                        .append(entry.getEval().toString())
                        .append(eol);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
    }

    public void writeOutputToAFileFile(ResultsFormated resultsFormated, String name, String algorithm) throws IOException {
        String fileNameFinal = getFileName("src/main/resources/results/", name, algorithm, "output", "json");
        String path = "src/main/resources/results/" + fileNameFinal;
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(path), resultsFormated);
    }

    public String getFileName(String path, String name, String algorithm, String postfix, String extension) {
        StringBuilder instanceName = new StringBuilder();
        if (name.contains(".json")) {
            instanceName.append(name.split("\\.")[0]);
        } else {
            instanceName.append(name);
        }
        instanceName.append("-").append(algorithm).append("-").append(postfix);
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        List<String> strings = Arrays.stream(listOfFiles).map(File::getName).toList();

        List<String> fileResults = strings.stream().filter(fileName -> fileName.contains(instanceName.toString()))
                .toList();

        if (fileResults.isEmpty()) {
            return instanceName.append("-1").append(".").append(extension).toString();
        } else {
            Integer max = fileResults.stream()
                    .map(fileName -> {
                        String nameWithoutExtension = fileName.split("\\.")[0];
                        return Integer.valueOf(nameWithoutExtension.replace(instanceName.toString() + "-", ""));
                    })
                    .max(Integer::compareTo).get();
            return instanceName.append("-").append(max + 1).append(".").append(extension).toString();
        }


    }

    public void saSolution(String name) throws IOException {
        PotentialSolution bestSolutionYet = initSolution(name);
        SAConfigs saConfigs = new SAConfigs();
        saConfigs.temperature = 0.8;
        saConfigs.coolingRate = 0.03;
        saConfigs.numOfSolutionBeforeDecrease = 500;
        List<Entry> listOfData = new ArrayList<>(100000);
        long initTime = Instant.now().getEpochSecond();
        bestSolutionYet.listOfData = listOfData;
        PotentialSolution potentialSolution = saLocalSearch(bestSolutionYet, bestSolutionYet, saConfigs, initTime);
        ResultsFormated resultsFormated = ResultsFormated.fromPotentialSolution(potentialSolution);
        resultsFormated.setExecutionTimeSeconds(Instant.now().getEpochSecond() - initTime);
        writeOutputToAFileFile(resultsFormated, potentialSolution.instanceName, "sa");
        writeFile(listOfData, potentialSolution.getInstanceName(), "sa");

    }

    public void vnsSolution(String name) throws IOException {
        PotentialSolution bestSolutionYet = initSolution(name);
        VNSConfigs vnsConfigs = new VNSConfigs();
        vnsConfigs.maxNumOfSameEval = 20;
        vnsConfigs.maxNeighbourhoodSize = 10;
        Structure singleExamMoveStructure = new Structure();
        singleExamMoveStructure.setStructureType(SINGLE_EXAMS_MOVE);
        singleExamMoveStructure.setNumOfExamsWithConflictsThreshold(1);

//        Structure twoExamMoveStructure = new Structure();
//        twoExamMoveStructure.setStructureType(TWO_EXAMS_MOVE);
//        twoExamMoveStructure.setNumOfExamsWithConflictsThreshold(8);
//
//        Structure threeExamMoveStructure = new Structure();
//        threeExamMoveStructure.setStructureType(StructureType.THREE_EXAMS_MOVE);
//        threeExamMoveStructure.setNumOfExamsWithConflictsThreshold(12);
//
//        Structure swapPeriodsMoveStructure = new Structure();
//        swapPeriodsMoveStructure.setStructureType(SWAP_PERIODS);
//        swapPeriodsMoveStructure.setNumOfExamsWithConflictsThreshold(12);
        List<Structure> structures = List
                .of(singleExamMoveStructure);
        List<Entry> listOfData = new ArrayList<>(10000);
        long initTime = Instant.now().getEpochSecond();
        bestSolutionYet.listOfData = listOfData;
        PotentialSolution potentialSolution = vnsLocalSearch2(bestSolutionYet, structures, vnsConfigs, initTime);
        ResultsFormated resultsFormated = ResultsFormated.fromPotentialSolution(potentialSolution);
        resultsFormated.setExecutionTimeSeconds(Instant.now().getEpochSecond() - initTime);
        writeOutputToAFileFile(resultsFormated, potentialSolution.instanceName, "vns");
        writeFile(listOfData, potentialSolution.getInstanceName(), "vns");

    }

    public void vnsAndSaSolution(String name) throws IOException {
        PotentialSolution bestSolutionYet = initSolution(name);
        PotentialSolution iterationBestSolution = copySolution(bestSolutionYet);

        Structure singleExamMoveStructure = new Structure();
        singleExamMoveStructure.setStructureType(SINGLE_EXAMS_MOVE);
        singleExamMoveStructure.setNumOfExamsWithConflictsThreshold(1);

        Structure twoExamMoveStructure = new Structure();
        twoExamMoveStructure.setStructureType(TWO_EXAMS_MOVE);
        twoExamMoveStructure.setNumOfExamsWithConflictsThreshold(8);

//        Structure threeExamMoveStructure = new Structure();
//        threeExamMoveStructure.setStructureType(THREE_EXAMS_MOVE);
//        threeExamMoveStructure.setNumOfExamsWithConflictsThreshold(12);
//
//        Structure swapPeriodsMoveStructure = new Structure();
//        swapPeriodsMoveStructure.setStructureType(THREE_EXAMS_MOVE);
//        swapPeriodsMoveStructure.setNumOfExamsWithConflictsThreshold(12);
        List<Structure> structures = List
                .of(singleExamMoveStructure, twoExamMoveStructure);
        List<Entry> listOfData = new ArrayList<>(100000);
        long initTime = Instant.now().getEpochSecond();
        int solutionNotImprovedNumOfTimes = 0;
        int numOfIterations = 0;
        int maxNumOfIterations = 4;
        int bestEval = iterationBestSolution.getEval();
        iterationBestSolution.listOfData = listOfData;
        bestSolutionYet.listOfData = listOfData;
        while (numOfIterations < maxNumOfIterations + 1) {
            VNSConfigs vnsConfigs = new VNSConfigs();
            vnsConfigs.maxNumOfSameEval = 20;
            vnsConfigs.maxNeighbourhoodSize = 20;
            iterationBestSolution = vnsLocalSearch2(iterationBestSolution, structures, vnsConfigs, initTime);
            if (iterationBestSolution.eval <= bestSolutionYet.eval) {
                bestSolutionYet = iterationBestSolution;
            }
            if (bestSolutionYet.eval < bestEval) {
                bestEval = bestSolutionYet.eval;
            } else {
                solutionNotImprovedNumOfTimes++;
            }
            if (solutionNotImprovedNumOfTimes == 2) {
                shakeSolution(iterationBestSolution);
                continue;
            }
            if (numOfIterations == maxNumOfIterations) {
                break;
            }
            if (bestSolutionYet.eval == 0) {
                break;
            }
            SAConfigs saConfigs = new SAConfigs();
            saConfigs.temperature = 0.4;
            saConfigs.coolingRate = 0.02;
            saConfigs.numOfSolutionBeforeDecrease = 200;
            System.out.println("Switching to Simulated Annealing");
            PotentialSolution localOptimumSolution = copySolution(iterationBestSolution);
            iterationBestSolution = saLocalSearch(localOptimumSolution, bestSolutionYet, saConfigs, initTime);
            if (iterationBestSolution.eval <= bestSolutionYet.eval) {
                bestSolutionYet = iterationBestSolution;
            }
            if (bestSolutionYet.eval == 0) {
                break;
            }
            bestSolutionYet.listOfData = iterationBestSolution.listOfData;
            System.out.println("Switching to VNS");
            numOfIterations++;
        }
        ResultsFormated resultsFormated = ResultsFormated.fromPotentialSolution(bestSolutionYet);
        resultsFormated.setExecutionTimeSeconds(Instant.now().getEpochSecond() - initTime);
        writeOutputToAFileFile(resultsFormated, iterationBestSolution.instanceName, "hybrid");
        writeFile(listOfData, iterationBestSolution.getInstanceName(), "hybrid");
    }


    public PotentialSolution initSolution(String name) throws IOException {
        Instance instance = LoadInstance.instance(name);
        List<Exam> exams = generateRandomSolution(instance);
        PotentialSolution potentialSolution = new PotentialSolution();
        potentialSolution.setInstance(instance);
        potentialSolution.setInstanceName(name);
        potentialSolution.setExams(exams);
        potentialSolution.evaluate();
        return potentialSolution;
    }

    public PotentialSolution vnsLocalSearch2(PotentialSolution potentialSolution, List<Structure> structureTypeList, VNSConfigs vnsConfigs, long aLong) throws IOException {
        int latestEvalValue = potentialSolution.getEval();
        List<Entry> listOfData = potentialSolution.getListOfData();
        int evalRepeated = 0;
        int numOfSolutionsPerStructGenerated = 0;
        Set<Integer> hashesChoosen = new HashSet<>(5000);
        while (!(potentialSolution.eval == 0 || evalRepeated == vnsConfigs.maxNumOfSameEval)) {
            List<PotentialSolution> potentialSolutionList = new ArrayList<>(100);
            int numOfCollisions;
            if (!potentialSolution.getExamsWithHardConstraintConflictSet().isEmpty()) {
                numOfCollisions = potentialSolution.examsWithHardConstraintConflictSet.size();
            } else {
                numOfCollisions = potentialSolution.examsWithSoftConstraintConflictSet.size();
            }
            int maxNeighbourhoodSize = numOfCollisions * potentialSolution.getInstance().periods - 2;
            int neighbourhoodSize = Math.min(maxNeighbourhoodSize, vnsConfigs.maxNeighbourhoodSize);
            Set<Integer> hashes = new HashSet<>(neighbourhoodSize);
            int numOfExamsWithConflict;
            if (potentialSolution.getExamsWithHardConstraintConflictSet().size() > 0) {
                numOfExamsWithConflict = potentialSolution.getExamsWithHardConstraintConflictSet().size();
            } else {
                numOfExamsWithConflict = potentialSolution.getExamsWithSoftConstraintConflictSet().size();
            }


            int neighbourhoodSizePerStructure = Math.min(maxNeighbourhoodSize, vnsConfigs.maxNeighbourhoodSize);
            for (Structure structure : structureTypeList) {
                if (structure.getNumOfExamsWithConflictsThreshold() < numOfExamsWithConflict) {
                    int sameSolutionRepeated = 0;
                    while (numOfSolutionsPerStructGenerated < neighbourhoodSizePerStructure) {
                        PotentialSolution neighbourSolution = copySolution(potentialSolution);
                        List<Exam> examsWithConflict;
                        if (potentialSolution.getExamsWithHardConstraintConflictSet().size() > 0) {
                            examsWithConflict = getExamsRefsWithConflictsOfType(neighbourSolution, ConflictTypes.HARD);
                        } else {
                            examsWithConflict = getExamsRefsWithConflictsOfType(neighbourSolution, ConflictTypes.SOFT);
                        }
                        applyStructUpdateToExams(examsWithConflict, neighbourSolution.getInstance().periods, structure.structureType);
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


            Optional<PotentialSolution> optionalNeighbourSolution = potentialSolutionList.stream()
                    .min(Comparator.comparingInt(PotentialSolution::getEval));

            PotentialSolution neighbourSolution;
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
                listOfData.add(new Entry(Instant.now().getEpochSecond() - aLong, potentialSolution.eval));
                hashesChoosen.add(neighbourSolution.getHash());
            } else {
                System.out.println("Test Conflict: " + optionalNeighbourSolution.get().eval);
            }
        }
        potentialSolution.listOfData = listOfData;
        return potentialSolution;
    }


    public PotentialSolution copySolution(PotentialSolution potentialSolution) throws IOException {
        String solutionString = JsonSerialization.writeValueAsString(potentialSolution);
        return JsonSerialization.readValue(solutionString, PotentialSolution.class);
    }

    private boolean updateSolution(int currentCost, int newCost, double temperature) {
        if (newCost < currentCost) {
            return true;
        }
        Random random = new Random();
        return Math.exp((currentCost - newCost) / temperature) > random.nextDouble();
    }

    public PotentialSolution modifySolution(PotentialSolution potentialSolution) throws IOException {
        String s = JsonSerialization.writeValueAsString(potentialSolution);
        PotentialSolution potentialSolution1 = JsonSerialization.readValue(s, PotentialSolution.class);
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
            int numOfPeriod = (int) (Math.random() * potentialSolution.instance.periods);
            while (numOfPeriod == 0) {
                numOfPeriod = (int) (Math.random() * potentialSolution.instance.periods);
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
            int numOfPeriod = (int) (Math.random() * potentialSolution.instance.periods);
            while (numOfPeriod == 0) {
                numOfPeriod = (int) (Math.random() * potentialSolution.instance.periods);
            }
            exam1.setPeriod(numOfPeriod);
        }
        return potentialSolution1;
    }

    public List<Exam> getExamsRefsWithConflictsOfType(PotentialSolution potentialSolution, ConflictTypes conflictTypes) throws IOException {
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


    public PotentialSolution swapPeriodsOf2Exams(PotentialSolution potentialSolution) throws IOException {
        String s = JsonSerialization.writeValueAsString(potentialSolution);
        PotentialSolution potentialSolution1 = JsonSerialization.readValue(s, PotentialSolution.class);
        ArrayList<Exam> examsToUpdate = new ArrayList<>();
        for (Exam exam : potentialSolution1.getExams()) {
            if (potentialSolution.getExamsWithSoftConstraintConflictSet().contains(exam)) {
                examsToUpdate.add(exam);
            }
        }
        if (examsToUpdate.size() > 2) {
            List<Exam> examsSelected = new ArrayList<>();
            List<Integer> indexGenerated = new ArrayList<>();
            while (indexGenerated.size() < 2) {
                int indexOfExam = (int) Math.floor((examsToUpdate.size() * Math.random()));
                while (indexGenerated.contains(indexOfExam)) {
                    indexOfExam = (int) Math.floor((examsToUpdate.size() * Math.random()));
                }
                examsSelected.add(examsToUpdate.get(indexOfExam));
                indexGenerated.add(indexOfExam);
            }
            Exam exam1 = examsSelected.get(0);
            Exam exam2 = examsSelected.get(1);
            int period = exam1.period;
            exam1.period = exam2.period;
            exam2.period = period;

        }
        return potentialSolution1;
    }


    public void changePeriodsOfNExams(List<Exam> examsToUpdate, int numOfPeriods, int numOfExams) throws IOException {
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


    public void applyStructUpdateToExams(List<Exam> examsToUpdate, int numOfPeriods, StructureType structureType) throws IOException {
        if (structureType.equals(SINGLE_EXAMS_MOVE)) {
            changePeriodsOfNExams(examsToUpdate, numOfPeriods, 1);
        }
        if (structureType.equals(TWO_EXAMS_MOVE)) {
            changePeriodsOfNExams(examsToUpdate, numOfPeriods, 2);
        }
        if (structureType.equals(THREE_EXAMS_MOVE)) {
            changePeriodsOfNExams(examsToUpdate, numOfPeriods, 3);
        }
        if (structureType.equals(SWAP_PERIODS)) {
            if (examsToUpdate.size() > 2) {
                List<Exam> examsSelected = new ArrayList<>();
                List<Integer> indexGenerated = new ArrayList<>();
                while (indexGenerated.size() < 2) {
                    int indexOfExam = (int) Math.floor((examsToUpdate.size() * Math.random()));
                    while (indexGenerated.contains(indexOfExam)) {
                        indexOfExam = (int) Math.floor((examsToUpdate.size() * Math.random()));
                    }
                    examsSelected.add(examsToUpdate.get(indexOfExam));
                    indexGenerated.add(indexOfExam);
                }
                Exam exam1 = examsSelected.get(0);
                Exam exam2 = examsSelected.get(1);
                int period = exam1.period;
                exam1.period = exam2.period;
                exam2.period = period;
            }
        }
    }


    public PotentialSolution shakeSolution(PotentialSolution potentialSolution) throws IOException {
        String s = JsonSerialization.writeValueAsString(potentialSolution);
        PotentialSolution potentialSolution1 = JsonSerialization.readValue(s, PotentialSolution.class);

        Optional<Map.Entry<String, AtomicInteger>> maxEntryOptional = potentialSolution1.curriculasWithConflicts.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue(Comparator.comparingInt(AtomicInteger::get)));


        String curriculaKey = maxEntryOptional.get().getKey();
        Curricula curriculaWithId = potentialSolution.getInstance().getCurriculaWithId(curriculaKey);
        List<Exam> curriculaExams = new ArrayList<>();
        for (Exam exam : potentialSolution1.getExams()) {
            List<String> courseIds = new ArrayList<>(curriculaWithId.getPrimaryCourses());
            if (courseIds.contains(exam.getCourse().id)) {
                curriculaExams.add(exam);
            }

        }


        int firstPeriod = (int) (Math.random() * potentialSolution.instance.periods);
        int modulus = firstPeriod % potentialSolution1.getInstance().primaryPrimaryDistance;

        for (Exam exam : curriculaExams) {
            exam.setHasSoftConflict(false);
            exam.setPeriod(firstPeriod);

            firstPeriod = (int) (Math.random() * potentialSolution.instance.periods);
            while (firstPeriod % potentialSolution1.getInstance().primaryPrimaryDistance == modulus && firstPeriod == 0)
                firstPeriod = (int) (Math.random() * potentialSolution.instance.periods);

        }
        curriculaExams.forEach(exam -> {
            exam.setHasSoftConflict(false);
            int numOfPeriod = (int) (Math.random() * potentialSolution.instance.periods);
            exam.setPeriod(numOfPeriod);
        });
        return potentialSolution1;
    }

    public List<Room> getRandomRooms(List<Room> rooms, RoomsRequested roomsRequested) {
        List<Room> roomsForExam = new ArrayList<>();
        int numOfRooms = roomsRequested.number;
        List<Room> validRooms = rooms.stream().filter(room -> room.type.equals(roomsRequested.type))
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


}
