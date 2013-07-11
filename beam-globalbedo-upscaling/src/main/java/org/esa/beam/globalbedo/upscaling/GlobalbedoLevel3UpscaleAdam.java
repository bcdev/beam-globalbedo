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
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.mosaic.GlobAlbedoMosaicProductReader;
import org.esa.beam.globalbedo.mosaic.MosaicConstants;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
public class GlobalbedoLevel3UpscaleAdam extends GlobalbedoLevel3UpscaleBasisOp {

    @Parameter(defaultValue = "", description = "ADAM root directory")
    private String adamRootDir;
    // products at MSSL are like /disk/Globalbedo90/adam/processed/Monthly/h23v06/Adam.nr.200506.h23v06.dim
    //   general:                /disk/Globalbedo90/adam/processed/Monthly/hXXvYy/Adam.nr.yyyyMM.hXXvYY.dim

    @Parameter(valueSet = {"1.2"}, description = "Scaling (1.2 = 1/10deg resolution)", defaultValue = "1.2")
    private double scaling;


    @TargetProduct
    private Product targetProduct;


    private static final String ADAM_NSAMPLES_NAME = "GROUND_N_SAMPLES";
    private static final String ADAM_STAGE1_PRIOR_NSAMPLES_NAME = "Weighted number of samples";
    private static final String ADAM_STAGE2_PRIOR_NSAMPLES_NAME = "N samples";

    private Band nsamplesBand;

    @Override
    public void initialize() throws OperatorException {
        final File refTile = findRefTile();
        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No ADAM files for mosaicing found.");
        }

        final ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        if (productReader instanceof GlobAlbedoMosaicProductReader) {
            if (isPriors) {
                ((GlobAlbedoMosaicProductReader) productReader).setMosaicAdamPriors(true);
                ((GlobAlbedoMosaicProductReader) productReader).setAdamPriorStage(priorStage);
            } else {
                ((GlobAlbedoMosaicProductReader) productReader).setMosaicAdam(true);
            }
        }

        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(refTile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }

        setReprojectedProduct(mosaicProduct, MosaicConstants.ADAM_TILE_SIZE);

        final int width = (int) (MosaicConstants.ADAM_TILE_SIZE * MosaicConstants.NUM_H_TILES / scaling);
        final int height = (int) (MosaicConstants.ADAM_TILE_SIZE * MosaicConstants.NUM_V_TILES / scaling);
        final int tileWidth = (int) (MosaicConstants.ADAM_TILE_SIZE / scaling / 2);
        final int tileHeight = (int) (MosaicConstants.ADAM_TILE_SIZE / scaling / 2);

        setUpscaledProduct(mosaicProduct, width, height, tileWidth, tileHeight);

        String nsamplesBandName;
        if (isPriors) {
            if (priorStage == 1) {
                nsamplesBandName = ADAM_STAGE1_PRIOR_NSAMPLES_NAME;
            } else {
                nsamplesBandName = ADAM_STAGE2_PRIOR_NSAMPLES_NAME;
            }
        } else {
            nsamplesBandName = ADAM_NSAMPLES_NAME;
        }
        nsamplesBand = reprojectedProduct.getBand(nsamplesBandName);

        // we need ALL bands...
        for (Band srcBand : reprojectedProduct.getBands()) {
            String targetBandName = getTargetBandName(srcBand);
            Band band = upscaledProduct.addBand(targetBandName, srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        upscaledProduct.setStartTime(reprojectedProduct.getStartTime());
        upscaledProduct.setEndTime(reprojectedProduct.getEndTime());
        ProductUtils.copyMetadata(reprojectedProduct, upscaledProduct);
        upscaledProduct.setPreferredTileSize((int) (MosaicConstants.ADAM_TILE_SIZE / scaling / 4),
                                             (int) (MosaicConstants.ADAM_TILE_SIZE / scaling / 4));

        attachUpscaleGeoCoding(mosaicProduct, scaling, width, height, true);

        targetProduct = upscaledProduct;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle((int) (targetRect.x * scaling),
                                          (int) (targetRect.y * scaling),
                                          (int) (targetRect.width * scaling),
                                          (int) (targetRect.height * scaling));
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);
        Map<String, Tile> srcTiles = getSourceTiles(srcRect);

        if (hasValidPixel(getSourceTile(nsamplesBand, srcRect), nsamplesBand.getNoDataValue())) {
            for (Band srcBand : reprojectedProduct.getBands()) {
                String targetBandName = getTargetBandName(srcBand);
                computeNearest(srcTiles.get(srcBand.getName()), targetTiles.get(targetBandName),
                               srcTiles.get(nsamplesBand.getName()), scaling);
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

    private String getTargetBandName(Band srcBand) {
        String targetBandName = srcBand.getName();
        // if we have weird band names with colons and blanks, replace those with '_' ...
        if (targetBandName.contains(":") || srcBand.getName().contains(" ")) {
            final String s1 = targetBandName.replace(':', '_');
            targetBandName = s1.replace(' ', '_');
        }
        return targetBandName;
    }

    private File findRefTile() {
        FilenameFilter adamFilter = null;
        File[] adamFiles;
        if (isPriors) {
            if (priorStage == 1) {
                // e.g. <adamRootDir>/<tile>/stage1prior/processed/Kernels.001.005.h18v04.SnowAndNoSnow.hdr
                adamFilter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        String expectedFilenamePrefix = "Kernels." + IOUtils.getDoyString(doy);
                        String expectedFilenameSuffix = "SnowAndNoSnow.hdr";
                        return name.startsWith(expectedFilenamePrefix) && name.endsWith(expectedFilenameSuffix);
                    }
                };
            } else if (priorStage == 2) {
                // e.g. <adamRootDir>/<tile>/stage2prior/background/processed/Kernels.001.005.h18v04.backGround.SnowAndNoSnow.hdr
                adamFilter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        String expectedFilenamePrefix = "Kernels." + IOUtils.getDoyString(doy);
                        String expectedFilenameSuffix = "backGround.SnowAndNoSnow.hdr";
                        return name.startsWith(expectedFilenamePrefix) && name.endsWith(expectedFilenameSuffix);
                    }
                };
            } else if (priorStage == 3) {
                // e.g. <adamRootDir>/<tile>/background/processed/Kernels.001.005.h18v04.background.SnowAndNoSnow.nc
                adamFilter = new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        String expectedFilenamePrefix = "Kernels." + IOUtils.getDoyString(doy);
                        String expectedFilenameSuffix = "background.SnowAndNoSnow.nc";
                        return name.startsWith(expectedFilenamePrefix) && name.endsWith(expectedFilenameSuffix);
                    }
                };
            }
            adamFiles = GlobAlbedoMosaicProductReader.getAdamPriorTileDirectories(adamRootDir, priorStage);
        } else {
            adamFilter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    // Adam.nr.yyyyMM.hXXvYY.dim
                    String expectedFilename = "Adam.nr." + year + IOUtils.getMonthString(monthIndex) + "." + dir.getName() + ".dim";
                    return name.equalsIgnoreCase(expectedFilename);
                }
            };
            adamFiles = GlobAlbedoMosaicProductReader.getAdamTileDirectories(adamRootDir);
        }

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

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleAdam.class);
        }
    }
}
