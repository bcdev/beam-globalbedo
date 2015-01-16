/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.gpf.operators.meris.MerisBasisOp;
import org.esa.beam.idepix.algorithms.CloudShadowFronts;
import org.esa.beam.idepix.util.IdepixUtils;
import org.esa.beam.util.RectangleExtender;

import java.awt.*;
import java.util.HashMap;


/**
 * Operator used to consolidate status for LandCover:
 * - coastline refinement
 * - cloud buffer (LC algo as default)
 * - cloud shadow (from Fronts)
 */
@OperatorMetadata(alias = "lc.postprocess",
                  version = "2.1",
                  internal = true,
                  authors = "Marco Zuehlke, Olaf Danne",
                  copyright = "(c) 2014 by Brockmann Consult",
                  description = "Refines the cloud classification of Meris.GlobAlbedoCloudClassification operator.")
public class StatusPostProcessOp extends MerisBasisOp {

    public static final int STATUS_INVALID = 0;
    public static final int STATUS_LAND = 1;
    public static final int STATUS_WATER = 2;
    public static final int STATUS_SNOW = 3;
    public static final int STATUS_CLOUD = 4;
    public static final int STATUS_CLOUD_SHADOW = 5;
    public static final int STATUS_CLOUD_BUFFER = 6;
    public static final int STATUS_UCL_CLOUD = 10;

    static final String STATUS_BAND = "status";

    @Parameter(defaultValue = "true", label = " Use the LandCover advanced cloud buffer algorithm")
    private boolean gaLcCloudBuffer;

    @Parameter(defaultValue = "true",
               label = " Compute cloud shadow",
               description = " Compute cloud shadow with a preliminary algorithm")
    private boolean gaComputeCloudShadow;

    @Parameter(defaultValue = "true",
               label = " Refine pixel classification near coastlines",
               description = "Refine pixel classification near coastlines. ")
    private boolean gaRefineClassificationNearCoastlines;

    @Parameter(defaultValue = "true")
    private boolean useUclCloudForShadow;

    @SourceProduct(alias = "l1b")
    private Product l1bProduct;
    @SourceProduct(alias = "status")
    private Product statusProduct;
    @SourceProduct(alias = "ctp")
    private Product ctpProduct;

    private Band waterFractionBand;
    private Band origStatusBand;
    private Band ctpBand;
    private TiePointGrid szaTPG;
    private TiePointGrid saaTPG;
    private TiePointGrid altTPG;
    private GeoCoding geoCoding;

    private RectangleExtender rectCalculator;

