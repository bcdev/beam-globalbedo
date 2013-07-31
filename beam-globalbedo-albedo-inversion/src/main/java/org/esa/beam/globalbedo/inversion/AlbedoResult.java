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
    private double[] bsaAlpha;
    private Matrix[] bsaSigma;
    private double[] bsaSigmaArray;
    private double[] wsa;
    private double[] wsaAlpha;
    private Matrix[] wsaSigma;
    private double[] wsaSigmaArray;
    private double weightedNumberOfSamples;
    private double relEntropy;
    private double goodnessOfFit;
    private double snowFraction;
    private double dataMask;
    private double sza;

    public AlbedoResult(double[] bsa, double[] bsaAlpha, Matrix[] bsaSigma,
                        double[] wsa, double[] wsaAlpha, Matrix[] wsaSigma,
                        double weightedNumberOfSamples,
                        double relEntropy, double goodnessOfFit, double snowFraction, double dataMask, double sza) {
        this.bsa = bsa;
        this.bsaAlpha = bsaAlpha;
        this.wsa = wsa;
        this.wsaAlpha = wsaAlpha;
        this.bsaSigma = bsaSigma;
        this.wsaSigma = wsaSigma;
        this.weightedNumberOfSamples = weightedNumberOfSamples;
        this.relEntropy = relEntropy;
        this.goodnessOfFit = goodnessOfFit;
        this.snowFraction = snowFraction;
        this.dataMask = dataMask;
        this.sza = sza;
    }

    public AlbedoResult(double[] bsa, double[] bsaAlpha, double[] bsaSigma,
                        double[] wsa, double[] wsaAlpha, double[] wsaSigma,
                        double weightedNumberOfSamples,
                        double relEntropy, double goodnessOfFit, double snowFraction, double dataMask, double sza) {
        this.bsa = bsa;
        this.bsaAlpha = bsaAlpha;
        this.wsa = wsa;
        this.wsaAlpha = wsaAlpha;
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

    public void setBsa(double[] bsa) {
        this.bsa = bsa;
    }

    public double[] getBsaAlpha() {
        return bsaAlpha;
    }

    public void setBsaAlpha(double[] bsaAlpha) {
        this.bsaAlpha = bsaAlpha;
    }

    public double[] getWsa() {
        return wsa;
    }

    public void setWsa(double[] wsa) {
        this.wsa = wsa;
    }

    public double[] getWsaAlpha() {
        return wsaAlpha;
    }

    public void setWsaAlpha(double[] wsaAlpha) {
        this.wsaAlpha = wsaAlpha;
    }

    public Matrix[] getBsaSigma() {
        return bsaSigma;
    }

    public void setBsaSigma(Matrix[] bsaSigma) {
        this.bsaSigma = bsaSigma;
    }

    public Matrix[] getWsaSigma() {
        return wsaSigma;
    }

    public void setWsaSigma(Matrix[] wsaSigma) {
        this.wsaSigma = wsaSigma;
    }

    public double getWeightedNumberOfSamples() {
        return weightedNumberOfSamples;
    }

    public void setWeightedNumberOfSamples(double weightedNumberOfSamples) {
        this.weightedNumberOfSamples = weightedNumberOfSamples;
    }

    public double getRelEntropy() {
        return relEntropy;
    }

    public void setRelEntropy(double relEntropy) {
        this.relEntropy = relEntropy;
    }

    public double getGoodnessOfFit() {
        return goodnessOfFit;
    }

    public void setGoodnessOfFit(double goodnessOfFit) {
        this.goodnessOfFit = goodnessOfFit;
    }

    public double getSnowFraction() {
        return snowFraction;
    }

    public void setSnowFraction(double snowFraction) {
        this.snowFraction = snowFraction;
    }

    public double getDataMask() {
        return dataMask;
    }

    public void setDataMask(double dataMask) {
        this.dataMask = dataMask;
    }

    public double getSza() {
        return sza;
    }

    public void setSza(double sza) {
        this.sza = sza;
    }

    public double[] getBsaSigmaArray() {
        return bsaSigmaArray;
    }

    public double[] getWsaSigmaArray() {
        return wsaSigmaArray;
    }
}
