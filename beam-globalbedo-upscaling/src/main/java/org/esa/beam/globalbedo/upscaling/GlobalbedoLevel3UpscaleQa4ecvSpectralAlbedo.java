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
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.spectral.SpectralIOUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.globalbedo.mosaic.GlobAlbedoQa4ecvMosaicProductReader;
import org.esa.beam.globalbedo.mosaic.MosaicConstants;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Reprojects and upscales horizontal subsets of GlobAlbedo tile products
 * into a SIN or a 0.5 or 0.05 degree  Plate Caree product.
 * QA4ECV improvement: new options to generate subsets of mosaics (defined by tile numbers
 *
 * @author olafd
 */
@OperatorMetadata(
        alias = "ga.l3.upscale.albedo.spectral.qa4ecv",
        authors = "Olaf Danne",
        copyright = "2016 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Reprojects and upscales horizontal subsets of GlobAlbedo tile products \n" +
                " into a SIN or a 0.5 or 0.05 degree  Plate Caree product.")
public class GlobalbedoLevel3UpscaleQa4ecvSpectralAlbedo extends GlobalbedoLevel3UpscaleBasisOp {

    @Parameter(valueSet = {"1200", "200"},
            description = "Input product tile size (default = 1200 (MODIS), 200 for AVHRR/GEO",
            defaultValue = "1200")
    private int inputProductTileSize;

    @Parameter(defaultValue = "NETCDF", valueSet = {"DIMAP", "NETCDF"}, description = "Input format, either DIMAP or NETCDF.")
    private String inputFormat;

    @Parameter(defaultValue = "Merge", valueSet = {"Merge", "Snow", "NoSnow"},
            description = "Input BRDF type, either Merge, Snow or NoSnow.")
    private String snowMode;

    @Parameter(defaultValue = "Albedo_spectral", description = "Name of albedo subdirectory.")
    private String albedoSubdirName;

    @Parameter(defaultValue = "false",
            description = "If set, not all bands (i.e. no alphas/sigmas) are written. Set to true to save computation time and disk space.")
    private boolean reducedOutput;

    @Parameter(label = "If set, only these bands will be written into mosaic")
    private String[] bandsToWrite;

    @Parameter(defaultValue = "17", description = "Tile h start index of mosaic")
    protected int hStartIndex;

    @Parameter(defaultValue = "20", description = "Tile h end index of mosaic")
    protected int hEndIndex;

    @Parameter(defaultValue = "2", description = "Tile v start index of mosaic")
    protected int vStartIndex;

    @Parameter(defaultValue = "5", description = "Tile v end index of mosaic")
    protected int vEndIndex;

    @Parameter(defaultValue = "7", description = "Number of spectral bands (7 for standard MODIS spectral mapping")
    private int numSdrBands;


    @TargetProduct
    private Product targetProduct;

    // bands:
    // DHR_b1,...,DHR_b7
    // BHR_b1,...,BHR_b7
    // DHR_sigma_b1,...,DHR_sigma_b7
    // BHR_sigma_b1,...,BHR_sigma_b7

    private String[] dhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private Band dataMaskBand;
    private Band szaBand;

    private Map<Integer, String> spectralWaveBandsMap = new HashMap<>();

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

        setupSpectralWaveBandsMap(numSdrBands);

        dhrBandNames = SpectralIOUtils.getSpectralAlbedoDhrBandNames(numSdrBands, spectralWaveBandsMap);
        bhrBandNames = SpectralIOUtils.getSpectralAlbedoBhrBandNames(numSdrBands, spectralWaveBandsMap);
        dhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoDhrSigmaBandNames(numSdrBands, spectralWaveBandsMap);
        bhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoBhrSigmaBandNames(numSdrBands, spectralWaveBandsMap);

        Product mosaicProduct;
        try {
            BeamLogManager.getSystemLogger().log(Level.INFO, "refTile: " + refTile);
            mosaicProduct = productReader.readProductNodes(refTile, null);    // this is a mosaic on SIN projection!!
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }

//        setReprojectedProduct(mosaicProduct, inputProductTileSize);
        setReprojectedProduct(mosaicProduct, inputProductTileSize, hStartIndex, vStartIndex);

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

//        attachQa4ecvUpscaleGeoCoding(mosaicProduct, scaling, hStartIndex, vStartIndex, width, height, reprojection);
        attachQa4ecvSpectralAlbedoUpscaleGeoCoding(mosaicProduct, scaling, hStartIndex, vStartIndex, width, height, reprojection);

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
            for (String dhrSigmaBandName : dhrSigmaBandNames) {
                computeNearestAlbedo(srcTiles.get(dhrSigmaBandName), targetTiles.get(dhrSigmaBandName),
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            for (String bhrSigmaBandName : bhrSigmaBandNames) {
                computeNearestAlbedo(srcTiles.get(bhrSigmaBandName), targetTiles.get(bhrSigmaBandName),
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }

            computeMajority(srcTiles.get(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME),
                            targetTiles.get(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME),
                            srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME), scaling);

            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME),
                                 targetTiles.get(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME),
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
        if (!upscaledProduct.containsBand(srcBand.getName())) {
            Band band = upscaledProduct.addBand(srcBand.getName(), ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        }
    }

    private File findRefTile() {

        final FilenameFilter albedoFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // e.g.:
                // Qa4ecv.albedo.spectral.2005339.h17v03.NoSnow.nc
                // Qa4ecv.albedo.spectral.2005339.h17v03.NoSnow.nc
                final String expectedPrefix = "Qa4ecv.";
                String expectedNamepart;
                final String expectedFilenameExt = ".nc";

                expectedNamepart = year + IOUtils.getDoyString(doy) + "." + dir.getName();

                final boolean isTileToProcess =
                        GlobAlbedoQa4ecvMosaicProductReader.isTileToProcess(dir.getName(),
                                                                            hStartIndex, hEndIndex,
                                                                            vStartIndex, vEndIndex);

                return isTileToProcess &&
                        name.startsWith(expectedPrefix) &&
                        name.contains(expectedNamepart) &&
                        name.endsWith(expectedFilenameExt) &&
                        name.contains("albedo");
            }
        };

        String albedoDirString = gaRootDir + File.separator + albedoSubdirName + File.separator + snowMode +
                File.separator + year;

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
        }
    }

    private void setupSpectralWaveBandsMap(int numSdrBands) {
        for (int i = 0; i < numSdrBands; i++) {
            spectralWaveBandsMap.put(i, "b" + (i + 1));
        }
    }

    private void attachQa4ecvSpectralAlbedoUpscaleGeoCoding(Product mosaicProduct,
                                                double scaling,
                                                int hStartIndex, int vStartIndex,
                                                int width, int height,
                                                String reprojection) {
        if (reprojection.equals("PC")) {
            final CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
//            final double pixelSizeX = 0.05; // todo - this is for 360deg
            final double pixelSizeX = 1./120.; // todo
//            final double pixelSizeY = 0.05;
            final double pixelSizeY = 1./120.;
            final double northing = 90.0 - 10.*vStartIndex;
            final double easting = -180.0 + 10.*hStartIndex;
            final CrsGeoCoding crsGeoCoding;
            try {
                crsGeoCoding = new CrsGeoCoding(crs, width, height, easting, northing, pixelSizeX, pixelSizeY);
                upscaledProduct.setGeoCoding(crsGeoCoding);
            } catch (Exception e) {
                e.printStackTrace();
                throw new OperatorException("Cannot attach geocoding for mosaic: ", e);
            }
        } else {
            final CoordinateReferenceSystem mapCRS = mosaicProduct.getGeoCoding().getMapCRS();
            try {

                final double pixelSizeX = MosaicConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * scaling;
                final double pixelSizeY = MosaicConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * scaling;
                final double northing = MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_Y -
                        vStartIndex * pixelSizeY * inputProductTileSize;
                final double easting = MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_X +
                        hStartIndex * pixelSizeX * inputProductTileSize;
                ModisTileGeoCoding geoCoding = new ModisTileGeoCoding(mapCRS, easting, northing, pixelSizeX, pixelSizeY);

//                final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * scaling;
//                final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * scaling;
//                                                new ModisTileGeoCoding(mapCRS,
//                                                                      MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_X,
//                                                                      MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_Y,
//                                                                      pixelSizeX,
//                                                                      pixelSizeY);
                upscaledProduct.setGeoCoding(geoCoding);
            } catch (Exception e) {
                throw new OperatorException("Cannot attach geocoding for mosaic: ", e);
            }
        }
    }



    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleQa4ecvSpectralAlbedo.class);
        }
    }
}
