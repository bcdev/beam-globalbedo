package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.experimental.PixelOperator;

/**
 * Pixel operator implementing the daily accumulation part of python breadboard.
 * The breadboard file is 'AlbedoInversionDailyAccumulator.py' provided by Gerardo López Saldaña.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.optest",
                  description = "Computes optimal estimation matrices for a single BBDR observation",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")
public class OptimalEstimationOp extends PixelOperator {

    private static final int SRC_BB_VIS = 0;
    private static final int SRC_BB_NIR = 1;
    private static final int SRC_BB_SW = 2;
    private static final int SRC_SIG_BB_VIS_VIS = 3;
    private static final int SRC_SIG_BB_VIS_NIR = 4;
    private static final int SRC_SIG_BB_VIS_SW = 5;
    private static final int SRC_SIG_BB_NIR_NIR = 6;
    private static final int SRC_SIG_BB_NIR_SW = 7;
    private static final int SRC_SIG_BB_SW_SW = 8;
    private static final int SRC_KVOL_BRDF_VIS = 9;
    private static final int SRC_KVOL_BRDF_NIR = 10;
    private static final int SRC_KVOL_BRDF_SW = 11;
    private static final int SRC_KGEO_BRDF_VIS = 12;
    private static final int SRC_KGEO_BRDF_NIR = 13;
    private static final int SRC_KGEO_BRDF_SW = 14;
    private static final int SRC_AOD550 = 15;
    private static final int SRC_NDVI = 16;
    private static final int SRC_SIG_NDVI = 17;
    private static final int SRC_VZA = 18;
    private static final int SRC_SZA = 19;
    private static final int SRC_RAA = 20;
    private static final int SRC_DEM = 21;
    private static final int SRC_SNOW_MASK = 22;
    private static final int SRC_LAND_MASK = 23;

    private static final int[][] TRG_M =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands]
                    [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int[] TRG_V =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int TRG_E = 90;
    private static final int TRG_MASK = 91;

    private static final int sourceSampleOffset = 30;  // this value must be >= number of bands in a source product

    private String[][] mBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
            [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[] vBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands];

    @SourceProducts(description = "BBDR source product")
    private Product[] sourceProducts;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;


    @Override
    protected void configureSourceSamples(Configurator configurator) {
        for (int i = 0; i < sourceProducts.length; i++) {
            Product sourceProduct = sourceProducts[i];

            configurator.defineSample((sourceSampleOffset * i) + SRC_BB_VIS, AlbedoInversionConstants.BBDR_BB_VIS_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_BB_NIR, AlbedoInversionConstants.BBDR_BB_NIR_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_BB_SW, AlbedoInversionConstants.BBDR_BB_SW_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_VIS_VIS,
                                      AlbedoInversionConstants.BBDR_SIG_BB_VIS_VIS_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_VIS_NIR,
                                      AlbedoInversionConstants.BBDR_SIG_BB_VIS_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_VIS_SW,
                                      AlbedoInversionConstants.BBDR_SIG_BB_VIS_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_NIR_NIR,
                                      AlbedoInversionConstants.BBDR_SIG_BB_NIR_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_NIR_SW,
                                      AlbedoInversionConstants.BBDR_SIG_BB_NIR_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_SW_SW,
                                      AlbedoInversionConstants.BBDR_SIG_BB_SW_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KVOL_BRDF_VIS,
                                      AlbedoInversionConstants.BBDR_KVOL_BRDF_VIS_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KVOL_BRDF_NIR,
                                      AlbedoInversionConstants.BBDR_KVOL_BRDF_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KVOL_BRDF_SW,
                                      AlbedoInversionConstants.BBDR_KVOL_BRDF_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KGEO_BRDF_VIS,
                                      AlbedoInversionConstants.BBDR_KGEO_BRDF_VIS_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KGEO_BRDF_NIR,
                                      AlbedoInversionConstants.BBDR_KGEO_BRDF_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KGEO_BRDF_SW,
                                      AlbedoInversionConstants.BBDR_KGEO_BRDF_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_AOD550, AlbedoInversionConstants.BBDR_AOD550_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_NDVI, AlbedoInversionConstants.BBDR_NDVI_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_NDVI,
                                      AlbedoInversionConstants.BBDR_SIG_NDVI_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_VZA, AlbedoInversionConstants.BBDR_VZA_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SZA, AlbedoInversionConstants.BBDR_SZA_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_RAA, AlbedoInversionConstants.BBDR_RAA_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_DEM, AlbedoInversionConstants.BBDR_DEM_NAME,
                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SNOW_MASK,
                                      AlbedoInversionConstants.BBDR_SNOW_MASK_NAME, sourceProduct);

            // get land mask (sensor dependent)
            AlbedoInversionUtils.setLandMaskSourceSample(configurator, (sourceSampleOffset * i) + SRC_LAND_MASK,
                                                         sourceProduct);
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

        String eBandName = "E";
        Band band = targetProduct.addBand(eBandName, ProductData.TYPE_FLOAT32);
        band.setNoDataValue(Float.NaN);
        band.setNoDataValueUsed(true);

        targetProduct.addBand("mask", ProductData.TYPE_INT8);
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
        configurator.defineSample(TRG_E, "E");
        configurator.defineSample(TRG_MASK, "mask");
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples,
                                WritableSample[] targetSamples) {

        Matrix M = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                              3 * AlbedoInversionConstants.numBBDRWaveBands);
        Matrix V = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                              AlbedoInversionConstants.numBBDRWaveBands);
        Matrix E = new Matrix(1, 1);
        int mask = 0;

        if (x == 1095 && y == 806) {
            System.out.println("computePixel: x,y = " + x + "," + y);
        }

        // accumulate the matrices from the single products...
        for (int i = 0; i < sourceProducts.length; i++) {
            OptimalEstimationMatrixContainer container = getMatricesPerBBDRDataset(sourceSamples, i);
            M.plusEquals(container.getM());
            V.plusEquals(container.getV());
            E.plusEquals(container.getE());
            mask += container.getMask();
        }

        // fill target samples...
        fillTargetSamples(targetSamples, M, V, E, mask);
    }

    private OptimalEstimationMatrixContainer getMatricesPerBBDRDataset(Sample[] sourceSamples, int sourceProductIndex) {
        OptimalEstimationMatrixContainer container = new OptimalEstimationMatrixContainer();

        // do not consider non-land pixels, non-snowfilter pixels, pixels with BBDR == 0.0 or -9999.0, SD == 0.0
        if (isLandFilter(sourceSamples, sourceProductIndex) || isSnowFilter(sourceSamples, sourceProductIndex) ||
            isBBDRFilter(sourceSamples, sourceProductIndex) || isSDFilter(sourceSamples, sourceProductIndex)) {
            Matrix zeroM = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                                      3 * AlbedoInversionConstants.numBBDRWaveBands);
            Matrix zeroV = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                                      AlbedoInversionConstants.numBBDRWaveBands);
            Matrix zeroE = new Matrix(1, 1);
            container.setM(zeroM);
            container.setV(zeroV);
            container.setE(zeroE);
            container.setMask(0);

            return container;
        }

        // get kernels...
        Matrix kernels = initKernels(sourceSamples);

        // compute C matrix...
        double[] correlation = new double[AlbedoInversionConstants.numBBDRWaveBands];
        correlation[0] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_VIS_NIR].getDouble();
        correlation[1] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_VIS_SW].getDouble();
        correlation[2] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_NIR_SW].getDouble();

        double[] SD = new double[3];
        SD[0] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_VIS_VIS].getDouble();
        SD[1] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_NIR_NIR].getDouble();
        SD[2] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_SW_SW].getDouble();

        Matrix C = new Matrix(AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numBBDRWaveBands, 1);
        Matrix thisC = new Matrix(AlbedoInversionConstants.numBBDRWaveBands, AlbedoInversionConstants.numBBDRWaveBands);

        Matrix bbdr = new Matrix(AlbedoInversionConstants.numBBDRWaveBands, 1);
        bbdr.set(0, 0, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_VIS].getDouble());
        bbdr.set(1, 0, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_NIR].getDouble());
        bbdr.set(2, 0, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_SW].getDouble());

        int count = 0;
        int cCount = 0;
        for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
            for (int k = j + 1; k < AlbedoInversionConstants.numBBDRWaveBands; k++) {
                if (k == j + 1) {
                    cCount++;
                }
                C.set(cCount, 0, correlation[count] * SD[j] * SD[k]);
                count++;
                cCount++;
            }
        }

        cCount = 0;
        for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
            C.set(cCount, 0, SD[j] * SD[j]);
            cCount = cCount + AlbedoInversionConstants.numBBDRWaveBands - j;
        }

        count = 0;
        for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
            for (int k = j; k < AlbedoInversionConstants.numBBDRWaveBands; k++) {
                thisC.set(j, k, C.get(count, 0));
                thisC.set(k, j, thisC.get(j, k));
                count++;
            }
        }

        // compute M, V, E matrices...
        final Matrix inverseC = thisC.inverse();
        final Matrix M = (kernels.transpose().times(inverseC)).times(kernels);
        final Matrix inverseCDiagFlat = AlbedoInversionUtils.getRectangularDiagonalFlatMatrix(inverseC);
        final Matrix V = (kernels.transpose().times(inverseCDiagFlat)).times(bbdr.transpose());
        final Matrix E = (bbdr.transpose().times(inverseC)).times(bbdr);

        // return result
        container.setM(M);
        container.setV(V);
        container.setE(E);
        container.setMask(1);

        return container;
    }

    private void fillTargetSamples(WritableSample[] targetSamples, Matrix m, Matrix v, Matrix e, int mask) {
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                targetSamples[TRG_M[i][j]].set(m.get(i, j));
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            targetSamples[TRG_V[i]].set(v.get(i, 0));
        }
        targetSamples[TRG_E].set(e.get(0, 0));
        targetSamples[TRG_MASK].set(mask);
    }

    private Matrix initKernels(Sample[] sourceSamples) {
        Matrix kernels = new Matrix(AlbedoInversionConstants.numBBDRWaveBands,
                                    3 * AlbedoInversionConstants.numBBDRWaveBands);

        kernels.set(0, 0, 1.0);
        kernels.set(1, 3, 1.0);
        kernels.set(2, 6, 1.0);
        kernels.set(0, 1, sourceSamples[SRC_KVOL_BRDF_VIS].getDouble());
        kernels.set(1, 4, sourceSamples[SRC_KVOL_BRDF_NIR].getDouble());
        kernels.set(2, 7, sourceSamples[SRC_KVOL_BRDF_SW].getDouble());
        kernels.set(0, 2, sourceSamples[SRC_KGEO_BRDF_VIS].getDouble());
        kernels.set(1, 5, sourceSamples[SRC_KGEO_BRDF_NIR].getDouble());
        kernels.set(2, 8, sourceSamples[SRC_KGEO_BRDF_SW].getDouble());

        return kernels;
    }

    private boolean isLandFilter(Sample[] sourceSamples, int sourceProductIndex) {
        if (sourceProducts[sourceProductIndex].getProductType().startsWith("MER")) {
            if (!sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_LAND_MASK].getBoolean()) {
                return true;
            }
        } else if (sourceProducts[sourceProductIndex].getProductType().startsWith("VGT")) {
            if ((sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_LAND_MASK].getInt() & 8) == 0) {
                return true;
            }
        } else {
            // todo: AATSR
        }
        return false;
    }

    private boolean isSnowFilter(Sample[] sourceSamples, int sourceProductIndex) {
        return ((computeSnow && !(sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SNOW_MASK].getInt() == 1)) ||
                (!computeSnow && (sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SNOW_MASK].getInt() == 1)));
    }


    private boolean isBBDRFilter(Sample[] sourceSamples, int sourceProductIndex) {
        return (sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_VIS].getDouble() == 0.0 ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_VIS].getDouble() == 9999.0 ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_NIR].getDouble() == 0.0 ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_NIR].getDouble() == 9999.0 ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_SW].getDouble() == 0.0 ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_SW].getDouble() == 9999.0);
    }

    private boolean isSDFilter(Sample[] sourceSamples, int sourceProductIndex) {
        return (sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_VIS_VIS].getDouble() == 0.0 &&
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_NIR_NIR].getDouble() == 0.0 &&
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_SW_SW].getDouble() == 0.0);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OptimalEstimationOp.class);
        }
    }

}
