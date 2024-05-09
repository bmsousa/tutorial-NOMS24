package eu.tutorial.components;

import java.util.*;
import org.apache.commons.math3.distribution.BetaDistribution;

public class ReputationSystemModel {

    private final HashMap<String, BetaDistribution> betaDistributions ;
    private final HashMap<String, Integer> ratingMap;
    final static double AGEING_FACTOR = 0.5; // Range [0.0 - 1.0]
    final static double NOT_FOUND = -1.0;

    public ReputationSystemModel(){
        betaDistributions = new HashMap<>();
        ratingMap = new HashMap<>();
    }

    public ReputationSystemModel(List<String> dataProcessors){
        betaDistributions = new HashMap<>();
        ratingMap = new HashMap<>();
        for (String dp : dataProcessors) {
            betaDistributions.put(dp, new BetaDistribution(1, 1));
        }
    }

    public double getReputationScore(String dataProcessor) {
        if (betaDistributions.containsKey(dataProcessor)) {
            return betaDistributions.get(dataProcessor).getNumericalMean();
        } else {
            return NOT_FOUND;
        }
    }

    public void updateReputationScore(String dataProcessor, boolean anomaly) {
        double old_alpha = betaDistributions.get(dataProcessor).getAlpha();
        double old_beta = betaDistributions.get(dataProcessor).getBeta();
        BetaDistribution newBeta;
        if (!anomaly) {
            newBeta = new BetaDistribution((old_alpha * AGEING_FACTOR) + 1.0, old_beta);
        } else {
            newBeta = new BetaDistribution(old_alpha, (old_beta * AGEING_FACTOR) + 1.0);
        }
        betaDistributions.put(dataProcessor, newBeta);
    }

    public void updateReputationScoreSeverity(String dataProcessor, boolean anomaly, double severity) {
        double old_alpha = betaDistributions.get(dataProcessor).getAlpha();
        double old_beta = betaDistributions.get(dataProcessor).getBeta();
        BetaDistribution newBeta;
        if (!anomaly) {
            newBeta = new BetaDistribution((old_alpha * AGEING_FACTOR) + 1.0, old_beta);
        } else {
            newBeta = new BetaDistribution(old_alpha, (old_beta * AGEING_FACTOR) + severity);
        }
        betaDistributions.put(dataProcessor, newBeta);
    }

    public void addNewDataProcessor(String dataProcessor) {
        betaDistributions.putIfAbsent(dataProcessor, new BetaDistribution(1, 1));
    }

    public void addExistingDataProcessor(String dataProcessor, double alpha, double beta) {
        betaDistributions.putIfAbsent(dataProcessor, new BetaDistribution(alpha, beta));
    }

    public HashMap<String, BetaDistribution> getBetaDistributions() {
        return betaDistributions;
    }

    public boolean checkEntityExists(String entityId) {
        return betaDistributions.containsKey(entityId);
    }
    
}
