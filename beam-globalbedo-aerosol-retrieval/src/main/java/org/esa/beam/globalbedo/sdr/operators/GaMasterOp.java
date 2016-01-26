/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Main Operator producing AOT for GlobAlbedo
 */
@OperatorMetadata(alias = "ga.MasterOp",
        description = "",
        authors = "Andreas Heckel",
        version = "1.1",
        copyright = "(C) 2010 by University Swansea (a.heckel@swansea.ac.uk)")
public class GaMasterOp extends Operator {

    public static final Product EMPTY_PRODUCT = new Product("empty", "empty", 0, 0);

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false")
    private boolean copyToaRadBands;
    @Parameter(defaultValue = "true")
    private boolean copyToaReflBands;
    @Parameter(defaultValue = "false")
    private boolean noFilling;
    @Parameter(defaultValue = "false")
    private boolean noUpscaling;
    @Parameter(defaultValue = "1")
    private int soilSpecId;
    @Parameter(defaultValue = "5")
    private int vegSpecId;
    @Parameter(defaultValue = "9")
    private int scale;
    @Parameter(defaultValue = "0.3")
    private float ndviThr;

    @Parameter(defaultValue = "true",
            label = "Perform equalization",
            description = "Perform removal of detector-to-detector systematic radiometric differences in MERIS L1b data products.")
    private boolean doEqualization;

    @Parameter(label = "Include the named Rayleigh Corrected Reflectances in target product")
    private String[] gaOutputRayleigh;

    @Parameter(defaultValue = "false", label = " Use the LC cloud buffer algorithm")
    private boolean gaLcCloudBuffer;

    @Parameter(defaultValue = "false", label = " If set, we shall use AOT climatology (no retrieval)")
    private boolean useAotClimatology;

    @Parameter(defaultValue = "false")     // cloud/snow flag refinement. Was not part of GA FPS processing.
    private boolean gaRefineClassificationNearCoastlines;

    @Parameter(defaultValue = "false")     // in line with GA FPS processing, BEAM 4.9.0.1. Better set to true??
    private boolean gaUseL1bLandWaterFlag;

    @Parameter(defaultValue = "false", label = " Use the LC cloud buffer algorithm")
    private boolean gaComputeCloudShadow;

    @Parameter(defaultValue = "true", label = "Compute cloud buffer")
    private boolean gaComputeCloudBuffer;

    @Parameter(defaultValue = "false", label = "Copy cloud top pressure")
    private boolean gaCopyCTP;

