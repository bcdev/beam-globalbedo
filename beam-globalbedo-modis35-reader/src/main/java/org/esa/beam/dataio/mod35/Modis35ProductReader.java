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

package org.esa.beam.dataio.mod35;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.Modis35NetCdfReadProfile;
import org.esa.beam.dataio.netcdf.Modis35ProfileReadContext;
import org.esa.beam.dataio.netcdf.Modis35ProfileReadContextImpl;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.*;
import org.esa.beam.dataio.netcdf.util.*;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.util.BitSetter;
import ucar.nc2.NetcdfFile;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Product reader responsible for reading MODIS MOD35 cloud mask HDF product.
 *
 * @author Olaf Danne
 */
public class Modis35ProductReader extends AbstractProductReader {

    private static final String MOD35_CLOUD_DETERMINED_FLAG_NAME = "CLOUD_DETERMINED";
    private static final String MOD35_CLOUD_CERTAIN_FLAG_NAME = "CLOUD_CERTAIN";
    private static final String MOD35_CLOUD_UNCERTAIN_FLAG_NAME = "CLOUD_PROBABLY";
    private static final String MOD35_PROBABLY_CLEAR_FLAG_NAME = "CLEAR_PROBABLY";
    private static final String MOD35_CONFIDENT_CLEAR_FLAG_NAME = "CLEAR_CERTAIN";
    private static final String MOD35_DAYTIME_FLAG_NAME = "DAYTIME";
    private static final String MOD35_GLINT_FLAG_NAME = "GLINT";
    private static final String MOD35_SNOW_ICE_FLAG_NAME = "SNOW_ICE";
    private static final String MOD35_WATER_FLAG_NAME = "WATER";
    private static final String MOD35_COASTAL_FLAG_NAME = "COAST";
    private static final String MOD35_DESERT_FLAG_NAME = "DESERT";
    private static final String MOD35_LAND_FLAG_NAME = "LAND";

    private static final int CLOUD_DETERMINED_BIT_INDEX = 0;
    private static final int CLOUD_CERTAIN_BIT_INDEX = 1;
    private static final int CLOUD_UNCERTAIN_BIT_INDEX = 2;
    private static final int CLOUD_PROBABLY_CLEAR_BIT_INDEX = 3;
    private static final int CLOUD_CONFIDENT_CLEAR_BIT_INDEX = 4;
    private static final int DAYTIME_BIT_INDEX = 5;
    private static final int GLINT_BIT_INDEX = 6;
    private static final int SNOW_ICE_BIT_INDEX = 7;
    private static final int WATER_BIT_INDEX = 8;
    private static final int COASTAL_BIT_INDEX = 9;
    private static final int DESERT_BIT_INDEX = 10;
    private static final int LAND_BIT_INDEX = 11;

    private String MOD35_CLOUD_DETERMINED_FLAG_DESCR = "Cloud mask was determined for this pixel";
    private String MOD35_CLOUD_CERTAIN_FLAG_DESCR = "Certainly cloudy pixel";
    private String MOD35_CLOUD_UNCERTAIN_FLAG_DESCR = "Probably cloudy pixel";
    private String MOD35_PROBABLY_CLEAR_FLAG_DESCR = "Probably clear pixel";
    private String MOD35_CERTAINLY_CLEAR_FLAG_DESCR = "Certainly clear pixel";
    private String MOD35_DAYTIME_FLAG_DESCR = "Daytime pixel";
    private String MOD35_GLINT_FLAG_DESCR = "Glint pixel";
    private String MOD35_SNOW_ICE_FLAG_DESCR = "Snow/ice pixel";
    private String MOD35_WATER_FLAG_DESCR = "Water pixel";
    private String MOD35_COASTAL_FLAG_DESCR = "Coastal pixel";
    private String MOD35_DESERT_FLAG_DESCR = "Desert pixel";
    private String MOD35_LAND_FLAG_DESCR = "Land pixel";

    private String inputFilePath;

    private String filename;
    public static final String PIXEL_CLASSIF_FLAG_BAND_NAME = "cloudmask_flags";

    public static final String QA_FLAG_BAND_NAME = "qa_flags";

    public Modis35ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    // todo: cleanup/remove unmodified classes from module which are just copies from netcdf reader

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inFile = getInputFile();
        inputFilePath = inFile.getPath();
        filename = inFile.getName();

