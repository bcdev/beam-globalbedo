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
import org.esa.beam.dataio.modis.ModisConstants;
import org.esa.beam.dataio.modis.ModisGlobalAttributes;
import org.esa.beam.dataio.modis.attribute.ImappAttributes;
import org.esa.beam.dataio.modis.bandreader.ModisBandReader;
import org.esa.beam.dataio.modis.bandreader.ModisInt8BandReader;
import org.esa.beam.dataio.modis.netcdf.NetCDFAttributes;
import org.esa.beam.dataio.modis.netcdf.NetCDFVariables;
import org.esa.beam.dataio.netcdf.*;
import org.esa.beam.dataio.netcdf.Modis35NetCdfReadProfile;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.*;
import org.esa.beam.dataio.netcdf.util.Modis35Constants;
import org.esa.beam.dataio.netcdf.util.Modis35NetcdfFileOpener;
import org.esa.beam.dataio.netcdf.util.Modis35RasterDigest;
import org.esa.beam.dataio.netcdf.util.ScaleImageOp;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

/**
 * Product reader responsible for reading MODIS MOD35 cloud mask HDF product.
 *
 * @author Olaf Danne
 */
public class Modis35ProductReader extends AbstractProductReader {

    private ModisGlobalAttributes globalAttributes;
    private NetcdfFile netcdfFile;
    private NetCDFAttributes netCDFAttributes;
    private NetCDFVariables netCDFVariables;

    ModisBandReader cloudMaskReader;

    public Modis35ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    static boolean isGlobalAttributeName(String variableName) {
        return ModisConstants.STRUCT_META_KEY.equalsIgnoreCase(variableName)
                || ModisConstants.CORE_META_KEY.equalsIgnoreCase(variableName)
                || ModisConstants.ARCHIVE_META_KEY.equalsIgnoreCase(variableName);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inFile = getInputFile();
        final String inputFilePath = inFile.getPath();

        netcdfFile = Modis35NetcdfFileOpener.open(inputFilePath);
        if (netcdfFile == null) {
            throw new IOException("Failed top open file: " + inputFilePath);
        }

        Modis35ProfileReadContext context = new Modis35ProfileReadContextImpl(netcdfFile);
        String filename = inFile.getName();
        context.setProperty(Modis35Constants.PRODUCT_FILENAME_PROPERTY, filename);

        // sequence:
        // 1. read first product with modified RasterDigest to get cloud mask on correct grid, skip weird metadata --> OK
        System.setProperty("raster.digest", "EXTENDED");
        System.setProperty("raster.dim.names", "Byte_Segment,Cell_Along_Swath_1km,Cell_Across_Swath_1km");
        initReadContext(context);
        Modis35NetCdfReadProfile profile = new Modis35NetCdfReadProfile();
        configureProfile(profile);
        final Product cloudMaskFirstProduct = profile.readProduct(context);
        cloudMaskFirstProduct.setFileLocation(inFile);
        cloudMaskFirstProduct.setProductReader(this);
        cloudMaskFirstProduct.setModified(false);
        try {
            netcdfFile.close();
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }

        // 2. make subset: bands (first 2 bytes of cloudmask) and grid (cut residual columns in x-dir)
        // 3. read second product with original RasterDigest: get the variables on 5km grid
        System.clearProperty("raster.digest");
        System.clearProperty("raster.dim.names");
        netcdfFile = Modis35NetcdfFileOpener.open(inputFilePath);
        if (netcdfFile == null) {
            throw new IOException("Failed top open file: " + inputFilePath);
        }
        context = new Modis35ProfileReadContextImpl(netcdfFile);
        context.setProperty(Modis35Constants.PRODUCT_FILENAME_PROPERTY, filename);
        initReadContext(context);
        profile = new Modis35NetCdfReadProfile();
        configureProfile(profile);
        final Product tiePoints5kmProduct = profile.readProduct(context);
        tiePoints5kmProduct.setFileLocation(inFile);
        tiePoints5kmProduct.setProductReader(this);
        tiePoints5kmProduct.setModified(false);
        // 4. upscale this with factor 5.0 to get same grid as subsetted cloud mask product (use ScaleImageOp)
        ScaleImageOp scaleImageOp = new ScaleImageOp();
        scaleImageOp.setParameterDefaultValues();

        // 5. final product: merge the two products
        // return the final product

//        return cloudMaskFirstProduct;
        return tiePoints5kmProduct;
    }

