package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.experimental.PixelOperator;
import org.esa.beam.util.ProductUtils;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
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
    private static final int SRC_AOD550 = 15; // ??? todo check
    private static final int SRC_NDVI = 16;
    private static final int SRC_SIG_NDVI = 17;
    private static final int SRC_VZA = 18;
    private static final int SRC_SZA = 19;
    private static final int SRC_RAA = 20;
    private static final int SRC_DEM = 21;
    private static final int SRC_SNOW_MASK = 22;
    private static final int SRC_LAND_MASK = 23;
    // todo: provide VGT SM as flag band
    // todo: provide AATSR flags

    private static final int TRG_M_00 = 0;
    private static final int TRG_M_01 = 1;
    private static final int TRG_M_02 = 2;
    private static final int TRG_M_10 = 3;
    private static final int TRG_M_11 = 4;
    private static final int TRG_M_12 = 5;
    private static final int TRG_M_20 = 6;
    private static final int TRG_M_21 = 7;
    private static final int TRG_M_22 = 8;
    private static final int TRG_V_0 = 9;
    private static final int TRG_V_1 = 10;
    private static final int TRG_V_2 = 11;
    private static final int TRG_E = 12;
    private static final int TRG_MASK = 13;

    @Parameter(description = "BBDR source product")
    private Product sourceProduct;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Override
    protected void configureSourceSamples(Configurator configurator) {
        configurator.defineSample(SRC_BB_VIS, AlbedoInversionConstants.BBDR_BB_VIS_NAME);
        configurator.defineSample(SRC_BB_NIR, AlbedoInversionConstants.BBDR_BB_NIR_NAME);
        configurator.defineSample(SRC_BB_SW, AlbedoInversionConstants.BBDR_BB_SW_NAME);
        configurator.defineSample(SRC_SIG_BB_VIS_VIS, AlbedoInversionConstants.BBDR_SIG_BB_VIS_VIS_NAME);
        configurator.defineSample(SRC_SIG_BB_VIS_NIR, AlbedoInversionConstants.BBDR_SIG_BB_VIS_NIR_NAME);
        configurator.defineSample(SRC_SIG_BB_VIS_SW, AlbedoInversionConstants.BBDR_SIG_BB_VIS_SW_NAME);
        configurator.defineSample(SRC_SIG_BB_NIR_NIR, AlbedoInversionConstants.BBDR_SIG_BB_NIR_NIR_NAME);
        configurator.defineSample(SRC_SIG_BB_NIR_SW, AlbedoInversionConstants.BBDR_SIG_BB_NIR_SW_NAME);
        configurator.defineSample(SRC_SIG_BB_SW_SW, AlbedoInversionConstants.BBDR_SIG_BB_SW_SW_NAME);
        configurator.defineSample(SRC_KVOL_BRDF_VIS, AlbedoInversionConstants.BBDR_KVOL_BRDF_VIS_NAME);
        configurator.defineSample(SRC_KVOL_BRDF_NIR, AlbedoInversionConstants.BBDR_KVOL_BRDF_NIR_NAME);
        configurator.defineSample(SRC_KVOL_BRDF_SW, AlbedoInversionConstants.BBDR_KVOL_BRDF_SW_NAME);
        configurator.defineSample(SRC_KGEO_BRDF_VIS, AlbedoInversionConstants.BBDR_KGEO_BRDF_VIS_NAME);
        configurator.defineSample(SRC_KGEO_BRDF_NIR, AlbedoInversionConstants.BBDR_KGEO_BRDF_NIR_NAME);
        configurator.defineSample(SRC_KGEO_BRDF_SW, AlbedoInversionConstants.BBDR_KGEO_BRDF_SW_NAME);
        configurator.defineSample(SRC_AOD550, AlbedoInversionConstants.BBDR_AOD550_NAME);
        configurator.defineSample(SRC_NDVI, AlbedoInversionConstants.BBDR_NDVI_NAME);
        configurator.defineSample(SRC_SIG_NDVI, AlbedoInversionConstants.BBDR_SIG_NDVI_NAME);
        configurator.defineSample(SRC_VZA, AlbedoInversionConstants.BBDR_VZA_NAME);
        configurator.defineSample(SRC_SZA, AlbedoInversionConstants.BBDR_SZA_NAME);
        configurator.defineSample(SRC_RAA, AlbedoInversionConstants.BBDR_RAA_NAME);
        configurator.defineSample(SRC_DEM, AlbedoInversionConstants.BBDR_DEM_NAME);
        configurator.defineSample(SRC_SNOW_MASK, AlbedoInversionConstants.BBDR_SNOW_MASK_NAME);

        // get land mask from first source product // todo: discuss
        AlbedoInversionUtils.setLandMaskSourceSample(configurator, SRC_LAND_MASK, sourceProduct);
    }

    @Override
    protected void configureTargetProduct(Product targetProduct) {
        String[] bandNames = {
                "M_00", "M_01", "M_02",
                "M_10", "M_11", "M_12",
                "M_20", "M_21", "M_22",
                "V_0", "V_1", "V_2",
                "E", "mask"
        };
        for (String bandName : bandNames) {
            Band band = targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }
        targetProduct.addBand("mask", ProductData.TYPE_INT8);

        // copy flag coding and flag images
        ProductUtils.copyFlagBands(sourceProduct, targetProduct);
        final Band[] bands = sourceProduct.getBands();
        for (Band band : bands) {
            if (band.getFlagCoding() != null) {
                targetProduct.getBand(band.getName()).setSourceImage(band.getSourceImage());
            }
        }
    }

    @Override
    protected void configureTargetSamples(Configurator configurator) {
        configurator.defineSample(TRG_M_00, "M_00");
        configurator.defineSample(TRG_M_01, "M_01");
        configurator.defineSample(TRG_M_02, "M_02");
        configurator.defineSample(TRG_M_10, "M_10");
        configurator.defineSample(TRG_M_11, "M_11");
        configurator.defineSample(TRG_M_12, "M_12");
        configurator.defineSample(TRG_M_20, "M_20");
        configurator.defineSample(TRG_M_21, "M_21");
        configurator.defineSample(TRG_M_22, "M_22");
        configurator.defineSample(TRG_V_0, "V_0");
        configurator.defineSample(TRG_V_1, "V_1");
        configurator.defineSample(TRG_V_2, "V_2");
        configurator.defineSample(TRG_E, "E");
        configurator.defineSample(TRG_MASK, "mask");
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples,
                                WritableSample[] targetSamples) {

        // do not consider non-land and non-snowfilter pixels
        // todo: simplify this if we get mask from flag bands for all sensors
        if (sourceProduct.getProductType().startsWith("MER")) {
            if (!sourceSamples[SRC_LAND_MASK].getBoolean() || isSnowFilter(sourceSamples)) {
                // only compute over land
                fillTargetSampleWithNoDataValue(targetSamples);
                return;
            }
        } else if (sourceProduct.getProductType().startsWith("VGT")) {
            if ((sourceSamples[SRC_LAND_MASK].getInt() & 8) == 0 || isSnowFilter(sourceSamples)) {
                fillTargetSampleWithNoDataValue(targetSamples);
                return;
            }
        } else {
            // todo: AATSR
        }

        // get kernels...
        Matrix kernels = initKernels(sourceSamples);

        // compute C matrix...
        double[] correlation = new double[AlbedoInversionConstants.numBBDRWaveBands];
        correlation[0] = sourceSamples[SRC_SIG_BB_VIS_NIR].getDouble();
        correlation[1] = sourceSamples[SRC_SIG_BB_VIS_SW].getDouble();
        correlation[2] = sourceSamples[SRC_SIG_BB_NIR_SW].getDouble();

        double[] SD = new double[3];
        SD[0] = sourceSamples[SRC_SIG_BB_VIS_VIS].getDouble();
        SD[1] = sourceSamples[SRC_SIG_BB_NIR_NIR].getDouble();
        SD[2] = sourceSamples[SRC_SIG_BB_SW_SW].getDouble();

        Matrix C = new Matrix(AlbedoInversionConstants.numBBDRWaveBands*AlbedoInversionConstants.numBBDRWaveBands, 1);
        Matrix inverseC = new Matrix(AlbedoInversionConstants.numBBDRWaveBands*AlbedoInversionConstants.numBBDRWaveBands, 1);
        Matrix thisC = new Matrix(AlbedoInversionConstants.numBBDRWaveBands*AlbedoInversionConstants.numBBDRWaveBands, 1);

        Matrix bbdr = new Matrix(AlbedoInversionConstants.numBBDRWaveBands, 1);
        bbdr.set(0, 0, sourceSamples[SRC_BB_VIS].getDouble());
        bbdr.set(1, 0, sourceSamples[SRC_BB_NIR].getDouble());
        bbdr.set(2, 0, sourceSamples[SRC_BB_SW].getDouble());

        int count = 0;
        int cCount = 0;
        for (int j=0; j<AlbedoInversionConstants.numBBDRWaveBands; j++) {
            for (int k=0; k<AlbedoInversionConstants.numBBDRWaveBands; k++) {
                if (k == j+1) {
                    cCount++;
                }
                C.set(cCount, 0, correlation[count] * SD[j] * SD[k]);
                count++;
                cCount++;
            }
        }

        cCount = 0;
        for (int j=0; j<AlbedoInversionConstants.numBBDRWaveBands; j++) {
           C.set(cCount, 0, SD[j] * SD[j]);
           cCount = cCount + AlbedoInversionConstants.numBBDRWaveBands - j;
        }

        count = 0;
         for (int j=0; j<AlbedoInversionConstants.numBBDRWaveBands; j++) {
            for (int k=0; k<AlbedoInversionConstants.numBBDRWaveBands; k++) {
                thisC.set(j, k, C.get(j, k));
                thisC.set(k, j, thisC.get(j, k));
            }
         }

        // compute M, V, E matrices...
        Matrix M = new Matrix(3*AlbedoInversionConstants.numBBDRWaveBands, 3*AlbedoInversionConstants.numBBDRWaveBands);
        Matrix V = new Matrix(3*AlbedoInversionConstants.numBBDRWaveBands, 1);
        Matrix E = new Matrix(1, 1);
//      for columns in range(0,xsize):
//         for rows in range(0,ysize):
//              if Mask[columns,rows] <> 0:
//                  Cinv[:,:,columns,rows] = numpy.matrix(thisC[:,:,columns,rows]).I
//
//                  M[:,:,columns,rows] = numpy.matrix(kernels[:,:,columns,rows]).T *
//                                        numpy.matrix(Cinv[:,:,columns,rows]) *
//                                        numpy.matrix(kernels[:,:,columns,rows])
//                  # Multiply only using lead diagonal of Cinv, additionally transpose the result to
//                    store V as a 1 x 9 vector
//                  V[:,columns,rows] = (numpy.matrix(kernels[:,:,columns,rows]).T *
//                                       numpy.diagflat(numpy.diagonal(Cinv[:,:,columns,rows])) *
//                                       BBDR[:,:,columns,rows]).T
//                  E[columns,rows] = numpy.matrix(BBDR[:,:,columns,rows]).T *
//                                    numpy.matrix(Cinv[:,:,columns,rows]) * BBDR[:,:,columns,rows]



        // fill target bands...

    }

    private Matrix initKernels(Sample[] sourceSamples) {
        Matrix kernels = new Matrix(3,AlbedoInversionConstants.numBBDRWaveBands, AlbedoInversionConstants.numBBDRWaveBands);

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

    private void fillTargetSampleWithNoDataValue(WritableSample[] targetSamples) {
        for (WritableSample targetSample : targetSamples) {
            targetSample.set(Float.NaN);
        }
    }

    private boolean isSnowFilter(Sample[] sourceSamples) {
        return (( computeSnow && !(sourceSamples[SRC_LAND_MASK].getInt() == 1)) ||
                (!computeSnow &&  (sourceSamples[SRC_LAND_MASK].getInt() == 1)));
    }


}
