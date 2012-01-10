package org.esa.beam.dataio.mfg;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
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

    private final Logger logger;
    private VirtualDir virtualDir;
    private Product albedoInputProduct;
    private Product ancillaryInputProduct;
    private Product staticInputProduct;

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
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        Product product = new Product(getInputFile().getName(),
                albedoInputProduct.getProductType(),
                albedoInputProduct.getSceneRasterWidth(),
                albedoInputProduct.getSceneRasterHeight(),
                this);
        ProductUtils.copyMetadata(albedoInputProduct, product);
        ProductUtils.copyMetadata(ancillaryInputProduct, product);
        ProductUtils.copyMetadata(staticInputProduct, product);
        attachAlbedoDataToProduct(product);
        attachAncillaryDataToProduct(product);
        attachGeoCodingToProduct(product);

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
            if (sourceBand.getName().startsWith("Surface Albedo") &&
                    hasSameRasterDimension(product, albedoInputProduct)) {
                String bandName = sourceBand.getName();
                flipImage(sourceBand);
                Band targetBand = ProductUtils.copyBand(bandName, albedoInputProduct, product);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }
    }
    
    private void attachAncillaryDataToProduct(Product product) {
        for (final Band sourceBand : ancillaryInputProduct.getBands()) {
            if (sourceBand.getName().startsWith("Quality Indicators") &&
                    hasSameRasterDimension(product, ancillaryInputProduct)) {
                String bandName = sourceBand.getName();
                flipImage(sourceBand);
                Band targetBand = ProductUtils.copyBand(bandName, ancillaryInputProduct, product);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }
    }

    private void attachGeoCodingToProduct(Product product) {
        final Band latBand = staticInputProduct.getBand("Navigation/Latitude");
        final Band lonBand = staticInputProduct.getBand("Navigation/Longitude");
        flipImage(latBand);
        flipImage(lonBand);

        // test:
//        double[] multiplyBy = new double[1];
//        multiplyBy[0] = 1.E10;
//        ParameterBlock pbMult = new ParameterBlock();
//        pbMult.addSource(lonBand.getSourceImage());
//        pbMult.add(multiplyBy);
//        RenderedOp im1 = JAI.create("multiplyconst",pbMult,null);
//        for (int i=0; i<2; i++) {
//            pbMult.setSource(im1, 0);
//            RenderedOp imNew = JAI.create("multiplyconst",pbMult,null);
//            im1 = imNew;
//        }
//        lonBand.setSourceImage(im1);
        // end test
        // todo: longitudes in static product are corrupt (order of 1.E-312), find better product and test

        Band targetBand = ProductUtils.copyBand(latBand.getName(), staticInputProduct, product);
        targetBand.setSourceImage(latBand.getSourceImage());
        targetBand = ProductUtils.copyBand(lonBand.getName(), staticInputProduct, product);
        targetBand.setSourceImage(lonBand.getSourceImage());

        product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5));
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