    private void initReadContext(Modis35ProfileReadContext ctx) throws IOException {
        NetcdfFile netcdfFile = ctx.getNetcdfFile();
        // todo: cleanup Mod35RasterDigest, get rid of system properties above, use a parameter instead
        final Modis35RasterDigest rasterDigest = Modis35RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
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
        profile.addProfilePartReader(new Modis35CfGeocodingPart());
        profile.addProfilePartReader(new Modis35CfTimePart());
        profile.addProfilePartReader(new Modis35CfDescriptionPart());
    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

        if (cloudMaskReader == null) {
            throw new IOException("No band reader for band '" + destBand.getName() + "' available!");
        }

        cloudMaskReader.readBandData(sourceOffsetX, sourceOffsetY,
                            sourceWidth, sourceHeight,
                            sourceStepX, sourceStepY,
                            destBuffer, pm);
    }

    private void readGlobalMetaData(File inFile) throws IOException {
        netCDFAttributes = new NetCDFAttributes();
        netCDFAttributes.add(netcdfFile.getGlobalAttributes());

        netCDFVariables = new NetCDFVariables();
        netCDFVariables.add(netcdfFile.getVariables());

        // check wheter daac or imapp
        if (isImappFormat()) {
            globalAttributes = new ImappAttributes(inFile, netCDFVariables, netCDFAttributes);
        } else {
            globalAttributes = new Modis35DaacAttributes(netCDFVariables);
        }
    }

    private boolean isImappFormat() {
        return netCDFVariables.get(ModisConstants.STRUCT_META_KEY) == null;
    }

    private void addRastersAndGeoCoding(final Product product, ModisGlobalAttributes globalAttribs, NetCDFVariables netCDFVariables) throws IOException {
        String productType = product.getProductType();
        if (globalAttribs.isImappFormat()) {
            productType += "_IMAPP";
        }

        addBandsToProduct(netCDFVariables, productType, product);
        // todo: add geocoding
//        if (isEosGridType(globalAttribs)) {
//            addMapGeocoding(product, globalAttribs);
//        } else {
//            addTiePointGrids(netCDFVariables, productType, product, globalAttribs);
//            addModisTiePointGeoCoding(product, null);
//        }
    }

    private void addBandsToProduct(NetCDFVariables netCDFVariables, final String type, final Product product) throws IOException {

        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        final String[] bandNames = new String[]{"Cloud_Mask"}; // todo: complete

        for (String bandName : bandNames) {

            final Variable variable = netCDFVariables.get(bandName);
            if (variable == null) {
                BeamLogManager.getSystemLogger().warning("Name Variable '" + bandName + "' of product type '" + type + "' not found");
                continue;
            }
            final java.util.List<Attribute> attributes = variable.getAttributes();
            NetCDFAttributes netCDFAttributes = new NetCDFAttributes();
            netCDFAttributes.add(attributes);

            int bandType = ProductData.TYPE_INT8; // todo
            final Band byte1Band = new Band(bandName + "_byte1", bandType, width, height);
            final Band byte2Band = new Band(bandName + "_byte2", bandType, width, height);
            product.addBand(byte1Band);
            product.addBand(byte2Band);
            cloudMaskReader = new ModisInt8BandReader(variable, 0, true);
        }
    }

    private void addMetadata(Product prod) throws IOException {
        // todo: check what we need and get rid of weird stuff!

        final MetadataElement mdElem = prod.getMetadataRoot();
        if (mdElem == null) {
            return;
        }

        final MetadataElement globalElem = new MetadataElement(ModisConstants.GLOBAL_META_NAME);

        final Attribute[] attributes = netCDFAttributes.getAll();
//        for (final Attribute attribute : attributes) {
//            globalElem.addAttribute(NetCDFUtils.toMetadataAttribute(attribute));
//        }
//
//        // we need to scan the variables because the NetCDF lib puts three important
//        // global attributes into the list of variables tb 2012-06-19
//        final Variable[] variables = netCDFVariables.getAll();
//        for (final Variable variable : variables) {
//            if (isGlobalAttributeName(variable.getFullName())) {
//                globalElem.addAttribute(NetCDFUtils.toMetadataAttribute(variable));
//            }
//        }
//
        mdElem.addElement(globalElem);
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

