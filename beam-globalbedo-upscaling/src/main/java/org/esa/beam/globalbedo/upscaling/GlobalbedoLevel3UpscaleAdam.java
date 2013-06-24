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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.mosaic.GlobAlbedoMosaicProductReader;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reprojects and upscales the GlobAlbedo product
 * that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.
 *
 * @author MarcoZ
 */
@OperatorMetadata(
        alias = "ga.l3.upscale.adam",
        authors = "Olaf Danne",
        copyright = "2011 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Upscales and mosaics ADAM products (pretty similar to the MODIS priors) \n" +
                " that exist in multiple Sinusoidal tiles into a 0.1 degree  Plate Caree product.")
public class GlobalbedoLevel3UpscaleAdam extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /disk/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "ADAM root directory")
    // e.g., /disk/Globalbedo90/adam/processed/Monthly
    private String adamRootDir;
    // products at MSSL are like /disk/Globalbedo90/adam/processed/Monthly/h23v06/Adam.nr.200506.h23v06.dim
    //   general:                /disk/Globalbedo90/adam/processed/Monthly/hXXvYy/Adam.nr.yyyyMM.hXXvYY.dim

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "01", description = "MonthIndex", interval = "[1,12]")
    private int monthIndex;

    @Parameter(valueSet = {"1", "5"}, description = "Scaling (10 = 1/10deg, 50 = 1/2deg resolution", defaultValue = "1")
    private int scaling;

    @Parameter(defaultValue = "true", description = "If True product will be reprojected")
    private boolean reprojectToPlateCarre;


    @TargetProduct
    private Product targetProduct;


    private static final String ADAM_NSAMPLES_NAME = "GROUND_N_SAMPLES";

    private static final String WGS84_CODE = "EPSG:4326";
    private static final int ADAM_TILE_SIZE = 120;

    private Product reprojectedProduct;

    private Band nsamplesBand;
    private String nsamplesBandName;

    private File refTile;

    private Logger logger;

    @Override
    public void initialize() throws OperatorException {

        logger = BeamLogManager.getSystemLogger();
        refTile = findRefTile();

        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No ADAM files for mosaicing found.");
        }
        ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        if (productReader instanceof GlobAlbedoMosaicProductReader) {
            ((GlobAlbedoMosaicProductReader) productReader).setMosaicAdam(true);
        }
        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(refTile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }

        if (reprojectToPlateCarre) {
            ReprojectionOp reprojection = new ReprojectionOp();
            reprojection.setParameter("crs", WGS84_CODE);
            reprojection.setSourceProduct(mosaicProduct);
            reprojectedProduct = reprojection.getTargetProduct();
            reprojectedProduct.setPreferredTileSize(ADAM_TILE_SIZE / 4, ADAM_TILE_SIZE / 4);
        } else {
            reprojectedProduct = mosaicProduct;
        }

        int width = reprojectedProduct.getSceneRasterWidth() / scaling;
        int height = reprojectedProduct.getSceneRasterHeight() / scaling;

        Product upscaledProduct = new Product(mosaicProduct.getName() + "_upscaled", "GA_UPSCALED", width, height);

        nsamplesBandName = ADAM_NSAMPLES_NAME;
        nsamplesBand = reprojectedProduct.getBand(nsamplesBandName);

        // we need ALL bands...
        for (Band srcBand : reprojectedProduct.getBands()) {
            Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        upscaledProduct.setStartTime(reprojectedProduct.getStartTime());
        upscaledProduct.setEndTime(reprojectedProduct.getEndTime());
        ProductUtils.copyMetadata(reprojectedProduct, upscaledProduct);
        upscaledProduct.setPreferredTileSize(ADAM_TILE_SIZE / scaling / 4, ADAM_TILE_SIZE / scaling / 4);

        if (reprojectToPlateCarre) {
            final AffineTransform modelTransform = ImageManager.getImageToModelTransform(reprojectedProduct.getGeoCoding());
            final double pixelSizeX = modelTransform.getScaleX();
            final double pixelSizeY = modelTransform.getScaleY();
            ReprojectionOp reprojectionUpscaleGeoCoding = new ReprojectionOp();
            reprojectionUpscaleGeoCoding.setParameter("crs", WGS84_CODE);
            reprojectionUpscaleGeoCoding.setParameter("pixelSizeX", pixelSizeX * scaling);
            reprojectionUpscaleGeoCoding.setParameter("pixelSizeY", -pixelSizeY * scaling);
            reprojectionUpscaleGeoCoding.setParameter("width", width);
            reprojectionUpscaleGeoCoding.setParameter("height", height);
            reprojectionUpscaleGeoCoding.setSourceProduct(reprojectedProduct);
            Product targetGeoCodingProduct = reprojectionUpscaleGeoCoding.getTargetProduct();
            ProductUtils.copyGeoCoding(targetGeoCodingProduct, upscaledProduct);
        } else {
            try {
                final GeoCoding mosaicGeoCoding = mosaicProduct.getGeoCoding();
                if (mosaicGeoCoding != null) {
                    upscaledProduct.setGeoCoding(mosaicGeoCoding);
                } else {
                    logger.log(Level.WARNING, "No geocoding available for mosaic product.");
                }
            } catch (Exception e) {
                throw new OperatorException("Cannot attach geocoding for mosaic product: ", e);
            }
        }

        targetProduct = upscaledProduct;
    }

    private File findRefTile() {

        final FilenameFilter adamFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // Adam.nr.yyyyMM.hXXvYY.dim
                String expectedFilename = "Adam.nr." + year + IOUtils.getMonthString(monthIndex) + "." + dir.getName() + ".dim";
                return name.equalsIgnoreCase(expectedFilename);
            }
        };

        logger.log(Level.ALL, "ADAM root directory is: '" + adamRootDir + "'...");
        final File[] adamFiles = GlobAlbedoMosaicProductReader.getAdamTileDirectories(adamRootDir);
        for (File adamFile : adamFiles) {
            File[] adamTileFiles = adamFile.listFiles(adamFilter);
            for (File adamTileFile : adamTileFiles) {
                if (adamTileFile.exists()) {
                    return adamTileFile;
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
        if (hasValidPixel(getSourceTile(nsamplesBand, srcRect))) {
            Map<String, Tile> srcTiles = getSourceTiles(srcRect);

            for (Band srcBand : reprojectedProduct.getBands()) {
                computeNearest(srcTiles.get(srcBand.getName()), targetTiles.get(srcBand.getName()),
                               srcTiles.get(ADAM_NSAMPLES_NAME));
            }
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
        double noDataValue = nsamplesBand.getNoDataValue();
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

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleAdam.class);
        }
    }
}
