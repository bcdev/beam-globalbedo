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


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.globalbedo.bbdr.attic.BbdrOldOp;
import org.esa.beam.gpf.operators.standard.WriteOp;

import javax.media.jai.JAI;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class LandCoverProcessing {
    public static void main(String[] args) throws IOException {
        JAI.enableDefaultTileCache();
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(2000 * 1024 * 1024);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(8);
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
//        System.setProperty("beam.envisat.usePixelGeoCoding", "true");

        try {
            printMemory();
            String dirname = args[0];
            File rootDir = new File(dirname);
            File aotDir = new File(rootDir, "aots");

            File[] srcFileList = aotDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".dim");
                }
            });
            File sdrDir = new File(rootDir, "sdrs");
            sdrDir.mkdir();
            for (File srcFile : srcFileList) {
                File targetFile = new File(sdrDir, srcFile.getName().replaceFirst("_aot\\.dim", "_sdr.dim"));
                if (!targetFile.exists()) {
                    try {
                    System.out.println("processing: " + srcFile + " to "+ targetFile);
                    process(srcFile, targetFile);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    } finally {
                        printMemory();
                        JAI.getDefaultInstance().getTileCache().flush();
                        printMemory();
                        Runtime.getRuntime().gc();
                        printMemory();
                    }
                }
            }
        } catch (Throwable t) {
            printMemory();
            t.printStackTrace();
        }
    }

    public static void process(File sourceFile, File targetFile) throws IOException {

        Product aProduct = ProductIO.readProduct(sourceFile);
        printProductSize(aProduct);

//        SubsetOp subsetOp = new SubsetOp();
//        subsetOp.setSourceProduct(aProduct);
//        subsetOp.setGeoRegion(createBBOX(26, 36, 19, 6));
//        aProduct = subsetOp.getTargetProduct();

//        GaMasterOp gaMasterOp = new GaMasterOp();
//        gaMasterOp.setSourceProduct(aProduct);
//        aProduct = gaMasterOp.getTargetProduct();
//
        Operator bbdrOp = new BbdrOldOp();
        bbdrOp.setParameterDefaultValues();
        bbdrOp.setSourceProduct(aProduct);
        aProduct = bbdrOp.getTargetProduct();

        System.out.println("have a product");
        printProductSize(aProduct);
        System.out.println("geocoding = " + aProduct.getGeoCoding());
        System.out.println("PreferredTileSize = " + aProduct.getPreferredTileSize());

        final WriteOp writeOp = new WriteOp(aProduct, targetFile, ProductIO.DEFAULT_FORMAT_NAME);
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    // currently not needed
//    private static Polygon createBBOX(double x, double y, double w, double h) {
//        GeometryFactory factory = new GeometryFactory();
//        final LinearRing ring = factory.createLinearRing(new Coordinate[]{
//                new Coordinate(x, y),
//                new Coordinate(x + w, y),
//                new Coordinate(x + w, y + h),
//                new Coordinate(x, y + h),
//                new Coordinate(x, y)
//        });
//        return factory.createPolygon(ring, null);
//    }

    private static void printProductSize(Product aProduct) {
        System.out.println("SceneRasterWidth  = " + aProduct.getSceneRasterWidth());
        System.out.println("SceneRasterHeight = " + aProduct.getSceneRasterHeight());
    }

    private static void printMemory() {
        System.out.println("freeMemory  = " + inMB(Runtime.getRuntime().freeMemory()));
        System.out.println("totalMemory = " + inMB(Runtime.getRuntime().totalMemory()));
        System.out.println("maxMemory   = " + inMB(Runtime.getRuntime().maxMemory()));
    }

    private static String inMB(long l) {
        return String.format("%dMB", (l / 1024 / 1024));
    }

}