        // 1. read product with original RasterDigest: get the variables on 5km grid
        Product tiePoints5kmProduct = readProduct(Modis35Constants.LOW_RES_RASTER_DIM_NAMES);

        // 2. read product with modified RasterDigest to get cloud mask on high res 1km grid, skip weird metadata
        Product cloudMaskFirstProduct = readProduct(Modis35Constants.HIGH_RES_CLOUDMASK_RASTER_DIM_NAMES);

        // 3. read product with modified RasterDigest to get QA band on high res 1km grid, skip weird metadata
        Product qualityAssuranceProduct = readProduct(Modis35Constants.HIGH_RES_QA_RASTER_DIM_NAMES);

        // 4. make subsets: cut residual columns in x-dir
        final int subsetWidth = tiePoints5kmProduct.getSceneRasterWidth() * Modis35Constants.MOD35_SCALE_FACTOR;
        final int subsetHeight = tiePoints5kmProduct.getSceneRasterHeight() * Modis35Constants.MOD35_SCALE_FACTOR;
        final Rectangle subsetRegion = new Rectangle(0, 0, subsetWidth, subsetHeight);

        Map<String, Object> cloudMaskSubsetParams = new HashMap<>();
        cloudMaskSubsetParams.put("region", subsetRegion);
        cloudMaskSubsetParams.put("copyMetadata", true);
        cloudMaskSubsetParams.put("bandNames", Modis35Constants.CLOUD_MASK_BAND_NAMES);
        Product cloudMaskSubsetProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SubsetOp.class),
                cloudMaskSubsetParams, cloudMaskFirstProduct);

        Map<String, Object> qualityAssuranceSubsetParams = new HashMap<>();
        qualityAssuranceSubsetParams.put("region", subsetRegion);
        qualityAssuranceSubsetParams.put("copyMetadata", true);
        qualityAssuranceSubsetParams.put("bandNames", Modis35Constants.QUALITY_ASSURANCE_BAND_NAMES);
        Product qualityAssuranceSubsetProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SubsetOp.class),
                qualityAssuranceSubsetParams, qualityAssuranceProduct);

        // 5. upscale low res product with factor 5.0 to get same grid as sub-set cloud mask product (use ScaleImageOp)
        ScaleImageOp scaleImageOp = new ScaleImageOp();
        scaleImageOp.setParameterDefaultValues();
        Map<String, Object> scaleParams = new HashMap<>();
        final double mod35ScaleFactor = Modis35Constants.MOD35_SCALE_FACTOR;
        scaleParams.put("scaleFactor", mod35ScaleFactor);
        Product tiePoints1kmProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(ScaleImageOp.class), scaleParams, tiePoints5kmProduct);

        // 6. merge the products
        // first, merge cloudMaskSubsetProduct to tiePoints1kmProduct
        Mod35MergeOp mergeOp1 = new Mod35MergeOp();
        mergeOp1.setParameterDefaultValues();
        mergeOp1.setSourceProduct("masterProduct", tiePoints1kmProduct);
        mergeOp1.setSourceProduct("slaveProduct", cloudMaskSubsetProduct);
        Product mergeProduct1 = mergeOp1.getTargetProduct();

        // then, merge qualityAssuranceSubsetProduct to first merge product
        Mod35MergeOp mergeOp2 = new Mod35MergeOp();
        mergeOp2.setParameterDefaultValues();
        mergeOp2.setSourceProduct("masterProduct", mergeProduct1);
        mergeOp2.setSourceProduct("slaveProduct", qualityAssuranceSubsetProduct);
        Product finalProduct = mergeOp2.getTargetProduct();

        // 7. add geocoding to final product
        final PixelGeoCoding pixelGeoCoding =
                new PixelGeoCoding(tiePoints1kmProduct.getBand("Latitude"), tiePoints1kmProduct.getBand("Longitude"), "", 4);
        finalProduct.setGeoCoding(pixelGeoCoding);

        finalProduct.setAutoGrouping("Cloud_Mask:Quality_Assurance");

        // todo: we need to extract properly the cloud and quality info from the single bits of the
        // Cloud_Mask and Quality_Assurance bytes. Also, corresponding bit masks in Visat would be nice.

        // return the final product
        return finalProduct;
    }

    private synchronized Product readProduct(String rasterDimNames) throws IOException {
        NetcdfFile netcdfFile = NetcdfFileOpener.open(inputFilePath);
        if (netcdfFile == null) {
            throw new IOException("Failed to open file: " + inputFilePath);
        }

        Modis35ProfileReadContext context = new Modis35ProfileReadContextImpl(netcdfFile);
        context.setProperty(Modis35Constants.PRODUCT_FILENAME_PROPERTY, filename);
        initReadContext(context, rasterDimNames);
        Modis35NetCdfReadProfile profile = new Modis35NetCdfReadProfile();
        configureProfile(profile);
        return profile.readProduct(context);
    }

    private void initReadContext(Modis35ProfileReadContext ctx, String rasterDimNames) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final Modis35RasterDigest rasterDigest =
                Modis35RasterDigest.createRasterDigest(rasterDimNames, netcdfFile.getRootGroup());
        if (rasterDigest == null) {
            throw new IOException("File does not contain any bands.");
        }
        ctx.setRasterDigest(rasterDigest);
    }

    private void configureProfile(Modis35NetCdfReadProfile profile) {
        profile.setInitialisationPartReader(new Modis35CfInitialisationPart());
        profile.addProfilePartReader(new Modis35CfMetadataPart());
        profile.addProfilePartReader(new Modis35CfBandPart());
        profile.addProfilePartReader(new Modis35CfTimePart());
        profile.addProfilePartReader(new Modis35CfDescriptionPart());
    }

    private void attachPixelClassificationFlagBand(Product mod35Product) {
        FlagCoding mod35FC = new FlagCoding(PIXEL_CLASSIF_FLAG_BAND_NAME);

        mod35FC.addFlag(MOD35_CLOUD_DETERMINED_FLAG_NAME, BitSetter.setFlag(0, CLOUD_DETERMINED_BIT_INDEX),
                        MOD35_CLOUD_DETERMINED_FLAG_DESCR);
        mod35FC.addFlag(MOD35_CLOUD_CERTAIN_FLAG_NAME, BitSetter.setFlag(0, CLOUD_CERTAIN_BIT_INDEX),
                        MOD35_CLOUD_CERTAIN_FLAG_DESCR);
        mod35FC.addFlag(MOD35_CLOUD_UNCERTAIN_FLAG_NAME, BitSetter.setFlag(0, CLOUD_UNCERTAIN_BIT_INDEX),
                        MOD35_CLOUD_UNCERTAIN_FLAG_DESCR);
        mod35FC.addFlag(MOD35_PROBABLY_CLEAR_FLAG_NAME, BitSetter.setFlag(0, CLOUD_PROBABLY_CLEAR_BIT_INDEX),
                        MOD35_PROBABLY_CLEAR_FLAG_DESCR);
        mod35FC.addFlag(MOD35_CONFIDENT_CLEAR_FLAG_NAME, BitSetter.setFlag(0, CLOUD_CONFIDENT_CLEAR_BIT_INDEX),
                        MOD35_CERTAINLY_CLEAR_FLAG_DESCR);
        mod35FC.addFlag(MOD35_DAYTIME_FLAG_NAME, BitSetter.setFlag(0, DAYTIME_BIT_INDEX),
                        MOD35_DAYTIME_FLAG_DESCR);
        mod35FC.addFlag(MOD35_GLINT_FLAG_NAME, BitSetter.setFlag(0, GLINT_BIT_INDEX),
                        MOD35_GLINT_FLAG_DESCR);
        mod35FC.addFlag(MOD35_SNOW_ICE_FLAG_NAME, BitSetter.setFlag(0, SNOW_ICE_BIT_INDEX),
                        MOD35_SNOW_ICE_FLAG_DESCR);
        mod35FC.addFlag(MOD35_WATER_FLAG_NAME, BitSetter.setFlag(0, WATER_BIT_INDEX),
                        MOD35_WATER_FLAG_DESCR);
        mod35FC.addFlag(MOD35_COASTAL_FLAG_NAME, BitSetter.setFlag(0, COASTAL_BIT_INDEX),
                        MOD35_COASTAL_FLAG_DESCR);
        mod35FC.addFlag(MOD35_DESERT_FLAG_NAME, BitSetter.setFlag(0, DESERT_BIT_INDEX),
                        MOD35_DESERT_FLAG_DESCR);
        mod35FC.addFlag(MOD35_LAND_FLAG_NAME, BitSetter.setFlag(0, LAND_BIT_INDEX),
                        MOD35_LAND_FLAG_DESCR);

        ProductNodeGroup<Mask> maskGroup = mod35Product.getMaskGroup();
        addMask(mod35Product, maskGroup, MOD35_CLOUD_DETERMINED_FLAG_NAME, MOD35_CLOUD_DETERMINED_FLAG_DESCR, new Color(238, 223, 145), 0.0f);
        addMask(mod35Product, maskGroup, MOD35_CLOUD_CERTAIN_FLAG_NAME, MOD35_CLOUD_CERTAIN_FLAG_DESCR, Color.GREEN, 0.5f);
        addMask(mod35Product, maskGroup, MOD35_CLOUD_UNCERTAIN_FLAG_NAME, MOD35_CLOUD_UNCERTAIN_FLAG_DESCR, Color.white, 0.0f);
        addMask(mod35Product, maskGroup, MOD35_PROBABLY_CLEAR_FLAG_NAME, MOD35_PROBABLY_CLEAR_FLAG_DESCR, Color.YELLOW, 0.5f);
        addMask(mod35Product, maskGroup, MOD35_CONFIDENT_CLEAR_FLAG_NAME, MOD35_CERTAINLY_CLEAR_FLAG_DESCR, Color.RED, 0.5f);
        addMask(mod35Product, maskGroup, MOD35_DAYTIME_FLAG_NAME, MOD35_DAYTIME_FLAG_DESCR, Color.BLUE, 0.5f);
        addMask(mod35Product, maskGroup, MOD35_GLINT_FLAG_NAME, MOD35_GLINT_FLAG_DESCR, Color.CYAN, 0.5f);
        addMask(mod35Product, maskGroup, MOD35_SNOW_ICE_FLAG_NAME, MOD35_SNOW_ICE_FLAG_DESCR, Color.GREEN.darker().darker(), 0.5f);
        addMask(mod35Product, maskGroup, MOD35_WATER_FLAG_NAME, MOD35_WATER_FLAG_DESCR, Color.pink, 0.5f);
        addMask(mod35Product, maskGroup, MOD35_COASTAL_FLAG_NAME, MOD35_COASTAL_FLAG_DESCR, Color.pink, 0.5f);
        addMask(mod35Product, maskGroup, MOD35_DESERT_FLAG_NAME, MOD35_DESERT_FLAG_DESCR, Color.pink, 0.5f);
        addMask(mod35Product, maskGroup, MOD35_LAND_FLAG_NAME, MOD35_LAND_FLAG_DESCR, Color.pink, 0.5f);

        mod35Product.getFlagCodingGroup().add(mod35FC);
        final Band pixelClassifBand = mod35Product.addBand(PIXEL_CLASSIF_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        pixelClassifBand.setDescription("MOD35 pixel classification");
        pixelClassifBand.setSampleCoding(mod35FC);

        fillPixelClassifBand(pixelClassifBand, mod35Product.getBand(Modis35Constants.CLOUD_MASK_BAND_NAMES[0]));
    }

    private void fillPixelClassifBand(Band pixelClassifBand, Band clmaskByte1Band) {
        // todo

    }

    private void addMask(Product mod35Product, ProductNodeGroup<Mask> maskGroup, String flagName, String description, Color color,
                         float transparency) {
        int width = mod35Product.getSceneRasterWidth();
        int height = mod35Product.getSceneRasterHeight();
        String maskPrefix = "mod35_";
        Mask mask = Mask.BandMathsType.create(maskPrefix + flagName.toLowerCase(),
                                              description, width, height,
                                              PIXEL_CLASSIF_FLAG_BAND_NAME + "." + flagName,
                                              color, transparency);
        maskGroup.add(mask);
    }



    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException(String.format("No source to read from for band '%s'.", destBand.getName()));
    }

    private File getInputFile() {
        File inFile;
        if (getInput() instanceof String) {
            inFile = new File((String) getInput());
        } else if (getInput() instanceof File) {
            inFile = (File) getInput();
        } else {
            throw new IllegalArgumentException("unsupported input source: " + getInput());
        }
        return inFile;
    }

}

