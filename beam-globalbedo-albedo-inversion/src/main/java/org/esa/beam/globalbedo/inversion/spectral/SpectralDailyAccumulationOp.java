package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

import java.io.File;

/**
 * Pixel operator implementing the daily accumulation for spectral inversion.
 *
 * @author Olaf Danne
 */
@SuppressWarnings({"MismatchedReadAndWriteOfArray", "StatementWithEmptyBody"})
@OperatorMetadata(alias = "ga.inversion.dailyaccbinary.spectral",
        description = "Provides spectral daily accumulation of single SDR observations",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class SpectralDailyAccumulationOp extends PixelOperator {


    @SourceProducts(description = "SDR source products")
    private Product[] sourceProducts;


    @Parameter(defaultValue = "7", description = "Number of spectral bands (7 for standard MODIS spectral mapping")
    private int numSpectralBands;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Debug - run additional parts of code if needed.")
    private boolean debug;

    @Parameter(description = "Daily accumulator binary file")
    private File dailyAccumulatorBinaryFile;

    private static int[] SRC_SDR;
    private static int[] SRC_SIGMA_SDR;

//    private static final int SRC_KVOL_BRDF_VIS = 9;
//    private static final int SRC_KVOL_BRDF_NIR = 10;
//    private static final int SRC_KVOL_BRDF_SW = 11;
//    private static final int SRC_KGEO_BRDF_VIS = 12;
//    private static final int SRC_KGEO_BRDF_NIR = 13;
//    private static final int SRC_KGEO_BRDF_SW = 14;
//    private static final int SRC_AOD550 = 15;
//    private static final int SRC_NDVI = 16;
//    private static final int SRC_SIG_NDVI = 17;
//    private static final int SRC_VZA = 18;
//    private static final int SRC_SZA = 19;
//    private static final int SRC_RAA = 20;
//    private static final int SRC_DEM = 21;
//    private static final int SRC_SNOW_MASK = 22;
//    private static final int SRC_LAND_MASK = 23;

    private static int[][] TRG_M;
    private static int[] TRG_V;

    private static int TRG_E = 90;
    private static int TRG_MASK = 91;

    private static final int sourceSampleOffset = 100;  // this value must be >= number of bands in a source product


    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {

        for (int i = 0; i < sourceProducts.length; i++) {
            Product sourceProduct = sourceProducts[i];

            if (sourceProduct.getPreferredTileSize() == null) {
                sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 45);
            }

            int offset = 0;
            for (int j = 0; j < numSpectralBands; j++) {
                configurator.defineSample((sourceSampleOffset * i) + offset,
                                          AlbedoInversionConstants.MODIS_SPECTRAL_SDR_NAME_PREFIX + offset,
                                          sourceProduct);
                offset++;
            }
            offset = 0;
            for (int j = 0; j < numSpectralBands; j++) {
                configurator.defineSample((sourceSampleOffset * i) + offset,
                                          AlbedoInversionConstants.MODIS_SPECTRAL_SDR_NAME_PREFIX + offset,
                                          sourceProduct);
                offset++;
            }

            offset *= 2;
            configurator.defineSample((sourceSampleOffset * i) + (offset++),
                                      AlbedoInversionConstants.BBDR_KVOL_BRDF_VIS_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + (offset++),
                                      AlbedoInversionConstants.BBDR_KVOL_BRDF_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + (offset++),
                                      AlbedoInversionConstants.BBDR_KVOL_BRDF_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + (offset++),
                                      AlbedoInversionConstants.BBDR_KGEO_BRDF_VIS_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + (offset++),
                                      AlbedoInversionConstants.BBDR_KGEO_BRDF_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + (offset++),
                                      AlbedoInversionConstants.BBDR_KGEO_BRDF_SW_NAME, sourceProduct);
            // todo: AOD, NDVI, VZA, SZA, RAA, DEM not used. Check SDR input product anyway.
//            configurator.defineSample((sourceSampleOffset * i) + SRC_AOD550, AlbedoInversionConstants.BBDR_AOD550_NAME,
//                                      sourceProduct);
//            configurator.defineSample((sourceSampleOffset * i) + SRC_NDVI, AlbedoInversionConstants.BBDR_NDVI_NAME,
//                                      sourceProduct);
//            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_NDVI,
//                                      AlbedoInversionConstants.BBDR_SIG_NDVI_NAME, sourceProduct);
//            configurator.defineSample((sourceSampleOffset * i) + SRC_VZA, AlbedoInversionConstants.BBDR_VZA_NAME,
//                                      sourceProduct);
//            configurator.defineSample((sourceSampleOffset * i) + SRC_SZA, AlbedoInversionConstants.BBDR_SZA_NAME,
//                                      sourceProduct);
//            configurator.defineSample((sourceSampleOffset * i) + SRC_RAA, AlbedoInversionConstants.BBDR_RAA_NAME,
//                                      sourceProduct);
//            configurator.defineSample((sourceSampleOffset * i) + SRC_DEM, AlbedoInversionConstants.BBDR_DEM_NAME,
//                                      sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + offset++,
                                      AlbedoInversionConstants.BBDR_SNOW_MASK_NAME, sourceProduct);

            // get land mask (sensor dependent)
            AlbedoInversionUtils.setLandMaskSourceSample(configurator, (sourceSampleOffset * i) + offset,
                                                         sourceProduct, false);
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        TRG_M = new int[3 * numSpectralBands][3 * numSpectralBands];
        TRG_V = new int[3 * numSpectralBands];

        int offset = 0;
        for (int i = 0; i < 3 * numSpectralBands; i++) {
            for (int j = 0; j < 3 * numSpectralBands; j++) {
                TRG_M[i][j] = 3 * numSpectralBands * i + j;
                offset++;
            }
        }
        for (int i = 0; i < 3 * numSpectralBands; i++) {
            TRG_V[offset + i] = offset;
            offset++;
        }

        TRG_E = offset;
        TRG_MASK = TRG_E + 1;

        configurator.defineSample(TRG_MASK, AlbedoInversionConstants.ACC_MASK_NAME);
    }
}
