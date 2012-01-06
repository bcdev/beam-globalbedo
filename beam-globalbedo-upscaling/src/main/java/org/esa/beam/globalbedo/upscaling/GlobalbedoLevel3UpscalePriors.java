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
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.mosaic.GlobAlbedoMosaicProductReader;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.PRIOR_MASK_NAME;
import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.PRIOR_NSAMPLES_NAME;

/**
 * Reprojects and upscales the GlobAlbedo product
 * that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.
 *
 * @author MarcoZ
 */
@OperatorMetadata(
        alias = "ga.l3.upscale.priors",
        authors = "Olaf Danne",
        copyright = "2011 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Upscales and mosaics the MODIS priors \n" +
                " that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.")
public class GlobalbedoLevel3UpscalePriors extends Operator {

    private static final String WGS84_CODE = "EPSG:4326";
    private static final int TILE_SIZE = 1200;

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(valueSet = {"1", "2"}, description = "Stage of priors to mosaic", defaultValue = "2")
    private int priorStage;

    @Parameter(defaultValue = "001", description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute snow priors")
    private boolean computeSnow;

    @Parameter(valueSet = {"1", "5", "60"}, description = "Scaling (5 = 5km, 60 = 60km resolution", defaultValue = "60")
    private int scaling;

    @TargetProduct
    private Product targetProduct;

    private Product reprojectedProduct;

    private File refTile;

    private String[] priorMeanBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] priorSDBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String nSamplesBandName;
    private String maskBandName;
    private Band maskBand;

    private Logger logger;

    @Override
    public void initialize() throws OperatorException {

        logger = BeamLogManager.getSystemLogger();
        refTile = findRefTile();

        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No prior files for mosaicing found.");
        }
        ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        if (productReader instanceof GlobAlbedoMosaicProductReader) {
            ((GlobAlbedoMosaicProductReader) productReader).setMosaicPriors(true);
        }
        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(refTile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }
        reprojectedProduct = mosaicProduct;

        int width = reprojectedProduct.getSceneRasterWidth() / scaling;
        int height = reprojectedProduct.getSceneRasterHeight() / scaling;
        Product upscaledProduct = new Product(mosaicProduct.getName() + "_upscaled", "GA_UPSCALED", width, height);

        priorMeanBandNames = IOUtils.getPriorMeanBandNames();
        priorSDBandNames = IOUtils.getPriorSDMeanBandNames();
        nSamplesBandName = PRIOR_NSAMPLES_NAME;
        maskBandName = PRIOR_MASK_NAME;

        // for performance, just write one band ...
        Band band = upscaledProduct.addBand(priorMeanBandNames[0], ProductData.TYPE_FLOAT32);
        ProductUtils.copyRasterDataNodeProperties(reprojectedProduct.getBand(priorMeanBandNames[0]), band);
        band.setNoDataValue(Float.NaN);
        band.setNoDataValueUsed(true);
        band = upscaledProduct.addBand(maskBandName, ProductData.TYPE_FLOAT32);
        ProductUtils.copyRasterDataNodeProperties(reprojectedProduct.getBand(maskBandName), band);
        band.setNoDataValue(Float.NaN);
        band.setNoDataValueUsed(true);

//        for (Band srcBand : reprojectedProduct.getBands()) {
//            Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
//            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
//        }
        upscaledProduct.setStartTime(reprojectedProduct.getStartTime());
        upscaledProduct.setEndTime(reprojectedProduct.getEndTime());
        ProductUtils.copyMetadata(reprojectedProduct, upscaledProduct);
        upscaledProduct.setPreferredTileSize(TILE_SIZE / scaling / 4, TILE_SIZE / scaling / 4);

        ProductUtils.copyGeoCoding(mosaicProduct, upscaledProduct);

        maskBand = reprojectedProduct.getBand(maskBandName);