    @Override
    public void initialize() throws OperatorException {
        Product postProcessedProduct = IdepixUtils.cloneProduct(statusProduct);
        postProcessedProduct.setAutoGrouping(statusProduct.getAutoGrouping());


        postProcessedProduct.addMask("status_land", "status == " + StatusPostProcessOp.STATUS_LAND, "", Color.GREEN, 0.5f);
        postProcessedProduct.addMask("status_water", "status == " + StatusPostProcessOp.STATUS_WATER, "", Color.BLUE, 0.5f);
        postProcessedProduct.addMask("status_snow", "status == " + StatusPostProcessOp.STATUS_SNOW, "", Color.YELLOW, 0.5f);
        postProcessedProduct.addMask("status_cloud", "status == " + StatusPostProcessOp.STATUS_CLOUD, "", Color.WHITE, 0.5f);
        postProcessedProduct.addMask("status_cloud_shadow", "status == " + StatusPostProcessOp.STATUS_CLOUD_SHADOW, "", Color.GRAY, 0.5f);
        postProcessedProduct.addMask("status_cloud_buffer", "status == " + StatusPostProcessOp.STATUS_CLOUD_BUFFER, "", Color.RED, 0.5f);
        postProcessedProduct.addMask("status_cloud_ucl", "status == " + StatusPostProcessOp.STATUS_UCL_CLOUD, "", Color.ORANGE, 0.5f);

        HashMap<String, Object> waterParameters = new HashMap<>();
        waterParameters.put("resolution", 50);
        waterParameters.put("subSamplingFactorX", 3);
        waterParameters.put("subSamplingFactorY", 3);
        Product waterMaskProduct = GPF.createProduct("LandWaterMask", waterParameters, l1bProduct);
        waterFractionBand = waterMaskProduct.getBand("land_water_fraction");

        geoCoding = l1bProduct.getGeoCoding();

        origStatusBand = statusProduct.getBand(STATUS_BAND);
        szaTPG = l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME);
        saaTPG = l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME);
        altTPG = l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME);
        ctpBand = ctpProduct.getBand("cloud_top_press");
        int extendedWidth;
        int extendedHeight;
        if (l1bProduct.getProductType().startsWith("MER_F")) {
            extendedWidth = 64;
            extendedHeight = 64;
        } else {
            extendedWidth = 16;
            extendedHeight = 16;
        }

        rectCalculator = new RectangleExtender(new Rectangle(l1bProduct.getSceneRasterWidth(),
                                                             l1bProduct.getSceneRasterHeight()),
                                               extendedWidth, extendedHeight
        );

        postProcessedProduct.getBand(STATUS_BAND).setSourceImage(null);
        setTargetProduct(postProcessedProduct);
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetStatusTile, ProgressMonitor pm) throws OperatorException {

        final Rectangle targetRectangle = targetStatusTile.getRectangle();
        final Rectangle sourceRectangle = rectCalculator.extend(targetRectangle);

        final Tile sourceStatusTile = getSourceTile(origStatusBand, sourceRectangle);
        Tile szaTile = getSourceTile(szaTPG, sourceRectangle);
        Tile saaTile = getSourceTile(saaTPG, sourceRectangle);
        Tile ctpTile = getSourceTile(ctpBand, sourceRectangle);
        Tile altTile = getSourceTile(altTPG, targetRectangle);
        final Tile waterFractionTile = getSourceTile(waterFractionBand, sourceRectangle);

        for (int y = sourceRectangle.y; y < sourceRectangle.y + sourceRectangle.height; y++) {
            checkForCancellation();
            for (int x = sourceRectangle.x; x < sourceRectangle.x + sourceRectangle.width; x++) {

                if (targetRectangle.contains(x, y)) {
                    final int srcStatus = sourceStatusTile.getSampleInt(x, y);
                    targetStatusTile.setSample(x, y, srcStatus);

                    if (gaRefineClassificationNearCoastlines) {
                        if (srcStatus == STATUS_CLOUD || srcStatus == STATUS_SNOW || srcStatus == STATUS_UCL_CLOUD) {
                            if (isNearCoastline(x, y, waterFractionTile, sourceRectangle)) {
                                refineCloudSnowFlaggingForCoastlines(x, y, sourceStatusTile, waterFractionTile, targetStatusTile, sourceRectangle);
                            }
                        }
                    }
                }
            }
        }
        if (gaLcCloudBuffer) {
            computeCloudBufferLC(targetStatusTile);
        }
        if (gaComputeCloudShadow) {
            CloudShadowFronts cloudShadowFronts = new CloudShadowFronts(
                    geoCoding,
                    sourceRectangle,
                    targetRectangle,
                    szaTile, saaTile, ctpTile, altTile) {

                @Override
                protected boolean isCloudForShadow(int x, int y) {
                    int srcStatus;
                    if (!targetRectangle.contains(x, y)) {
                        srcStatus = sourceStatusTile.getSampleInt(x, y);
                    } else {
                        srcStatus = targetStatusTile.getSampleInt(x, y);
                    }
                    return srcStatus == STATUS_CLOUD || (useUclCloudForShadow && srcStatus == STATUS_UCL_CLOUD);
                }

                @Override
                protected boolean isCloudFree(int x, int y) {
                    int srcStatus;
                    if (!targetRectangle.contains(x, y)) {
                        srcStatus = sourceStatusTile.getSampleInt(x, y);
                    } else {
                        srcStatus = targetStatusTile.getSampleInt(x, y);
                    }
                    return srcStatus == STATUS_LAND || srcStatus == STATUS_SNOW;
                }

                @Override
                protected boolean isSurroundedByCloud(int x, int y) {
                    int surroundingPixelCount = 0;
                    for (int i = x - 1; i <= x + 1; i++) {
                        for (int j = y - 1; j <= y + 1; j++) {
                            int srcStatus;
                            if (!targetRectangle.contains(x, y)) {
                                srcStatus = sourceStatusTile.getSampleInt(x, y);
                            } else {
                                srcStatus = targetStatusTile.getSampleInt(x, y);
                            }
                            if (srcStatus == STATUS_CLOUD || (useUclCloudForShadow && srcStatus == STATUS_UCL_CLOUD)) {
                                surroundingPixelCount++;
                            }
                        }
                    }
                    return (surroundingPixelCount >= 6);  // at least 6 pixel in a 3x3 box
                }

                @Override
                protected void setCloudShadow(int x, int y) {
                    int srcStatus = targetStatusTile.getSampleInt(x, y);
                    if (srcStatus == STATUS_LAND || srcStatus == STATUS_SNOW) {
                        targetStatusTile.setSample(x, y, STATUS_CLOUD_SHADOW);
                    }
                }
            };
            cloudShadowFronts.computeCloudShadow();
        }
    }

    private boolean isCoastlinePixel(int x, int y, Tile waterFractionTile) {
        boolean isCoastline = false;
        // the water mask ends at 59 Degree south, stop earlier to avoid artefacts
        if (getGeoPos(x, y).lat > -58f) {
            final int waterFraction = waterFractionTile.getSampleInt(x, y);
            // values bigger than 100 indicate no data
            if (waterFraction <= 100) {
                // todo: this does not work if we have a PixelGeocoding. In that case, waterFraction
                // is always 0 or 100!! (TS, OD, 20140502)
                isCoastline = waterFraction < 100 && waterFraction > 0;
            }
        }
        return isCoastline;
    }

    private GeoPos getGeoPos(int x, int y) {
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos(x, y);
        geoCoding.getGeoPos(pixelPos, geoPos);
        return geoPos;
    }

    private boolean isNearCoastline(int x, int y, Tile waterFractionTile, Rectangle rectangle) {
        final int windowWidth = 1;
        final int LEFT_BORDER = Math.max(x - windowWidth, rectangle.x);
        final int RIGHT_BORDER = Math.min(x + windowWidth, rectangle.x + rectangle.width - 1);
        final int TOP_BORDER = Math.max(y - windowWidth, rectangle.y);
        final int BOTTOM_BORDER = Math.min(y + windowWidth, rectangle.y + rectangle.height - 1);

        final int waterFractionCenter = waterFractionTile.getSampleInt(x, y);
        boolean isTiePointGeo = geoCoding instanceof TiePointGeoCoding;
        boolean isCrsGeo = geoCoding instanceof CrsGeoCoding;

        for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
            for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                // TODO pixel-geo-coding with fraction-accuracy does work as well !!!
                if ((isTiePointGeo || isCrsGeo)) {
                    if (isCoastlinePixel(i, j, waterFractionTile)) {
                        return true;
                    }
                } else {
                    if (waterFractionTile.getSampleInt(i, j) != waterFractionCenter) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void refineCloudSnowFlaggingForCoastlines(int x, int y, Tile sourceFlagTile, Tile waterFractionTile, Tile targetTile, Rectangle srcRectangle) {
        final int windowWidth = 1;
        final int LEFT_BORDER = Math.max(x - windowWidth, srcRectangle.x);
        final int RIGHT_BORDER = Math.min(x + windowWidth, srcRectangle.x + srcRectangle.width - 1);
        final int TOP_BORDER = Math.max(y - windowWidth, srcRectangle.y);
        final int BOTTOM_BORDER = Math.min(y + windowWidth, srcRectangle.y + srcRectangle.height - 1);
        boolean removeCloudFlag = true;
        if (isPixelSurroundedByCloudOrSnow(x, y, sourceFlagTile)) {
            removeCloudFlag = false;
        } else {
            Rectangle targetTileRectangle = targetTile.getRectangle();
            for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
                for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                    if (targetTileRectangle.contains(i, j)) {
                        int srcStatus = sourceFlagTile.getSampleInt(i, j);
                        boolean is_cloud_or_snow = srcStatus == STATUS_CLOUD || srcStatus == STATUS_SNOW || srcStatus == STATUS_UCL_CLOUD;
                        if (is_cloud_or_snow && !isNearCoastline(i, j, waterFractionTile, srcRectangle)) {
                            removeCloudFlag = false;
                            break;
                        }
                    }
                }
            }
        }

        if (removeCloudFlag) {
            targetTile.setSample(x, y, STATUS_LAND);
        }
    }

    private static boolean isPixelSurroundedByCloudOrSnow(int x, int y, Tile sourceFlagTile) {
        // check if pixel is surrounded by other pixels flagged as 'pixelFlag'
        int surroundingPixelCount = 0;
        Rectangle rectangle = sourceFlagTile.getRectangle();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (rectangle.contains(i, j)) {
                    int srcStatus = sourceFlagTile.getSampleInt(i, j);
                    if (srcStatus == STATUS_CLOUD || srcStatus == STATUS_SNOW || srcStatus == STATUS_UCL_CLOUD) {
                        surroundingPixelCount++;
                    }
                }
            }
        }
        return (surroundingPixelCount * 1.0 / 9 >= 0.7);  // at least 6 pixel in a 3x3 box
    }

    private static boolean isStatusCloud(int x, int y, Tile statusTile) {
        int srcStatus = statusTile.getSampleInt(x, y);
        return srcStatus == STATUS_CLOUD || srcStatus == STATUS_UCL_CLOUD;
    }

    private static void setStatusCloudBuffer(int x, int y, Tile statusTile) {
        int srcStatus = statusTile.getSampleInt(x, y);
        if (!(srcStatus == STATUS_CLOUD || srcStatus == STATUS_UCL_CLOUD || srcStatus == STATUS_INVALID)) {
            statusTile.setSample(x, y, STATUS_CLOUD_BUFFER);
        }
    }

    public static void computeCloudBufferLC(Tile targetTile) {
        //  set alternative cloud buffer flag as used in LC-CCI project:
        // 1. use 2x2 square with reference pixel in upper left
        // 2. move this square row-by-row over the tile
        // 3. if reference pixel is not clouds, don't do anything
        // 4. if reference pixel is cloudy:
        //    - if 2x2 square only has cloud pixels, then set cloud buffer of two pixels
        //      in both x and y direction of reference pixel.
        //    - if 2x2 square also has non-cloudy pixels, do the same but with cloud buffer of only 1

        Rectangle rectangle = targetTile.getRectangle();
        for (int y = rectangle.y; y < rectangle.y + rectangle.height - 1; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width - 1; x++) {
                if (isStatusCloud(x, y, targetTile)) {
                    // reference pixel is upper left (x, y)
                    // first set buffer of 1 in each direction
                    int bufferWidth = 1;
                    int LEFT_BORDER = Math.max(x - bufferWidth, rectangle.x);
                    int RIGHT_BORDER = Math.min(x + bufferWidth, rectangle.x + rectangle.width - 1);
                    int TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
                    int BOTTOM_BORDER = Math.min(y + bufferWidth, rectangle.y + rectangle.height - 1);
                    // now check if whole 2x2 square (x+1,y), (x, y+1), (x+1, y+1) is cloudy
                    if (isStatusCloud(x + 1, y, targetTile) &&
                            isStatusCloud(x, y + 1, targetTile) &&
                            isStatusCloud(x + 1, y + 1, targetTile)) {
                        // set buffer of 2 in each direction
                        bufferWidth = 2;
                        LEFT_BORDER = Math.max(x - bufferWidth, rectangle.x);
                        RIGHT_BORDER = Math.min(x + 1 + bufferWidth, rectangle.x + rectangle.width - 1);
                        TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
                        BOTTOM_BORDER = Math.min(y + 1 + bufferWidth, rectangle.y + rectangle.height - 1);
                    }
                    for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
                        for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                            setStatusCloudBuffer(i, j, targetTile);
                        }
                    }
                }
            }
        }
        int bufferWidth = 1;

        // south tile boundary...
        final int ySouth = rectangle.y + rectangle.height - 1;
        for (int x = rectangle.x; x < rectangle.x + rectangle.width - 1; x++) {
            int LEFT_BORDER = Math.max(x - bufferWidth, rectangle.x);
            int RIGHT_BORDER = Math.min(x + bufferWidth, rectangle.x + rectangle.width - 1);
            int TOP_BORDER = Math.max(rectangle.y, ySouth - bufferWidth);
            if (isStatusCloud(x, ySouth, targetTile)) {
                for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
                    for (int j = TOP_BORDER; j <= ySouth; j++) {
                        setStatusCloudBuffer(i, j, targetTile);
                    }
                }
            }
        }

        // east tile boundary...
        final int xEast = rectangle.x + rectangle.width - 1;
        for (int y = rectangle.y; y < rectangle.y + rectangle.height - 1; y++) {
            int LEFT_BORDER = Math.max(rectangle.x, xEast - bufferWidth);
            int TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
            int BOTTOM_BORDER = Math.min(y + bufferWidth, rectangle.y + rectangle.height - 1);
            if (isStatusCloud(xEast, y, targetTile)) {
                for (int i = LEFT_BORDER; i <= xEast; i++) {
                    for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                        setStatusCloudBuffer(i, j, targetTile);
                    }
                }
            }
        }
        // pixel in lower right corner...
        if (isStatusCloud(xEast, ySouth, targetTile)) {
            for (int i = Math.max(rectangle.x, xEast - 1); i <= xEast; i++) {
                for (int j = Math.max(rectangle.y, ySouth - 1); j <= ySouth; j++) {
                    setStatusCloudBuffer(i, j, targetTile);
                }
            }
        }
    }



    public static class Spi extends OperatorSpi {

        public Spi() {
            super(StatusPostProcessOp.class);
        }
    }
}
