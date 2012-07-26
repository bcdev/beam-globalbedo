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

package org.esa.beam.dataio.msg;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.MeteosatGeoCoding;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Product reader responsible for reading Meteosat MSA data products in HDF format.
 *
 * @author Olaf Danne
 * @since 1.0
 */
public class MsgMSAProductReader extends AbstractProductReader {

    private final Logger logger;
    private VirtualDir virtualDir;
    private static final String ALBEDO_BAND_NAME_PREFIX = "AL-";
    private static final String QUALITY_FLAG_BAND_NAME = "Q-Flag";
    private static final int F_LAND_OR_CONTINENTAL_WATER = 0;

    private static final int F_SPACE_OR_CONTINENTAL_WATER = 1;
    private static final int F_MSG_OBSERVATIONS = 2;
    private static final int F_EPS_OBSERVATIONS = 3;
    private static final int F_EXTERNAL_INFORMATION = 4;
    private static final int F_SNOW = 5;
    private static final int F_UNUSED = 6;
    private static final int F_FAILURE = 7;

    static final String MSA_ALBEDO_BZ2_FILENAME_REGEXP = "HDF5_LSASAF_MSG_ALBEDO_[a-zA-Z]{4}_[0-9]{12}.(?i)(bz2)";
    static final String MSA_ALBEDO_HDF_FILENAME_REGEXP = "HDF5_LSASAF_MSG_ALBEDO_[a-zA-Z]{4}_[0-9]{12}.(?i)(hdf)";
    static final String MSA_ALBEDO_NOEXT_FILENAME_REGEXP = "HDF5_LSASAF_MSG_ALBEDO_[a-zA-Z]{4}_[0-9]{12}";
    static final String MSA_LONGITUDE_HDF_FILENAME_REGEXP = "HDF5_LSASAF_MSG_LON_[a-zA-Z]{4}_[0-9]{12}";
    static final String MSA_LATITUDE_HDF_FILENAME_REGEXP = "HDF5_LSASAF_MSG_LAT_[a-zA-Z]{4}_[0-9]{12}";