        targetProduct = upscaledProduct;
    }

    private File findRefTile() {

        final FilenameFilter priorFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
//                e.g. Kernels.129.005.h18v04.backGround.NoSnow
                String expectedFilenamePrefix = "Kernels." + IOUtils.getDoyString(doy);
                String expectedFilenameSuffix = ".hdr";
                return name.startsWith(expectedFilenamePrefix) && name.endsWith(expectedFilenameSuffix);
            }
        };

        logger.log(Level.ALL, "Prior root directory is: '" + priorRootDir + "'...");
        final File[] priorFiles = GlobAlbedoMosaicProductReader.getPriorTileDirectories(priorRootDir);
        for (File priorFile : priorFiles) {
            File[] tileFiles = priorFile.listFiles(priorFilter);
            for (File tileFile : tileFiles) {
                if (tileFile.exists()) {
                    return tileFile;
                }
            }
        }
        return null;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle(targetRect.x * scaling,
                targetRect.y * scaling,
                targetRect.width * scaling,
                targetRect.height * scaling);
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);
        if (hasValidPixel(getSourceTile(maskBand, srcRect))) {
            Map<String, Tile> srcTiles = getSourceTiles(srcRect);

            computeNearest(srcTiles.get(priorMeanBandNames[0]), targetTiles.get(priorMeanBandNames[0]),
                    srcTiles.get(maskBandName));
//            for (int i = 0; i < priorMeanBandNames.length; i++) {
//                computeNearest(srcTiles.get(priorMeanBandNames[i]), targetTiles.get(priorMeanBandNames[i]),
//                        srcTiles.get(maskBandName));
//            }
//            for (int i = 0; i < priorSDBandNames.length; i++) {
//                computeNearest(srcTiles.get(priorSDBandNames[i]), targetTiles.get(priorSDBandNames[i]),
//                        srcTiles.get(maskBandName));
//            }

            computeNearest(srcTiles.get(maskBandName),
                    targetTiles.get(maskBandName),
                    srcTiles.get(maskBandName));
//            computeNearest(srcTiles.get(nSamplesBandName),
//                    targetTiles.get(nSamplesBandName),
//                    srcTiles.get(maskBandName));
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

    private Map<String, Tile> getSourceTiles(Rectangle srcRect) {
        Band[] srcBands = reprojectedProduct.getBands();
        Map<String, Tile> srcTiles = new HashMap<String, Tile>(srcBands.length);
        for (Band band : srcBands) {
            srcTiles.put(band.getName(), getSourceTile(band, srcRect));
        }
        return srcTiles;
    }

    private Map<String, Tile> getTargetTiles(Map<Band, Tile> targetBandTiles) {
        Map<String, Tile> targetTiles = new HashMap<String, Tile>();
        for (Map.Entry<Band, Tile> entry : targetBandTiles.entrySet()) {
            targetTiles.put(entry.getKey().getName(), entry.getValue());
        }
        return targetTiles;
    }

    private boolean hasValidPixel(Tile entropy) {
        double noDataValue = maskBand.getNoDataValue();
        Rectangle rect = entropy.getRectangle();
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                double sample = entropy.getSampleDouble(x, y);
                if (sample != 0.0 && sample != noDataValue && !Double.isNaN(sample)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void computeNearest(Tile src, Tile target, Tile mask) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                float sample = src.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                final float sampleMask = mask.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                if (sample == 0.0 || sampleMask == 0.0 || Float.isNaN(sample)) {
                    sample = Float.NaN;
                }
                target.setSample(x, y, sample);
            }
        }
    }

    private void computeMajority(Tile src, Tile target, Tile mask) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                Rectangle pixelSrc = new Rectangle(x * scaling, y * scaling, scaling, scaling);
                int max = -1;
                for (int sy = pixelSrc.y; sy < pixelSrc.y + pixelSrc.height; sy++) {
                    for (int sx = pixelSrc.x; sx < pixelSrc.x + pixelSrc.width; sx++) {
                        max = Math.max(max, src.getSampleInt(sx, sy));
                    }
                }
                final float sampleMask = mask.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                if (sampleMask > 0.0) {
                    target.setSample(x, y, max);
                } else {
                    target.setSample(x, y, Float.NaN);
                }
            }
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscalePriors.class);
        }
    }
}
