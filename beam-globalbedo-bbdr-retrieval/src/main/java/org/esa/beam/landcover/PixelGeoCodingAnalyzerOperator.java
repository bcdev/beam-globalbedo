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

package org.esa.beam.landcover;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Color;
import java.awt.Rectangle;
import java.lang.Float;
import java.lang.Math;
import java.lang.Override;
import java.lang.String;
import java.util.Map;

/**
 * This operator computes how many pixel in x and in y direction are
 * the difference between the first guess from the estimator and the best
 * matching pixel from the latitude longitude bands.
 * Also the number of iterations of shifting the search window are given.
 */
@OperatorMetadata(alias = "PixelGeocodingAnalyzer",
                  authors = "Marco Zuehlke, Martin Boettcher",
                  description = "Computes the required search distance i x and y direction " +
                          "and the number of required iterations for a PixelGeoCodding")
public class PixelGeoCodingAnalyzerOperator extends Operator {

    @SourceProduct
    private Product sourceProduct;

    private Band latBand;
    private Band lonBand;
    private Band best_delta_x;
    private Band best_delta_y;
    private Band iterCounter;
    private GeoCoding geoCoding;
    private Mask validMask;
    private static final int SEARCH_RADIUS_SRC = 30;
    private static final int SEARCH_RADIUS = 6;

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        String[] bandNames = sourceProduct.getBandNames();
        for (String bandName : bandNames) {
            if (!targetProduct.containsBand(bandName)) {
                ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
            }
        }
        validMask = sourceProduct.addMask("_mask_", "!l1_flags.INVALID", "", Color.RED, 0.5f);
        latBand = sourceProduct.getBand("corr_latitude");
        lonBand = sourceProduct.getBand("corr_longitude");
        geoCoding = sourceProduct.getGeoCoding();

        best_delta_x = targetProduct.addBand("best_delta_x", ProductData.TYPE_INT8);
        best_delta_x.setNoDataValue(-1.0);
        best_delta_x.setNoDataValueUsed(true);

        best_delta_y = targetProduct.addBand("best_delta_y", ProductData.TYPE_INT8);
        best_delta_y.setNoDataValue(-1.0);
        best_delta_y.setNoDataValueUsed(true);

        iterCounter = targetProduct.addBand("iterations", ProductData.TYPE_INT8);

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle srcRect = new Rectangle(targetRectangle);
        srcRect.grow(SEARCH_RADIUS_SRC, SEARCH_RADIUS_SRC);
        Rectangle sourceRect = srcRect.intersection(new Rectangle(0, 0, sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight()));

        Tile latTile = getSourceTile(latBand, sourceRect);
        Tile lonTile = getSourceTile(lonBand, sourceRect);
        Tile validTile = getSourceTile(validMask, sourceRect);

        Tile bestDelatX = targetTiles.get(best_delta_x);
        Tile bestDelatY = targetTiles.get(best_delta_y);
        Tile counter = targetTiles.get(iterCounter);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                if (validTile.getSampleBoolean(x, y)) {

                    PixelPos pixelPos = new PixelPos();
                    pixelPos.setLocation(x, y);
                    GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
                    int y1;
                    int x1;
                    int iterations = 0;
                    do {
                        x1 = (int) Math.floor(pixelPos.x);
                        y1 = (int) Math.floor(pixelPos.y);
                        findBestPixel(x1, y1, geoPos.lat, geoPos.lon, pixelPos, sourceRect, validTile, latTile, lonTile);
                        iterations++;
                    } while (isBestPixelOnSearchBorder(x1, y1, pixelPos));

                    bestDelatX.setSample(x, y, Math.abs(pixelPos.x - x));
                    bestDelatY.setSample(x, y, Math.abs(pixelPos.y - y));
                    counter.setSample(x, y, iterations);
                } else {
                    bestDelatX.setSample(x, y, -1);
                    bestDelatY.setSample(x, y, -1);
                    counter.setSample(x, y, 0);
                }
            }
        }
    }

    private boolean isBestPixelOnSearchBorder(int x0, int y0, PixelPos bestPixel) {
        final int diffX = Math.abs((int) bestPixel.x - x0);
        final int diffY = Math.abs((int) bestPixel.y - y0);
        return diffX > (SEARCH_RADIUS - 2) || diffY > (SEARCH_RADIUS - 2);
    }

    private void findBestPixel(int x0, int y0,
                               float lat0, float lon0,
                               PixelPos bestPixel,
                               Rectangle sourceRect,
                               Tile validTile, Tile latTile, Tile lonTile) {
        int x1 = x0 - SEARCH_RADIUS;
        int y1 = y0 - SEARCH_RADIUS;
        int x2 = x0 + SEARCH_RADIUS;
        int y2 = y0 + SEARCH_RADIUS;
        x1 = Math.max(x1, sourceRect.x);
        y1 = Math.max(y1, sourceRect.y);
        x2 = Math.min(x2, sourceRect.x + sourceRect.width - 1);
        y2 = Math.min(y2, sourceRect.y + sourceRect.height - 1);

        float r = (float) Math.cos(Math.toRadians(lat0));
        float minDelta = Float.NaN;

        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                if (validTile.getSampleBoolean(x, y)) {
                    float lat = latTile.getSampleFloat(x, y);
                    float lon = lonTile.getSampleFloat(x, y);

                    float dlat = Math.abs(lat - lat0);
                    float dlon = r * lonDiff(lon, lon0);
                    float delta = dlat * dlat + dlon * dlon;
                    if (Float.isNaN(minDelta) || delta < minDelta) {
                        minDelta = delta;
                        bestPixel.setLocation(x, y);
                    }
                }
            }
        }
    }

    /*
    * Computes the absolute and smaller difference for two angles.
    * @param a1 the first angle in the degrees (-180 <= a1 <= 180)
    * @param a2 the second angle in degrees (-180 <= a2 <= 180)
    * @return the difference between 0 and 180 degrees
    */

    private static float lonDiff(float a1, float a2) {
        float d = a1 - a2;
        if (d < 0.0f) {
            d = -d;
        }
        if (d > 180.0f) {
            d = 360.0f - d;
        }
        return d;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PixelGeoCodingAnalyzerOperator.class);
        }
    }


}
