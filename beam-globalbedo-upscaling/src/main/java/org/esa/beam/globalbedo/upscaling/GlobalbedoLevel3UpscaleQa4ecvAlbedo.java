/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.globalbedo.upscaling;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.mosaic.GlobAlbedoQa4ecvMosaicProductReader;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;

/**
 * Reprojects and upscales horizontal subsets of GlobAlbedo tile products
 * into a SIN or a 0.5 or 0.05 degree  Plate Caree product.
 * QA4ECV improvement: new options to generate subsets of mosaics (defined by tile numbers
 *
 * @author olafd
 */
@OperatorMetadata(
        alias = "ga.l3.upscale.albedo.qa4ecv",
        authors = "Olaf Danne",
        copyright = "2016 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Reprojects and upscales horizontal subsets of GlobAlbedo tile products \n" +
                " into a SIN or a 0.5 or 0.05 degree  Plate Caree product.")
public class GlobalbedoLevel3UpscaleQa4ecvAlbedo extends GlobalbedoLevel3UpscaleBasisOp {

    @Parameter(valueSet = {"1200", "200"},
            description = "Input product tile size (default = 1200 (MODIS), 200 for AVHRR/GEO",
            defaultValue = "1200")
    private int inputProductTileSize;

    @Parameter(defaultValue = "NETCDF", valueSet = {"DIMAP", "NETCDF"}, description = "Input format, either DIMAP or NETCDF.")
    private String inputFormat;

    @Parameter(defaultValue = "Albedo", description = "Name of albedo subdirectory.")
    private String albedoSubdirName;

    @Parameter(defaultValue = "false",
            description = "If set, not all bands (i.e. no alphas/sigmas) are written. Set to true to save computation time and disk space.")
    private boolean reducedOutput;

    @Parameter(label = "If set, only these bands will be written into mosaic")
    private String[] bandsToWrite;

    @Parameter(defaultValue = "0", description = "Tile h start index of mosaic")
    protected int hStartIndex;

    @Parameter(defaultValue = "35", description = "Tile h end index of mosaic")
    protected int hEndIndex;

    @Parameter(defaultValue = "0", description = "Tile v start index of mosaic")
    protected int vStartIndex;

    @Parameter(defaultValue = "17", description = "Tile v end index of mosaic")
    protected int vEndIndex;


    @TargetProduct
    private Product targetProduct;


    private String[] dhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrAlphaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrAlphaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private Band dataMaskBand;
    private Band szaBand;

    @Override
    public void initialize() throws OperatorException {
        final File refTile = findRefTile();
        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No albedo files for mosaicing found.");
        }

