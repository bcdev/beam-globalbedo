package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;

/**
 * Object holding all results of an inversion computation
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class InversionResult {
    private Matrix parameters;
    private Matrix uncertainties;
    private double entropy;
    private double relEntropy;
    private double weightedNumberOfSamples;
    private int dayToTheClosestSample;
    private double goodnessOfFit;

    public InversionResult(Matrix parameters, Matrix uncertainties, double entropy, double relEntropy,
                           double weightedNumberOfSamples, int dayToTheClosestSample, double goodnessOfFit) {
        this.parameters = parameters;
        this.uncertainties = uncertainties;
        this.entropy = entropy;
        this.relEntropy = relEntropy;
        this.weightedNumberOfSamples = weightedNumberOfSamples;
        this.dayToTheClosestSample = dayToTheClosestSample;
        this.goodnessOfFit = goodnessOfFit;
    }

    public Matrix getParameters() {
        return parameters;
    }

    public Matrix getUncertainties() {
        return uncertainties;
    }

    public double getEntropy() {
        return entropy;
    }

    public double getRelEntropy() {
        return relEntropy;
    }

    public double getWeightedNumberOfSamples() {
        return weightedNumberOfSamples;
    }

    public int getDoyClosestSample() {
        return dayToTheClosestSample;
    }

    public double getGoodnessOfFit() {
        return goodnessOfFit;
    }
}
