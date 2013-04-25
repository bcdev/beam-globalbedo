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
 * Applies IDEPIX to a MERIS/AATSR collocation product and then computes AOT.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.colloc.aotpst",
                  description = "Reprojects a MERIS/AATSR collocation product onto up to 4 PST 'quadrants' products " + " " +
                          "if the MERIS and AATSR data intersects with them.",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class CollocToPstQuadrantsOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Override
    public void initialize() throws OperatorException {

        // find PST quadrants with data...
        // 1. make product band subset: 1 band + lat/lon TPGs...
        Product subsetSourceProduct = getCollocationBandSubset();
        // 2. reproject this to each of the 4 PSTquadrants
        Product qZero90WSubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                                                                                           1125.0, 0.0,
                                                                                           1000.0, 1000.0,
                                                                                           2250, 2250);
        Product q90W180WSubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                                                                                           1125.0, 1125.0,
                                                                                           1000.0, 1000.0,
                                                                                           2250, 2250);
        Product qZero90ESubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                                                                                           0.0, 0.0,
                                                                                           1000.0, 1000.0,
                                                                                           2250, 2250);
        Product q90E180ESubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                                                                                           0.0, 1125.0,
                                                                                           1000.0, 1000.0,
                                                                                           2250, 2250);


        // 3. check for each of the 4 PST quadrants if we have intersection with the collocation product
        // if so, reproject source product onto this quadrant and write output
        // write output e.g. to
        //  ../COLLOC/180W_90W
        //  ../COLLOC/90W_0
        //  ../COLLOC/0_90E
        //  ../COLLOC/90E_180E
        writeQuadrants(qZero90WSubsetProduct, q90W180WSubsetProduct, qZero90ESubsetProduct, q90E180ESubsetProduct);

        setTargetProduct(new Product("dummy", "dummy", 0, 0));
    }

    private void writeQuadrants(Product qZero90WSubsetProduct, Product q90W180WSubsetProduct, Product qZero90ESubsetProduct, Product q90E180ESubsetProduct) {
        final Geometry sourceGeometry = TileExtractor.computeProductGeometry(sourceProduct);
        PstQuadrant[] quadrants = new PstQuadrant[4];
        quadrants[0].setIdentifier("90W_180W");
        quadrants[0].setGeometry(TileExtractor.computeProductGeometry(q90W180WSubsetProduct));
        quadrants[0].setProjParms(new PstProjectionParameters(1125.0, 1125.0, 1000.0, 1000.0, 2250, 2250));
        quadrants[1].setIdentifier("90W_0");
        quadrants[1].setGeometry(TileExtractor.computeProductGeometry(qZero90WSubsetProduct));
        quadrants[1].setProjParms(new PstProjectionParameters(1125.0, 0.0, 1000.0, 1000.0, 2250, 2250));
        quadrants[2].setIdentifier("0_90E");
        quadrants[2].setGeometry(TileExtractor.computeProductGeometry(qZero90ESubsetProduct));
        quadrants[2].setProjParms(new PstProjectionParameters(0.0, 0.0, 1000.0, 1000.0, 2250, 2250));
        quadrants[3].setIdentifier("90E_180E");
        quadrants[3].setGeometry(TileExtractor.computeProductGeometry(q90E180ESubsetProduct));
        quadrants[3].setProjParms(new PstProjectionParameters(0.0, 1125.0, 1000.0, 1000.0, 2250, 2250));

        // todo:  maybe do this like in doExtract_executor() in TileExtractor
        for (PstQuadrant quad : quadrants) {
            if (sourceGeometry.intersects(quad.getGeometry())) {
                // reproject full source product and write
                final Product q90W180WProduct = PolarStereographicOp.reprojectToPolarStereographic(sourceProduct,
                                                                                                   quad.getProjParms().getReferencePixelX(),
                                                                                                   quad.getProjParms().getReferencePixelY(),
                                                                                                   quad.getProjParms().getPixelSizeX(),
                                                                                                   quad.getProjParms().getPixelSizeY(),
                                                                                                   quad.getProjParms().getWidth(),
                                                                                                   quad.getProjParms().getHeight());
                final String pstTargetFilePath = getPstTargetFileName(sourceProduct, quad.getIdentifier());
                final File pstTargetFile = new File(pstTargetFilePath);
                final WriteOp bbdrWriteOp = new WriteOp(q90W180WProduct, pstTargetFile, ProductIO.DEFAULT_FORMAT_NAME);
                System.out.println("Writing PST " + quad.getIdentifier() + " product '" + pstTargetFile.getName() + "'...");
                bbdrWriteOp.writeProduct(ProgressMonitor.NULL);
            }
        }
    }

//    private void doWriteQuadrants_executor() {
//        final int parallelism = JAI.getDefaultInstance().getTileScheduler().getParallelism();
//        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
//        ExecutorCompletionService<Product> ecs = new ExecutorCompletionService<Product>(executorService);
//
//        for (int index = 0; index < tileCoordinates.getTileCount(); index++) {
//            final String tileName = tileCoordinates.getTileName(index);
//            Callable<TileProduct> callable = new Callable<TileProduct>() {
//                @Override
//                public TileProduct call() throws Exception {
//                    Product reprojected = getReprojectedProductWithData(sourceProduct, sourceGeometry, tileName);
//                    return new TileProduct(reprojected, tileName);
//                }
//            };
//            ecs.submit(callable);
//        }
//        for (int i = 0; i < tileCoordinates.getTileCount(); i++) {
//            try {
//                Future<TileProduct> future = ecs.take();
//                TileProduct tileProduct = future.get();
//                if (tileProduct.product != null) {
//                    writeTileProduct(tileProduct.product, tileProduct.tileName);
//                }
//            } catch (InterruptedException e) {
//                throw new OperatorException(e);
//            } catch (ExecutionException e) {
//                throw new OperatorException(e);
//            }
//        }
//        executorService.shutdown();
//    }


    private String getPstTargetFileName(Product sourceProduct, String quadrant) {
        return sourceProduct.getFileLocation().getParent() + File.separator + sourceProduct.getName() +
                "_" + quadrant + "_PST.dim";
    }

    private Product getCollocationBandSubset() {
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
        ProductUtils.copyTiePointGrid("latitude", sourceProduct, bandSubsetProduct);
        ProductUtils.copyTiePointGrid("longitude", sourceProduct, bandSubsetProduct);

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
            super(CollocToPstQuadrantsOp.class);
        }
    }
}
