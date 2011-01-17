/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

/**
 * Main Operator producing AOT for GlobAlbedo
 */
@OperatorMetadata(alias = "ga.MasterOp",
                  description = "",
                  authors = "Andreas Heckel",
                  version = "1.1",
                  copyright = "(C) 2010 by University Swansea (a.heckel@swansea.ac.uk)")
public class GaMasterOp  extends Operator {

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
    @Parameter(defaultValue="2")
    private int vegSpecId;
    @Parameter(defaultValue="9")
    private int scale;
    private String instrument;

    @Override
    public void initialize() throws OperatorException {
        //if (tc1G) org.esa.beam.util.jai.JAIUtils.setDefaultTileCacheCapacity(1024);
        int npix=1;
        //int tarTileWidth = Math.min(targetProduct.getSceneRasterWidth(), npix * 51 * scale);
        Dimension fillTS = new Dimension(npix, npix);
        Dimension aotTS = new Dimension(npix*9, npix*9);
        //Dimension targetTS = new Dimension(tarTileWidth, scale);
        Dimension targetTS = new Dimension(npix*9*scale, npix*9*scale);
        RenderingHints rhTarget = new RenderingHints(GPF.KEY_TILE_SIZE, targetTS);
        RenderingHints rhAot = new RenderingHints(GPF.KEY_TILE_SIZE, aotTS);
        RenderingHints rhFill = new RenderingHints(GPF.KEY_TILE_SIZE, fillTS);


        boolean isMerisProduct = sourceProduct.getProductType().equals(EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME);
        isMerisProduct = isMerisProduct || sourceProduct.getProductType().equals(EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME);
        final boolean isAatsrProduct = sourceProduct.getProductType().equals(EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME);
        final boolean isVgtProduct = sourceProduct.getProductType().startsWith("VGT PRODUCT FORMAT V1.");

        Guardian.assertTrue("not a valid source product", (isMerisProduct ^ isAatsrProduct ^ isVgtProduct));

        Product reflProduct = null;
        if (isMerisProduct) {
            instrument = "MERIS";
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(MerisPrepOp.class), GPF.NO_PARAMS, sourceProduct);
        }
        else if (isAatsrProduct) {
            instrument = "AATSR";
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AatsrPrepOp.class), GPF.NO_PARAMS, sourceProduct);
        }
        else if (isVgtProduct) {
            instrument = "VGT";
            if (sourceProduct.getSceneRasterWidth() > 40000) throw new OperatorException("Product too large, who would do a global grid at 0.008deg resolution???");
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(VgtPrepOp.class), GPF.NO_PARAMS, sourceProduct);
        }


        Map<String, Object> aotParams = new HashMap<String, Object>(4);
        aotParams.put("vegSpecId", vegSpecId);
        aotParams.put("scale", scale);
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
            fillAotProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(GapFillingOp.class), GPF.NO_PARAMS, fillSourceProds, rhFill);
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
        String bname;
        if (copyToaRadBands){
            for (Band b : reflProduct.getBands()){
                bname = b.getName();
                if (b.getSpectralWavelength() > 0){
                    tarBand = ProductUtils.copyBand(bname, reflProduct, tarP);
                    tarBand.setSourceImage(b.getSourceImage());
                }
            }
        }
        for (Band b : reflProduct.getBands()){
            bname = b.getName();
            if (b.isFlagBand()){
                tarBand = tarP.getBand(bname);
                tarBand.setSourceImage(b.getSourceImage());
            }
            boolean copyBand = (copyToaReflBands && !tarP.containsBand(bname) && b.getSpectralWavelength() > 0);
            copyBand = copyBand || (instrument.equals("VGT") && InstrumentConsts.getInstance().isVgtAuxBand(b));
            copyBand = copyBand || (bname.equals("elevation"));

            if (copyBand){
                tarBand = ProductUtils.copyBand(bname, reflProduct, tarP);
                tarBand.setSourceImage(b.getSourceImage());
            }
        }
        for (Band b : aotHiresProduct.getBands()){
            bname = b.getName();
            if (b.isFlagBand()){
                tarBand = tarP.getBand(bname);
            }
            else {
                tarBand = ProductUtils.copyBand(bname, aotHiresProduct, tarP);
            }
            tarBand.setSourceImage(b.getSourceImage());
        }
        return tarP;
    }

    private void copyFlagBandsWithMask(Product sourceProduct, Product targetProduct) {
        ProductUtils.copyFlagBands(sourceProduct, targetProduct);
        Mask mask;
        for (int i=0; i<sourceProduct.getMaskGroup().getNodeCount(); i++){
            mask = sourceProduct.getMaskGroup().get(i);
            targetProduct.getMaskGroup().add(mask);
        }
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
