package org.fiek;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        File instancesFolder = new File("src/main/resources/InstancesJSON");
        File[] listOfInstancesFiles = instancesFolder.listFiles();

        List<String> strings = Arrays.stream(listOfInstancesFiles).map(File::getName).toList();


        Solver solver = new Solver();
        solver.vnsAndSaSolution("D1-1-16.json");
//            strings.forEach(name ->
//            {
//                System.out.println("====================================================================================");
//                System.out.println("Starting to execute instance with name " + name);
//                try {
//                    solver.vnsAndSaSolution(name);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });


//        File folder = new File("src/main/resources/results-vns-4");
//        File[] listOfFiles = folder.listFiles();
//        Map<String, String> map = new TreeMap<>();
//        List<Pair<String, String>> data =new ArrayList<>();
//        ObjectMapper mapper = new ObjectMapper();
//
//        for (int i=0; i< listOfFiles.length - 3;i= i+3){
//            ResultsFormated resultsFormated1 = mapper.readValue(listOfFiles[i], ResultsFormated.class);
//            ResultsFormated resultsFormated2 = mapper.readValue(listOfFiles[i+1], ResultsFormated.class);
//            ResultsFormated resultsFormated3 = mapper.readValue(listOfFiles[i+2], ResultsFormated.class);
//            String value = "";
//            if (resultsFormated1.getExecutionTimeSeconds() != null){
//                value = value + "/" + resultsFormated1.getExecutionTimeSeconds();
//            }
//            if (resultsFormated2.getExecutionTimeSeconds() != null){
//                value = value + "/" + resultsFormated2.getExecutionTimeSeconds();
//            }
//            if (resultsFormated3.getExecutionTimeSeconds() != null){
//                value = value + "/" + resultsFormated3.getExecutionTimeSeconds();
//            }
//
//
////            String value= resultsFormated1.getCost() + "/" + resultsFormated2.getCost() + "/" + resultsFormated3.getCost() + "__" +
////            resultsFormated1.getExecutionTimeSeconds() + "/" + resultsFormated2.getExecutionTimeSeconds() + "/" + resultsFormated3.getExecutionTimeSeconds();
//            data.add(Pair.of(listOfFiles[i].getName(), value));
//        }
//        List<Output> outputs = Arrays.stream(listOfFiles)
//                .map(file -> {
//                    try {
//                        ResultsFormated resultsFormated = mapper.readValue(file, ResultsFormated.class);
//                        return new Output(file.getName(), resultsFormated.getCost(), resultsFormated.executionTimeSeconds);
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }).toList();
//
//        mapper.writeValue(new File("output-vns-4-struct.json"), outputs);
//        mapper.writeValue(new File("output-vns-4-formated.json"), data);


    }
}