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
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.mosaic.MosaicConstants;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;

/**
 * Reprojects and upscales the GlobAlbedo product
 * that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.
 *
 * @author MarcoZ
 */
@OperatorMetadata(
        alias = "ga.l3.upscale.albedo",
        authors = "Marco Zuehlke",
        copyright = "2011 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Reprojects and upscales the GlobAlbedo product \n" +
                " that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.")
public class GlobalbedoLevel3UpscaleAlbedo extends GlobalbedoLevel3UpscaleBasisOp {

    @Parameter(valueSet = {"5", "6", "60"}, description = "Scaling (5 = 1/24deg, 6 = 1/20deg, 60 = 1/2deg resolution", defaultValue = "60")
    int scaling;

    @TargetProduct
    private Product targetProduct;


    private String[] dhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private Band relEntropyBand;

    @Override
    public void initialize() throws OperatorException {
        final File refTile = findRefTile();
        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No albedo files for mosaicing found.");
        }

        final ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }

        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(refTile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }

        setReprojectedProduct(mosaicProduct, MosaicConstants.MODIS_TILE_SIZE);

        final int width  = MosaicConstants.MODIS_TILE_SIZE * MosaicConstants.NUM_H_TILES / scaling;
        final int height = MosaicConstants.MODIS_TILE_SIZE * MosaicConstants.NUM_V_TILES / scaling;
        final int tileWidth = MosaicConstants.MODIS_TILE_SIZE / scaling / 2;
        final int tileHeight = MosaicConstants.MODIS_TILE_SIZE / scaling / 2;

        setUpscaledProduct(mosaicProduct, width, height, tileWidth, tileHeight);
        for (Band srcBand : reprojectedProduct.getBands()) {
            Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        }

        attachUpscaleGeoCoding(mosaicProduct, scaling, width, height, reprojectToPlateCarre);

        dhrBandNames = IOUtils.getAlbedoDhrBandNames();
        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
        bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();

        relEntropyBand = reprojectedProduct.getBand(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME);

        targetProduct = upscaledProduct;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle(targetRect.x * scaling,
                targetRect.y * scaling,
                targetRect.width * scaling,
                targetRect.height * scaling);
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);
        if (hasValidPixel(getSourceTile(relEntropyBand, srcRect), relEntropyBand.getNoDataValue())) {
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
        } else {
            for (Map.Entry<String, Tile> tileEntry : targetTiles.entrySet()) {
                checkForCancellation();

                Tile targetTile = tileEntry.getValue();
                String bandName = tileEntry.getKey();
                double noDataValue = getTargetProduct().getBand(bandName).getNoDataValue();
                for (int y = targetRect.y; y < targetRect.y + targetRect.height; y++) {
                    for (int x = targetRect.x; x < targetRect.x + targetRect.width; x++) {
                        targetTile.setSample(x, y, noDataValue);
                    }
                }
            }
        }
    }

    private File findRefTile() {

        final FilenameFilter albedoFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String expectedFilename;
                String expectedFilenameOld;
                if (isMonthlyAlbedo) {
                    expectedFilename = "GlobAlbedo.albedo." + year + IOUtils.getMonthString(monthIndex) + "." + dir.getName() + ".dim";
                    expectedFilenameOld = "GlobAlbedo." + year + IOUtils.getMonthString(monthIndex) + "." + dir.getName() + ".dim";
                } else {
                    expectedFilename = "GlobAlbedo.albedo." + year + IOUtils.getDoyString(doy) + "." + dir.getName() + ".dim";
                    expectedFilenameOld = "GlobAlbedo." + year + IOUtils.getDoyString(doy) + "." + dir.getName() + ".dim";
                }
                return (name.equals(expectedFilename) || name.equals(expectedFilenameOld));
            }
        };

        String albedoDirString;
        if (isMonthlyAlbedo) {
            albedoDirString = gaRootDir + File.separator + "MonthlyAlbedo";
        } else {
            albedoDirString = gaRootDir + File.separator + "Albedo";
        }
        final File[] albedoFiles = IOUtils.getTileDirectories(albedoDirString);
        for (File albedoFile : albedoFiles) {
            File[] tileFiles = albedoFile.listFiles(albedoFilter);
            for (File tileFile : tileFiles) {
                if (tileFile.exists()) {
                    return tileFile;
                }
            }
        }
        return null;
    }

    private void computeNearestAlbedo(Tile src, Tile target, Tile mask) {
        computeNearest(src, target, mask, scaling);
        applySouthPoleCorrection(src, target, mask);
    }

    private void applySouthPoleCorrection(Tile src, Tile target, Tile mask) {
        Rectangle targetRectangle = target.getRectangle();
        final PixelPos pixelPos = new PixelPos(targetRectangle.x * scaling,
                (targetRectangle.y + targetRectangle.height) * scaling);
        final GeoPos geoPos = reprojectedProduct.getGeoCoding().getGeoPos(pixelPos, null);

        // correct for projection failures near south pole...
        if (reprojectToPlateCarre && geoPos.getLat() < -86.0) {
            float[][] correctedSampleWest = null;    // i.e. tile h17v17
            if (geoPos.getLon() < 0.0) {
                correctedSampleWest = correctFromWest(target);    // i.e. tile h17v17
            }
            float[][] correctedSampleEast = null;    // i.e. tile h18v17
            if (geoPos.getLon() >= 0.0) {
                correctedSampleEast = correctFromEast(target);    // i.e. tile h17v17
            }

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    float sample = src.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                    final float sampleMask = mask.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                    if (sample == 0.0 || sampleMask == 0.0 || Float.isNaN(sample)) {
                        sample = Float.NaN;
                    }
                    final int xCorr = x - targetRectangle.x;
                    final int yCorr = y - targetRectangle.y;
                    if (geoPos.getLon() < 0.0) {
                        if (correctedSampleWest != null) {
                            if (correctedSampleWest[xCorr][yCorr] != 0.0) {
                                target.setSample(x, y, correctedSampleWest[xCorr][yCorr]);
                            } else {
                                target.setSample(x, y, sample);
                            }
                        }
                    } else {
                        if (correctedSampleEast != null) {
                            if (correctedSampleEast[xCorr][yCorr] != 0.0) {
                                target.setSample(x, y, correctedSampleEast[xCorr][yCorr]);
                            } else {
                                target.setSample(x, y, sample);
                            }
                        }
                    }
                }
            }
        }
    }

    private float[][] correctFromWest(Tile src) {
        final Rectangle sourceRectangle = src.getRectangle();
        float[][] correctedSample = new float[sourceRectangle.width][sourceRectangle.height];
        for (int y = sourceRectangle.y; y < sourceRectangle.y + sourceRectangle.height; y++) {
            // go east direction
            int xIndex = sourceRectangle.x;
            float corrValue;
            float value = src.getSampleFloat(xIndex, y);
            while ((value == 0.0 || Float.isNaN(value))
                    && xIndex < sourceRectangle.x + sourceRectangle.width - 1) {
                xIndex++;
                value = src.getSampleFloat(xIndex, y);
            }
            if (value == 0.0 || Float.isNaN(value)) {
                // go north direction, we WILL find a non-zero value
                int yIndex = y;
                float value2 = src.getSampleFloat(xIndex, yIndex);
                while ((value2 == 0.0 || Float.isNaN(value2))
                        && yIndex > sourceRectangle.y + 1) {
                    yIndex--;
                    value2 = src.getSampleFloat(xIndex, yIndex);
                }
                corrValue = value2;
            } else {
                corrValue = value;
            }
            for (int i = sourceRectangle.x; i <= xIndex; i++) {
                correctedSample[i - sourceRectangle.x][y - sourceRectangle.y] = corrValue;
            }
        }
        return correctedSample;
    }

    private float[][] correctFromEast(Tile src) {
        final Rectangle sourceRectangle = src.getRectangle();
        float[][] correctedSample = new float[sourceRectangle.width][sourceRectangle.height];
        for (int y = sourceRectangle.y; y < sourceRectangle.y + sourceRectangle.height; y++) {
            // go west direction
            int xIndex = sourceRectangle.x + sourceRectangle.width - 1;
            float corrValue;
            float value = src.getSampleFloat(xIndex, y);
            while ((value == 0.0 || Float.isNaN(value))
                    && xIndex > sourceRectangle.x) {
                xIndex--;
                value = src.getSampleFloat(xIndex, y);
            }
            if (value == 0.0 || Float.isNaN(value)) {
                // go north direction, we WILL find a non-zero value
                int yIndex = y;
                float value2 = src.getSampleFloat(xIndex, yIndex);
                while ((value2 == 0.0 || Float.isNaN(value2))
                        && yIndex > sourceRectangle.y + 1) {
                    yIndex--;
                    value2 = src.getSampleFloat(xIndex, yIndex);
                }
                corrValue = value2;
            } else {
                corrValue = value;
            }
            for (int i = sourceRectangle.x + sourceRectangle.width - 1; i >= xIndex; i--) {
                correctedSample[i - sourceRectangle.x][y - sourceRectangle.y] = corrValue;
            }
        }
        return correctedSample;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleAlbedo.class);
        }
    }
}
