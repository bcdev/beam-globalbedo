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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Product reader responsible for reading MISR data products in HDF format.
 *
 * @author Olaf Danne
 */
public class MisrProductReader extends AbstractProductReader {

    private final Logger logger;
    private VirtualDir virtualDir;
    private static final String RED_RADIANCE_BAND_NAME_PREFIX = "Red";

    protected MisrProductReader(org.esa.beam.dataio.MisrProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        logger = Logger.getLogger(getClass().getSimpleName());
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        virtualDir = org.esa.beam.dataio.MisrProductReaderPlugIn.getInput(getInput());
        return createProduct();
    }

    private Product createProduct() throws IOException {
        Product misrInputProduct = null;
        try {
            final String[] productFileNames = virtualDir.list("");

            for (String fileName : productFileNames) {
                if (fileName.endsWith("dim") && misrFileNameMatches(fileName)) {
                    misrInputProduct = createMisrInputProduct(virtualDir.getFile(fileName));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("MISR product '" + getInputFile().getName() +
                    "' cannot be read.");
        }

        if (misrInputProduct != null) {
            Product product = new Product(getInputFile().getName(),
                    misrInputProduct.getProductType(),
                    misrInputProduct.getSceneRasterWidth(),
                    misrInputProduct.getSceneRasterHeight(),
                    this);

            product.getMetadataRoot().addElement(new MetadataElement("Global_Attributes"));
            product.getMetadataRoot().addElement(new MetadataElement("Variable_Attributes"));
            ProductUtils.copyMetadata(misrInputProduct.getMetadataRoot().getElement("Global_Attributes"),
                    product.getMetadataRoot().getElement("Global_Attributes"));
            ProductUtils.copyMetadata(misrInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                    product.getMetadataRoot().getElement("Variable_Attributes"));

            attachMisrDataToProduct(product, misrInputProduct);
            attachGeoCodingToProduct(product, misrInputProduct);
            return product;
        } else {
            throw new IllegalStateException("No MISR input product found, or cannot be read.");
        }
    }

    static boolean misrFileNameMatches(String fileName) {
        //      e.g.  MISR_AM1_GRP_ELLIPSOID_GM_P119_O061188_AA_F03_0024.dim
        if (!(fileName.matches("MISR_AM1_GRP_ELLIPSOID_GM_P[0-9]{3}_O[0-9]{6}_AA_F[0-9]{2}_[0-9]{4}.(?i)(dim)")) &&
                !(fileName.matches("MISR_AM1_GRP_ELLIPSOID_GM_P[0-9]{3}_O[0-9]{6}_AA_F[0-9]{2}_[0-9]{4}_small.(?i)(dim)"))) {
            throw new IllegalArgumentException("Input MISR Dimap file name '" + fileName + "' does not match naming convention.");
        }
        return true;
    }

    private Product createMisrInputProduct(File inputFile) {
        Product misrProduct = null;
        try {
            misrProduct = ProductIO.readProduct(inputFile);
            if (misrProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                        inputFile.getName());
                logger.log(Level.WARNING, msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", inputFile.getName());
            logger.log(Level.WARNING, msg, e);
        }
        return misrProduct;
    }

    private void attachMisrDataToProduct(Product product, Product misrInputProduct) {
        for (final Band sourceBand : misrInputProduct.getBands()) {
            if (sourceBand.getName().startsWith(RED_RADIANCE_BAND_NAME_PREFIX)) {
                final String bandName = sourceBand.getName();
                final String targetBandName = bandName;
                Band targetBand = ProductUtils.copyBand(bandName, misrInputProduct, targetBandName, product, false);
                targetBand.setSourceImage(sourceBand.getSourceImage());
            }
        }
    }

    private void attachGeoCodingToProduct(Product product, Product misrInputProduct) throws IOException {
        final Band latBand = misrInputProduct.getBand("latitude");
        final Band lonBand = misrInputProduct.getBand("longitude");

        String bandName = latBand.getName();
        String targetBandName = bandName;
        Band targetBand = ProductUtils.copyBand(bandName, misrInputProduct, targetBandName, product, false);
        targetBand.setSourceImage(latBand.getSourceImage());
        targetBand.setValidPixelExpression("'latitude' != -99999.0");

        bandName = lonBand.getName();
        targetBandName = bandName;
        targetBand = ProductUtils.copyBand(lonBand.getName(), misrInputProduct, targetBandName, product, false);
        targetBand.setSourceImage(lonBand.getSourceImage());
        targetBand.setValidPixelExpression("'longitude' != -99999.0");

        // implement specific GeoCoding which takes into account missing lat/lon data 'outside the Earth' ,
        // by using a LUT with: latlon <--> pixel for all pixels 'INside the Earth'
        // --> special solution for Meteosat, but general solution is still under discussion
//        product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 5));    // this does not work correctly!
        // todo!
        final Band latBandT = product.getBand("latitude");
        latBandT.setValidPixelExpression("'latitude' != -99999.0");
        final Band lonBandT = product.getBand("longitude");
        lonBandT.setValidPixelExpression("'longitude' != -99999.0");
        product.setGeoCoding(new MisrGeoCoding(latBandT, lonBandT, ""));
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
