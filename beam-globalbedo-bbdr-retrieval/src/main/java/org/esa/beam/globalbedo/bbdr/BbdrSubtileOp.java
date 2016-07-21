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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.gpf.operators.standard.WriteOp;

import javax.media.jai.JAI;
import java.awt.*;
import java.io.File;
import java.util.concurrent.*;

/**
 * Divides a SDR or BBDR MODIS SIN tile product onto subtiles and writes them into new products.
 */
@OperatorMetadata(alias = "ga.bbdr.tile.subtile",
        autoWriteDisabled = true,
        description = "Divides a SDR or BBDR MODIS SIN tile product onto subtiles and writes them into new products. ",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2016 by Brockmann Consult")
public class BbdrSubtileOp extends Operator implements Output {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(description = "Sub tiling factor (e.g. 4 for 300x300 subtile size")
    private int subtileFactor;

    private int numSubTiles;

    private int width;

    private int height;
    private int parallelism;
    private String tileDir;

    @Override
    public void initialize() throws OperatorException {
        width = AlbedoInversionConstants.MODIS_TILE_WIDTH;
        height = AlbedoInversionConstants.MODIS_TILE_HEIGHT;
        if (sourceProduct.getSceneRasterWidth() != width ||
                sourceProduct.getSceneRasterHeight() != height) {
            throw new OperatorException("SubTileOp: source tile product '" +
                                                sourceProduct.getName() +
                                                "' has wrong dimensions - must be '" + Integer.toString(width) + "x" +
                                                Integer.toString(height) + "'.");
        }

        tileDir = sourceProduct.getFileLocation().getParent();
        numSubTiles = subtileFactor * subtileFactor;
        parallelism = JAI.getDefaultInstance().getTileScheduler().getParallelism();
        doExtract_executor();
        setTargetProduct(new Product("n", "d", 1, 1));
    }

    private void doExtract_executor() {
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        ExecutorCompletionService<Product> ecs = new ExecutorCompletionService<>(executorService);

        final int subWidth = width / subtileFactor;
        final int subHeight = height / subtileFactor;
        for (int i = 0; i < subtileFactor; i++) {
            final int startX = subWidth * i;
            for (int j = 0; j < subtileFactor; j++) {
                final int startY = subHeight * j;
                Callable<Product> callable = new Callable<Product>() {
                    @Override
                    public Product call() throws Exception {
                        SubsetOp subsetOp = new SubsetOp();
                        subsetOp.setParameterDefaultValues();
                        subsetOp.setSourceProduct(sourceProduct);
                        subsetOp.setRegion(new Rectangle(startX, startY, subWidth, subHeight));
                        final Product targetProduct = subsetOp.getTargetProduct();
                        targetProduct.setName(sourceProduct.getName() + "_SUB_" +
                                                      Integer.toString(startX) + "_" + Integer.toString(startY));
                        return targetProduct;
                    }
                };
                ecs.submit(callable);
            }
        }

        for (int i = 0; i < numSubTiles; i++) {
            try {
                Future<Product> future = ecs.take();
                final Product tileProduct = future.get();
                if (tileProduct != null) {
                    writeTileProduct(tileProduct);
                }
            } catch (Exception e) {
                throw new OperatorException(e);
            }
        }
        executorService.shutdown();
    }

    private void writeTileProduct(Product product) {
        final int suffixStart = product.getName().indexOf("SUB");
        final String subDir = product.getName().substring(suffixStart);
        final String fullDir = tileDir + File.separator + subDir;
        final File targetDir = new File(fullDir);
        if (!targetDir.exists()) {
            final boolean madeDir = targetDir.mkdirs();
            if (!madeDir) {
                throw new OperatorException("Could not create sub tile dir '" + fullDir + "'.");
            }
        }

        File file = new File(targetDir, sourceProduct.getName() + "_" + subDir + ".nc");
        String writeFormat;
        if (product.getName().toUpperCase().contains("BBDR")) {
            writeFormat = "NetCDF4-GA-BBDR";
        } else {
            writeFormat = "NetCDF4-BEAM";
        }
        WriteOp writeOp = new WriteOp(product, file, writeFormat);
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrSubtileOp.class);
        }
    }
}