        final ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC-QA4ECV");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC-QA4ECV' reader available.");
        }
        if (productReader instanceof GlobAlbedoQa4ecvMosaicProductReader) {
            ((GlobAlbedoQa4ecvMosaicProductReader) productReader).setBandsToWrite(bandsToWrite);
            ((GlobAlbedoQa4ecvMosaicProductReader) productReader).setTileSize(inputProductTileSize);
            ((GlobAlbedoQa4ecvMosaicProductReader) productReader).setHStartIndex(hStartIndex);
            ((GlobAlbedoQa4ecvMosaicProductReader) productReader).setHEndIndex(hEndIndex);
            ((GlobAlbedoQa4ecvMosaicProductReader) productReader).setVStartIndex(vStartIndex);
            ((GlobAlbedoQa4ecvMosaicProductReader) productReader).setVEndIndex(vEndIndex);
        }

        dhrBandNames = IOUtils.getAlbedoDhrBandNames();
        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        dhrAlphaBandNames = IOUtils.getAlbedoDhrAlphaBandNames();
        bhrAlphaBandNames = IOUtils.getAlbedoBhrAlphaBandNames();
        dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
        bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();

        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(refTile, null);    // this is a mosaic on SIN projection!!
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }

        setReprojectedProduct(mosaicProduct, inputProductTileSize);

        final int numHorizontalTiles = hEndIndex - hStartIndex + 1;
        final int numVerticalTiles = vEndIndex - vStartIndex + 1;
        final int width = inputProductTileSize * numHorizontalTiles / scaling;
        final int height = inputProductTileSize * numVerticalTiles / scaling;
        final int tileWidth = inputProductTileSize / scaling / 2;
        final int tileHeight = inputProductTileSize / scaling / 2;

        setUpscaledProduct(mosaicProduct, width, height, tileWidth, tileHeight);
        for (Band srcBand : reprojectedProduct.getBands()) {
            if (bandsToWrite == null ||
                    srcBand.getName().equals(BRDF_ALBEDO_PRODUCT_LAT_NAME) ||
                    srcBand.getName().equals(BRDF_ALBEDO_PRODUCT_LON_NAME)) {
                addTargetBand(srcBand);
            } else {
                for (String bandToWrite : bandsToWrite) {
                    if (bandToWrite.equals(srcBand.getName())) {
                        addTargetBand(srcBand);
                    }
                }
            }
        }

        attachQa4ecvUpscaleGeoCoding(mosaicProduct, scaling, hStartIndex, vStartIndex, width, height, reprojection);

        dataMaskBand = reprojectedProduct.getBand(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME);
        szaBand = reprojectedProduct.getBand(AlbedoInversionConstants.ALB_SZA_BAND_NAME);

        targetProduct = upscaledProduct;
        targetProduct.setPreferredTileSize(inputProductTileSize, inputProductTileSize);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle(targetRect.x * scaling,
                                          targetRect.y * scaling,
                                          targetRect.width * scaling,
                                          targetRect.height * scaling);
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);

        final Tile latTile = targetTiles.get(BRDF_ALBEDO_PRODUCT_LAT_NAME);
        final Tile lonTile = targetTiles.get(BRDF_ALBEDO_PRODUCT_LON_NAME);
        if (reprojection.equals("PC") && latTile != null && lonTile != null) {
            computeLatLon(latTile, lonTile, latTile);
        }

        if (szaBand != null) {
            final Tile szaSrcTile = getSourceTile(szaBand, srcRect);
            computeNearestAlbedo(szaSrcTile,
                                 targetTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME),
                                 getSourceTile(dataMaskBand, srcRect));
        }

