package org.fiek.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SAConfigs {
    double temperature;
    double coolingRate;
    int numOfSolutionBeforeDecrease;

    public static SAConfigs generateConfig(double temperature, double coolingRate, int numOfSolutionBeforeDecrease){
        SAConfigs saConfigs = new SAConfigs();
        saConfigs.temperature = temperature;
        saConfigs.coolingRate = coolingRate;
        saConfigs.numOfSolutionBeforeDecrease = numOfSolutionBeforeDecrease;
        return saConfigs;
    }

}
