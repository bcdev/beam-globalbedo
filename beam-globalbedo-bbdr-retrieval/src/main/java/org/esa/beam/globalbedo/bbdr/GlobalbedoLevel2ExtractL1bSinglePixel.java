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

package org.esa.beam.globalbedo.bbdr;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.io.FileUtils;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.*;
import java.io.File;

/**
 * GlobAlbedo Level 2: generates a single pixel L1b CSV file, given a full L1b product and lat/lon coordinate
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.single.extract", autoWriteDisabled = true)
public class GlobalbedoLevel2ExtractL1bSinglePixel extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(description = "Latitude", interval = "(-90.0,90.0)")
    private float latSinglePixel;

    @Parameter(description = "Longitude", interval = "(-180.0, 180.0)")
    private float lonSinglePixel;

    @Parameter(defaultValue = ".")
    private String singlePixelDir;

//    private int w;
//    private int h;

    @Override
    public void initialize() throws OperatorException {
        final String sourceProductName = ((File) sourceProduct.getProductReader().getInput()).getName();
//        w = sourceProduct.getSceneRasterWidth();
//        h = sourceProduct.getSceneRasterHeight();

        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        if (geoCoding == null) {
            throw new OperatorException("source product '" + sourceProduct.getName() + "' has no geocoding - cannot proceed.");
        }

        final GeoPos geoPosSinglePixel = new GeoPos(latSinglePixel, lonSinglePixel);
        final PixelPos pixelPosSinglePixel = geoCoding.getPixelPos(geoPosSinglePixel, null);
        final int xSinglePixel = (int) pixelPosSinglePixel.x;
        final int ySinglePixel = (int) pixelPosSinglePixel.y;

        SubsetOp subsetOp = new SubsetOp();
        subsetOp.setParameterDefaultValues();
        subsetOp.setSourceProduct(sourceProduct);
        subsetOp.setRegion(new Rectangle(xSinglePixel, ySinglePixel, 1, 1));
        final Product singlePixelProduct = subsetOp.getTargetProduct();
        setTargetProduct(singlePixelProduct);

        final String singlePixelProductName =
                FileUtils.getFilenameWithoutExtension("subset_1x1_of_" + sourceProductName);
        singlePixelProduct.setName(singlePixelProductName);

        // lat/lon bands must be in source csv files!
        if (!singlePixelProduct.containsRasterDataNode("latitude")) {
            singlePixelProduct.addBand("latitude", ProductData.TYPE_FLOAT32).
                    setSourceImage(ConstantDescriptor.create((float) 1, (float) 1, new Float[]{latSinglePixel}, null));
        }
        if (!singlePixelProduct.containsRasterDataNode("longitude")) {
            singlePixelProduct.addBand("longitude", ProductData.TYPE_FLOAT32).
                    setSourceImage(ConstantDescriptor.create((float) 1, (float) 1, new Float[]{lonSinglePixel}, null));
        }

        singlePixelProduct.setProductType(sourceProduct.getProductType());
        singlePixelProduct.setStartTime(sourceProduct.getStartTime());
        singlePixelProduct.setEndTime(sourceProduct.getEndTime());

        writeCsvProduct(singlePixelProduct, singlePixelProductName);
    }


    private void writeCsvProduct(Product product, String productName) {
        File dir = new File(singlePixelDir);
        dir.mkdirs();
        File file = new File(dir, productName + ".csv");
        WriteOp writeOp = new WriteOp(product, file, "CSV");
        writeOp.writeProduct(ProgressMonitor.NULL);
    }



    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel2ExtractL1bSinglePixel.class);
        }
    }
}
