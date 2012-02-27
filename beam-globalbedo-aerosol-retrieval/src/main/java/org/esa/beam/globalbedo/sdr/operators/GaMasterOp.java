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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
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

import java.awt.Dimension;
import java.awt.RenderingHints;
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
public class GaMasterOp  extends Operator {

    public static final Product EMPTY_PRODUCT = new Product("empty", "empty", 0, 0);

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(defaultValue="false")
    private boolean copyToaRadBands;
    @Parameter(defaultValue="true")
    private boolean copyToaReflBands;
    @Parameter(defaultValue="false")
    private boolean noFilling;
    @Parameter(defaultValue="false")
    private boolean noUpscaling;
/*
    @Parameter(defaultValue="false")
    private boolean tc1G;
    @Parameter(defaultValue="true")
    private boolean retrieveAOT = true;
    @Parameter(defaultValue="false")
    private boolean saveSdrBands = false;
    @Parameter(defaultValue="false")
    private boolean saveModelBands = false;
*/
    @Parameter(defaultValue="1")
    private int soilSpecId;
    @Parameter(defaultValue="5")
    private int vegSpecId;
    @Parameter(defaultValue="9")
    private int scale;
    @Parameter(defaultValue="0.3")
    private float ndviThr;

    @Parameter(defaultValue = "true",
               label = "Perform equalization",
               description = "Perform removal of detector-to-detector systematic radiometric differences in MERIS L1b data products.")
    private boolean doEqualization;
    @Parameter(defaultValue = "true",
               label = " Use land-water flag from L1b product instead")
    private boolean gaUseL1bLandWaterFlag;
    @Parameter(label = "Include the named Rayleigh Corrected Reflectances in target product")
    private String[] gaOutputRayleigh;
    @Parameter(defaultValue = "false", label = " 'P1' (LISE, O2 project, all surfaces)")
    private boolean pressureOutputP1Lise = false;
    @Parameter(defaultValue = "false", label = " Use the LC cloud buffer algorithm")
    private boolean gaLcCloudBuffer = false;

    private String instrument;

    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct.getSceneRasterWidth() < 9 || sourceProduct.getSceneRasterHeight() < 9) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }
        Dimension targetTS = ImageManager.getPreferredTileSize(sourceProduct);
        Dimension aotTS = new Dimension(targetTS.width/9, targetTS.height/9);
        RenderingHints rhTarget = new RenderingHints(GPF.KEY_TILE_SIZE, targetTS);
        RenderingHints rhAot = new RenderingHints(GPF.KEY_TILE_SIZE, aotTS);

        String productType = sourceProduct.getProductType();
        boolean isMerisProduct = EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(productType).matches();
        final boolean isAatsrProduct = productType.equals(EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME);
        final boolean isVgtProduct = productType.startsWith("VGT PRODUCT FORMAT V1.");

        Guardian.assertTrue("not a valid source product", (isMerisProduct ^ isAatsrProduct ^ isVgtProduct));

        Product reflProduct = null;
        if (isMerisProduct) {
            instrument = "MERIS";
            Map<String, Object> params = new HashMap<String, Object>(4);
            params.put("gaUseL1bLandWaterFlag", gaUseL1bLandWaterFlag);
            params.put("doEqualization", doEqualization);
            params.put("gaOutputRayleigh", gaOutputRayleigh);
            params.put("pressureOutputP1Lise", pressureOutputP1Lise);
            params.put("gaLcCloudBuffer", gaLcCloudBuffer);
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(MerisPrepOp.class), params, sourceProduct);
        }
        else if (isAatsrProduct) {
            instrument = "AATSR";
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AatsrPrepOp.class), GPF.NO_PARAMS, sourceProduct);
        }
        else if (isVgtProduct) {
            instrument = "VGT";
//            if (sourceProduct.getSceneRasterWidth() > 40000) throw new OperatorException("Product too large, who would do a global grid at 0.008deg resolution???");
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(VgtPrepOp.class), GPF.NO_PARAMS, sourceProduct);
        }
        if (reflProduct == EMPTY_PRODUCT) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }

        Map<String, Object> aotParams = new HashMap<String, Object>(4);
        aotParams.put("soilSpecId", soilSpecId);
        aotParams.put("vegSpecId", vegSpecId);
        aotParams.put("scale", scale);
        aotParams.put("ndviThreshold", ndviThr);
        /*
        aotParams.put("retrieveAOT", retrieveAOT);
        aotParams.put("saveToaBands", saveToaBands);
        aotParams.put("saveSdrBands", saveSdrBands);
        aotParams.put("saveModelBands", saveModelBands);
         *
         */
        Product aotDownsclProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AerosolOp2.class), aotParams, reflProduct, rhAot);

        Product fillAotProduct = aotDownsclProduct;
        if (!noFilling){
            Map<String, Product> fillSourceProds = new HashMap<String, Product>(2);
            fillSourceProds.put("aotProduct", aotDownsclProduct);
            fillAotProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(GapFillingOp.class), GPF.NO_PARAMS, fillSourceProds);
        }

        targetProduct = fillAotProduct;
        if (!noUpscaling){
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
        ProductUtils.copyFlagBands(reflProduct, tarP);
        ProductUtils.copyFlagBands(aotHiresProduct, tarP);
        //copyFlagBandsWithMask(reflProduct, tarP);
        //copyFlagBandsWithMask(aotHiresProduct, tarP);
        Band tarBand;
        String sourceBandName;
        if (copyToaRadBands){
            for (Band sourceBand : reflProduct.getBands()){
                sourceBandName = sourceBand.getName();
                if (sourceBand.getSpectralWavelength() > 0){
                    ProductUtils.copyBand(sourceBandName, reflProduct, tarP);
                }
            }
        }
        for (Band sourceBand : reflProduct.getBands()){
            sourceBandName = sourceBand.getName();
            if (sourceBand.isFlagBand()){
                tarBand = tarP.getBand(sourceBandName);
                tarBand.setSourceImage(sourceBand.getSourceImage());
            }
            boolean copyBand = (copyToaReflBands && !tarP.containsBand(sourceBandName) && sourceBand.getSpectralWavelength() > 0);
            copyBand = copyBand || (instrument.equals("VGT") && InstrumentConsts.getInstance().isVgtAuxBand(sourceBand));
            copyBand = copyBand || (sourceBandName.equals("elevation"));
            copyBand = copyBand || (sourceBandName.equals("p1_lise") && pressureOutputP1Lise);

            if (copyBand){
                ProductUtils.copyBand(sourceBandName, reflProduct, tarP);
            }
        }
        for (Band sourceBand : aotHiresProduct.getBands()){
            sourceBandName = sourceBand.getName();
            if (sourceBand.isFlagBand()){
                tarBand = tarP.getBand(sourceBandName);
                tarBand.setSourceImage(sourceBand.getSourceImage());
            } else if (!tarP.containsBand(sourceBandName)) {
                ProductUtils.copyBand(sourceBandName, aotHiresProduct, tarP);
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
