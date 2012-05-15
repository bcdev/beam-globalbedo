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

    private GeoCoding geoCoding;
    private Mask validMask;
    private Band latBand;
    private Band lonBand;

    private Band bestDeltaXBand;
    private Band bestDeltaYBand;
    private Band iterCounterBand;

    private Band minDeltaBand;
    private static final int SEARCH_RADIUS_SRC = 30;
    private static final int SEARCH_RADIUS = 6;
    private double deltaThreshold;
    private Band goodDeltaBand;

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

        bestDeltaXBand = targetProduct.addBand("best_delta_x", ProductData.TYPE_INT8);
        bestDeltaXBand.setNoDataValue(-1.0);
        bestDeltaXBand.setNoDataValueUsed(true);

        bestDeltaYBand = targetProduct.addBand("best_delta_y", ProductData.TYPE_INT8);
        bestDeltaYBand.setNoDataValue(-1.0);
        bestDeltaYBand.setNoDataValueUsed(true);

        iterCounterBand = targetProduct.addBand("iterations", ProductData.TYPE_INT8);

        minDeltaBand = targetProduct.addBand("min_delta", ProductData.TYPE_FLOAT32);
        minDeltaBand.setNoDataValue(Float.NaN);
        minDeltaBand.setNoDataValueUsed(true);

        goodDeltaBand = targetProduct.addBand("good_delta", ProductData.TYPE_INT8);

        GeoPos p0 = geoCoding.getGeoPos(new PixelPos(0.5f, 0.5f), null);
        GeoPos p1 = geoCoding.getGeoPos(new PixelPos(1.5f, 0.5f), null);
//
//        double distMeters = MathUtils.sphereDistanceDeg(6371.0f, p1.lon, p1.lat, p0.lon, p0.lat);
//        System.out.println("distMeters = " + distMeters);
//
//        double distDeg = MathUtils.sphereDistanceDeg(360.0f/2/Math.PI, p1.lon, p1.lat, p0.lon, p0.lat);
//        System.out.println("distDeg = " + distDeg);
//
//        double pixelKmdist = (6371 * 2 * Math.PI / 360.0) * distDeg;
//        System.out.println("pixelKmdist = " + pixelKmdist);

        float r = (float) Math.cos(Math.toRadians(p1.lat));
        float dlat = Math.abs(p0.lat - p1.lat);
        float dlon = r * lonDiff(p0.lon, p1.lon);
        float delta = dlat * dlat + dlon * dlon;
        deltaThreshold = Math.sqrt(delta) * Math.sqrt(2);


        System.out.println("deltaThreshold = " + deltaThreshold);
//        System.out.println("deltaThreshold sqr = " + (deltaThreshold * deltaThreshold));

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

        Tile bestDeltaX = targetTiles.get(bestDeltaXBand);
        Tile bestDeltaY = targetTiles.get(bestDeltaYBand);
        Tile iterCounter = targetTiles.get(iterCounterBand);
        Tile minDelta = targetTiles.get(minDeltaBand);
        Tile goodDelta = targetTiles.get(goodDeltaBand);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                PixelPos pixelPos = new PixelPos();
                pixelPos.setLocation(x, y);
                GeoPos geoPos = geoCoding.getGeoPos(pixelPos, null);
                int y1;
                int x1;
                int iterations = 0;
                float bestMinDelta;
                do {
                    x1 = (int) Math.floor(pixelPos.x);
                    y1 = (int) Math.floor(pixelPos.y);
                    bestMinDelta = findBestPixel(x1, y1, geoPos.lat, geoPos.lon, pixelPos, sourceRect, validTile, latTile, lonTile);
                    iterations++;
                } while (isBestPixelOnSearchBorder(x1, y1, pixelPos));

                bestDeltaX.setSample(x, y, Math.abs(pixelPos.x - x));
                bestDeltaY.setSample(x, y, Math.abs(pixelPos.y - y));
                iterCounter.setSample(x, y, iterations);
                minDelta.setSample(x, y, bestMinDelta);

                if (Math.sqrt(bestMinDelta) < deltaThreshold) {
                    goodDelta.setSample(x, y, 1);
                } else {
                    goodDelta.setSample(x, y, 0);
                }
            }
        }
    }

    private boolean isBestPixelOnSearchBorder(int x0, int y0, PixelPos bestPixel) {
        final int diffX = Math.abs((int) bestPixel.x - x0);
        final int diffY = Math.abs((int) bestPixel.y - y0);
        return diffX > (SEARCH_RADIUS - 2) || diffY > (SEARCH_RADIUS - 2);
    }

    private float findBestPixel(int x0, int y0,
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
        return minDelta;
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
