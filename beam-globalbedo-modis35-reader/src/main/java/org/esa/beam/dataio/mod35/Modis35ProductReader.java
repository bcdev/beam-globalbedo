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
        Mod35BitMaskUtils.attachPixelClassificationFlagBand(finalProduct);

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

