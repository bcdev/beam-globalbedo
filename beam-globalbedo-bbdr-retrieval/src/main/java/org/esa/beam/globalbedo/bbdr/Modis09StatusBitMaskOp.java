package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.dataio.ProbaVConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.ProductUtils;

/**
 * todo: add comment
 *
 * @author olafd
 */
public class Modis09StatusBitMaskOp extends PixelOperator {

    private static final int SRC_FLAG = 0;
    private static final int TRG_FLAG = 0;

    @SourceProduct
    private Product sourceProduct;

    private static final String TARGET_FLAG_BAND_NAME = Mod09Constants.MOD09_STATE_BAND_NAME;


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        ProductUtils.copyMetadata(sourceProduct, getTargetProduct());
        ProductUtils.copyMasks(sourceProduct, getTargetProduct());
        ProductUtils.copyGeoCoding(sourceProduct, getTargetProduct());
        for (Band b: sourceProduct.getBands()) {
            ProductUtils.copyBand(b.getName(), sourceProduct, getTargetProduct(), true);
        }

        final Product targetProduct = productConfigurer.getTargetProduct();
        targetProduct.addBand(TARGET_FLAG_BAND_NAME, ProductData.TYPE_INT16);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        sampleConfigurer.defineSample(SRC_FLAG, Mod09Constants.MOD09_STATE_FLAG_BAND_NAME);
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
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLOUD_BIT_INDEX, isCloud(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLOUD_SHADOW_BIT_INDEX, isCloudShadow(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_LAND_WATER_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_AEROSOL_QUANTITY_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CIRRUS_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_INTERNAL_CLOUD_ALGORITHM_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_INTERNAL_FIRE_ALGORITHM_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_MOD35_SNOW_ICE_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_CLOUD_ADJACENCY_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_SALT_PAN_BIT_INDEX, isUndefined(srcValue));
        targetSamples[TRG_FLAG].set(Mod09Constants.STATE_INTERNAL_SNOW_MASK_BIT_INDEX, isUndefined(srcValue));

    }

    private boolean isClear(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isUndefined(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isCloud(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isSnowIce(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isCloudShadow(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private boolean isLand(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 3);
    }

    private boolean isGoodSwir(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 4);
    }

    private boolean isGoodNir(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 5);
    }

    private boolean isGoodRed(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 6);
    }

    private boolean isGoodBlue(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 7);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(Modis09StatusBitMaskOp.class);
        }
    }
}
