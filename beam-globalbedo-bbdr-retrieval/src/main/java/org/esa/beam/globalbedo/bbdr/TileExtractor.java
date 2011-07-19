/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.ProductUtils;

import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Creates subprodcuts for all intersecting tiles
 */
@OperatorMetadata(alias = "ga.tile")
public class TileExtractor extends Operator implements Output {

    @SourceProduct
    private Product sourceProduct;

    @Parameter
    private String bbdrDir;

    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct.getPreferredTileSize() == null) {
            sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 45);
            System.out.println("adjusting tile size to: " + sourceProduct.getPreferredTileSize());
        }
        long t1 = System.currentTimeMillis();
        ModisTileCoordinates tileCoordinates = ModisTileCoordinates.getInstance();
        Geometry sourceGeometry = computeProductGeometry(sourceProduct);
        if (sourceGeometry != null) {

            for (int index = 0; index < tileCoordinates.getTileCount(); index++) {
                String tileName = tileCoordinates.getTileName(index);

                Product reproject = reproject(sourceProduct, tileName);
                Geometry reprojectGeometry = computeProductGeometry(reproject);
                if (reprojectGeometry != null && reprojectGeometry.intersects(sourceGeometry)) {
                    reproject.setPreferredTileSize(reproject.getSceneRasterWidth(), reproject.getSceneRasterHeight() / 4);
                    System.out.println("tileName = " + tileName);
                    Band bb_vis = reproject.getBand("BB_VIS");
                    if (containsFloatData(bb_vis, bb_vis.getNoDataValue())) {
                        long tt1 = System.currentTimeMillis();
                        System.out.println("repro: " + reproject.getPreferredTileSize());
                        File dir = new File(bbdrDir, tileName);
                        dir.mkdirs();
                        File file = new File(dir, "subset_" + sourceProduct.getName() + "_BBDR_Geo.dim");
                        try {
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        WriteOp writeOp = new WriteOp(reproject, file, DimapProductConstants.DIMAP_FORMAT_NAME);
                        writeOp.writeProduct(ProgressMonitor.NULL);
                        long tt2 = System.currentTimeMillis();
                        System.out.println("done " + tileName + " intime [" + (tt2 - tt1) + " ms]");
                    } else {
                        System.out.println("contains no data");
                    }
                }
            }
            long t2 = System.currentTimeMillis();
            System.out.println("tiles extracted: " + (t2 - t1) + " ms");
        }
        setTargetProduct(new Product("n", "d", 1, 1));
    }

    private boolean containsFloatData(Band band, double noDataValue) {
        Raster data = band.getSourceImage().getData(null);
        DataBuffer dataBuffer = data.getDataBuffer();
        Object primitiveArray = ImageUtils.getPrimitiveArray(dataBuffer);
        float[] dataBufferFloat = (primitiveArray instanceof float[]) ? (float[]) primitiveArray : null;
        for (float aFloat : dataBufferFloat) {
            if (Double.compare(noDataValue, aFloat) != 0) {
                return true;
            }
        }
        return false;
    }

    static Product reproject(Product bbdrProduct, String tileName) {
        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        int tileIndex = modisTileCoordinates.findTileIndex(tileName);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tileName + "");
        }
        double easting = modisTileCoordinates.getUpperLeftX(tileIndex);
        double northing = modisTileCoordinates.getUpperLeftY(tileIndex);

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
        repro.setParameter("width", BbdrConstants.MODIS_TILE_WIDTH);
        repro.setParameter("height", BbdrConstants.MODIS_TILE_HEIGHT);
        repro.setParameter("orthorectify", true);
        repro.setParameter("noDataValue", 0.0);
        repro.setSourceProduct(bbdrProduct);
        return repro.getTargetProduct();
    }

    static Geometry computeProductGeometry(Product product) {
        try {
            final GeneralPath[] paths = ProductUtils.createGeoBoundaryPaths(product);
            final Polygon[] polygons = new Polygon[paths.length];
            final GeometryFactory factory = new GeometryFactory();
            for (int i = 0; i < paths.length; i++) {
                polygons[i] = convertAwtPathToJtsPolygon(paths[i], factory);
            }
            final DouglasPeuckerSimplifier peuckerSimplifier = new DouglasPeuckerSimplifier(
                    polygons.length == 1 ? polygons[0] : factory.createMultiPolygon(polygons));
            return peuckerSimplifier.getResultGeometry();
        } catch (Exception e) {
            return null;
        }
    }

    static Polygon convertAwtPathToJtsPolygon(Path2D path, GeometryFactory factory) {
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
            super(TileExtractor.class);
        }
    }
}
