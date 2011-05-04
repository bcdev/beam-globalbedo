package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.PixelOperator;
import org.esa.beam.util.ProductUtils;

import java.util.Vector;

/**
 * // todo: Class will probably not be needed  - remove later!
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class FullAccumulationWriteOp extends PixelOperator {

    private static final int[][] TRG_M =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands]
                    [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int[] TRG_V =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int TRG_E = 90;
    private static final int TRG_MASK = 91;
    private static final int TRG_DOY_CLOSEST_SAMPLE = 92;

    private String[][] mBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
            [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[] vBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands];

    @Parameter(description = "Array with sums of matrix elements")
//    private double[][][] sumMatrices;
    private Vector<double[][][]> sumMatricesVector;

    @Parameter(description = "Array with daysToTheClosestSample")
    private int[][] daysToTheClosestSample;

    private double[][][] sumMatrices;

    @Override
    protected void configureSourceSamples(Configurator configurator) {
        sumMatrices = sumMatricesVector.elementAt(0);
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

        final String eBandName = AlbedoInversionConstants.ACC_E_NAME;
        Band band = targetProduct.addBand(eBandName, ProductData.TYPE_FLOAT32);
        band.setNoDataValue(Float.NaN);
        band.setNoDataValueUsed(true);

        final String maskBandName = AlbedoInversionConstants.ACC_MASK_NAME;
        targetProduct.addBand(maskBandName, ProductData.TYPE_FLOAT32);

        final String dayClosestSampleName = AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME;
        targetProduct.addBand(dayClosestSampleName, ProductData.TYPE_INT16);

        targetProduct.setPreferredTileSize(100, 100);
    }

    @Override
    protected void configureTargetSamples(Configurator configurator) {
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
        configurator.defineSample(TRG_E, AlbedoInversionConstants.ACC_E_NAME);
        configurator.defineSample(TRG_MASK, AlbedoInversionConstants.ACC_MASK_NAME);
        configurator.defineSample(TRG_DOY_CLOSEST_SAMPLE, AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples,
                                WritableSample[] targetSamples) {

        int bandIndex = 0;
//        System.out.println("bandIndex = " + bandIndex);
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                targetSamples[TRG_M[i][j]].set(sumMatrices[bandIndex++][x][y]);
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            targetSamples[TRG_V[i]].set(sumMatrices[bandIndex++][x][y]);
        }
        targetSamples[TRG_E].set(sumMatrices[bandIndex++][x][y]);
        targetSamples[TRG_MASK].set(sumMatrices[bandIndex++][x][y]);
        targetSamples[TRG_DOY_CLOSEST_SAMPLE].set(daysToTheClosestSample[x][y]);
    }
}
