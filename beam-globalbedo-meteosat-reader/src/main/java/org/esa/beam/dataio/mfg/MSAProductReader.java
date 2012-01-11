package org.esa.beam.dataio.mfg;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TransposeDescriptor;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Product reader responsible for reading Meteosat MSA data products in HDF format.
 *
 * @author Marco Peters
 * @since 1.0
 */
public class MSAProductReader extends AbstractProductReader {

    public static final String AUTO_GROUPING_PATTERN = "Surface_Albedo:Quality_Indicators:Navigation";
    private final Logger logger;
    private VirtualDir virtualDir;
    private Product albedoInputProduct;
    private Product ancillaryInputProduct;
    private Product staticInputProduct;

    private static final String ALBEDO_BAND_NAME_PREFIX = "Surface Albedo";
    private static final String ANCILLARY_BAND_NAME_PREFIX = "Quality_Indicators";
    private static final String ANCILLARY_BAND_NAME_PRE_PREFIX = "Quality";
    private static final String STATIC_BAND_NAME_PREFIX = "Navigation";

    protected MSAProductReader(MSAProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        logger = Logger.getLogger(getClass().getSimpleName());
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File inputFile = getInputFile();
        virtualDir = VirtualDir.create(inputFile);
        return createProduct();
    }

    private Product createProduct() {
        try {
            final String[] productFileNames = virtualDir.list(".");

            for (String fileName : productFileNames) {
                if (fileName.matches("MSA_Albedo_L2.0_V[0-9].[0-9]{2}_[0-9]{3}_[0-9]{4}_[0-9]{3}_[0-9]{3}.(?i)(hdf)")) {
                    albedoInputProduct = createAlbedoInputProduct(virtualDir.getFile(fileName));
                }
                if (fileName.matches("MSA_Ancillary_L2.0_V[0-9].[0-9]{2}_[0-9]{3}_[0-9]{4}_[0-9]{3}_[0-9]{3}.(?i)(hdf)")) {
                    ancillaryInputProduct = createAncillaryInputProduct(virtualDir.getFile(fileName));
                }
                if (fileName.matches("MSA_Static_L2.0_V[0-9].[0-9]{2}_[0-9]{3}_[0-9]{1}.(?i)(hdf)")) {
                    staticInputProduct = createStaticInputProduct(virtualDir.getFile(fileName));
                }
            }
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }

        Product product = new Product(getInputFile().getName(),
                albedoInputProduct.getProductType(),
                albedoInputProduct.getSceneRasterWidth(),
                albedoInputProduct.getSceneRasterHeight(),
                this);
//        ProductUtils.copyMetadata(albedoInputProduct, product);
//        ProductUtils.copyMetadata(ancillaryInputProduct, product);
//        ProductUtils.copyMetadata(staticInputProduct, product);

        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Variable_Attributes"));
        ProductUtils.copyMetadata(albedoInputProduct.getMetadataRoot().getElement("Global_Attributes"), product.getMetadataRoot().getElement("Global_Attributes"));

        ProductUtils.copyMetadata(albedoInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                product.getMetadataRoot().getElement("Variable_Attributes"));
        ProductUtils.copyMetadata(ancillaryInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                product.getMetadataRoot().getElement("Variable_Attributes"));
        ProductUtils.copyMetadata(staticInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                product.getMetadataRoot().getElement("Variable_Attributes"));

        attachAlbedoDataToProduct(product);
        attachAncillaryDataToProduct(product);
        attachGeoCodingToProduct(product);
        product.setAutoGrouping(AUTO_GROUPING_PATTERN);

        return product;
    }

