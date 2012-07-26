package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;

/**
 * Container holding all results of a final albedo computation
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoResult {

    private double[] bsa;
    private double[] wsa;
    private Matrix[] bsaSigma;
    private double[] bsaSigmaArray;
    private double[] wsaSigmaArray;
    private Matrix[] wsaSigma;
    private double weightedNumberOfSamples;
    private double relEntropy;
    private double goodnessOfFit;
    private double snowFraction;
    private double dataMask;
    private double sza;

    public AlbedoResult(double[] bsa, double[] wsa, Matrix[] bsaSigma, Matrix[] wsaSigma, double weightedNumberOfSamples,
                        double relEntropy, double goodnessOfFit, double snowFraction, double dataMask, double sza) {
        this.bsa = bsa;
        this.wsa = wsa;
        this.bsaSigma = bsaSigma;
        this.wsaSigma = wsaSigma;
        this.weightedNumberOfSamples = weightedNumberOfSamples;
        this.relEntropy = relEntropy;
        this.goodnessOfFit = goodnessOfFit;
        this.snowFraction = snowFraction;
        this.dataMask = dataMask;
        this.sza = sza;
    }

public AlbedoResult(double[] bsa, double[] wsa, double[] bsaSigma, double[] wsaSigma, double weightedNumberOfSamples,
                        double relEntropy, double goodnessOfFit, double snowFraction, double dataMask, double sza) {
        this.bsa = bsa;
        this.wsa = wsa;
        this.bsaSigmaArray = bsaSigma;
        this.wsaSigmaArray = wsaSigma;
        this.weightedNumberOfSamples = weightedNumberOfSamples;
        this.relEntropy = relEntropy;
        this.goodnessOfFit = goodnessOfFit;
        this.snowFraction = snowFraction;
        this.dataMask = dataMask;
        this.sza = sza;
    }


    public double[] getBsa() {
        return bsa;
    }

    public double[] getWsa() {
        return wsa;
    }

    public Matrix[] getBsaSigma() {
        return bsaSigma;
    }

    public Matrix[] getWsaSigma() {
        return wsaSigma;
    }

    public double getWeightedNumberOfSamples() {
        return weightedNumberOfSamples;
    }

    public double getRelEntropy() {
        return relEntropy;
    }

    public double getGoodnessOfFit() {
        return goodnessOfFit;
    }

    public double getSnowFraction() {
        return snowFraction;
    }

    public double getDataMask() {
        return dataMask;
    }

    public double getSza() {
        return sza;
    }

    public double[] getBsaSigmaArray() {
        return bsaSigmaArray;
    }

    public double[] getWsaSigmaArray() {
        return wsaSigmaArray;
    }
}
