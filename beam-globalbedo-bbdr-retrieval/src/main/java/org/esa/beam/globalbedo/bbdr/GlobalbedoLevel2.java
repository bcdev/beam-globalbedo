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

package org.esa.beam.globalbedo.bbdr;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.sdr.operators.GaMasterOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;

import java.awt.Dimension;

@OperatorMetadata(alias = "ga.l2")
public class GlobalbedoLevel2 extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        GaMasterOp gaMasterOp = new GaMasterOp();
        gaMasterOp.setParameter("copyToaRadBands", false);
        gaMasterOp.setParameter("copyToaReflBands", true);
        gaMasterOp.setSourceProduct(sourceProduct);
        Product aotProduct = gaMasterOp.getTargetProduct();
        Dimension preferredTileSizeAOT = aotProduct.getPreferredTileSize();
        System.out.println("preferredTileSize.aot = " + preferredTileSizeAOT);

        BbdrOp bbdrOp = new BbdrOp();
        bbdrOp.setSourceProduct(aotProduct);
        Product bbdrProduct = bbdrOp.getTargetProduct();
        Dimension preferredTileSizeBbdr = aotProduct.getPreferredTileSize();
        System.out.println("preferredTileSize.bbdr = " + preferredTileSizeBbdr);

        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameter("easting", 7783653.6);
        repro.setParameter("northing", 3335851.6);

        repro.setParameter("crs", "PROJCS[\"MODIS Sinusoidal\"," +
                "GEOGCS[\"WGS 84\"," +
                "  DATUM[\"WGS_1984\"," +
                "    SPHEROID[\"WGS 84\",6378137,298.257223563," +
                "      AUTHORITY[\"EPSG\",\"7030\"]]," +
                "    AUTHORITY[\"EPSG\",\"6326\"]]," +
                "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," +
                "  UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]]," +
                "   AUTHORITY[\"EPSG\",\"4326\"]]," +
                "PROJECTION[\"Sinusoidal\"]," +
                "PARAMETER[\"false_easting\",0.0]," +
                "PARAMETER[\"false_northing\",0.0]," +
                "PARAMETER[\"central_meridian\",0.0]," +
                "PARAMETER[\"semi_major\",6371007.181]," +
                "PARAMETER[\"semi_minor\",6371007.181]," +
                "UNIT[\"m\",1.0]," +
                "AUTHORITY[\"SR-ORG\",\"6974\"]]");

        repro.setParameter("resampling", "Nearest");
        repro.setParameter("includeTiePointGrids", true);
        repro.setParameter("referencePixelX", 0.0);
        repro.setParameter("referencePixelY", 0.0);
        repro.setParameter("orientation", 0.0);
        repro.setParameter("pixelSizeX", 926.6254330558);
        repro.setParameter("pixelSizeY", 926.6254330558);
        repro.setParameter("width", 1200);
        repro.setParameter("height", 1200);
        repro.setParameter("orthorectify", true);
        repro.setParameter("noDataValue", 0.0);
        repro.setSourceProduct(bbdrProduct);

        Product reproProduct = repro.getTargetProduct();
        Dimension preferredTileSizeRepro = reproProduct.getPreferredTileSize();
        System.out.println("preferredTileSize.repro = " + preferredTileSizeRepro);
        setTargetProduct(reproProduct);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(GlobalbedoLevel2.class);
        }
    }
}
