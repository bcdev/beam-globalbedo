package org.esa.beam.globalbedo.inversion.spectral;

import Jama.LUDecomposition;
import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.FullAccumulator;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

import java.util.HashMap;
import java.util.Map;

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS;

/**
 * Pixel operator implementing the inversion part extended from broadband to spectral appropach (MODIS bands).
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.inversion.spectral.uncertainties",
        description = "Implements the inversion part extended from broadband to spectral appropach (MODIS bands).",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")

public class SpectralUncertaintiesOp extends PixelOperator {

    @SourceProduct(description = "Source product", optional = true)
    private Product sourceProduct;

    @SourceProduct(description = "Prior product")
    private Product priorProduct;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "Tile")
    private String tile;

    @Parameter(description = "Day of year")
    private int doy;

    @Parameter(defaultValue = "180", description = "Wings")  // means 3 months wings on each side of the year
    private int wings;

    @Parameter(defaultValue = "", description = "Daily acc binary files root directory")
    private String dailyAccRootDir;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "4,3,2", description = "Band indices (3 spectral bands)")
    private int[] bandIndices;

    private String priorMeanBandNamePrefix = "BRDF_Albedo_Parameters_";
    private String priorSdBandNamePrefix = "BRDF_Albedo_Parameters_";


    private double priorScaleFactor = 30.0;

    private String[] uncertaintyBandNames;

    static final Map<Integer, String> spectralWaveBandsMap = new HashMap<>();

    private FullAccumulator fullAccumulator;

    private int numSdrBands = 3;     // bands to process
    private int totalNumSdrBands = 7;     // all MODIS bands

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        if (bandIndices.length != 3) {
            throw new OperatorException("3 band indices must be specified!");
        }
        for (int i = 0; i < bandIndices.length; i++) {
            if (bandIndices[i] > 7 || bandIndices[i] < 1) {
                throw new OperatorException("band index '" + bandIndices[i] + "' out of range!");
            }
            spectralWaveBandsMap.put(i, "b" + (bandIndices[i]));
        }
        uncertaintyBandNames = SpectralIOUtils.getSpectralInversionUncertainty3BandNames(bandIndices, spectralWaveBandsMap);

        SpectralFullAccumulation fullAccumulation = new SpectralFullAccumulation(numSdrBands,
                                                                                 dailyAccRootDir,
                                                                                 bandIndices[0],     // todo: for first test we accumulate first band only but take 3 from Prior...
                                                                                 tile, year, doy,
                                                                                 wings, computeSnow);
        fullAccumulator = fullAccumulation.getResult();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        Matrix uncertainties;

        if (x == 200 && y == 200) {
            System.out.println("x = " + x);
        }

        double maskAcc = 0.0;
        SpectralAccumulator accumulator = null;
        if (fullAccumulator != null) {
            accumulator = SpectralAccumulator.createForInversion(fullAccumulator.getSumMatrices(), x, y, numSdrBands);
            maskAcc = accumulator.getMask();
        }

        double maskPrior;
        SpectralPrior3Bands prior = SpectralPrior3Bands.createForInversion(sourceSamples, priorScaleFactor,
                                                                           computeSnow, bandIndices);
        maskPrior = prior.getMask();

        if (accumulator != null && maskAcc > 0 && maskPrior > 0) {
            final Matrix mAcc = accumulator.getM();

            for (int i = 0; i < 3 * numSdrBands; i++) {
                double m_ii_accum = mAcc.get(i, i);
                if (prior.getM() != null) {
                    m_ii_accum += prior.getM().get(i, i);
                }
                mAcc.set(i, i, m_ii_accum);
            }

            final LUDecomposition lud = new LUDecomposition(mAcc);
            if (lud.isNonsingular()) {
                Matrix tmpM = mAcc.inverse();   // 3x9
                if (AlbedoInversionUtils.matrixHasNanElements(tmpM) ||
                        AlbedoInversionUtils.matrixHasZerosInDiagonale(tmpM)) {
                    tmpM = new Matrix(3 * numSdrBands, 3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS,
                                      AlbedoInversionConstants.NO_DATA_VALUE);
                }
                uncertainties = tmpM;
            } else {
                uncertainties = new Matrix(3 * numSdrBands,
                                           3 * numSdrBands,
                                           AlbedoInversionConstants.NO_DATA_VALUE);     // 3x3
                maskAcc = 0.0;
            }
        } else {
            if (maskPrior > 0.0) {
                final LUDecomposition lud = new LUDecomposition(prior.getM());
                if (lud.isNonsingular()) {
                    uncertainties = prior.getM().inverse();
                } else {
                    uncertainties = new Matrix(
                            3 * numSdrBands,
                            3 * numSdrBands, AlbedoInversionConstants.NO_DATA_VALUE);
                }
            } else {
                uncertainties = new Matrix(3 * numSdrBands, 3 * numSdrBands,
                                           AlbedoInversionConstants.NO_DATA_VALUE);
            }
        }

        // we have the final result - fill target samples...
        fillTargetSamples(targetSamples, uncertainties, maskAcc);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        configurePriorSourceSamples(sampleConfigurer);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < uncertaintyBandNames.length; i++) {
            configurator.defineSample(i, uncertaintyBandNames[i]);
            // Nov. 2016: only provide the SD (diagonal terms), not the full matrix!
            // configurator.defineSample(numTargetParameters + index, uncertaintyBandNames[i][i]);
            // index++;
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        for (int i = 0; i < uncertaintyBandNames.length; i++) {
            productConfigurer.addBand(uncertaintyBandNames[i], ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
            // Nov. 2016: only provide the SD (diagonal terms), not the full matrix!
            // productConfigurer.addBand(uncertaintyBandNames[i][i], ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
        }

        for (Band b : getTargetProduct().getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }
    }

    private void fillTargetSamples(WritableSample[] targetSamples,
                                   Matrix uncertainties,
                                   double weightedNumberOfSamples) {
        // uncertainties
        int index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            // Feb. 2018: new 3-band approach
            for (int j = i; j < 3 * numSdrBands; j++) {
                targetSamples[index++].set(uncertainties.get(i, j));
            }
            // Nov. 2016: only provide the SD (diagonal terms), not the full matrix!
            // targetSamples[3 * numSdrBands + index].set(uncertainties.get(i, i));
            // index++;
        }
    }

    private void configurePriorSourceSamples(SampleConfigurer configurator) {
        int offset = 0;
        for (int i = 0; i < totalNumSdrBands; i++) {
            for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                final String meanBandName = priorMeanBandNamePrefix + "Band" + (i + 1) + "_f" + j + "_avr";
                configurator.defineSample(offset++, meanBandName, priorProduct);

                final String sdBandName = priorMeanBandNamePrefix + "Band" + (i + 1) + "_f" + j + "_sd";
                ;
                configurator.defineSample(offset++, sdBandName, priorProduct);
            }
        }
        String snowFractionBandName = "snowFraction";
        configurator.defineSample(offset++, snowFractionBandName, priorProduct);
        String landWaterBandName = "landWaterType";
        configurator.defineSample(offset++, landWaterBandName, priorProduct);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SpectralUncertaintiesOp.class);
        }
    }
}
