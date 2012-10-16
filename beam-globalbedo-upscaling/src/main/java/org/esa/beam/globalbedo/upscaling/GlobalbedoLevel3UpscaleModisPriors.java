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
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
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

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.PRIOR_MASK_NAME;

/**
 * Reprojects and upscales MODIS prior products
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
public class GlobalbedoLevel3UpscaleModisPriors extends GlobalbedoLevel3UpscaleBasisOp {

    @Parameter(valueSet = {"5", "6", "60"}, description = "Scaling (5 = 1/24deg, 6 = 1/20deg, 60 = 1/2deg resolution", defaultValue = "60")
    private int scaling;

    @TargetProduct
    private Product targetProduct;

    private String[] priorMeanBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String maskBandName;
    private Band maskBand;

    @Override
    public void initialize() throws OperatorException {
        final File refTile = findRefTile();
        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No prior files for mosaicing found.");
        }

        final ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        if (productReader instanceof GlobAlbedoMosaicProductReader) {
            ((GlobAlbedoMosaicProductReader) productReader).setMosaicModisPriors(true);
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

        priorMeanBandNames = IOUtils.getPriorMeanBandNames();
        maskBandName = PRIOR_MASK_NAME;
        maskBand = reprojectedProduct.getBand(maskBandName);

        // for performance, just write one band ...
        Band band = upscaledProduct.addBand(priorMeanBandNames[0], ProductData.TYPE_FLOAT32);
        ProductUtils.copyRasterDataNodeProperties(reprojectedProduct.getBand(priorMeanBandNames[0]), band);
        band.setNoDataValue(Float.NaN);
        band.setNoDataValueUsed(true);
        band = upscaledProduct.addBand(maskBandName, ProductData.TYPE_FLOAT32);
        ProductUtils.copyRasterDataNodeProperties(reprojectedProduct.getBand(maskBandName), band);
        band.setNoDataValue(Float.NaN);
        band.setNoDataValueUsed(true);

        upscaledProduct.setStartTime(reprojectedProduct.getStartTime());
        upscaledProduct.setEndTime(reprojectedProduct.getEndTime());
        ProductUtils.copyMetadata(reprojectedProduct, upscaledProduct);
        upscaledProduct.setPreferredTileSize(MosaicConstants.MODIS_TILE_SIZE / scaling / 4,
                                             MosaicConstants.MODIS_TILE_SIZE / scaling / 4);

        attachUpscaleGeoCoding(mosaicProduct, scaling, width, height, reprojectToPlateCarre);

        targetProduct = upscaledProduct;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle(targetRect.x * scaling,
                targetRect.y * scaling,
                targetRect.width * scaling,
                targetRect.height * scaling);
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);
        if (hasValidPixel(getSourceTile(maskBand, srcRect), maskBand.getNoDataValue())) {
            Map<String, Tile> srcTiles = getSourceTiles(srcRect);

            computeNearest(srcTiles.get(priorMeanBandNames[0]), targetTiles.get(priorMeanBandNames[0]),
                    srcTiles.get(maskBandName), scaling);

            computeNearest(srcTiles.get(maskBandName), targetTiles.get(maskBandName),
                    srcTiles.get(maskBandName), scaling);
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
        final FilenameFilter priorFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
//                e.g. Kernels.129.005.h18v04.backGround.NoSnow
                String expectedFilenamePrefix = "Kernels." + IOUtils.getDoyString(doy);
                String expectedFilenameSuffix = ".hdr";
                return name.startsWith(expectedFilenamePrefix) && name.endsWith(expectedFilenameSuffix);
            }
        };

        final File[] priorFiles = GlobAlbedoMosaicProductReader.getModisPriorTileDirectories(priorRootDir);
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

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleModisPriors.class);
        }
    }
}
