package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.spectral.SpectralInversionUtils;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.ProductUtils;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 26.05.2016
 * Time: 10:01
 *
 * @author olafd
 */
public class SdrSpectralMappingOp extends BbdrMasterOp {

    private static final int SRC_VZA = 50;
    private static final int SRC_SZA = 51;
    private static final int SRC_VAA = 52;
    private static final int SRC_SAA = 53;
    private static final int SRC_DEM = 54;
    private static final int SRC_AOD = 55;
    private static final int SRC_AOD_ERR = 56;

    private static final int SRC_LAND_MASK = 60;
    private static final int SRC_SNOW_MASK = 61;

    private static final int SRC_STATUS = 70;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Write ancillary bands (angles, AOT) to target product")
    private boolean writeGeometryAndAOT;


    @Parameter(defaultValue = "7", description = "Spectral mapped SDR bands (usually the 7 MODIS channels)")
    protected int numMappedSdrBands;

    private String[] sdrMappedBandNames;
    private int numSigmaSdrBands;
    private String[] sigmaSdrMappedBandNames;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        sdrMappedBandNames = SpectralInversionUtils.getSdrBandNames(numMappedSdrBands);
        numSigmaSdrBands = (numMappedSdrBands * numMappedSdrBands - numMappedSdrBands)/2 + numMappedSdrBands;
        sigmaSdrMappedBandNames = SpectralInversionUtils.getSigmaSdrBandNames(numMappedSdrBands, numSigmaSdrBands);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {
        final String commonLandExpr;
        if (landExpression != null && !landExpression.isEmpty()) {
            commonLandExpr = landExpression;
        } else {
            commonLandExpr = sensor.getLandExpr();
        }

        final String snowMaskExpression = "cloud_classif_flags.F_CLEAR_SNOW";

        BandMathsOp.BandDescriptor bdSnow = new BandMathsOp.BandDescriptor();
        bdSnow.name = "snow_mask";
        bdSnow.expression = snowMaskExpression;
        bdSnow.type = ProductData.TYPESTRING_INT8;

        BandMathsOp snowOp = new BandMathsOp();
        snowOp.setParameterDefaultValues();
        snowOp.setSourceProduct(sourceProduct);
        snowOp.setTargetBandDescriptors(bdSnow);
        Product snowMaskProduct = snowOp.getTargetProduct();

        configurator.defineSample(SRC_SNOW_MASK, snowMaskProduct.getBandAt(0).getName(), snowMaskProduct);

        BandMathsOp.BandDescriptor bdLand = new BandMathsOp.BandDescriptor();
        bdLand.name = "land_mask";
        bdLand.expression = commonLandExpr;
        bdLand.type = ProductData.TYPESTRING_INT8;

        for (int i = 0; i < sensor.getSdrBandNames().length; i++) {
            configurator.defineSample(i, sensor.getSdrBandNames()[i], sourceProduct);
        }
        int index = sensor.getSdrBandNames().length;
        for (int i = 0; i < sensor.getSdrErrorBandNames().length; i++) {
            configurator.defineSample(index + i, sensor.getSdrErrorBandNames()[i], sourceProduct);
        }

        configurator.defineSample(SRC_STATUS, "status", sourceProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        int index = 0;
        for (int i = 0; i < sdrMappedBandNames.length; i++) {
            configurator.defineSample(index++, sdrMappedBandNames[i]);
        }
        for (int i = 0; i < sigmaSdrMappedBandNames.length; i++) {
            configurator.defineSample(index++, sigmaSdrMappedBandNames[i]);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();

        targetProduct.setAutoGrouping("sdr_sigma:sdr");
        addMappedSdrBands(targetProduct);
        addMappedSdrErrorBands(targetProduct);
        final String[] kernelBandNames = {
                "Kvol_BRDF_VIS", "Kvol_BRDF_NIR", "Kvol_BRDF_SW",
                "Kgeo_BRDF_VIS", "Kgeo_BRDF_NIR", "Kgeo_BRDF_SW",
        };
        for (String bandName : kernelBandNames) {
            Band band = targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }
        targetProduct.addBand("snow_mask", ProductData.TYPE_INT8);

        // copy flag coding and flag images
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        if (writeGeometryAndAOT) {
            ProductUtils.copyBand("VZA",sourceProduct, targetProduct, true);
            ProductUtils.copyBand("SZA",sourceProduct, targetProduct, true);
            ProductUtils.copyBand("VAA",sourceProduct, targetProduct, true);
            ProductUtils.copyBand("SAA",sourceProduct, targetProduct, true);
            ProductUtils.copyBand("DEM",sourceProduct, targetProduct, true);
            ProductUtils.copyBand("AOD550",sourceProduct, targetProduct, true);
            ProductUtils.copyBand("sig_AOD550",sourceProduct, targetProduct, true);
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int status = sourceSamples[SRC_STATUS].getInt();
        targetSamples[TRG_SNOW].set(sourceSamples[SRC_SNOW_MASK].getInt());

        if (status != 1 && status != 3) {
            // only compute over clear land or snow
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        // prepare SDR spectral mapping...
        double[] sdr = new double[sensor.getNumBands()];
        for (int i = 0; i < sensor.getNumBands(); i++) {
            sdr[i] = sourceSamples[i].getDouble();
        }
        double[] sigmaSdr = new double[sensor.getNumBands()];
        for (int i = 0; i < sensor.getNumBands(); i++) {
            sigmaSdr[i] = sourceSamples[sensor.getNumBands() + i].getDouble();
        }

        int[] sinCoordinates = null;  // todo: clarify what means sinCoordinates
        final double[] sdrMapped =
                getSpectralMappedSdr(numMappedSdrBands, sensor.name(), sdr, year, doy, sinCoordinates, computeSnow);
        final double[] sdrSigmaMapped =
                getSpectralMappedSigmaSdr(numMappedSdrBands, sensor.name(), sigmaSdr, year, doy, sinCoordinates, computeSnow);
    }

    private void addMappedSdrBands(Product targetProduct) {
        for (int i = 0; i < sdrMappedBandNames.length; i++) {
            Band band = targetProduct.addBand(sdrMappedBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
            band.setSpectralBandIndex(i);
            band.setSpectralWavelength(BbdrConstants.MODIS_WAVELENGHTS[i]);
        }
    }

    private void addMappedSdrErrorBands(Product targetProduct) {
        for (int i = 0; i < sigmaSdrMappedBandNames.length; i++) {
            Band band = targetProduct.addBand(sigmaSdrMappedBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }
    }

    static double[] getSpectralMappedSdr(int numMappedSdrBands, String sensorName, double[] sdr, int year, int doy,
                                          int[] sinCoordinates, boolean snow) {
        double[] sdrMapped = new double[numMappedSdrBands];   // the 7 MODIS channels

        // TODO: 26.05.2016 : include implementation from SK
        return sdrMapped;
    }

    static double[] getSpectralMappedSigmaSdr(int numMappedSdrBands, String sensorName, double[] sdrErrors, int year, int doy,
                                               int[] sinCoordinates, boolean snow) {
        final int numSigmaSdrMappedBands = (numMappedSdrBands * numMappedSdrBands - numMappedSdrBands) / 2 + numMappedSdrBands;
        double[] sdrSigmaMapped = new double[numSigmaSdrMappedBands];   // 28 sigma bands for the 7 MODIS channels

        // TODO: 26.05.2016 : include implementation from SK
        return sdrSigmaMapped;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SdrSpectralMappingOp.class);
        }
    }
}
