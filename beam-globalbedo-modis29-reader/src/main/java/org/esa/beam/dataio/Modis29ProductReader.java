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

package org.esa.beam.dataio;

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
 * Product reader responsible for reading Meteosat First Generation MSA data products in HDF format.
 *
 * @author Olaf Danne
 */
public class Modis29ProductReader extends AbstractProductReader {

    private final Logger logger;
    private VirtualDir virtualDir;
    private static final String SEAICE_BAND_NAME_PREFIX = "MOD_Swath_Sea_Ice/Data Fields";
    private static final String GEO_BAND_NAME_PREFIX = "MODIS_Swath_Type_GEO/Geolocation Fields";

    private static final String AUTO_GROUPING_PATTERN = "MOD_Swath_Sea_Ice:MODIS_Swath_Type_GEO";

    protected Modis29ProductReader(Modis29ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        logger = Logger.getLogger(getClass().getSimpleName());
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        virtualDir = Modis29ProductReaderPlugIn.getInput(getInput());
        return createProduct();
    }

    private Product createProduct() throws IOException {
        Product seaiceInputProduct = null;
        Product geoInputProduct = null;
        try {
            final String[] productFileNames = virtualDir.list("");

            for (String fileName : productFileNames) {
                if (fileName.contains("MOD29") && seaiceFileNameMatches(fileName)) {
                    seaiceInputProduct = createSeaiceInputProduct(virtualDir.getFile(fileName));
                }
                if (fileName.contains("MOD03") && geoFileNameMatches(fileName)) {
                    geoInputProduct = createGeoInputProduct(virtualDir.getFile(fileName));
                }
            }
//            if (seaiceInputProduct != null) {
//                if (geoInputProduct == null) {
//                    // product was not included in the tar archive
//                    // alternatively, it is by convention expected in same directory as input product
//                    geoInputProduct = getGeoInputProductFromAuxdataFile(seaiceInputProduct.getName());
//                }
//            } else {
//                throw new IllegalStateException("Content of MOD29 Seaice product '" + getInputFile().getName() +
//                                                        "' incomplete or corrupt.");
//            }
        } catch (IOException e) {
            throw new IllegalStateException("MOD29 Seaice product '" + getInputFile().getName() +
                                                    "' cannot be read.");
        }

        Product product = new Product(getInputFile().getName(),
                                      seaiceInputProduct.getProductType(),
                                      seaiceInputProduct.getSceneRasterWidth(),
                                      seaiceInputProduct.getSceneRasterHeight(),
                                      this);

        product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
        product.getMetadataRoot().addElement(new MetadataElement("Variable_Attributes"));
        ProductUtils.copyMetadata(seaiceInputProduct.getMetadataRoot().getElement("Global_Attributes"),
                                  product.getMetadataRoot().getElement("Global_Attributes"));
        ProductUtils.copyMetadata(seaiceInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                                  product.getMetadataRoot().getElement("Variable_Attributes"));
        ProductUtils.copyMetadata(geoInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                                  product.getMetadataRoot().getElement("Variable_Attributes"));

        attachSeaiceDataToProduct(product, seaiceInputProduct);
        attachGeoCodingToProduct(product, geoInputProduct);
        product.setAutoGrouping(AUTO_GROUPING_PATTERN);

        return product;
    }

    static boolean geoFileNameMatches(String fileName) {
//        e.g. MOD03.A2002076.0140.005.2010080092148.hdf
        if (!(fileName.matches("MOD03.A[0-9]{7}.[0-9]{4}.[0-9]{3}.[0-9]{13}.(?i)(hdf)"))) {
            throw new IllegalArgumentException("Input MOD03 file name '" + fileName + "' does not match naming convention.");
        }
        return true;
    }

    static boolean seaiceFileNameMatches(String fileName) {
//        if (!(fileName.matches("MOD29.[0-9]{7}.[0-9]{4}.(?i)(hdf)"))) {
//        e.g. MOD29.A2002076.0140.005.2010080092148.hdf
        if (!(fileName.matches("MOD29.A[0-9]{7}.[0-9]{4}.[0-9]{3}.[0-9]{13}.(?i)(hdf)")) &&
                !(fileName.matches("MOD29.A[0-9]{7}.[0-9]{4}.[0-9]{3}.[0-9]{13}_HEGOUT.(?i)(hdf)"))) {
            throw new IllegalArgumentException("Input MOD29.5 file name '" + fileName + "' does not match naming convention.");
        }
        return true;
    }

    private Product createSeaiceInputProduct(File inputFile) {
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

    private Product createGeoInputProduct(File file) {
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

    private void attachSeaiceDataToProduct(Product product, Product albedoInputProduct) {
        for (final Band sourceBand : albedoInputProduct.getBands()) {
            if (sourceBand.getName().startsWith(SEAICE_BAND_NAME_PREFIX) &&
                    hasSameRasterDimension(product, albedoInputProduct)) {
                String bandName = sourceBand.getName();
                String targetBandName = bandName.substring(SEAICE_BAND_NAME_PREFIX.length() + 1, bandName.length());
                Band targetBand = ProductUtils.copyBand(bandName, albedoInputProduct, targetBandName, product, false);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }
    }

    private void attachGeoCodingToProduct(Product product, Product staticInputProduct) throws IOException {
        final Band latBand = staticInputProduct.getBand("MODIS_Swath_Type_GEO/Geolocation Fields/Latitude");
        final Band lonBand = staticInputProduct.getBand("MODIS_Swath_Type_GEO/Geolocation Fields/Longitude");

        String bandName = latBand.getName();
        String targetBandName = bandName.substring(GEO_BAND_NAME_PREFIX.length() + 1, bandName.length());
        Band targetBand = ProductUtils.copyBand(bandName, staticInputProduct, targetBandName, product, false);
        targetBand.setSourceImage(latBand.getSourceImage());

        bandName = lonBand.getName();
        targetBandName = bandName.substring(GEO_BAND_NAME_PREFIX.length() + 1, bandName.length());
        targetBand = ProductUtils.copyBand(lonBand.getName(), staticInputProduct, targetBandName, product, false);
        targetBand.setSourceImage(lonBand.getSourceImage());

        // implement specific GeoCoding which takes into account missing lat/lon data 'outside the Earth' ,
        // by using a LUT with: latlon <--> pixel for all pixels 'INside the Earth'
        // --> special solution for Meteosat, but general solution is still under discussion

        product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5));    // todo: does this work??
//        final Band latBandT = product.getBand("Navigation_Latitude");
//        latBandT.setValidPixelExpression("'Navigation_Latitude' != -9999.0");
//        final Band lonBandT = product.getBand("Navigation_Longitude");
//        lonBandT.setValidPixelExpression("'Navigation_Latitude' != -9999.0");
//        product.setGeoCoding(new MeteosatGeoCoding(latBandT, lonBandT, "MFG_0deg"));
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
