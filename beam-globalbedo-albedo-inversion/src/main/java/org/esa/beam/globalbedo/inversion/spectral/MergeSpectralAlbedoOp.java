package org.esa.beam.globalbedo.inversion.spectral;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Operator for merging Albedo Snow/NoSnow products.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.albedo.mergealbedo.spectral",
        description = "Merges Albedo Snow/NoSnow products. Can be applied to tiles or mosaics.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2017 by Brockmann Consult")
public class MergeSpectralAlbedoOp extends PixelOperator {


    @SourceProduct(description = "Albedo Snow product")
    private Product snowProduct;

    @SourceProduct(description = "Albedo NoSnow product")
    private Product noSnowProduct;

    @SourceProduct(description = "Prior Info NoSnow product")
    private Product priorInfoNoSnowProduct;

    @SourceProduct(description = "Prior Info Snow product")
    private Product priorInfoSnowProduct;

    @Parameter(defaultValue = "6", description = "Number of spectral bands (7 for standard MODIS spectral mapping")
    private int numSdrBands;

    // source samples:
    private String[] dhrBandNames;
    private String[] dhrSigmaBandNames;
    private String[] bhrBandNames;
    private String[] bhrSigmaBandNames;

    private String weightedNumberOfSamplesBandName;
    private String goodnessOfFitBandName;
    private String relEntropyBandName;
    private String dataMaskBandName;
    private String snowFractionBandName;
    private String szaBandName;

    private int[][] SRC_DHR;
    private int[][] SRC_DHR_SIGMA;
    private int[][] SRC_BHR;
    private int[][] SRC_BHR_SIGMA;
    private int[] SRC_WEIGHTED_NUM_SAMPLES = new int[2];
    private int[] SRC_GOODNESS_OF_FIT = new int[2];
    private int[] SRC_REL_ENTROPY = new int[2];
    private int[] SRC_DATA_MASK = new int[2];
    private int[] SRC_SZA = new int[2];

    private int[] TRG_DHR;
    private int[] TRG_DHR_SIGMA;
    private int[] TRG_BHR;
    private int[] TRG_BHR_SIGMA;
    //    private static final int TRG_WEIGHTED_NUM_SAMPLES = 14;
//    private static final int TRG_SNOW_FRACTION = 15;
//    private static final int TRG_GOODNESS_OF_FIT = 16;
//    private static final int TRG_DATA_MASK = 17;
//    private static final int TRG_SZA = 18;
    private int TRG_WEIGHTED_NUM_SAMPLES;
    private int TRG_SNOW_FRACTION;
    private int TRG_GOODNESS_OF_FIT;
    private int TRG_REL_ENTROPY;
    private int TRG_DATA_MASK;
    private int TRG_SZA;


    private Map<Integer, String> spectralWaveBandsMap = new HashMap<>();

    private int priorWidth;
    private int priorHeight;

    private ProductData priorLandWaterMaskData;
    private ProductData priorSnowFractionNoSnowData;
    private ProductData priorSnowFractionSnowData;


