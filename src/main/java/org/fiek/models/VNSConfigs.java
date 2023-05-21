package org.fiek.models;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class VNSConfigs {
    int maxNeighbourhoodSize;
    int maxNumOfSameEval;
    List<Structure> structures;

    public static VNSConfigs compute(int maxNumOfSameEval, int maxNeighbourhoodSize, Structure... types) {
        VNSConfigs vnsConfigs = new VNSConfigs();
        vnsConfigs.maxNumOfSameEval = maxNumOfSameEval;
        vnsConfigs.maxNeighbourhoodSize = maxNeighbourhoodSize;
        vnsConfigs.structures = Arrays.asList(types);
        return vnsConfigs;
    }

}