    protected MsgMSAProductReader(MsgMSAProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        logger = Logger.getLogger(getClass().getSimpleName());
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        virtualDir = MsgMSAProductReaderPlugIn.getInput(getInput());
        return createProduct();
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

    static boolean msgLatInputFileNameMatches(String fileName) {
        return (fileName.matches(MSA_LATITUDE_HDF_FILENAME_REGEXP));
    }

    static boolean msgLonInputFileNameMatches(String fileName) {
        return (fileName.matches(MSA_LONGITUDE_HDF_FILENAME_REGEXP));
    }

    static boolean msgAlbedoFileNameMatches(String fileName) {
        if (!(fileName.matches(MSA_ALBEDO_HDF_FILENAME_REGEXP))) {
            throw new IllegalArgumentException("Input file name '" + fileName +
                                                       "' does not match naming convention: 'HDF5_LSASAF_MSG_ALBEDO_<area>_yyyymmddhhmm'");
        }
        return true;
    }

    static String getRegionFromAlbedoInputFilename(String albedoInputFilename) {
        // region rrrr from 'HDF5_LSASAF_MSG_ALBEDO_<rrrr>'  (Euro, NAfr, SAfr)
        return albedoInputFilename.substring(23, 27);
    }

    private Product createProduct() throws IOException {
        Product albedoInputProduct = null;
        Product latInputProduct;
        Product lonInputProduct;
        try {
            final String[] productFileNames = virtualDir.list("");   // should contain only one file

            for (String fileName : productFileNames) {
                if (fileName.contains("ALBEDO") && msgAlbedoFileNameMatches(fileName)) {
                    albedoInputProduct = createAlbedoInputProduct(virtualDir.getFile(fileName));
                }
            }
            if (albedoInputProduct != null) {
                String region = getRegionFromAlbedoInputFilename(albedoInputProduct.getName());
                // lat/lon files are by convention expected in same directory as albedo input product
                latInputProduct = getLatInputProductFromSeparateFile(region);
                // lat/lon files are by convention expected in same directory as albedo input product
                lonInputProduct = getLonInputProductFromSeparateFile(region);
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
        ProductUtils.copyMetadata(latInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                                  product.getMetadataRoot().getElement("Variable_Attributes"));
        ProductUtils.copyMetadata(lonInputProduct.getMetadataRoot().getElement("Variable_Attributes"),
                                  product.getMetadataRoot().getElement("Variable_Attributes"));

        attachAlbedoDataToProduct(product, albedoInputProduct);
        attachGeoInfoToProduct(product, latInputProduct, lonInputProduct);

        setupQualityFlagBitmasks(product);

        return product;
    }

    private Product getLatInputProductFromSeparateFile(String region) throws IOException {
        String latInputFileName = null;
        final String[] allInputFiles = getInputFile().getParentFile().list();
        for (String anyInputFile : allInputFiles) {
            String filenameWithoutExtension = FileUtils.getFilenameWithoutExtension(anyInputFile);
            if (filenameWithoutExtension.contains(region) && msgLatInputFileNameMatches(filenameWithoutExtension)) {
                latInputFileName = filenameWithoutExtension + ".bz2";
                break;
            }
        }
        if (latInputFileName == null) {
            close();
            throw new IllegalStateException("Geolocation latitude file missing - cannot open Meteosat Surface Albedo product,");
        }

        return createLatLonInputProduct(latInputFileName);
    }

    private Product getLonInputProductFromSeparateFile(String region) throws IOException {
        String lonInputFileName = null;
        final String[] allInputFiles = getInputFile().getParentFile().list();
        for (String anyInputFile : allInputFiles) {
            String filenameWithoutExtension = FileUtils.getFilenameWithoutExtension(anyInputFile);
            if (filenameWithoutExtension.contains(region) && msgLonInputFileNameMatches(filenameWithoutExtension)) {
                lonInputFileName = FileUtils.getFilenameWithoutExtension(anyInputFile) + ".bz2";
                break;
            }
        }
        if (lonInputFileName == null) {
            close();
            throw new IllegalStateException("Geolocation longitude file missing - cannot open Meteosat Surface Albedo product,");
        }

        return createLatLonInputProduct(lonInputFileName);
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

    private Product createLatLonInputProduct(String filename) {
        Product latLonProduct = null;
        final String inputParentDirectory = getInputFile().getParent();
        File file = new File(inputParentDirectory + File.separator + filename);
        try {
            latLonProduct = ProductIO.readProduct(file);
            if (latLonProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                                           file.getName());
                logger.log(Level.WARNING, msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", file.getName());
            logger.log(Level.WARNING, msg, e);
        }
        return latLonProduct;
    }

    private void attachAlbedoDataToProduct(Product product, Product albedoInputProduct) {
        if (hasSameRasterDimension(product, albedoInputProduct)) {
            for (final Band sourceBand : albedoInputProduct.getBands()) {
                String bandName = sourceBand.getName();
                Band targetBand = ProductUtils.copyBand(bandName, albedoInputProduct, product, true);
                if (sourceBand.getName().startsWith(ALBEDO_BAND_NAME_PREFIX)) {
                    targetBand.setScalingFactor(0.0001);
                } else if (sourceBand.getName().equals(QUALITY_FLAG_BAND_NAME)) {
                    final FlagCoding qualityFlagCoding = createQualityFlagCoding(QUALITY_FLAG_BAND_NAME);
                    targetBand.setSampleCoding(qualityFlagCoding);
                    product.getFlagCodingGroup().add(qualityFlagCoding);
                }
            }
        }
    }

    private void attachGeoInfoToProduct(Product product, Product latInputProduct, Product lonInputProduct) throws IOException {
        final Band latBand = latInputProduct.getBand("LAT");
        final Band lonBand = lonInputProduct.getBand("LON");
        latBand.setScalingFactor(0.01);
        lonBand.setScalingFactor(0.01);

        ProductUtils.copyBand(latBand.getName(), latInputProduct, product, true);
        ProductUtils.copyBand(lonBand.getName(), lonInputProduct, product, true);

        // use specific MeteosatGeoCoding which takes into account missing lat/lon data 'outside the Earth' ,
        // by using a LUT with: latlon <--> pixel for all pixels 'INside the Earth'
        // --> special solution for Meteosat, but general solution is still under discussion
        // PixelGeoCoding needs lat and lon from same product!
        final Band latBandT = product.getBand("LAT");
        latBandT.setValidPixelExpression("LAT != 90 && LON != 90");
        final Band lonBandT = product.getBand("LON");
        lonBandT.setValidPixelExpression("LAT != 90 && LON != 90");
//        product.setGeoCoding(new PixelGeoCoding(latBandT, lonBandT, null, 50));    // this does not work correctly!
        product.setGeoCoding(new MeteosatGeoCoding(latBandT, lonBandT, "MSG_Euro"));
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

    private static FlagCoding createQualityFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_LAND_OR_CONTINENTAL_WATER", BitSetter.setFlag(0, F_LAND_OR_CONTINENTAL_WATER), null);
        flagCoding.addFlag("F_SPACE_OR_CONTINENTAL_WATER", BitSetter.setFlag(0, F_SPACE_OR_CONTINENTAL_WATER), null);
        flagCoding.addFlag("F_MSG_OBSERVATIONS", BitSetter.setFlag(0, F_MSG_OBSERVATIONS), null);
        flagCoding.addFlag("F_EPS_OBSERVATIONS", BitSetter.setFlag(0, F_EPS_OBSERVATIONS), null);
        flagCoding.addFlag("F_EXTERNAL_INFORMATION", BitSetter.setFlag(0, F_EXTERNAL_INFORMATION), null);
        flagCoding.addFlag("F_SNOW", BitSetter.setFlag(0, F_SNOW), null);
        flagCoding.addFlag("F_UNUSED", BitSetter.setFlag(0, F_UNUSED), null);
        flagCoding.addFlag("F_FAILURE", BitSetter.setFlag(0, F_FAILURE), null);

        return flagCoding;
    }

    private static int setupQualityFlagBitmasks(Product msaProduct) {

        int index = 0;
        int w = msaProduct.getSceneRasterWidth();
        int h = msaProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("F_LAND_OR_CONTINENTAL_WATER", "Land or continental water pixels", w, h,
                                         "'Q-Flag.F_LAND_OR_CONTINENTAL_WATER'", Color.GREEN, 0.5f);
        msaProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_SPACE_OR_CONTINENTAL_WATER", "Space (outside disk) or continental water pixels", w, h,
                                         "'Q-Flag.F_SPACE_OR_CONTINENTAL_WATER'", Color.BLUE, 0.5f);
        msaProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_MSG_OBSERVATIONS", "Pixel has MSG observations", w, h,
                                         "'Q-Flag.F_MSG_OBSERVATIONS'", Color.YELLOW, 0.5f);
        msaProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_EPS_OBSERVATIONS", "Pixel has EPS observations", w, h,
                                         "'Q-Flag.F_EPS_OBSERVATIONS'", Color.CYAN, 0.5f);
        msaProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_EXTERNAL_INFORMATION", "Pixel has external information", w, h,
                                         "'Q-Flag.F_EXTERNAL_INFORMATION'", Color.ORANGE, 0.5f);
        msaProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_SNOW", "Snow pixels", w, h,
                                         "'Q-Flag.F_SNOW'", Color.MAGENTA, 0.5f);
        msaProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_UNUSED", "Pixel was not used", w, h,
                                         "'Q-Flag.F_UNUSED'", Color.DARK_GRAY, 0.5f);
        msaProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_FAILURE", "Algorithm failed", w, h,
                                         "'Q-Flag.F_FAILURE'", Color.RED, 0.5f);
        msaProduct.getMaskGroup().add(index++, mask);

        return index;
    }

}