    private String instrument;

    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct.getSceneRasterWidth() < 9 || sourceProduct.getSceneRasterHeight() < 9) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }
        Dimension targetTS = ImageManager.getPreferredTileSize(sourceProduct);
        Dimension aotTS = new Dimension(targetTS.width / 9, targetTS.height / 9);
        RenderingHints rhTarget = new RenderingHints(GPF.KEY_TILE_SIZE, targetTS);
        RenderingHints rhAot = new RenderingHints(GPF.KEY_TILE_SIZE, aotTS);

        String productType = sourceProduct.getProductType();
        boolean isMerisProduct = EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(productType).matches();
        final boolean isAatsrProduct = productType.equals(EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME);
        final boolean isVgtProduct = productType.startsWith("VGT PRODUCT FORMAT V1.");
        final boolean isProbavProduct = productType.startsWith("PROBA-V SYNTHESIS");

        Guardian.assertTrue("not a valid source product", (isMerisProduct ^ isAatsrProduct ^ isVgtProduct ^ isProbavProduct));

        Product reflProduct = null;
        if (isMerisProduct) {
            instrument = "MERIS";
            Map<String, Object> params = new HashMap<String, Object>(4);
            params.put("gaUseL1bLandWaterFlag", gaUseL1bLandWaterFlag);
            params.put("gaRefineClassificationNearCoastlines", gaRefineClassificationNearCoastlines);
            params.put("doEqualization", doEqualization);
            params.put("gaOutputRayleigh", gaOutputRayleigh);
            params.put("gaLcCloudBuffer", gaLcCloudBuffer);
            params.put("gaComputeCloudShadow", gaComputeCloudShadow);
            params.put("gaComputeCloudBuffer", gaComputeCloudBuffer);
            params.put("gaCopyCTP", gaCopyCTP);
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(MerisPrepOp.class), params, sourceProduct);
        } else if (isAatsrProduct) {
            instrument = "AATSR";
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AatsrPrepOp.class), GPF.NO_PARAMS, sourceProduct);
        } else if (isVgtProduct) {
            instrument = "VGT";
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(VgtPrepOp.class), GPF.NO_PARAMS, sourceProduct);
        } else if (isProbavProduct) {
            instrument = "PROBAV";
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ProbavPrepOp.class), GPF.NO_PARAMS, sourceProduct);
        }
        if (reflProduct == EMPTY_PRODUCT) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }

        // if bbdr seaice, just add 2 bands:
        // - aot
        // - aot_err
        // to reflProduct, and set this as targetProduct, and return.
        // we will set the climatological value (or zero AOT) in BBDR Op
        if (useAotClimatology) {
            reflProduct.addBand("aot", ProductData.TYPE_FLOAT32);
            reflProduct.addBand("aot_err", ProductData.TYPE_FLOAT32);
            setTargetProduct(reflProduct);
            return;
        }


        Map<String, Object> aotParams = new HashMap<String, Object>(4);
        aotParams.put("soilSpecId", soilSpecId);
        aotParams.put("vegSpecId", vegSpecId);
        aotParams.put("scale", scale);
        aotParams.put("ndviThreshold", ndviThr);

        Product aotDownsclProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AerosolOp2.class), aotParams, reflProduct, rhAot);

        Product fillAotProduct = aotDownsclProduct;
        if (!noFilling) {
            Map<String, Product> fillSourceProds = new HashMap<String, Product>(2);
            fillSourceProds.put("aotProduct", aotDownsclProduct);
            fillAotProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(GapFillingOp.class), GPF.NO_PARAMS, fillSourceProds);
        }

        targetProduct = fillAotProduct;
        if (!noUpscaling) {
            Map<String, Product> upsclProducts = new HashMap<String, Product>(2);
            upsclProducts.put("lowresProduct", fillAotProduct);
            upsclProducts.put("hiresProduct", reflProduct);
            Map<String, Object> sclParams = new HashMap<String, Object>(1);
            sclParams.put("scale", scale);
            Product aotHiresProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(UpSclOp.class), sclParams, upsclProducts, rhTarget);

            targetProduct = mergeToTargetProduct(reflProduct, aotHiresProduct);
            ProductUtils.copyPreferredTileSize(reflProduct, targetProduct);
        }
        setTargetProduct(targetProduct);
    }

    private Product mergeToTargetProduct(Product reflProduct, Product aotHiresProduct) {
        String pname = reflProduct.getName() + "_AOT";
        String ptype = reflProduct.getProductType() + " GlobAlbedo AOT";
        int rasterWidth = reflProduct.getSceneRasterWidth();
        int rasterHeight = reflProduct.getSceneRasterHeight();
        Product tarP = new Product(pname, ptype, rasterWidth, rasterHeight);
        tarP.setStartTime(reflProduct.getStartTime());
        tarP.setEndTime(reflProduct.getEndTime());
        tarP.setPointingFactory(reflProduct.getPointingFactory());
        ProductUtils.copyMetadata(aotHiresProduct, tarP);
        ProductUtils.copyTiePointGrids(reflProduct, tarP);
        ProductUtils.copyGeoCoding(reflProduct, tarP);
        ProductUtils.copyFlagBands(reflProduct, tarP, true);
        ProductUtils.copyFlagBands(aotHiresProduct, tarP, true);
        String sourceBandName;
        if (copyToaRadBands) {
            for (Band sourceBand : reflProduct.getBands()) {
                sourceBandName = sourceBand.getName();
                if (sourceBand.getSpectralWavelength() > 0) {
                    ProductUtils.copyBand(sourceBandName, reflProduct, tarP, true);
                }
            }
        }
        for (Band sourceBand : reflProduct.getBands()) {
            sourceBandName = sourceBand.getName();

            boolean copyBand = (copyToaReflBands && !tarP.containsBand(sourceBandName) && sourceBand.getSpectralWavelength() > 0);
            copyBand = copyBand || (instrument.equals("VGT") && InstrumentConsts.getInstance().isVgtAuxBand(sourceBand));
            copyBand = copyBand || (instrument.equals("PROBAV") && InstrumentConsts.getInstance().isProbavAuxBand(sourceBand));
            copyBand = copyBand || (sourceBandName.equals("elevation"));
            copyBand = copyBand || (gaCopyCTP && sourceBandName.equals("cloud_top_press"));

            if (copyBand) {
                ProductUtils.copyBand(sourceBandName, reflProduct, tarP, true);
            }
        }
        for (Band sourceBand : aotHiresProduct.getBands()) {
            sourceBandName = sourceBand.getName();
            if (!sourceBand.isFlagBand() && !tarP.containsBand(sourceBandName)) {
                ProductUtils.copyBand(sourceBandName, aotHiresProduct, tarP, true);
            }
        }
        return tarP;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(GaMasterOp.class);
        }
    }
}
