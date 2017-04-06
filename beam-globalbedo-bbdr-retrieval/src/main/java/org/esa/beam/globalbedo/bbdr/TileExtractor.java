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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.ProductUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.JAI;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Creates subprodcuts for all intersecting tiles
 */
@OperatorMetadata(alias = "ga.tile")
public class TileExtractor extends Operator implements Output {

    @SourceProduct
    private Product sourceProduct;

    @Parameter
    private String bbdrDir;

    @Parameter(defaultValue = "false")
    protected boolean sdrOnly;

    @Parameter(defaultValue = "648")   // default 36x18 (all). set resasonable limit for testing in case of low memory
    protected int maxTiles;

    @Parameter(defaultValue = "1.0",
            valueSet = {"0.5", "1.0", "2.0", "4.0", "6.0", "10.0", "12.0", "20.0", "60.0"},
            description = "Scale factor with regard to MODIS default 1200x1200. Values > 1.0 reduce product size.")
    protected double modisTileScaleFactor;

    private int parallelism;
    private ModisTileCoordinates tileCoordinates;
    private Geometry sourceGeometry;

    private int tileCounter = 0;

    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct.getPreferredTileSize() == null) {
            sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 45);
            System.out.println("adjusting tile size to: " + sourceProduct.getPreferredTileSize());
        }
        parallelism = JAI.getDefaultInstance().getTileScheduler().getParallelism();
        tileCoordinates = ModisTileCoordinates.getInstance();
        sourceGeometry = computeProductGeometry(sourceProduct);
        if (sourceGeometry != null) {
//          doExtract_simple();
            doExtract_executor();
        }
        setTargetProduct(new Product("n", "d", 1, 1));
    }

    public static Product reprojectToModisTile(Product sourceProduct, String tileName) {
        return reprojectToModisTile(sourceProduct, tileName, "Nearest");
    }

    public static Product reprojectToModisTile(Product sourceProduct, String tileName, double scaleFactor) {
        return reprojectToModisTile(sourceProduct, tileName, "Nearest", scaleFactor);
    }

    public static Product reprojectToModisTile(Product sourceProduct, String tileName, String resampling) {
        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        int tileIndex = modisTileCoordinates.findTileIndex(tileName);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tileName + "");
        }
        double easting = modisTileCoordinates.getUpperLeftX(tileIndex);
        double northing = modisTileCoordinates.getUpperLeftY(tileIndex);

        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameterDefaultValues();
        repro.setParameter("easting", easting);
        repro.setParameter("northing", northing);
        repro.setParameter("crs", AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING);
        repro.setParameter("resampling", resampling);
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
        repro.setSourceProduct(sourceProduct);
        return repro.getTargetProduct();
    }

    public static Product reprojectToModisTile(Product sourceProduct, String tileName, String resampling, double scaleFactor) {
        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        int tileIndex = modisTileCoordinates.findTileIndex(tileName);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tileName + "");
        }
        double easting = modisTileCoordinates.getUpperLeftX(tileIndex);
        double northing = modisTileCoordinates.getUpperLeftY(tileIndex);

        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameterDefaultValues();
        repro.setParameter("easting", easting);
        repro.setParameter("northing", northing);
        repro.setParameter("crs", AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING);
        repro.setParameter("resampling", resampling);
        repro.setParameter("includeTiePointGrids", false);
        repro.setParameter("referencePixelX", 0.0);
        repro.setParameter("referencePixelY", 0.0);
        repro.setParameter("orientation", 0.0);

        // scale factor > 1 increases pixel size and decreases number of pixels;
        final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * scaleFactor;
        final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * scaleFactor;
        final int width = (int) (BbdrConstants.MODIS_TILE_WIDTH/scaleFactor);
        final int height = (int) (BbdrConstants.MODIS_TILE_HEIGHT/scaleFactor);

        repro.setParameter("pixelSizeX", pixelSizeX);
        repro.setParameter("pixelSizeY", pixelSizeY);
        repro.setParameter("width", width);
        repro.setParameter("height", height);

        repro.setParameter("orthorectify", true);
        repro.setParameter("noDataValue", 0.0);
        repro.setSourceProduct(sourceProduct);
        return repro.getTargetProduct();
    }


    public static Geometry computeProductGeometry(Product product) {
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

    private static Product getReprojectedProductWithData(Product src, Geometry sourceGeometry,
                                                         String tileName, boolean sdrOnly, double scaleFactor) {
        Product reproject = reprojectToModisTile(src, tileName, scaleFactor);
        Geometry reprojectGeometry = computeProductGeometry(reproject);

        if (reprojectGeometry != null && reprojectGeometry.intersects(sourceGeometry)) {
            int parallelism = JAI.getDefaultInstance().getTileScheduler().getParallelism();
            reproject.setPreferredTileSize(reproject.getSceneRasterWidth(), reproject.getSceneRasterHeight() / parallelism);
            Band referenceBand = null;
            if (sdrOnly) {
                for (String bandname : reproject.getBandNames()) {
                    if (bandname.startsWith("sdr") || isMfgBand(bandname) ||
                            isAvhrrLtdrBand(bandname) || isMsslFlagBand(bandname)) {
                        referenceBand = reproject.getBand(bandname);
                    }
                }
            } else {
                referenceBand = reproject.getBand("BB_VIS");
            }
            if (referenceBand != null) {
                if (containsFloatData(referenceBand, referenceBand.getNoDataValue())) {
                    return reproject;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    private static boolean isMfgBand(String bandname) {
//        return bandname.toLowerCase().startsWith("brf");
        return bandname.toLowerCase().startsWith("spectral_brf") || bandname.toLowerCase().startsWith("broadband_brf");
    }

    private static boolean isAvhrrLtdrBand(String bandname) {
        return bandname.toLowerCase().startsWith("srefl") || bandname.toLowerCase().equals("szen") ||
                bandname.toLowerCase().equals("vzen") || bandname.toLowerCase().equals("relaz");
    }

    private static boolean isMsslFlagBand(String bandname) {
        return bandname.toLowerCase().equals("mask") || bandname.toLowerCase().equals("probability");
    }

    private static boolean containsFloatData(Band band, double noDataValue) {
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

    private void doExtract_executor() {
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        ExecutorCompletionService<TileProduct> ecs = new ExecutorCompletionService<TileProduct>(executorService);

        for (int index = 0; index < tileCoordinates.getTileCount(); index++) {
            final String tileName = tileCoordinates.getTileName(index);
            Callable<TileProduct> callable = new Callable<TileProduct>() {
                @Override
                public TileProduct call() throws Exception {
                    Product reprojected = getReprojectedProductWithData(sourceProduct, sourceGeometry, tileName, sdrOnly, modisTileScaleFactor);
                    return new TileProduct(reprojected, tileName);
                }
            };
            ecs.submit(callable);
        }
        for (int i = 0; i < tileCoordinates.getTileCount(); i++) {
            try {
                Future<TileProduct> future = ecs.take();
                TileProduct tileProduct = future.get();
                if (tileProduct.product != null && tileCounter < maxTiles) {
                    writeTileProduct(tileProduct.product, tileProduct.tileName);
                    tileCounter++;
                }
            } catch (InterruptedException e) {
                throw new OperatorException(e);
            } catch (ExecutionException e) {
                throw new OperatorException(e);
            }
        }
        executorService.shutdown();
    }

    private void writeTileProduct(Product product, String tileName) {
        File dir = new File(bbdrDir, tileName);
        dir.mkdirs();
        File file = new File(dir, sourceProduct.getName() + "_" + tileName + ".nc");  // 20150401 for QA4ECV chain on CEMS
        WriteOp writeOp = new WriteOp(product, file, "NetCDF4-BEAM");
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    public static class TileProduct {
        private final Product product;
        private final String tileName;

        public TileProduct(Product product, String tileName) {
            this.product = product;
            this.tileName = tileName;
        }

        public Product getProduct() {
            return product;
        }

        public String getTileName() {
            return tileName;
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(TileExtractor.class);
        }
    }
}
