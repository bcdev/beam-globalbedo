package org.esa.beam.dataio.mfg;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ProductUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Product reader responsible for reading Meteosat MSA data products in HDF format.
 *
 * @author Marco Peters
 * @since 1.0
 */
public class MSAProductReader extends AbstractProductReader {

    private static final float[] surfaceAlbedo = new float[4];
    private static final float[] qualityIndicators = new float[20];
    private static final float[] navigation = new float[2];

    private final Logger logger;
    private Product albedoInputProduct;
    private Product navigationInputProduct;

    protected MSAProductReader(MSAProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        logger = Logger.getLogger(getClass().getSimpleName());
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File inputFile = getInputFile();
        return createProduct(inputFile);
    }

    private Product createProduct(File inputFile) {
        albedoInputProduct = createAlbedoInputProduct();
        Product product = new Product(albedoInputProduct.getName(),
                albedoInputProduct.getProductType(),
                albedoInputProduct.getSceneRasterWidth(),
                albedoInputProduct.getSceneRasterHeight(),
                this);
        ProductUtils.copyMetadata(albedoInputProduct, product);
        attachAlbedoDataToProduct(product);
        attachGeoCodingToProduct(product);

        return product;
    }

    private void attachGeoCodingToProduct(Product product) {
        final Product geoInfoProduct = createNavigationInputProduct();
        final Band latBand = geoInfoProduct.getBand("Navigation/Latitude");
        final Band lonBand = geoInfoProduct.getBand("Navigation/Longitude");

        product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 1));
    }

    private void attachAlbedoDataToProduct(Product product) {
        addAlbedoAndQualityBands(product);
    }

    private Product createAlbedoInputProduct() {
        Product albedoProduct = null;
        try {
            albedoProduct = ProductIO.readProduct(getInputFile());
            if (albedoProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                        getInputFile().getName());
                logger.log(Level.WARNING, msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", getInputFile().getName());
            logger.log(Level.WARNING, msg, e);
        }
        return albedoProduct;
    }

    private Product createQualityInputProduct() {
        Product qualityProduct = null;
        try {
            qualityProduct = ProductIO.readProduct(getAncillaryInputFile());
            if (qualityProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                        getAncillaryInputFile().getName());
                logger.log(Level.WARNING, msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", getAncillaryInputFile().getName());
            logger.log(Level.WARNING, msg, e);
        }
        return qualityProduct;
    }

    private Product createNavigationInputProduct() {
        Product navigationProduct = null;
        try {
            navigationProduct = ProductIO.readProduct(getNavigationInputFile());
            if (navigationProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                        getNavigationInputFile().getName());
                logger.log(Level.WARNING, msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", getNavigationInputFile().getName());
            logger.log(Level.WARNING, msg, e);
        }
        return navigationProduct;
    }

    private void addAlbedoAndQualityBands(Product product) {
        for (final Band sourceBand : albedoInputProduct.getBands()) {
            if (sourceBand.getName().startsWith("Surface Albedo") &&
                    hasSameRasterDimension(product, albedoInputProduct)) {
                String bandName = sourceBand.getName();
                Band targetBand = ProductUtils.copyBand(bandName, albedoInputProduct, product);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }
        final Product qualityInputProduct = createQualityInputProduct();
        for (final Band sourceBand : qualityInputProduct.getBands()) {
            if (sourceBand.getName().startsWith("Quality Indicators") &&
                    hasSameRasterDimension(product, albedoInputProduct)) {
                String bandName = sourceBand.getName();
                Band targetBand = ProductUtils.copyBand(bandName, albedoInputProduct, product);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }
    }

    private boolean hasSameRasterDimension(Product productOne, Product productTwo) {
        int widthOne = productOne.getSceneRasterWidth();
        int heightOne = productOne.getSceneRasterHeight();
        int widthTwo = productTwo.getSceneRasterWidth();
        int heightTwo = productTwo.getSceneRasterHeight();
        return widthOne == widthTwo && heightOne == heightTwo;
    }

    static boolean validateAlbedoInputFileName(String fileName) {

        if (!fileName.matches("MSA_Albedo_L2.0_V[0-9].[0-9]{2}_[0-9]{3}_[0-9]{4}_[0-9]{3}_[0-9]{3}.(?i)(hdf)")) {
//        if (!fileName.matches("MSA_Albedo_L2.0_V[0-9].[0-9]{2}_[0-9]{3}_[0-9]{4}_[0-9]{3}_[0-9]{3}.HDF")) {
            throw new IllegalArgumentException("Input file name '" + fileName +
                    "' does not match naming convention: 'MSA Albedo L2.0 Vm.nn sss yyyy fff lll.HDF'");
        }
        return true;
    }

    private File getParentInputDirectory() {
        return getInputFile().getParentFile();
    }

    private File getInputFile() {
        return new File(getInput().toString());
    }

    private File getAncillaryInputFile() {
        final String albedoInputFileName = getInputFile().getName();
        final String ancillaryInputFileName = albedoInputFileName.replace("MSA_Albedo", "MSA_Ancillary");
        return new File(getParentInputDirectory() + File.separator + ancillaryInputFileName);
    }

    private File getNavigationInputFile() {
        // todo: check later where this file will finally be localized. Does not come with each single MSA product!
        final String albedoInputFileName = getInputFile().getName();
        final String navigationInputFileName;
        navigationInputFileName = albedoInputFileName.replace("MSA_Albedo", "MSA_Static").substring(0, 26).concat("_0");
        return new File(getParentInputDirectory() + File.separator + navigationInputFileName);
    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException(String.format("No source to read from for band '%s'.", destBand.getName()));
    }

    @Override
    public void close() throws IOException {
        albedoInputProduct.dispose();
        super.close();
    }
}
