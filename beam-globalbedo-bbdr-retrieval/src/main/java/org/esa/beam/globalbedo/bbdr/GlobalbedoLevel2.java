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

@OperatorMetadata(alias = "ga.l2")
public class GlobalbedoLevel2 extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Parameter(defaultValue = "false")
    private boolean computeL1ToAotProductOnly;

    @Parameter(defaultValue = "false")
    private boolean computeAotToBbdrProductOnly;

    @Parameter
    private double easting;

    @Parameter
    private double northing;

    @Override
    public void initialize() throws OperatorException {
        // old setup
//        Geometry geometry = computeProductGeometry(reproject(sourceProduct));
//        SubsetOp subsetOp = new SubsetOp();
//        subsetOp.setGeoRegion(geometry);
//        subsetOp.setSourceProduct(sourceProduct);
//        Product targetProduct = subsetOp.getTargetProduct();
//
//        GaMasterOp gaMasterOp = new GaMasterOp();
//        gaMasterOp.setParameter("copyToaRadBands", false);
//        gaMasterOp.setParameter("copyToaReflBands", true);
//        gaMasterOp.setSourceProduct(targetProduct);
//        Product aotProduct = gaMasterOp.getTargetProduct();
//
//        if (computeL1ToAotProductOnly) {
//            setTargetProduct(aotProduct);
//        } else {
//            BbdrOp bbdrOp = new BbdrOp();
//            bbdrOp.setSourceProduct(aotProduct);
//            bbdrOp.setParameter("sensor", sensor);
//            Product bbdrProduct = bbdrOp.getTargetProduct();
//
//            setTargetProduct(reproject(bbdrProduct));
//        }

        // this setup allows to split processing in two parts:
        // 1. L1b --> AOT
        // 2. AOT --> BBDR
        // which seems to improve performance tremendously (more than factor 10 for a MERIS full orbit test product)
        Product targetProduct = null;
        Product aotProduct = null;
        if (!computeAotToBbdrProductOnly) {
            Geometry geometry = computeProductGeometry(reproject(sourceProduct));
            SubsetOp subsetOp = new SubsetOp();
            subsetOp.setGeoRegion(geometry);
            subsetOp.setSourceProduct(sourceProduct);
            targetProduct = subsetOp.getTargetProduct();

            GaMasterOp gaMasterOp = new GaMasterOp();
            gaMasterOp.setParameter("copyToaRadBands", false);
            gaMasterOp.setParameter("copyToaReflBands", true);
            gaMasterOp.setSourceProduct(targetProduct);
            aotProduct = gaMasterOp.getTargetProduct();
        }  else {
            aotProduct = sourceProduct;
        }

        if (computeL1ToAotProductOnly) {
            setTargetProduct(aotProduct);
        } else {
            BbdrOp bbdrOp = new BbdrOp();
            bbdrOp.setSourceProduct(aotProduct);
            bbdrOp.setParameter("sensor", sensor);
            Product bbdrProduct = bbdrOp.getTargetProduct();

            setTargetProduct(reproject(bbdrProduct));
        }
    }

    private Product reproject(Product bbdrProduct) {
        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameter("easting", easting);
        repro.setParameter("northing", northing);

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
        repro.setParameter("includeTiePointGrids", false);
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
        return repro.getTargetProduct();
    }

    static Geometry computeProductGeometry(Product product) {
        final GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(product);
        final Polygon[] polygons = new Polygon[paths.length];
        final GeometryFactory factory = new GeometryFactory();
        for (int i = 0; i < paths.length; i++) {
            polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
        }
        final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(
                polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
        return peuckerSimplifier.getResultGeometry();
    }

    private static Polygon convertAwtPathToJtsPolygon(Path2D path, GeometryFactory factory) {
        final PathIterator pathIterator = path.getPathIterator(null);
        ArrayList<double[]> coordList = new ArrayList<double[]>();
        int lastOpenIndex = 0;
        while (!pathIterator.isDone()) {
            final double[] coords = new double[6];
            final int segType = pathIterator.currentSegment(coords);
            if (segType == PathIterator.SEG_CLOSE) {
                // we should only detect a single SEG_CLOSE
                coordList.add(coordList.get(lastOpenIndex));
                lastOpenIndex = coordList.size();
            } else {
                coordList.add(coords);
            }
            pathIterator.next();
        }
        final Coordinate[] coordinates = new Coordinate[coordList.size()];
        for (int i1 = 0; i1 < coordinates.length; i1++) {
            final double[] coord = coordList.get(i1);
            coordinates[i1] = new Coordinate(coord[0], coord[1]);
        }

        return factory.createPolygon(factory.createLinearRing(coordinates), null);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel2.class);
        }
    }
}