    @Override
    protected void prepareInputs() throws OperatorException {
        dhrBandNames = new String[numSdrBands];
        dhrSigmaBandNames = new String[numSdrBands];
        bhrBandNames = new String[numSdrBands];
        bhrSigmaBandNames = new String[numSdrBands];
        SRC_DHR = new int[2][numSdrBands];
        SRC_BHR = new int[2][numSdrBands];
        SRC_DHR_SIGMA = new int[2][numSdrBands];
        SRC_BHR_SIGMA = new int[2][numSdrBands];
        TRG_DHR = new int[numSdrBands];
        TRG_BHR = new int[numSdrBands];
        TRG_DHR_SIGMA = new int[numSdrBands];
        TRG_BHR_SIGMA = new int[numSdrBands];
        setupSpectralWaveBandsMap(numSdrBands);

        priorWidth = priorInfoNoSnowProduct.getSceneRasterWidth();
        priorHeight = priorInfoNoSnowProduct.getSceneRasterHeight();

        final CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        final double pixelSizeX = 0.05; // todo - this is for 360deg
        final double pixelSizeY = 0.05;
        final double northing = 90.0;
        final double easting = -180.0;
        final CrsGeoCoding crsGeoCoding;
        try {
            crsGeoCoding = new CrsGeoCoding(crs, 7200, 3600, easting, northing, pixelSizeX, pixelSizeY);
            priorInfoNoSnowProduct.setGeoCoding(crsGeoCoding);
            priorInfoSnowProduct.setGeoCoding(crsGeoCoding);
        } catch (Exception e) {
            e.printStackTrace();
            throw new OperatorException("Cannot attach geocoding for mosaic: ", e);
        }

        final Band landWaterTypeBand = priorInfoNoSnowProduct.getBand("landWaterType");
//        final Band priorSnowFractionNoSnowBand = priorInfoNoSnowProduct.getBand("snowFraction");
//        final Band priorSnowFractionSnowBand = priorInfoSnowProduct.getBand("snowFraction");
        try {
            landWaterTypeBand.readRasterDataFully(ProgressMonitor.NULL);
//            priorSnowFractionNoSnowBand.readRasterDataFully(ProgressMonitor.NULL);
//            priorSnowFractionSnowBand.readRasterDataFully(ProgressMonitor.NULL);
            priorLandWaterMaskData = landWaterTypeBand.getRasterData();
//            priorSnowFractionNoSnowData = priorSnowFractionNoSnowBand.getRasterData();
//            priorSnowFractionSnowData = priorSnowFractionSnowBand.getRasterData();
        } catch (IOException e) {
            e.printStackTrace();
            throw new OperatorException("Cannot read prior info product data - check!!");
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double nSamplesNoSnowDataValue = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[0]].getDouble();
        final double nSamplesNoSnow = AlbedoInversionUtils.isValid(nSamplesNoSnowDataValue) ? nSamplesNoSnowDataValue : 0.0;
        final double nSamplesSnowDataValue = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[1]].getDouble();
        final double nSamplesSnow = AlbedoInversionUtils.isValid(nSamplesSnowDataValue) ? nSamplesSnowDataValue : 0.0;
        final double totalNSamples = nSamplesSnow + nSamplesNoSnow;

        double proportionNsamplesSnow;
        double proportionNsamplesNoSnow;

        final PixelPos srcPixelPos = new PixelPos(x, y);
        final GeoPos srcGeoPos = noSnowProduct.getGeoCoding().getGeoPos(srcPixelPos, null);
        final PixelPos priorPixelPos = priorInfoNoSnowProduct.getGeoCoding().getPixelPos(srcGeoPos, null);

        final double priorLandWaterMaskDataValue = getPriorLandWaterMaskDataValue(priorPixelPos);
        if (priorLandWaterMaskDataValue != 1.0) {
            setMergedBandsToNoData(targetSamples);
        } else {
            if (nSamplesNoSnow > 0 && nSamplesSnow > 0) {
                proportionNsamplesNoSnow = nSamplesNoSnow / totalNSamples;
                proportionNsamplesSnow = nSamplesSnow / totalNSamples;
//                setMergedBands(sourceSamples, targetSamples, proportionNsamplesSnow, proportionNsamplesNoSnow, priorPixelPos);
                setMergedBands(sourceSamples, targetSamples, proportionNsamplesSnow, proportionNsamplesNoSnow, null);
            } else if (nSamplesNoSnow > 0) {
//                setMergedBandsToNoSnowBands(sourceSamples, targetSamples, priorPixelPos);
                setMergedBandsToNoSnowBands(sourceSamples, targetSamples, null);
            } else if (nSamplesSnow > 0) {
//                setMergedBandsToSnowBands(sourceSamples, targetSamples, priorPixelPos);
                setMergedBandsToSnowBands(sourceSamples, targetSamples, null);
            } else {
//                setMergedBandsToPrior(sourceSamples, targetSamples, priorPixelPos);
                setMergedBandsToPrior(sourceSamples, targetSamples, null);
            }
        }
    }

    private double getPriorLandWaterMaskDataValue(PixelPos priorPixelPos) {
        final int x = (int) priorPixelPos.getX();
        final int y = (int) priorPixelPos.getY();
        if (x >= 0 && x < priorWidth && y >= 0 && y < priorHeight) {
            return priorLandWaterMaskData.getElemDoubleAt(y * priorWidth + x);
        } else {
            return AlbedoInversionConstants.NO_DATA_VALUE;
        }
    }

