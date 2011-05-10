package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.PointOperator;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;

/**
 * Pixel operator implementing the full accumulation part of python breadboard.
 * The breadboard file is 'AlbedoInversion_multisensor_FullAccum_MultiProcessing.py' provided by Gerardo López Saldaña.
 *
 * // todo: Class will probably not be needed (replaced by FullAccumulationJAIOp) - remove later!
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.fullacc",
                  description = "Provides full accumulation of daily accumulators",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")
public class FullAccumulationOp extends PixelOperator {

    private static final int[][] SRC_M =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands]
                    [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int[] SRC_V =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int SRC_E = 90;
    private static final int SRC_MASK = 91;


    private static final int[][] TRG_M =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands]
                    [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int[] TRG_V =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int TRG_E = 90;
    private static final int TRG_MASK = 91;
    private static final int TRG_DOY_CLOSEST_SAMPLE = 92;

    private static final int sourceSampleOffset = 100;  // this value must be >= number of bands in a source product

    private String[][] mBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
            [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[] vBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands];

    @SourceProducts(description = "Accumulator matrix source product")
    private Product[] sourceProducts;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(description = "All DoYs for full accumulation")
    private int[] allDoys;

    @Parameter(description = "All weights vector for full accumulation")
    private double weight;


    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {
        for (int k = 0; k < sourceProducts.length; k++) {
            Product sourceProduct = sourceProducts[k];
            for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
                for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                    SRC_M[i][j] = 3 * AlbedoInversionConstants.numBBDRWaveBands * i + j;
                    configurator.defineSample((sourceSampleOffset * k) + SRC_M[i][j], mBandNames[i][j], sourceProduct);
                }
            }
            for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
                SRC_V[i] = 3 * 3 * AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numBBDRWaveBands + i;
                configurator.defineSample((sourceSampleOffset * k) + SRC_V[i], vBandNames[i], sourceProduct);
            }
            configurator.defineSample((sourceSampleOffset * k) + SRC_E, "E", sourceProduct);
            configurator.defineSample((sourceSampleOffset * k) + SRC_MASK, "mask", sourceProduct);
        }
    }

    @Override
    protected void configureTargetProduct(Product targetProduct) {
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                mBandNames[i][j] = "M_" + i + "" + j;
                Band band = targetProduct.addBand(mBandNames[i][j], ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }
        }

        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            vBandNames[i] = "V_" + i;
            Band band = targetProduct.addBand(vBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        // todo: define constants for names
        String eBandName = "E";
        Band band = targetProduct.addBand(eBandName, ProductData.TYPE_FLOAT32);
        band.setNoDataValue(Float.NaN);
        band.setNoDataValueUsed(true);

        targetProduct.addBand("mask", ProductData.TYPE_INT8);
        targetProduct.addBand("doy_closest_sample", ProductData.TYPE_INT16);
        targetProduct.setPreferredTileSize(100, 100);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) {
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                TRG_M[i][j] = 3 * AlbedoInversionConstants.numBBDRWaveBands * i + j;
                configurator.defineSample(TRG_M[i][j], mBandNames[i][j]);
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            TRG_V[i] = 3 * 3 * AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numBBDRWaveBands + i;
            configurator.defineSample(TRG_V[i], vBandNames[i]);
        }
        configurator.defineSample(TRG_E, "E");
        configurator.defineSample(TRG_MASK, "mask");
        configurator.defineSample(TRG_DOY_CLOSEST_SAMPLE, "doy_closest_sample");
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples,
                                WritableSample[] targetSamples) {

        Matrix M = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                              3 * AlbedoInversionConstants.numBBDRWaveBands);
        Matrix V = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands, 1);
        Matrix E = new Matrix(1, 1);
        int mask = 0;
        int doyClosestSample = 0;

        for (int k = 0; k < sourceProducts.length; k++) {  // BB: for BBDR in AllFiles
            for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
                for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                    final double allm_ij = M.get(i, j);
                    final double m_ij = weight * sourceSamples[k * sourceSampleOffset + SRC_M[i][j]].getDouble();
                    M.set(i, j, allm_ij + m_ij);
                }
                final double allv_i = V.get(i, 0);
                final double v_i = weight * sourceSamples[k * sourceSampleOffset + SRC_V[i]].getDouble();
                V.set(i, 0, allv_i + v_i);
            }
            final double alle_00 = E.get(0, 0) + sourceSamples[k * sourceSampleOffset + SRC_E].getDouble();
            final double e_00 = weight * sourceSamples[k * sourceSampleOffset + SRC_E].getDouble();
            E.set(0, 0, alle_00 + e_00);

            mask += weight * sourceSamples[k * sourceSampleOffset + SRC_MASK].getInt();

            final int bbdrDaysToDoY = Math.abs(allDoys[k]) + 1;
            if (k == 0) {
                if (mask > 0) {
                    doyClosestSample = bbdrDaysToDoY;
                }
            } else {
                if (mask > 0 && doyClosestSample == 0) {
                    doyClosestSample = bbdrDaysToDoY;
                }
                if (mask > 0 && doyClosestSample > 0) {
                    doyClosestSample = Math.min(bbdrDaysToDoY, doyClosestSample);
                }
            }
        }

        // fill target samples...
//        Accumulator accumulator = new Accumulator(M, V, E, mask, doyClosestSample);
        Accumulator accumulator = new Accumulator(M, V, E, mask);
        fillTargetSamples(targetSamples, accumulator);
    }

    private void fillTargetSamples(WritableSample[] targetSamples, Accumulator accumulator) {
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                targetSamples[TRG_M[i][j]].set(accumulator.getM().get(i, j));
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            targetSamples[TRG_V[i]].set(accumulator.getV().get(i, 0));
        }
        targetSamples[TRG_E].set(accumulator.getE().get(0, 0));
        targetSamples[TRG_MASK].set(accumulator.getMask());
//        targetSamples[TRG_DOY_CLOSEST_SAMPLE].set(accumulator.getDoyClosestSample());

    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FullAccumulationOp.class);
        }
    }

}
