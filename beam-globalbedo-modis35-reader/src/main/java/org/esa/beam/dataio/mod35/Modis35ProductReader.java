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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.gpf.operators.standard.SubsetOp;
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

    private String inputFilePath;
    private String filename;

    public Modis35ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    // todo: write tests!!
    // todo: cleanup/remove unmodified classes  from module which are just copies from netcdf reader

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inFile = getInputFile();
        inputFilePath = inFile.getPath();
        filename = inFile.getName();

        // 1. read product with original RasterDigest: get the variables on 5km grid
        Product tiePoints5kmProduct = readProduct(Modis35Constants.LOW_RES_MODE);

        // 2. read product with modified RasterDigest to get cloud mask on high res 1km grid, skip weird metadata
        Product cloudMaskFirstProduct = readProduct(Modis35Constants.HIGH_RES_MODE);

        // 3. make subset: bands (first 2 bytes of cloudmask) and grid (cut residual columns in x-dir)
        Map<String, Object> subsetParams = new HashMap<>();
        final int subsetWidth = tiePoints5kmProduct.getSceneRasterWidth() * Modis35Constants.MOD35_SCALE_FACTOR;
        final int subsetHeight = tiePoints5kmProduct.getSceneRasterHeight() * Modis35Constants.MOD35_SCALE_FACTOR;
        final Rectangle subsetRegion = new Rectangle(0, 0, subsetWidth, subsetHeight);
        subsetParams.put("region", subsetRegion);
        String[] bandNames = new String[]{"Cloud_Mask_Byte_Segment1", "Cloud_Mask_Byte_Segment2"};
        subsetParams.put("bandNames", bandNames);
        subsetParams.put("copyMetadata", true);
        Product cloudMaskSubsetProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(SubsetOp.class), subsetParams, cloudMaskFirstProduct);

        // 4. upscale low res product with factor 5.0 to get same grid as subsetted cloud mask product (use ScaleImageOp)
        ScaleImageOp scaleImageOp = new ScaleImageOp();
        scaleImageOp.setParameterDefaultValues();
        Map<String, Object> scaleParams = new HashMap<>();
        final double mod35ScaleFactor = Modis35Constants.MOD35_SCALE_FACTOR;
        scaleParams.put("scaleFactor", mod35ScaleFactor);
        Product tiePoints1kmProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(ScaleImageOp.class), scaleParams, tiePoints5kmProduct);

        // 5. merge the two products
        Mod35MergeOp mergeOp = new Mod35MergeOp();
        mergeOp.setParameterDefaultValues();
        mergeOp.setSourceProduct("masterProduct", tiePoints1kmProduct);
        mergeOp.setSourceProduct("slaveProduct", cloudMaskSubsetProduct);
        Product finalProduct = mergeOp.getTargetProduct();

        // 6. add geocoding to final product
        final PixelGeoCoding pixelGeoCoding =
                new PixelGeoCoding(finalProduct.getBand("Latitude"), finalProduct.getBand("Longitude"), "", 4);
        finalProduct.setGeoCoding(pixelGeoCoding);

        // todo: remove unused variables from product metadata!

        // return the final product
        return finalProduct;
    }

    private synchronized Product readProduct(int resolutionMode) throws IOException {
        NetcdfFile netcdfFile = Modis35NetcdfFileOpener.open(inputFilePath);
        if (netcdfFile == null) {
            throw new IOException("Failed to open file: " + inputFilePath);
        }

        Modis35ProfileReadContext context = new Modis35ProfileReadContextImpl(netcdfFile);
        context.setProperty(Modis35Constants.PRODUCT_FILENAME_PROPERTY, filename);
        initReadContext(context, resolutionMode);
        Modis35NetCdfReadProfile profile = new Modis35NetCdfReadProfile();
        configureProfile(profile);
        return profile.readProduct(context);
    }

    private void initReadContext(Modis35ProfileReadContext ctx, int resolutionMode) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        final Modis35RasterDigest rasterDigest =
                Modis35RasterDigest.createRasterDigest(resolutionMode, netcdfFile.getRootGroup());
        if (rasterDigest == null) {
            throw new IOException("File does not contain any bands.");
        }
        ctx.setRasterDigest(rasterDigest);
    }

    private void configureProfile(Modis35NetCdfReadProfile profile) {
        profile.setInitialisationPartReader(new Modis35CfInitialisationPart());
        profile.addProfilePartReader(new Modis35CfMetadataPart());
        profile.addProfilePartReader(new Modis35CfBandPart());
        profile.addProfilePartReader(new Modis35CfTiePointGridPart());
        profile.addProfilePartReader(new Modis35CfFlagCodingPart());
//        profile.addProfilePartReader(new Modis35CfGeocodingPart());
        profile.addProfilePartReader(new Modis35CfTimePart());
        profile.addProfilePartReader(new Modis35CfDescriptionPart());
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

