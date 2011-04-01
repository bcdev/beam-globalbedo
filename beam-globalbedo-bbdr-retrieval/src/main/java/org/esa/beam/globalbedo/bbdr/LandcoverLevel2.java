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


import com.bc.ceres.jai.tilecache.DefaultSwapSpace;
import com.bc.ceres.jai.tilecache.SwappingTileCache;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.internal.OperatorImage;
import org.esa.beam.globalbedo.sdr.operators.GaMasterOp;

import javax.media.jai.OpImage;
import javax.media.jai.TileCache;
import java.awt.image.RenderedImage;
import java.io.File;

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

    @Parameter(defaultValue = "true")
    private boolean useFileTileCache;

    private File tmpDir;

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = sourceProduct;
        if (step1) {
            targetProduct = processAot(targetProduct);
        }
        if (useFileTileCache) {
            attachFileTileCache(targetProduct);
        }
        if (step2) {
            targetProduct = processBbdr(targetProduct);
        }
        setTargetProduct(targetProduct);
    }

    private Product processAot(Product product) {
        GaMasterOp gaMasterOp = new GaMasterOp();
        gaMasterOp.setParameter("copyToaRadBands", false);
        gaMasterOp.setParameter("copyToaReflBands", true);
        gaMasterOp.setParameter("gaUseL1bLandWaterFlag", false);
        gaMasterOp.setSourceProduct(product);
        return gaMasterOp.getTargetProduct();
    }

    private Product processBbdr(Product product) {
        BbdrOp bbdrOp = new BbdrOp();
        bbdrOp.setSourceProduct(product);
        bbdrOp.setParameter("sensor", sensor);
        bbdrOp.setParameter("sdrOnly", true);
        bbdrOp.setParameter("landExpression", "cloud_classif_flags.F_CLEAR_LAND and not cloud_classif_flags.F_WATER and not cloud_classif_flags.F_CLOUD_SHADOW and not cloud_classif_flags.F_CLOUD_BUFFER");
        return bbdrOp.getTargetProduct();
    }
    private void attachFileTileCache(Product product) {
        String productName = sourceProduct.getName();
        tmpDir = new File(System.getProperty("java.io.tmpdir"), productName + "_" + System.currentTimeMillis());
        if (!tmpDir.mkdirs()) {
            throw new OperatorException("Failed to create tmp dir for SwappingTileCache: " + tmpDir.getAbsolutePath());
        }
        tmpDir.deleteOnExit();
        TileCache tileCache = new SwappingTileCache(16L * 1024L * 1024L, new DefaultSwapSpace(tmpDir));
        Band[] bands = product.getBands();
        for (Band band : bands) {
            RenderedImage image = band.getSourceImage().getImage(0);
            if (image instanceof OperatorImage) {//opImage is subclass of OpImage
                OpImage opImage = (OpImage) image;
                opImage.setTileCache(tileCache);
            }
        }
    }

   public static class Spi extends OperatorSpi {

        public Spi() {
            super(LandcoverLevel2.class);
        }
    }
}
