/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.mfg;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.MeteosatGeoCoding;
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
 * Product reader responsible for reading Meteosat First Generation MSA data products in HDF format.
 *
 * @author Olaf Danne
 */
public class MfgMSAProductReader extends AbstractProductReader {

    private final Logger logger;
    private VirtualDir virtualDir;
    private static final String ALBEDO_BAND_NAME_PREFIX = "Surface Albedo";

    private static final String ANCILLARY_BAND_NAME_PREFIX = "Quality_Indicators";
    private static final String ANCILLARY_BAND_NAME_PRE_PREFIX = "Quality";
    private static final String STATIC_BAND_NAME_PREFIX = "Navigation";

    private static final String AUTO_GROUPING_PATTERN = "Surface_Albedo:Quality_Indicators:Navigation";

    protected MfgMSAProductReader(MfgMSAProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        logger = Logger.getLogger(getClass().getSimpleName());
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        virtualDir = MfgMSAProductReaderPlugIn.getInput(getInput());
        return createProduct();
    }

    private Product createProduct() throws IOException {
        Product albedoInputProduct = null;
        Product ancillaryInputProduct = null;
        Product staticInputProduct = null;
        try {
            final String[] productFileNames = virtualDir.list("");

            for (String fileName : productFileNames) {
                if (fileName.contains("Albedo") && mfgAlbedoFileNameMatches(fileName)) {
                    albedoInputProduct = createAlbedoInputProduct(virtualDir.getFile(fileName));
                }
                if (fileName.contains("Ancillary") && mfgAncillaryFileNameMatches(fileName)) {
                    ancillaryInputProduct = createAncillaryInputProduct(virtualDir.getFile(fileName));
                }
                if (fileName.contains("Static") && mfgStaticInputFileNameMatches(fileName)) {
                    staticInputProduct = createStaticInputProduct(virtualDir.getFile(fileName));
                }
            }
            if (albedoInputProduct != null && ancillaryInputProduct != null) {
                if (staticInputProduct == null) {
                    // product was not included in the tar or tar.bz2 archive
                    // alternatively, it is by convention expected in same directory as input product
                    staticInputProduct = getStaticInputProductFromAuxdataFile(albedoInputProduct.getName());
                }
            } else {
                throw new IllegalStateException("Content of Meteosat Surface Albedo product '" + getInputFile().getName() +
                                                        "' incomplete or corrupt.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Meteosat Surface Albedo product '" + getInputFile().getName() +
                                                    "' cannot be read.");
        }

        Product product = new Product(getInputFile().getName(),
                                      albedoInputProduct.getProductType(),
                                      albedoInputProduct.getSceneRasterWidth(),
                                      albedoInputProduct.getSceneRasterHeight(),
                                      this);

        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Variable_Attributes"));
        ProductUtils.copyMetadata(albedoInputProduct.getMetadataRoot().getElement("Global_Attributes"),
                                  product.getMetadataRoot().getElement("Global_Attributes"));
        ProductUtils.copyMetadata(albedoInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                                  product.getMetadataRoot().getElement("Variable_Attributes"));
        ProductUtils.copyMetadata(ancillaryInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                                  product.getMetadataRoot().getElement("Variable_Attributes"));
        ProductUtils.copyMetadata(staticInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                                  product.getMetadataRoot().getElement("Variable_Attributes"));

        attachAlbedoDataToProduct(product, albedoInputProduct);
        attachAncillaryDataToProduct(product, ancillaryInputProduct);
        attachGeoCodingToProduct(product, staticInputProduct);
        product.setAutoGrouping(AUTO_GROUPING_PATTERN);

        return product;
    }

    static boolean mfgStaticInputFileNameMatches(String fileName) {
        if (!(fileName.matches("MSA_Static_L2.0_V[0-9].[0-9]{2}_[0-9]{3}_[0-9]{1}.(?i)(hdf)"))) {
            throw new IllegalArgumentException("Input file name '" + fileName +
                                                       "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'");
        }
        return true;
    }

    static boolean mfgAncillaryFileNameMatches(String fileName) {
        if (!(fileName.matches("MSA_Ancillary_L2.0_V[0-9].[0-9]{2}_[0-9]{3}_[0-9]{4}_[0-9]{3}_[0-9]{3}.(?i)(hdf)"))) {
            throw new IllegalArgumentException("Input file name '" + fileName +
                                                       "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'");
        }
        return true;
    }

    static boolean mfgAlbedoFileNameMatches(String fileName) {
        if (!(fileName.matches("MSA_Albedo_L2.0_V[0-9].[0-9]{2}_[0-9]{3}_[0-9]{4}_[0-9]{3}_[0-9]{3}.(?i)(hdf)"))) {
            throw new IllegalArgumentException("Input file name '" + fileName +
                                                       "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'");
        }
        return true;
    }

    Product getStaticInputProductFromAuxdataFile(String albedoInputProductName) throws IOException {
        Product staticInputProduct;
        final String inputParentDirectory = getInputFile().getParent();
        String staticFileExt = ".hdf";
        String staticInputFileName = "MSA_Static_" + albedoInputProductName.substring(11, 25);
        String staticInputFileNameWithExt = staticInputFileName + staticFileExt;
        File staticInputFile = new File(inputParentDirectory + File.separator + staticInputFileNameWithExt);

        if (staticInputFile.exists()) {
            staticInputProduct = createStaticInputProduct(staticInputFile);
        } else {
            staticInputFile = new File(inputParentDirectory + File.separator + staticInputFileName + staticFileExt.toUpperCase());
            if (staticInputFile.exists()) {
                staticInputProduct = createStaticInputProduct(staticInputFile);
            } else {
                throw new IllegalStateException("Static data file '" + staticInputFileName +
                                                        "' missing - cannot open Meteosat Surface Albedo product,");
            }
        }

        return staticInputProduct;
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
        // todo: fill missing lat/lon pixels, use algo described in MFG user handbook, section 5.3.4
        return navigationProduct;
    }

    private void attachAlbedoDataToProduct(Product product, Product albedoInputProduct) {
        for (final Band sourceBand : albedoInputProduct.getBands()) {
            if (sourceBand.getName().startsWith(ALBEDO_BAND_NAME_PREFIX) &&
                    hasSameRasterDimension(product, albedoInputProduct)) {
                String bandName = sourceBand.getName();
                RenderedOp flippedImage = flipImage(sourceBand);
                String targetBandName = ALBEDO_BAND_NAME_PREFIX + "_" +
                        bandName.substring(bandName.indexOf("/") + 1, bandName.length());
                targetBandName = targetBandName.replace(" ", "_");
                Band targetBand = ProductUtils.copyBand(bandName, albedoInputProduct, targetBandName, product, false);
                targetBand.setSourceImage(flippedImage);
            }
        }
    }

    private void attachAncillaryDataToProduct(Product product, Product ancillaryInputProduct) {
        for (final Band sourceBand : ancillaryInputProduct.getBands()) {
            if (sourceBand.getName().startsWith(ANCILLARY_BAND_NAME_PRE_PREFIX) &&
//                    sourceBand.getName().endsWith("NUMSOL") &&
                    hasSameRasterDimension(product, ancillaryInputProduct)) {
                String bandName = sourceBand.getName();
                RenderedOp flippedImage = flipImage(sourceBand);
                String targetBandName = ANCILLARY_BAND_NAME_PREFIX + "_" +
                        bandName.substring(bandName.indexOf("/") + 1, bandName.length());
                targetBandName = targetBandName.replace(" ", "_");
                Band targetBand = ProductUtils.copyBand(bandName, ancillaryInputProduct, targetBandName, product, false);
                targetBand.setSourceImage(flippedImage);
            }
        }
    }

    private void attachGeoCodingToProduct(Product product, Product staticInputProduct) throws IOException {
        final Band latBand = staticInputProduct.getBand("Navigation/Latitude");
        final Band lonBand = staticInputProduct.getBand("Navigation/Longitude");
        lonBand.setScalingFactor(1.0);   // current static data file contains a weird scaling factor for longitude band
        RenderedOp flippedLatImage = flipImage(latBand);
        RenderedOp flippedLonImage = flipImage(lonBand);

        String bandName = latBand.getName();
        String targetBandName = STATIC_BAND_NAME_PREFIX + "_" +
                bandName.substring(bandName.indexOf("/") + 1, bandName.length());
        Band targetBand = ProductUtils.copyBand(bandName, staticInputProduct, targetBandName, product, false);
        targetBand.setSourceImage(flippedLatImage);

        bandName = lonBand.getName();
        targetBandName = STATIC_BAND_NAME_PREFIX + "_" +
                bandName.substring(bandName.indexOf("/") + 1, bandName.length());
        targetBand = ProductUtils.copyBand(lonBand.getName(), staticInputProduct, targetBandName, product, false);
        targetBand.setSourceImage(flippedLonImage);

        // implement specific GeoCoding which takes into account missing lat/lon data 'outside the Earth' ,
        // by using a LUT with: latlon <--> pixel for all pixels 'INside the Earth'
        // --> special solution for Meteosat, but general solution is still under discussion
//        product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5));    // this does not work correctly!
        final Band latBandT = product.getBand("Navigation_Latitude");
        latBandT.setValidPixelExpression("'Navigation_Latitude' != -9999.0");
        final Band lonBandT = product.getBand("Navigation_Longitude");
        lonBandT.setValidPixelExpression("'Navigation_Latitude' != -9999.0");
        product.setGeoCoding(new MeteosatGeoCoding(latBandT, lonBandT, "MFG_0deg"));
    }

    private RenderedOp flipImage(Band sourceBand) {
        final RenderedOp verticalFlippedImage = TransposeDescriptor.create(sourceBand.getSourceImage(), TransposeDescriptor.FLIP_VERTICAL, null);
        return TransposeDescriptor.create(verticalFlippedImage, TransposeDescriptor.FLIP_HORIZONTAL, null);
    }

    private boolean hasSameRasterDimension(Product productOne, Product productTwo) {
        int widthOne = productOne.getSceneRasterWidth();
        int heightOne = productOne.getSceneRasterHeight();
        int widthTwo = productTwo.getSceneRasterWidth();
        int heightTwo = productTwo.getSceneRasterHeight();
        return widthOne == widthTwo && heightOne == heightTwo;
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
        virtualDir.close();
        virtualDir = null;
        super.close();
    }
}
