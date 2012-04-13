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
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;

@OperatorMetadata(alias = "lc.uclcloudbuffer",
                  description = "Create Meris product for input to Globalbedo aerosol retrieval and BBDR processor",
                  authors = "marco Zuehlke",
                  version = "1.0",
                  copyright = "(C) 2012 by Brockmann Consult")
public class LcUclCloudBuffer extends Operator {

    private static final int STATUS_GOOD = 3;
    private static final int UCL_CLOUD = 10;
    private static final int UCL_CLOUD_BUFFER = 11;

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        String[] bandNames = sourceProduct.getBandNames();
        for (String bandName : bandNames) {
            boolean isStatus = bandName.equals("status");
            ProductUtils.copyBand(bandName, sourceProduct, targetProduct, !isStatus);
            if (isStatus) {
                ImageInfo statusII = sourceProduct.getBand("status").getImageInfo();
                targetProduct.getBand("status").setImageInfo(statusII.createDeepCopy());
            }
        }
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        sourceProduct.transferGeoCodingTo(targetProduct, null);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();
        Tile sourceTile = getSourceTile(sourceProduct.getBand("status"), rectangle);

        //  set alternative cloud buffer flag as used in LC-CCI project:
        // 1. use 2x2 square with reference pixel in upper left
        // 2. move this square row-by-row over the tile
        // 3. if reference pixel is not clouds, don't do anything
        // 4. if reference pixel is cloudy:
        //    - if 2x2 square only has cloud pixels, then set cloud buffer of two pixels
        //      in both x and y direction of reference pixel.
        //    - if 2x2 square also has non-cloudy pixels, do the same but with cloud buffer of only 1

        ProductData rawSamples = sourceTile.getRawSamples();
        targetTile.setRawSamples(rawSamples);

        for (int y = rectangle.y; y < rectangle.y + rectangle.height - 1; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width - 1; x++) {
                if (sourceTile.getSampleInt(x, y) == UCL_CLOUD) {
                    // reference pixel is upper left (x, y)
                    // first set buffer of 1 in each direction
                    int bufferWidth = 1;
                    int LEFT_BORDER = Math.max(x - bufferWidth, rectangle.x);
                    int RIGHT_BORDER = Math.min(x + bufferWidth, rectangle.x + rectangle.width - 1);
                    int TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
                    int BOTTOM_BORDER = Math.min(y + bufferWidth, rectangle.y + rectangle.height - 1);
                    // now check if whole 2x2 square (x+1,y), (x, y+1), (x+1, y+1) is cloudy
                    if (targetTile.getSampleInt(x + 1, y) == UCL_CLOUD &&
                            targetTile.getSampleInt(x, y + 1) == UCL_CLOUD &&
                            targetTile.getSampleInt(x + 1, y + 1) == UCL_CLOUD) {
                        // set buffer of 2 in each direction
                        bufferWidth = 2;
                        LEFT_BORDER = Math.max(x - bufferWidth, rectangle.x);
                        RIGHT_BORDER = Math.min(x + 1 + bufferWidth, rectangle.x + rectangle.width - 1);
                        TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
                        BOTTOM_BORDER = Math.min(y + 1 + bufferWidth, rectangle.y + rectangle.height - 1);
                    }
                    for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
                        for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                            if (targetTile.getSampleInt(i, j) <= STATUS_GOOD) {
                                targetTile.setSample(i, j, UCL_CLOUD_BUFFER);
                            }
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
//                int TOP_BORDER = ySouth - bufferWidth;
            int TOP_BORDER = Math.max(rectangle.y, ySouth - bufferWidth);
            if (targetTile.getSampleInt(x, ySouth) == UCL_CLOUD) {
                for (int i = LEFT_BORDER; i <= RIGHT_BORDER; i++) {
                    for (int j = TOP_BORDER; j <= ySouth; j++) {
                        if (targetTile.getSampleInt(i, j) <= STATUS_GOOD) {
                            targetTile.setSample(i, j, UCL_CLOUD_BUFFER);
                        }
                    }
                }
            }
        }

        // east tile boundary...
        final int xEast = rectangle.x + rectangle.width - 1;
        for (int y = rectangle.y; y < rectangle.y + rectangle.height - 1; y++) {
//                int LEFT_BORDER = xEast - bufferWidth;
            int LEFT_BORDER = Math.max(rectangle.x, xEast - bufferWidth);
            int TOP_BORDER = Math.max(y - bufferWidth, rectangle.y);
            int BOTTOM_BORDER = Math.min(y + bufferWidth, rectangle.y + rectangle.height - 1);
            if (targetTile.getSampleInt(xEast, y) == UCL_CLOUD) {
                for (int i = LEFT_BORDER; i <= xEast; i++) {
                    for (int j = TOP_BORDER; j <= BOTTOM_BORDER; j++) {
                        if (targetTile.getSampleInt(i, j) <= STATUS_GOOD) {
                            targetTile.setSample(i, j, UCL_CLOUD_BUFFER);
                        }
                    }
                }
            }
        }
        // pixel in lower right corner...
        if (targetTile.getSampleInt(xEast, ySouth) == UCL_CLOUD) {
            for (int i = Math.max(rectangle.x, xEast - 1); i <= xEast; i++) {
                for (int j = Math.max(rectangle.y, ySouth - 1); j <= ySouth; j++) {
                    if (targetTile.getSampleInt(i, j) <= STATUS_GOOD) {
                        targetTile.setSample(i, j, UCL_CLOUD_BUFFER);
                    }
                }
            }
        }

        for (int y = rectangle.y; y < rectangle.y + rectangle.height - 1; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width - 1; x++) {
                int status = targetTile.getSampleInt(x, y);
                if (status == 10 || status == 11) {
                    // map ucl_clod and ucl_cloud_buffer to cloud
                    targetTile.setSample(x, y, 4);
                }
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcUclCloudBuffer.class);
        }
    }
}