//        if (hasValidPixel(getSourceTile(relEntropyBand, srcRect), relEntropyBand.getNoDataValue())) {
        // todo: check why we ever switched to relEntropy as data mask?!
        if (hasValidPixel(getSourceTile(dataMaskBand, srcRect), dataMaskBand.getNoDataValue())) {
            Map<String, Tile> srcTiles = getSourceTiles(srcRect);

            for (String dhrBandName : dhrBandNames) {
                computeNearestAlbedo(srcTiles.get(dhrBandName), targetTiles.get(dhrBandName),
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            for (String bhrBandName : bhrBandNames) {
                computeNearestAlbedo(srcTiles.get(bhrBandName), targetTiles.get(bhrBandName),
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            if (!reducedOutput) {
                for (String dhrAlphaBandName : dhrAlphaBandNames) {
                    computeNearestAlbedo(srcTiles.get(dhrAlphaBandName), targetTiles.get(dhrAlphaBandName),
                                         srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                }
                for (String bhrAlphaBandName : bhrAlphaBandNames) {
                    computeNearestAlbedo(srcTiles.get(bhrAlphaBandName), targetTiles.get(bhrAlphaBandName),
                                         srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                }
                for (String dhrSigmaBandName : dhrSigmaBandNames) {
                    computeNearestAlbedo(srcTiles.get(dhrSigmaBandName), targetTiles.get(dhrSigmaBandName),
                                         srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                }
                for (String bhrSigmaBandName : bhrSigmaBandNames) {
                    computeNearestAlbedo(srcTiles.get(bhrSigmaBandName), targetTiles.get(bhrSigmaBandName),
                                         srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                }
            }

            computeMajority(srcTiles.get(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME),
                            targetTiles.get(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME),
                            srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME), scaling);

            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME),
                                 targetTiles.get(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME),
                                 srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME),
                                 targetTiles.get(AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME),
                                 srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME),
                                 targetTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME),
                                 srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            if (srcTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME) != null) {
                computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME),
                                     targetTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME),
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME),
                                 targetTiles.get(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME),
                                 srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            if (reprojection.equals("SIN") && latTile != null && lonTile != null) {
                computeNearestAlbedo(srcTiles.get(BRDF_ALBEDO_PRODUCT_LAT_NAME), latTile,
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                computeNearestAlbedo(srcTiles.get(BRDF_ALBEDO_PRODUCT_LON_NAME), lonTile,
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
        } else {
            for (Map.Entry<String, Tile> tileEntry : targetTiles.entrySet()) {
                checkForCancellation();

                Tile targetTile = tileEntry.getValue();
                String bandName = tileEntry.getKey();
                if (!isLatLonBand(bandName) && !bandName.equals(AlbedoInversionConstants.ALB_SZA_BAND_NAME)) {
                    double noDataValue = getTargetProduct().getBand(bandName).getNoDataValue();
                    for (int y = targetRect.y; y < targetRect.y + targetRect.height; y++) {
                        for (int x = targetRect.x; x < targetRect.x + targetRect.width; x++) {
                            targetTile.setSample(x, y, noDataValue);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void computeLatLon(Tile latTile, Tile lonTile, Tile target) {
        // computes a simple lat/lon grid in given tile
        float lonStart = -180.0f + 10.0f * hStartIndex;
        float lonRange = 10.0f * (hEndIndex - hStartIndex + 1);
        float latStart = 90.0f - 10.0f * vStartIndex;
        float latRange = 10.0f * (vEndIndex - vStartIndex + 1);
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                float lon = lonStart + lonRange * (x + 0.5f) / upscaledProduct.getSceneRasterWidth();
                float lat = latStart - latRange * (y + 0.5f) / upscaledProduct.getSceneRasterHeight();
                latTile.setSample(x, y, lat);
                lonTile.setSample(x, y, lon);
            }
        }
    }

    private void addTargetBand(Band srcBand) {
        boolean skipBand = reducedOutput && (srcBand.getName().contains("alpha") || srcBand.getName().contains("sigma"));
        if (!skipBand) {
            if (!upscaledProduct.containsBand(srcBand.getName())) {
                Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
                ProductUtils.copyRasterDataNodeProperties(srcBand, band);
            }
        }
    }


    private File findRefTile() {

        final FilenameFilter albedoFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // e.g. GlobAlbedo.albedo.2006335.h18v04.nc
                String expectedFilenameExt = inputFormat.equals("DIMAP") ? ".dim" : ".nc";
//                String expectedFilename;
                final String expectedPrefix1 = "Qa4ecv.albedo.";
                final String expectedPrefix2 = "Qa4ecv.avhrrgeo.albedo.";
                final String expectedPrefix3 = "Qa4ecv.merisvgt.albedo.";
                String expectedSuffix;

                if (isMonthlyAlbedo) {
//                    expectedFilename = "GlobAlbedo.albedo." + year + IOUtils.getMonthString(monthIndex) + "." +
//                            dir.getName() + expectedFilenameExt;
                    expectedSuffix = year + IOUtils.getMonthString(monthIndex) + "." +
                            dir.getName() + expectedFilenameExt;
                } else {
//                    expectedFilename = "GlobAlbedo.albedo." + year + IOUtils.getDoyString(doy) + "." +
//                            dir.getName() + expectedFilenameExt;
                    expectedSuffix = year + IOUtils.getDoyString(doy) + "." +
                            dir.getName() + expectedFilenameExt;
                }

                final boolean isTileToProcess =
                        GlobAlbedoQa4ecvMosaicProductReader.isTileToProcess(dir.getName(),
                                                                            hStartIndex, hEndIndex,
                                                                            vStartIndex, vEndIndex);
//                return isTileToProcess && name.equals(expectedFilename);
                return isTileToProcess && name.endsWith(expectedSuffix) &&
                        (name.startsWith(expectedPrefix1) || name.startsWith(expectedPrefix2) || name.startsWith(expectedPrefix3));
            }
        };

        String albedoDirString = gaRootDir + File.separator + albedoSubdirName + File.separator + year;

        final File[] albedoFiles = IOUtils.getTileDirectories(albedoDirString);
        if (albedoFiles != null && albedoFiles.length > 0) {
            for (File albedoFile : albedoFiles) {
                File[] tileFiles = albedoFile.listFiles(albedoFilter);
                if (tileFiles != null && tileFiles.length > 0) {
                    for (File tileFile : tileFiles) {
                        if (tileFile.exists()) {
                            return tileFile;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void computeNearestAlbedo(Tile src, Tile target, Tile mask) {
        if (src != null && target != null && mask != null) {
            computeNearest(src, target, mask, scaling);
            applySouthPoleCorrection(src, target, mask);
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleQa4ecvAlbedo.class);
        }
    }
}