    private Product createAlbedoInputProduct(File inputFile) {
        Product albedoProduct = null;
        try {
            albedoProduct = ProductIO.readProduct(inputFile);
            if (albedoProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                        inputFile.getName());
                logger.log(Level.WARNING, msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", inputFile.getName());
            logger.log(Level.WARNING, msg, e);
        }
        return albedoProduct;
    }

    private Product createAncillaryInputProduct(File file) {
        Product qualityProduct = null;
        try {
            qualityProduct = ProductIO.readProduct(file);
            if (qualityProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                        file.getName());
                logger.log(Level.WARNING, msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", file.getName());
            logger.log(Level.WARNING, msg, e);
        }
        return qualityProduct;
    }

    private Product createStaticInputProduct(File file) {
        Product navigationProduct = null;
        try {
            navigationProduct = ProductIO.readProduct(file);
            if (navigationProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                        file.getName());
                logger.log(Level.WARNING, msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", file.getName());
            logger.log(Level.WARNING, msg, e);
        }
        return navigationProduct;
    }

    private void attachAlbedoDataToProduct(Product product) {
        for (final Band sourceBand : albedoInputProduct.getBands()) {
            if (sourceBand.getName().startsWith(ALBEDO_BAND_NAME_PREFIX) &&
                    hasSameRasterDimension(product, albedoInputProduct)) {
                String bandName = sourceBand.getName();
                flipImage(sourceBand);
                Band targetBand = ProductUtils.copyBand(bandName, albedoInputProduct, product);
                String targetBandName = ALBEDO_BAND_NAME_PREFIX + "_" +
                        bandName.substring(bandName.indexOf("/") + 1, bandName.length());
                targetBandName = targetBandName.replace(" ", "_");
                targetBand.setName(targetBandName);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }
    }

    private void attachAncillaryDataToProduct(Product product) {
        for (final Band sourceBand : ancillaryInputProduct.getBands()) {
            if (sourceBand.getName().startsWith(ANCILLARY_BAND_NAME_PRE_PREFIX) &&
                    hasSameRasterDimension(product, ancillaryInputProduct)) {
                String bandName = sourceBand.getName();
                flipImage(sourceBand);
                Band targetBand = ProductUtils.copyBand(bandName, ancillaryInputProduct, product);
                String targetBandName = ANCILLARY_BAND_NAME_PREFIX + "_" +
                        bandName.substring(bandName.indexOf("/") + 1, bandName.length());
                targetBandName.replace(" ", "_");
                targetBand.setName(targetBandName);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }
    }

    private void attachGeoCodingToProduct(Product product) {
        final Band latBand = staticInputProduct.getBand("Navigation/Latitude");
        final Band lonBand = staticInputProduct.getBand("Navigation/Longitude");
        lonBand.setScalingFactor(1.0);   // static data file contains a weird scaling factor for longitude band
        flipImage(latBand);
        flipImage(lonBand);

        String bandName = latBand.getName();
        Band targetBand = ProductUtils.copyBand(bandName, staticInputProduct, product);
        String targetBandName = STATIC_BAND_NAME_PREFIX + "_" +
                bandName.substring(bandName.indexOf("/") + 1, bandName.length());
        targetBand.setName(targetBandName);
        targetBand.setSourceImage(latBand.getSourceImage());

        bandName = lonBand.getName();
        targetBand = ProductUtils.copyBand(lonBand.getName(), staticInputProduct, product);
        targetBandName = STATIC_BAND_NAME_PREFIX + "_" +
                bandName.substring(bandName.indexOf("/") + 1, bandName.length());
        targetBand.setName(targetBandName);
        targetBand.setSourceImage(lonBand.getSourceImage());

        // todo: implement specific GeoCoding which takes into account missing lat/lon data 'outside the Earth' ,
        // by using a LUT with: latlon <--> pixel for all pixels 'INside the Earth'
        product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5));    // this does not work correctly!
    }

    private void flipImage(Band sourceBand) {
        final RenderedOp verticalFlippedImage = TransposeDescriptor.create(sourceBand.getSourceImage(), TransposeDescriptor.FLIP_VERTICAL, null);
        final RenderedOp horizontalFlippedImage = TransposeDescriptor.create(verticalFlippedImage, TransposeDescriptor.FLIP_HORIZONTAL, null);
        sourceBand.setSourceImage(horizontalFlippedImage);
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
            throw new IllegalArgumentException("Input file name '" + fileName +
                    "' does not match naming convention: 'MSA Albedo L2.0 Vm.nn sss yyyy fff lll.HDF'");
        }
        return true;
    }

    private File getInputFile() {
        return new File(getInput().toString());
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
        ancillaryInputProduct.dispose();
        staticInputProduct.dispose();
        super.close();
    }
}
