/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.landcover.StatusPostProcessOp;

import javax.media.jai.OpImage;
import javax.media.jai.TileCache;
import java.awt.image.RenderedImage;
import java.io.File;

@OperatorMetadata(alias = "lc.l2",
        description = "LC-CCI SDR Processor",
        authors = "Marco Zuehlke, Olaf Danne, Grit Kirches",
        version = "2.0",
        copyright = "(C) 2015 by Brockmann Consult")
public class LandcoverLevel2 extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @SourceProduct(optional = true)
    private Product eraInterimProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    // for testing purpose
    @Parameter(defaultValue = "true")
    private boolean doUclCloudDetection;

    @Parameter(defaultValue = "true")
    private boolean useUclCloudForShadow;

    @Parameter(defaultValue = "false")
    private boolean attachFileTileCache;

    @Override
    public void initialize() throws OperatorException {
        if (sensor == Sensor.PROBAV) {
            BbdrUtils.convertProbavToVgtReflectances(sourceProduct);
        }

        Product aotProduct = processAot(sourceProduct);
        if (aotProduct == GaMasterOp.EMPTY_PRODUCT) {
            System.err.println("aotProduct is empty");
            setTargetProduct(GaMasterOp.EMPTY_PRODUCT);
            return;
        }
        if (attachFileTileCache) {
            attachFileTileCache(aotProduct);
        }
        setTargetProduct(processSdr(aotProduct));
    }

    private Product processAot(Product product) {
        GaMasterOp gaMasterOp = new GaMasterOp();
        gaMasterOp.setParameterDefaultValues();
        gaMasterOp.setParameter("copyToaRadBands", false);
        gaMasterOp.setParameter("copyToaReflBands", true);
        gaMasterOp.setParameter("gaCopyCTP", true);
        gaMasterOp.setParameter("gaUseL1bLandWaterFlag", false);
        gaMasterOp.setParameter("doEqualization", false);
        gaMasterOp.setParameter("gaRefineClassificationNearCoastlines", false);
        gaMasterOp.setParameter("gaLcCloudBuffer", false);
        gaMasterOp.setParameter("gaComputeCloudShadow", false);
        gaMasterOp.setParameter("gaComputeCloudBuffer", false);
        gaMasterOp.setSourceProduct(product);
        return gaMasterOp.getTargetProduct();
    }

    private Product processSdr(Product aotProduct) {
        BbdrMasterOp sdrOp;
        switch (sensor.getInstrument()) {
            case "MERIS":
                sdrOp = new SdrMerisOp();
                break;
            case "VGT":
                sdrOp = new SdrVgtOp();
                break;
            case "PROBAV":
                sdrOp = new SdrProbavOp();
                if (eraInterimProduct != null) {
                    sdrOp.setSourceProduct("eraInterimProduct", eraInterimProduct);
                }
                break;
            case "AATSR":
            case "AATSR_FWARD":
            case "AVHRR":
                // todo
                throw new OperatorException("Sensor " + sensor.getInstrument() + " not supported.");  // remove later
            default:
                throw new OperatorException("Sensor " + sensor.getInstrument() + " not supported.");
        }
        sdrOp.setParameterDefaultValues();
        sdrOp.setSourceProduct(aotProduct);
        sdrOp.setParameter("sensor", sensor);
        sdrOp.setParameter("sdrOnly", true);
        sdrOp.setParameter("doUclCloudDetection", doUclCloudDetection);
        Product sdrProduct = sdrOp.getTargetProduct();

        if (sensor == Sensor.MERIS) {
            StatusPostProcessOp statusPostProcessOp = new StatusPostProcessOp();
            statusPostProcessOp.setParameterDefaultValues();
            statusPostProcessOp.setParameter("useUclCloudForShadow", useUclCloudForShadow);
            statusPostProcessOp.setSourceProduct("l1b", sourceProduct);
            statusPostProcessOp.setSourceProduct("status", sdrProduct);
            statusPostProcessOp.setSourceProduct("ctp", aotProduct);

            return statusPostProcessOp.getTargetProduct();
        } else {
            return sdrProduct;
        }
    }

    private void attachFileTileCache(Product product) {
        String productName = sourceProduct.getName();
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), productName + "_" + System.currentTimeMillis());
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
