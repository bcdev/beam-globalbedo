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

package org.esa.beam.globalbedo.bbdr.seaice;


import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.globalbedo.bbdr.TileExtractor;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.JAI;
import java.io.File;
import java.util.concurrent.*;

/**
 * Reprojects a BBDR product onto up to 4 PST 'quadrants' products
 * if the BBDR data intersects with them.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.bbdr.pst",
        description = "Reprojects a BBDR product onto up to 4 PST 'quadrants' products " + " " +
                "if the MERIS and AATSR data intersects with them.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2013 by Brockmann Consult")
public class BbdrToPstQuadrantsOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Override
    public void initialize() throws OperatorException {

        // find PST quadrants with data...
        // 1. make product band subset: 1 band + lat/lon TPGs...
        Product subsetSourceProduct = getBandSubsetProduct();
        // 2. reproject this to each of the 4 PSTquadrants
        Product qZero90WSubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                2250.0, 0.0,
                1000.0, 1000.0,
                2250, 2250);
        Product q90W180WSubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                2250.0, 2250.0,
                1000.0, 1000.0,
                2250, 2250);
        Product qZero90ESubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                0.0, 0.0,
                1000.0, 1000.0,
                2250, 2250);
        Product q90E180ESubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                0.0, 2250.0,
                1000.0, 1000.0,
                2250, 2250);


        // 3. check for each of the 4 PST quadrants if we have intersection with the BBDR product
        // if so, reproject source product onto this quadrant and write output
        // write output e.g. to
        //  ../BBDR/180W_90W
        //  ../BBDR/90W_0
        //  ../BBDR/0_90E
        //  ../BBDR/90E_180E
        writeQuadrants(qZero90WSubsetProduct, q90W180WSubsetProduct, qZero90ESubsetProduct, q90E180ESubsetProduct);

        setTargetProduct(new Product("dummy", "dummy", 0, 0));
    }

    private void writeQuadrants(Product qZero90WSubsetProduct, Product q90W180WSubsetProduct, Product qZero90ESubsetProduct, Product q90E180ESubsetProduct) {
        final Geometry sourceGeometry = TileExtractor.computeProductGeometry(sourceProduct);
        PstQuadrant[] quadrants = new PstQuadrant[4];
        quadrants[0] = new PstQuadrant(TileExtractor.computeProductGeometry(q90W180WSubsetProduct),
                "180W_90W",
                new PstProjectionParameters(2250.0, 2250.0, 1000.0, 1000.0, 2250, 2250));
        quadrants[1] = new PstQuadrant(TileExtractor.computeProductGeometry(qZero90WSubsetProduct),
                "90W_0",
                new PstProjectionParameters(2250.0, 0.0, 1000.0, 1000.0, 2250, 2250));
        quadrants[2] = new PstQuadrant(TileExtractor.computeProductGeometry(qZero90ESubsetProduct),
                "0_90E",
                new PstProjectionParameters(0.0, 0.0, 1000.0, 1000.0, 2250, 2250));
        quadrants[3] = new PstQuadrant(TileExtractor.computeProductGeometry(q90E180ESubsetProduct),
                "90E_180E",
                new PstProjectionParameters(0.0, 2250.0, 1000.0, 1000.0, 2250, 2250));

        // todo:  maybe do this like in doExtract_executor() in TileExtractor
        doWriteQuadrants_executor(quadrants);
//        for (PstQuadrant quad : quadrants) {
//            if (quad.getIdentifier().equals("0_90E") && quad.getGeometry() != null && sourceGeometry.intersects(quad.getGeometry())) {
//                // reproject full source product and write
//                final Product quadPstProduct = PolarStereographicOp.reprojectToPolarStereographic(sourceProduct,
//                        quad.getProjParms().getReferencePixelX(),
//                        quad.getProjParms().getReferencePixelY(),
//                        quad.getProjParms().getPixelSizeX(),
//                        quad.getProjParms().getPixelSizeY(),
//                        quad.getProjParms().getWidth(),
//                        quad.getProjParms().getHeight());
//                final String pstTargetFilePath = getPstTargetFileName(sourceProduct, quad.getIdentifier());
//                final File pstTargetFile = new File(pstTargetFilePath);
//                final WriteOp quadPstWriteOp = new WriteOp(quadPstProduct, pstTargetFile, ProductIO.DEFAULT_FORMAT_NAME);
//                System.out.println("Writing PST " + quad.getIdentifier() + " product '" + pstTargetFile.getName() + "'...");
//                quadPstWriteOp.writeProduct(ProgressMonitor.NULL);
//            }
//        }
    }

    private void doWriteQuadrants_executor(PstQuadrant[] quadrants) {
        final int parallelism = JAI.getDefaultInstance().getTileScheduler().getParallelism();
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        ExecutorCompletionService<TileExtractor.TileProduct> ecs = new ExecutorCompletionService<TileExtractor.TileProduct>(executorService);

        final Geometry sourceGeometry = TileExtractor.computeProductGeometry(sourceProduct);
        for (final PstQuadrant quad : quadrants) {
            if (quad.getGeometry() != null && sourceGeometry.intersects(quad.getGeometry())) {
                final String tileName = quad.getIdentifier();
                Callable<TileExtractor.TileProduct> callable = new Callable<TileExtractor.TileProduct>() {
                    @Override
                    public TileExtractor.TileProduct call() throws Exception {
                        final Product quadPstProduct = PolarStereographicOp.reprojectToPolarStereographic(sourceProduct,
                                quad.getProjParms().getReferencePixelX(),
                                quad.getProjParms().getReferencePixelY(),
                                quad.getProjParms().getPixelSizeX(),
                                quad.getProjParms().getPixelSizeY(),
                                quad.getProjParms().getWidth(),
                                quad.getProjParms().getHeight());
                        return new TileExtractor.TileProduct(quadPstProduct, tileName);
                    }
                };
                ecs.submit(callable);
            }
        }

        for (final PstQuadrant quad : quadrants) {
            if (quad.getGeometry() != null && sourceGeometry.intersects(quad.getGeometry())) {
                try {
                    Future<TileExtractor.TileProduct> future = ecs.take();
                    TileExtractor.TileProduct tileProduct = future.get();
                    if (tileProduct.getProduct() != null) {
                        writeQuadrantTileProduct(tileProduct);
                    }
                } catch (InterruptedException e) {
                    throw new OperatorException(e);
                } catch (ExecutionException e) {
                    throw new OperatorException(e);
                }
            }
        }
        executorService.shutdown();
    }

    private void writeQuadrantTileProduct(TileExtractor.TileProduct tileProduct) {
        final String pstTargetFilePath = getPstTargetFileName(sourceProduct, tileProduct.getTileName());
        final File pstTargetFile = new File(pstTargetFilePath);
        final WriteOp quadPstWriteOp = new WriteOp(tileProduct.getProduct(), pstTargetFile, ProductIO.DEFAULT_FORMAT_NAME);
        System.out.println("Writing PST " + tileProduct.getTileName() + " product '" + pstTargetFile.getName() + "'...");
        quadPstWriteOp.writeProduct(ProgressMonitor.NULL);
    }

    private String getPstTargetFileName(Product sourceProduct, String quadrant) {
        return sourceProduct.getFileLocation().getParent() +
                File.separator + quadrant +
                File.separator + sourceProduct.getName() +
                "_" + quadrant + "_PST.dim";
    }

    private Product getBandSubsetProduct() {
        Product bandSubsetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());
        ProductUtils.copyMetadata(sourceProduct, bandSubsetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, bandSubsetProduct);
        bandSubsetProduct.setStartTime(sourceProduct.getStartTime());
        bandSubsetProduct.setEndTime(sourceProduct.getEndTime());
        // just copy 1 band and lat/lon tpgs...
        ProductUtils.copyBand(sourceProduct.getBandAt(0).getName(), sourceProduct, bandSubsetProduct, true);
        if (!bandSubsetProduct.containsTiePointGrid("latitude")) {
            ProductUtils.copyTiePointGrid("latitude", sourceProduct, bandSubsetProduct);
        }
        if (!bandSubsetProduct.containsTiePointGrid("longitude")) {
            ProductUtils.copyTiePointGrid("longitude", sourceProduct, bandSubsetProduct);
        }

        return bandSubsetProduct;
    }

    private class PstProjectionParameters {
        double referencePixelX;
        double referencePixelY;
        double pixelSizeX;
        double pixelSizeY;
        int width;
        int height;

        private PstProjectionParameters(double referencePixelX, double referencePixelY, double pixelSizeX, double pixelSizeY, int width, int height) {
            this.referencePixelX = referencePixelX;
            this.referencePixelY = referencePixelY;
            this.pixelSizeX = pixelSizeX;
            this.pixelSizeY = pixelSizeY;
            this.width = width;
            this.height = height;
        }

        public double getReferencePixelX() {
            return referencePixelX;
        }

        public double getReferencePixelY() {
            return referencePixelY;
        }

        public double getPixelSizeX() {
            return pixelSizeX;
        }

        public double getPixelSizeY() {
            return pixelSizeY;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    private class PstQuadrant {
        Geometry geometry;
        String identifier;
        PstProjectionParameters projParms;

        private PstQuadrant() {
        }

        private PstQuadrant(Geometry geometry, String identifier, PstProjectionParameters projParms) {
            this.geometry = geometry;
            this.identifier = identifier;
            this.projParms = projParms;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public void setGeometry(Geometry geometry) {
            this.geometry = geometry;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public PstProjectionParameters getProjParms() {
            return projParms;
        }

        public void setProjParms(PstProjectionParameters projParms) {
            this.projParms = projParms;
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrToPstQuadrantsOp.class);
        }
    }
}
