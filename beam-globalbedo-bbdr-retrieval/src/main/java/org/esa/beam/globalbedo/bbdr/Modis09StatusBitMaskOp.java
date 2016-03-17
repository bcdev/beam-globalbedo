package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.ProductUtils;

import java.awt.*;

/**
 * Extracts pixel status from MOD09 state band into a new flag band
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.l2.mod09.state")
public class Modis09StatusBitMaskOp extends PixelOperator {

    private static final int SRC_FLAG = 0;
    private static final int TRG_FLAG = 0;

    @SourceProduct
    private Product sourceProduct;

    private static final String TARGET_FLAG_BAND_NAME = Mod09Constants.MOD09_STATE_FLAG_BAND_NAME;


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        for (Band b: sourceProduct.getBands()) {
            ProductUtils.copyBand(b.getName(), sourceProduct,targetProduct, true);
        }

        FlagCoding stateFlagCoding = new FlagCoding(Mod09Constants.MOD09_STATE_FLAG_BAND_NAME);
        addStateQualityFlags(stateFlagCoding);
        addStateQualityMasks(targetProduct);
        targetProduct.getFlagCodingGroup().add(stateFlagCoding);

        final Band stateFlagBand = targetProduct.addBand(TARGET_FLAG_BAND_NAME, ProductData.TYPE_INT32);
        stateFlagBand.setDescription("MODIS MOD09 State Flags");
        stateFlagBand.setSampleCoding(stateFlagCoding);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(SRC_FLAG, Mod09Constants.MOD09_STATE_BAND_NAME);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(TRG_FLAG, TARGET_FLAG_BAND_NAME);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int srcFlagValue = sourceSamples[SRC_FLAG].getInt();
        computeModis09StateMask(srcFlagValue, targetSamples);
    }

    private void computeModis09StateMask(int srcValue, WritableSample[] targetSamples) {
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLEAR_BIT_INDEX, isClear(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLOUD_BIT_INDEX, isCloud(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLOUD_MIXED_BIT_INDEX, isCloudMixed(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLOUD_UNKNOWN_BIT_INDEX, isCloudUnknown(srcValue));

        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLOUD_SHADOW_BIT_INDEX, isCloudShadow(srcValue));

        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_SHALLOW_OCEAN_BIT_INDEX, isShallowOcean(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_LAND_BIT_INDEX, isLand(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_COASTLINE_BIT_INDEX, isCoastline(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_SHALLOW_INLAND_WATER_BIT_INDEX, isShallowInlandWater(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_EPHEMERAL_WATER_BIT_INDEX, isEphemeralWater(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_DEEP_INLAND_WATER_BIT_INDEX, isDeepInlandWater(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_MODERATE_OCEAN_BIT_INDEX, isModerateOcean(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_DEEP_OCEAN_BIT_INDEX, isDeepOcean(srcValue));

        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_AEROSOL_CLIMATOLOGY_BIT_INDEX, isAerosolClimatology(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_AEROSOL_LOW_BIT_INDEX, isAerosolLow(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_AEROSOL_AVERAGE_BIT_INDEX, isAerosolAverage(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_AEROSOL_HIGH_BIT_INDEX, isAerosolHigh(srcValue));

        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CIRRUS_NONE_BIT_INDEX, isCirrusNone(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CIRRUS_SMALL_BIT_INDEX, isCirrusSmall(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CIRRUS_AVERAGE_BIT_INDEX, isCirrusAverage(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CIRRUS_HIGH_BIT_INDEX, isCirrusHigh(srcValue));

        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_INTERNAL_CLOUD_ALGORITHM_BIT_INDEX, isInternalCloudAlgorithm(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_INTERNAL_FIRE_ALGORITHM_BIT_INDEX, isInternalFireAlgorithm(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_MOD35_SNOW_ICE_BIT_INDEX, isMod35SnowIce(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLOUD_ADJACENCY_BIT_INDEX, isCloudAdjacent(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_SALT_PAN_BIT_INDEX, isSaltPan(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_INTERNAL_SNOW_MASK_BIT_INDEX, isSnow(srcValue));

    }

    // bits 0-1:
    private boolean isClear(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1));
    }

    private boolean isCloud(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1));
    }

    private boolean isCloudMixed(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1));
    }

    private boolean isCloudUnknown(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1));
    }

    // bit 2:
    private boolean isCloudShadow(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 2));
    }

    // bits 3-5:
    private boolean isShallowOcean(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 3) &&
                !BitSetter.isFlagSet(srcValue, 4) &&
                !BitSetter.isFlagSet(srcValue, 5));
    }

    private boolean isLand(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 3) &&
                !BitSetter.isFlagSet(srcValue, 4) &&
                !BitSetter.isFlagSet(srcValue, 5));
    }

    private boolean isCoastline(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 3) &&
                BitSetter.isFlagSet(srcValue, 4) &&
                !BitSetter.isFlagSet(srcValue, 5));
    }

    private boolean isShallowInlandWater(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 3) &&
                BitSetter.isFlagSet(srcValue, 4) &&
                !BitSetter.isFlagSet(srcValue, 5));
    }

    private boolean isEphemeralWater(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 3) &&
                !BitSetter.isFlagSet(srcValue, 4) &&
                BitSetter.isFlagSet(srcValue, 5));
    }

    private boolean isDeepInlandWater(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 3) &&
                !BitSetter.isFlagSet(srcValue, 4) &&
                BitSetter.isFlagSet(srcValue, 5));
    }

    private boolean isModerateOcean(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 3) &&
                BitSetter.isFlagSet(srcValue, 4) &&
                BitSetter.isFlagSet(srcValue, 5));
    }

    private boolean isDeepOcean(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 3) &&
                BitSetter.isFlagSet(srcValue, 4) &&
                BitSetter.isFlagSet(srcValue, 5));
    }

    // bits 6-7:
    private boolean isAerosolClimatology(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 6) &&
                !BitSetter.isFlagSet(srcValue, 7));
    }

    private boolean isAerosolLow(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 6) &&
                !BitSetter.isFlagSet(srcValue, 7));
    }

    private boolean isAerosolAverage(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 6) &&
                BitSetter.isFlagSet(srcValue, 7));
    }

    private boolean isAerosolHigh(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 6) &&
                BitSetter.isFlagSet(srcValue, 7));
    }

    // bits 8-9:
    private boolean isCirrusNone(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 8) &&
                !BitSetter.isFlagSet(srcValue, 9));
    }

    private boolean isCirrusSmall(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 8) &&
                !BitSetter.isFlagSet(srcValue, 9));
    }

    private boolean isCirrusAverage(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 8) &&
                BitSetter.isFlagSet(srcValue, 9));
    }

    private boolean isCirrusHigh(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 8) &&
                BitSetter.isFlagSet(srcValue, 9));
    }

    // bit 10:
    private boolean isInternalCloudAlgorithm(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 10));
    }

    // bit 11:
    private boolean isInternalFireAlgorithm(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 11));
    }

    // bit 12:
    private boolean isMod35SnowIce(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 12));
    }

    // bit 13:
    private boolean isCloudAdjacent(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 13));
    }

    // bit 14:
    private boolean isSaltPan(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 14));
    }

    // bit 15:
    private boolean isSnow(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 15));
    }

    public static void addStateQualityMasks(Product probavProduct) {
        ProductNodeGroup<Mask> maskGroup = probavProduct.getMaskGroup();
        addMask(probavProduct, maskGroup, Mod09Constants.MOD09_STATE_FLAG_BAND_NAME,
                Mod09Constants.STATE_CLOUD_FLAG_NAME,
                Mod09Constants.STATE_CLOUD_FLAG_DESCR,
                Mod09Constants.FLAG_COLORS[1], 0.5f);
        addMask(probavProduct, maskGroup, Mod09Constants.MOD09_STATE_FLAG_BAND_NAME,
                Mod09Constants.STATE_LAND_FLAG_NAME,
                Mod09Constants.STATE_LAND_FLAG_DESCR,
                Mod09Constants.FLAG_COLORS[9], 0.5f);
        addMask(probavProduct, maskGroup, Mod09Constants.MOD09_STATE_FLAG_BAND_NAME,
                Mod09Constants.STATE_MOD35_SNOW_ICE_FLAG_NAME,
                Mod09Constants.STATE_MOD35_SNOW_ICE_FLAG_DESCR,
                Mod09Constants.FLAG_COLORS[5], 0.5f);
        // todo: complete
    }

    public static void addStateQualityFlags(FlagCoding probavSmFlagCoding) {
        probavSmFlagCoding.addFlag(Mod09Constants.STATE_CLOUD_FLAG_NAME,
                BitSetter.setFlag(0, Mod09Constants.STATE_CLOUD_BIT_INDEX),
                Mod09Constants.STATE_CLOUD_FLAG_DESCR);
        probavSmFlagCoding.addFlag(Mod09Constants.STATE_LAND_FLAG_NAME,
                BitSetter.setFlag(0, Mod09Constants.STATE_LAND_BIT_INDEX),
                Mod09Constants.STATE_LAND_FLAG_DESCR);
        probavSmFlagCoding.addFlag(Mod09Constants.STATE_MOD35_SNOW_ICE_FLAG_NAME,
                BitSetter.setFlag(0, Mod09Constants.STATE_MOD35_SNOW_ICE_BIT_INDEX),
                Mod09Constants.STATE_MOD35_SNOW_ICE_FLAG_DESCR);
        // todo: complete
    }

    private static void addMask(Product mod35Product, ProductNodeGroup<Mask> maskGroup,
                                String bandName, String flagName, String description, Color color, float transparency) {
        int width = mod35Product.getSceneRasterWidth();
        int height = mod35Product.getSceneRasterHeight();
        String maskPrefix = "";
        Mask mask = Mask.BandMathsType.create(maskPrefix + flagName,
                description, width, height,
                bandName + "." + flagName,
                color, transparency);
        maskGroup.add(mask);
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(Modis09StatusBitMaskOp.class);
        }
    }
}
