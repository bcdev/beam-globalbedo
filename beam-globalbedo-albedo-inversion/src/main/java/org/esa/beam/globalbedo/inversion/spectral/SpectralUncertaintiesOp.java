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
import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS;

/**
 * Pixel operator implementing the uncertainties computation for the spectral appropach.
 * This operator provides covariance terms for a tupel of 3 input bands. Should be run for two sets of input bands
 * to finally cover MODIS bands 1-6 (skip band 7).
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.inversion.spectral.uncertainties",
        description = "Pixel operator implementing the uncertainties computation for the spectral appropach.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2018 by Brockmann Consult")

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

    //    VIS-NIR: 3,1,2 (3 sigma & 3 alpha) and
//    BGR=3,4,1; and NIR: b2,b5 & b6 (sigma only)
//    (email JPM, 20180122)
    @Parameter(defaultValue = "3,1,2", description = "Band indices (3 spectral bands)")
    private int[] bandIndices;

    private String priorMeanBandNamePrefix = "BRDF_Albedo_Parameters_";


    private double priorScaleFactor = 30.0;

    private String[] uncertaintyBandNames;

    static final Map<Integer, String> spectralWaveBandsMap = new HashMap<>();

    private FullAccumulator[] fullAccumulators;

    private int numSdrBands = 3;     // bands to process

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

        fullAccumulators = new FullAccumulator[numSdrBands];
        for (int i = 0; i < numSdrBands; i++) {
            SpectralFullAccumulation fullAccumulation = new SpectralFullAccumulation(numSdrBands,
                                                                dailyAccRootDir,
                                                                bandIndices[i],
                                                                tile, year, doy,
                                                                wings, computeSnow);
            fullAccumulators[i] = fullAccumulation.getResult();
        }

    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        Matrix uncertainties;

//        if (x == 580 && y == 550) {
//            System.out.println("x = " + x);
//        }

        SpectralAccumulator[] accumulators = new SpectralAccumulator[numSdrBands];
        double[] maskAccs = new double[numSdrBands];
        for (int i = 0; i < numSdrBands; i++) {
            if (fullAccumulators[i] != null) {
                accumulators[i] = SpectralAccumulator.createForInversion(fullAccumulators[i].getSumMatrices(), x, y, 1);
                maskAccs[i] = accumulators[i].getMask();
            }
        }

        double maskPrior;
        SpectralPrior3Bands prior = SpectralPrior3Bands.createForInversion(sourceSamples, priorScaleFactor,
                                                                           computeSnow, bandIndices);
        maskPrior = prior.getMask();

        if (accumulationDataAvailable(accumulators, maskAccs) && maskPrior > 0) {
            final Matrix mAcc = getMAccFor3Bands(accumulators);

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
        fillTargetSamples(targetSamples, uncertainties);
    }

    private Matrix getMAccFor3Bands(SpectralAccumulator[] accumulators) {
        Matrix mAccTotal = new Matrix(numSdrBands*NUM_ALBEDO_PARAMETERS, numSdrBands*NUM_BBDR_WAVE_BANDS);
        for (int i = 0; i < numSdrBands; i++) {
            final Matrix mAcc_i = accumulators[i].getM();
            for (int j = 0; j < mAcc_i.getColumnDimension(); j++) {
                for (int k = 0; k < mAcc_i.getRowDimension(); k++) {
                    final double mAcc_elem = mAcc_i.get(j, k);
                    mAccTotal.set(i*numSdrBands + j, i*numSdrBands + k, mAcc_elem);
                }
            }
        }

        // test:
//        for (int j = 0; j < mAccTotal.getColumnDimension(); j++) {
//            for (int k = 0; k < mAccTotal.getRowDimension(); k++) {
//                if (mAccTotal.get(j, k) == 0.0) {
//                    mAccTotal.set(j, k, Math.abs(10.0*(Math.random() - 0.5)));
//                }
//            }
//        }

        return mAccTotal;
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        configurePriorSourceSamples(sampleConfigurer);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < uncertaintyBandNames.length; i++) {
            configurator.defineSample(i, uncertaintyBandNames[i]);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        for (String uncertaintyBandName : uncertaintyBandNames) {
            productConfigurer.addBand(uncertaintyBandName, ProductData.TYPE_FLOAT32,
                                      AlbedoInversionConstants.NO_DATA_VALUE);
        }

        for (Band b : getTargetProduct().getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }
    }

    private boolean accumulationDataAvailable(SpectralAccumulator[] accumulators, double[] maskAccs) {
        if (accumulators.length != maskAccs.length) {
            return false;
        }
        for (int i = 0; i < accumulators.length; i++) {
            if (accumulators[i] == null || maskAccs[i] == 0.0) {
                return false;
            }
        }
        return true;
    }

    private void fillTargetSamples(WritableSample[] targetSamples,
                                   Matrix uncertainties) {
        // uncertainties
        int index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            // Feb. 2018: new 3-band approach
            for (int j = i; j < 3 * numSdrBands; j++) {
                targetSamples[index++].set(uncertainties.get(i, j));
            }
        }
    }

    private void configurePriorSourceSamples(SampleConfigurer configurator) {
        int offset = 0;
        int totalNumSdrBands = 7;
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
        configurator.defineSample(offset, landWaterBandName, priorProduct);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SpectralUncertaintiesOp.class);
        }
    }
}