//    private double getPriorSnowFractionNoSnowDataValue(PixelPos priorPixelPos) {
//        final int x = (int) priorPixelPos.getX();
//        final int y = (int) priorPixelPos.getY();
//        if (x >= 0 && x < priorWidth && y >= 0 && y < priorHeight) {
//            return priorSnowFractionNoSnowData.getElemDoubleAt(y * priorWidth + x);
//        } else {
//            return AlbedoInversionConstants.NO_DATA_VALUE;
//        }
//    }
//
//    private double getPriorSnowFractionSnowDataValue(PixelPos priorPixelPos) {
//        final int x = (int) priorPixelPos.getX();
//        final int y = (int) priorPixelPos.getY();
//        if (x >= 0 && x < priorWidth && y >= 0 && y < priorHeight) {
//            return priorSnowFractionSnowData.getElemDoubleAt(y * priorWidth + x);
//        } else {
//            return AlbedoInversionConstants.NO_DATA_VALUE;
//        }
//    }

    private void setMergedBandsToNoSnowBands(Sample[] sourceSamples, WritableSample[] targetSamples, PixelPos priorPixelPos) {
        setMergedBands(sourceSamples, targetSamples, 0.0, 1.0, priorPixelPos);
    }

    private void setMergedBandsToSnowBands(Sample[] sourceSamples, WritableSample[] targetSamples, PixelPos priorPixelPos) {
        setMergedBands(sourceSamples, targetSamples, 1.0, 0.0, priorPixelPos);
    }

    private void setMergedBandsToPrior(Sample[] sourceSamples, WritableSample[] targetSamples, PixelPos priorPixelPos) {
        setMergedBands(sourceSamples, targetSamples, 0.0, 0.0, priorPixelPos);
    }

    private void setMergedBands(Sample[] sourceSamples, WritableSample[] targetSamples, double proportionNsamplesSnow,
                                double proportionNsamplesNoSnow, PixelPos priorPixelPos) {

        // sza from one of the data sets where available
        final double szaNoSnowDataValue = sourceSamples[SRC_SZA[0]].getDouble();
        final double szaSnowDataValue = sourceSamples[SRC_SZA[1]].getDouble();
        if (AlbedoInversionUtils.isValid(szaNoSnowDataValue)) {
            targetSamples[TRG_SZA].set(szaNoSnowDataValue);
        } else if (AlbedoInversionUtils.isValid(szaSnowDataValue)) {
            targetSamples[TRG_SZA].set(szaSnowDataValue);
        } else {
            targetSamples[TRG_SZA].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }

//        double priorSnowFractionNoSnowDataValue = getPriorSnowFractionNoSnowDataValue(priorPixelPos);
//        double priorSnowFractionNoSnowDataValue = 0.5;
        double priorSnowFractionNoSnowDataValue = 0.0;      // setting both to zero means we use noSnow Prior value
//        double priorSnowFractionSnowDataValue = getPriorSnowFractionSnowDataValue(priorPixelPos);
//        double priorSnowFractionSnowDataValue = 0.5;
        double priorSnowFractionSnowDataValue = 0.0;

        // BHR/DHR, sigmas
        for (int i = 0; i < numSdrBands; i++) {
            final double bhrValue = getWeightedMergeValue(sourceSamples[SRC_BHR[0][i]].getDouble(),
                                                                    sourceSamples[SRC_BHR[1][i]].getDouble(),
                                                                    proportionNsamplesNoSnow, proportionNsamplesSnow,
                                                                    priorSnowFractionNoSnowDataValue,
                                                                    priorSnowFractionSnowDataValue,
                                                                    szaNoSnowDataValue);
            targetSamples[TRG_BHR[i]].set(Math.max(0.0, bhrValue));

            final double dhrValue = getWeightedMergeValue(sourceSamples[SRC_DHR[0][i]].getDouble(),
                                                                    sourceSamples[SRC_DHR[1][i]].getDouble(),
                                                                    proportionNsamplesNoSnow, proportionNsamplesSnow,
                                                                    priorSnowFractionNoSnowDataValue,
                                                                    priorSnowFractionSnowDataValue,
                                                                    szaNoSnowDataValue);
            targetSamples[TRG_DHR[i]].set(Math.max(0.0, dhrValue));

            double bhrSigmaValue = getWeightedMergeValue(sourceSamples[SRC_BHR_SIGMA[0][i]].getDouble(),
                                                                    sourceSamples[SRC_BHR_SIGMA[1][i]].getDouble(),
                                                                    proportionNsamplesNoSnow, proportionNsamplesSnow,
                                                                    priorSnowFractionNoSnowDataValue,
                                                                    priorSnowFractionSnowDataValue,
                                                                    szaNoSnowDataValue);
            if (bhrSigmaValue < 0.0 || bhrSigmaValue > 1.0) {
                bhrSigmaValue = 1.0;
            }
            targetSamples[TRG_BHR_SIGMA[i]].set(bhrSigmaValue);

            double dhrSigmaValue = getWeightedMergeValue(sourceSamples[SRC_DHR_SIGMA[0][i]].getDouble(),
                                                                    sourceSamples[SRC_DHR_SIGMA[1][i]].getDouble(),
                                                                    proportionNsamplesNoSnow, proportionNsamplesSnow,
                                                                    priorSnowFractionNoSnowDataValue,
                                                                    priorSnowFractionSnowDataValue,
                                                                    szaNoSnowDataValue);
            if (dhrSigmaValue < 0.0 || dhrSigmaValue > 1.0) {
                dhrSigmaValue = 1.0;
            }
            targetSamples[TRG_DHR_SIGMA[i]].set(dhrSigmaValue);
        }

        // WNSamples
        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].
                set(getWeightedMergeValue(sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[0]].getDouble(),
                                          sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[1]].getDouble(),
                                          proportionNsamplesNoSnow, proportionNsamplesSnow,
                                          priorSnowFractionNoSnowDataValue,
                                          priorSnowFractionSnowDataValue,
                                          szaNoSnowDataValue));

        // GoF
        targetSamples[TRG_GOODNESS_OF_FIT].
                set(getWeightedMergeValue(sourceSamples[SRC_GOODNESS_OF_FIT[0]].getDouble(),
                                          sourceSamples[SRC_GOODNESS_OF_FIT[1]].getDouble(),
                                          proportionNsamplesNoSnow, proportionNsamplesSnow,
                                          priorSnowFractionNoSnowDataValue,
                                          priorSnowFractionSnowDataValue,
                                          szaNoSnowDataValue));

        // rel Entropy
        targetSamples[TRG_REL_ENTROPY].
                set(getWeightedMergeValue(sourceSamples[SRC_REL_ENTROPY[0]].getDouble(),
                                          sourceSamples[SRC_REL_ENTROPY[1]].getDouble(),
                                          proportionNsamplesNoSnow, proportionNsamplesSnow,
                                          priorSnowFractionNoSnowDataValue,
                                          priorSnowFractionSnowDataValue,
                                          szaNoSnowDataValue));

        // data mask: 1.0 or 0.0
        final double maskNoSnowDataValue = sourceSamples[SRC_DATA_MASK[0]].getDouble();
        final double maskNoSnow = AlbedoInversionUtils.isValid(maskNoSnowDataValue) ? maskNoSnowDataValue : 0.0;
        final double maskSnowDataValue = sourceSamples[SRC_DATA_MASK[1]].getDouble();
        final double maskSnow = AlbedoInversionUtils.isValid(maskSnowDataValue) ? maskSnowDataValue : 0.0;
        final double maskMerged = Math.max(maskNoSnow, maskSnow);
        targetSamples[TRG_DATA_MASK].set(maskMerged);

        // fix of issue in WNSamples in snow/nosnow files if still present (20170922):
        if (maskMerged == 0.0) {
            targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }

        // snow fraction is just proportionNsamplesSnow
        final double snowFraction = maskMerged > 0.0 && proportionNsamplesSnow > 0.0 ?
                proportionNsamplesSnow : AlbedoInversionConstants.NO_DATA_VALUE;

        targetSamples[TRG_SNOW_FRACTION].set(snowFraction);

    }

    private void setMergedBandsToNoData(WritableSample[] targetSamples) {
        for (int i = 0; i < numSdrBands; i++) {
            targetSamples[TRG_BHR[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            targetSamples[TRG_DHR[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            targetSamples[TRG_BHR_SIGMA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            targetSamples[TRG_DHR_SIGMA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }

        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_GOODNESS_OF_FIT].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_REL_ENTROPY].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_DATA_MASK].set(0.0);
        targetSamples[TRG_SNOW_FRACTION].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_SZA].set(AlbedoInversionConstants.NO_DATA_VALUE);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {

        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        int[] sigmaBandIndices = new int[]{1, 2, 3, 4, 5, 6, 7};

        dhrBandNames = SpectralIOUtils.getSpectralAlbedoDhrBandNames(numSdrBands + 1, spectralWaveBandsMap);
        int index = 0;
        for (int i = 0; i < numSdrBands; i++) {
            SRC_DHR[0][i] = index;
            SRC_DHR[1][i] = index + 100;
            index++;
            final Band dhrBand = targetProduct.addBand(dhrBandNames[i], ProductData.TYPE_FLOAT32);
            dhrBand.setSpectralWavelength(AlbedoInversionConstants.MODIS_WAVELENGHTS[i]);
        }

        dhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoDhrSigmaBandNames(sigmaBandIndices);
        for (int i = 0; i < numSdrBands; i++) {
            SRC_DHR_SIGMA[0][i] = index;
            SRC_DHR_SIGMA[1][i] = index + 100;
            index++;
            targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrBandNames = SpectralIOUtils.getSpectralAlbedoBhrBandNames(numSdrBands + 1, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands; i++) {
            SRC_BHR[0][i] = index;
            SRC_BHR[1][i] = index + 100;
            index++;
            final Band bhrBand = targetProduct.addBand(bhrBandNames[i], ProductData.TYPE_FLOAT32);
            bhrBand.setSpectralWavelength(AlbedoInversionConstants.MODIS_WAVELENGHTS[i]);
        }

        bhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoBhrSigmaBandNames(sigmaBandIndices);
        for (int i = 0; i < numSdrBands; i++) {
            SRC_BHR_SIGMA[0][i] = index;
            SRC_BHR_SIGMA[1][i] = index + 100;
            index++;
            targetProduct.addBand(bhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        SRC_WEIGHTED_NUM_SAMPLES[0] = index;
        SRC_WEIGHTED_NUM_SAMPLES[1] = index + 100;
        index++;
        targetProduct.addBand(weightedNumberOfSamplesBandName, ProductData.TYPE_FLOAT32);

        SRC_GOODNESS_OF_FIT[0] = index;
        SRC_GOODNESS_OF_FIT[1] = index + 100;
        index++;
        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);

        SRC_REL_ENTROPY[0] = index;
        SRC_REL_ENTROPY[1] = index + 100;
        index++;
        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);

        SRC_DATA_MASK[0] = index;
        SRC_DATA_MASK[1] = index + 100;
        index++;
        dataMaskBandName = AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME;
        targetProduct.addBand(dataMaskBandName, ProductData.TYPE_FLOAT32);

        SRC_SZA[0] = index;
        SRC_SZA[1] = index + 100;
        szaBandName = AlbedoInversionConstants.ALB_SZA_BAND_NAME;
        targetProduct.addBand(szaBandName, ProductData.TYPE_FLOAT32);

        snowFractionBandName = AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME;
        targetProduct.addBand(snowFractionBandName, ProductData.TYPE_FLOAT32);

        for (Band b : targetProduct.getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }

    }


    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {

        // source products:
        // for a number of source products, we have:
        // 3x DHR, 3x BHR, 3x DHR_sigma, 3x BHR_sigma, weightedNumSamples, relEntropy, goodnessOfFit,
        // snowFraction, datamask, SZA

        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(SRC_DHR[0][i], dhrBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_DHR[1][i], dhrBandNames[i], snowProduct);
        }

        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(SRC_BHR[0][i], bhrBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_BHR[1][i], bhrBandNames[i], snowProduct);
        }

        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(SRC_DHR_SIGMA[0][i], dhrSigmaBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_DHR_SIGMA[1][i], dhrSigmaBandNames[i], snowProduct);
        }

        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(SRC_BHR_SIGMA[0][i], bhrSigmaBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_BHR_SIGMA[1][i], bhrSigmaBandNames[i], snowProduct);
        }


        configurator.defineSample(SRC_WEIGHTED_NUM_SAMPLES[0], weightedNumberOfSamplesBandName, noSnowProduct);
        configurator.defineSample(SRC_WEIGHTED_NUM_SAMPLES[1], weightedNumberOfSamplesBandName, snowProduct);
        configurator.defineSample(SRC_GOODNESS_OF_FIT[0], goodnessOfFitBandName, noSnowProduct);
        configurator.defineSample(SRC_GOODNESS_OF_FIT[1], goodnessOfFitBandName, snowProduct);
        configurator.defineSample(SRC_REL_ENTROPY[0], relEntropyBandName, noSnowProduct);
        configurator.defineSample(SRC_REL_ENTROPY[1], relEntropyBandName, snowProduct);
        configurator.defineSample(SRC_DATA_MASK[0], dataMaskBandName, noSnowProduct);
        configurator.defineSample(SRC_DATA_MASK[1], dataMaskBandName, snowProduct);
        configurator.defineSample(SRC_SZA[0], szaBandName, noSnowProduct);
        configurator.defineSample(SRC_SZA[1], szaBandName, snowProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        int index = 0;
        for (int i = 0; i < numSdrBands; i++) {
            TRG_DHR[i] = index;
            configurator.defineSample(TRG_DHR[i], dhrBandNames[i]);
            index++;
        }

        for (int i = 0; i < numSdrBands; i++) {
            TRG_DHR_SIGMA[i] = index;
            configurator.defineSample(TRG_DHR_SIGMA[i], dhrSigmaBandNames[i]);
            index++;
        }

        for (int i = 0; i < numSdrBands; i++) {
            TRG_BHR[i] = index;
            configurator.defineSample(TRG_BHR[i], bhrBandNames[i]);
            index++;
        }

        for (int i = 0; i < numSdrBands; i++) {
            TRG_BHR_SIGMA[i] = index;
            configurator.defineSample(TRG_BHR_SIGMA[i], bhrSigmaBandNames[i]);
            index++;
        }

        TRG_WEIGHTED_NUM_SAMPLES = SRC_WEIGHTED_NUM_SAMPLES[0];
        configurator.defineSample(TRG_WEIGHTED_NUM_SAMPLES, weightedNumberOfSamplesBandName);
        TRG_GOODNESS_OF_FIT = SRC_GOODNESS_OF_FIT[0];
        configurator.defineSample(TRG_GOODNESS_OF_FIT, goodnessOfFitBandName);
        TRG_REL_ENTROPY = SRC_REL_ENTROPY[0];
        configurator.defineSample(TRG_REL_ENTROPY, relEntropyBandName);
        TRG_DATA_MASK = SRC_DATA_MASK[0];
        configurator.defineSample(TRG_DATA_MASK, dataMaskBandName);
        TRG_SZA = SRC_SZA[0];
        configurator.defineSample(TRG_SZA, szaBandName);
        TRG_SNOW_FRACTION = TRG_SZA + 1;
        configurator.defineSample(TRG_SNOW_FRACTION, snowFractionBandName);
    }

    static double getWeightedMergeValue(double noSnowValue, double snowValue,
                                        double proportionNsamplesNoSnow, double proportionNsamplesSnow,
                                        double priorSnowFractionNoSnowDataValue,
                                        double priorSnowFractionSnowDataValue,
                                        double sza) {
        final boolean noSnowValid = AlbedoInversionUtils.isValid(noSnowValue);
        final boolean snowValid = AlbedoInversionUtils.isValid(snowValue);

        if (!noSnowValid && !snowValid) {
            return AlbedoInversionConstants.NO_DATA_VALUE;
        } else {
            double mergeValue;
            if (proportionNsamplesNoSnow > 0.0 && proportionNsamplesSnow > 0.0) {
                // snow and nosnow samples available
                mergeValue = noSnowValue * proportionNsamplesNoSnow + snowValue * proportionNsamplesSnow;
            } else if (proportionNsamplesNoSnow > 0.0) {
                // real nosnow sample available
                mergeValue = noSnowValue;
            } else if (proportionNsamplesSnow > 0.0) {
                // real snow sample available
                mergeValue = snowValue;
            } else {
                // prior info only
                if (noSnowValid && snowValid) {
                    final double priorSnowFraction =
                            Math.max(priorSnowFractionNoSnowDataValue, priorSnowFractionSnowDataValue);
                    if (Double.isNaN(priorSnowFraction)) {
                        mergeValue = noSnowValue;
                    } else {
                        mergeValue = (1.0 - priorSnowFraction) * noSnowValue + priorSnowFraction * snowValue;
                    }
                } else {
                    mergeValue = noSnowValid ? noSnowValue : snowValue;
                }
            }
            return mergeValue;
        }
    }

    private void setupSpectralWaveBandsMap(int numSdrBands) {
        for (int i = 0; i < numSdrBands; i++) {
            spectralWaveBandsMap.put(i, "b" + (i + 1));
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeSpectralAlbedoOp.class);
        }
    }

}
