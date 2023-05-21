package org.fiek;

import org.fiek.models.Algorithm;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new Exception("Invalid num of arguments");
        }
        String instanceName = args[0];

        List<String> algorithms = Arrays.stream(Algorithm.values()).map(Objects::toString).toList();
        String algorithm = args[1];
        if (!algorithms.contains(algorithm)) {
            throw new Exception("Invalid algorithm");
        }
        System.out.printf("Starting to run instance %s with algorithm: %s%n", instanceName, algorithm);
        Algorithm choosenAlgorithm = Algorithm.valueOf(algorithm);
        switch (choosenAlgorithm) {
            case SA -> Solver.sa(instanceName);
            case VNS -> Solver.vns(instanceName);
            case HYBRID -> Solver.hybrid(instanceName);
        }

    }
}