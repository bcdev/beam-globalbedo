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


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.sdr.operators.GaMasterOp;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.util.ProductUtils;

import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

@OperatorMetadata(alias = "lc.l2")
public class LandcoverLevel2 extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Parameter(defaultValue = "true")
    private boolean step1;

    @Parameter(defaultValue = "true")
    private boolean step2;

    @Override
    public void initialize() throws OperatorException {
         // this setup allows to split processing in two parts:
        // 1. L1b --> AOT
        // 2. AOT --> BBDR
        // which seems to improve performance tremendously (more than factor 10 for a MERIS full orbit test product)
        Product targetProduct = sourceProduct;
        if (step1) {
            GaMasterOp gaMasterOp = new GaMasterOp();
            gaMasterOp.setParameter("copyToaRadBands", false);
            gaMasterOp.setParameter("copyToaReflBands", true);
            gaMasterOp.setParameter("gaUseL1bLandWaterFlag", false);
            gaMasterOp.setSourceProduct(targetProduct);
            targetProduct = gaMasterOp.getTargetProduct();
        }
        if (step2) {
            BbdrOp bbdrOp = new BbdrOp();
            bbdrOp.setSourceProduct(targetProduct);
            bbdrOp.setParameter("sensor", sensor);
            bbdrOp.setParameter("sdrOnly", true);
            targetProduct = bbdrOp.getTargetProduct();
        }
        setTargetProduct(targetProduct);
    }


   public static class Spi extends OperatorSpi {

        public Spi() {
            super(LandcoverLevel2.class);
        }
    }
}
