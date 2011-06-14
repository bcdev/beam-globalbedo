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
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reprojects and upscales the GlobAlbedo product
 * that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.
 *
 * @author MarcoZ
 */
@OperatorMetadata(
        alias = "ga.upscale",
        authors = "Marco Zuehlke",
        copyright = "2011 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Reprojects and upscales the GlobAlbedo product \n" +
                " that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.")
public class Upscale extends Operator {

    private static final String WGS84_CODE = "EPSG:4326";
    private static final int TILE_SIZE = 1200;

    @Parameter
    private File tile;

    @Parameter(valueSet = {"6", "60"}, defaultValue = "60")
    private int scaling;

    @TargetProduct
    private Product targetProduct;

    private Product reprojectedProduct;

    @Override
    public void initialize() throws OperatorException {
        if (tile == null || !tile.exists()) {
            throw new OperatorException("No tile paramter given.");
        }
        ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(tile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + tile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }
        ReprojectionOp reprojection = new ReprojectionOp();
        reprojection.setParameter("crs", WGS84_CODE);
        reprojection.setSourceProduct(mosaicProduct);
        reprojectedProduct = reprojection.getTargetProduct();
        reprojectedProduct.setPreferredTileSize(TILE_SIZE / 4, TILE_SIZE / 4);

        /////////////////////////////////////////////////////////////////////////////////////////////////
        SubsetOp subsetOp = new SubsetOp();
        subsetOp.setSourceProduct(reprojectedProduct);
        subsetOp.setRegion(new Rectangle(17* TILE_SIZE, 3* TILE_SIZE, 3* TILE_SIZE, 3* TILE_SIZE));
        reprojectedProduct = subsetOp.getTargetProduct();
        /////////////////////////////////////////////////////////////////////////////////////////////////


        int width = reprojectedProduct.getSceneRasterWidth() / scaling;
        int height = reprojectedProduct.getSceneRasterHeight() / scaling;
        Product upscaledProduct = new Product(mosaicProduct.getName()+"_upscaled", "GA_UPSCALED", width, height);
        for (Band srcBand : reprojectedProduct.getBands()) {
            if (!srcBand.getName().startsWith("VAR") && !srcBand.getName().startsWith("mean")) {
                Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
                ProductUtils.copyRasterDataNodeProperties(srcBand, band);
            }
        }
        upscaledProduct.setStartTime(reprojectedProduct.getStartTime());
        upscaledProduct.setEndTime(reprojectedProduct.getEndTime());
        ProductUtils.copyMetadata(reprojectedProduct, upscaledProduct);
        upscaledProduct.setPreferredTileSize(TILE_SIZE / scaling / 4, TILE_SIZE / scaling / 4);



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

        targetProduct = upscaledProduct;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle(targetRectangle.x * scaling,
                                          targetRectangle.y * scaling,
                                          targetRectangle.width * scaling,
                                          targetRectangle.height * scaling);
        Band[] bands = reprojectedProduct.getBands();
        Map<String, Tile> srcTiles = new HashMap<String, Tile>(bands.length);
        for (int i = 0; i < bands.length; i++) {
            srcTiles.put(bands[i].getName(), getSourceTile(bands[i], srcRect));
        }

        handleMajority(AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, srcTiles, targetTiles);

        handleNearest(AlbedoInversionConstants.INV_ENTROPY_BAND_NAME, srcTiles, targetTiles);
        handleNearest(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME, srcTiles, targetTiles);
        handleNearest(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME, srcTiles, targetTiles);
        handleNearest(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME, srcTiles, targetTiles);
    }

    private void handleNearest(String bandName, Map<String, Tile> srcTiles, Map<Band, Tile> targetTiles) {
        Tile srcTile = srcTiles.get(bandName);
        Band targetBand = getTargetProduct().getBand(bandName);
        Tile targetTile = targetTiles.get(targetBand);
        handleNearest(srcTile, targetTile);
    }

    private void handleNearest(Tile src, Tile target) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                float sample = src.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                target.setSample(x, y, sample);
            }
        }
    }

    private void handleMajority(String bandName, Map<String, Tile> srcTiles, Map<Band, Tile> targetTiles) {
        Tile srcTile = srcTiles.get(bandName);
        Band targetBand = getTargetProduct().getBand(bandName);
        Tile targetTile = targetTiles.get(targetBand);
        handleMajority(srcTile, targetTile);
    }

    private void handleMajority(Tile src, Tile target) {
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
                target.setSample(x, y, max);
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Upscale.class);
        }
    }
}
