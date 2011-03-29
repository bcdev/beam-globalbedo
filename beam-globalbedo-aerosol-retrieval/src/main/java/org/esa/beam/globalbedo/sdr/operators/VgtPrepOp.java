/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import java.util.HashMap;
import java.util.Map;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.idepix.operators.CloudScreeningSelector;
import org.esa.beam.idepix.operators.ComputeChainOp;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

/**
 * Create Vgt input product for Globalbedo aerosol retrieval and BBDR processor
 * @author akheckel
 */
@OperatorMetadata(alias = "ga.VgtPrepOp",
                  description = "Create Vgt product for input to Globalbedo aerosol retrieval and BBDR processor",
                  authors = "Andreas Heckel",
                  version = "1.0",
                  copyright = "(C) 2010 by University Swansea (a.heckel@swansea.ac.uk)")
public class VgtPrepOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {

        InstrumentConsts instrC = InstrumentConsts.getInstance();
        final boolean needPixelClassif = (!sourceProduct.containsBand(instrC.getIdepixFlagBandName()));
        final boolean needElevation = (!sourceProduct.containsBand(instrC.getElevationBandName()));
        final boolean needSurfacePres = (!sourceProduct.containsBand(instrC.getSurfPressureName("VGT")));

        //general SzaSubset to less 70°
        // doesn't work so easy with this projected data set! :(

        //Map<String,Object> szaSubParam = new HashMap<String, Object>(3);
        //szaSubParam.put("szaBandName", "SZA");
        //szaSubParam.put("hasSolarElevation", false);
        //szaSubParam.put("szaLimit", 69.99);
        //Product szaSubProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SzaSubsetOp.class), szaSubParam, sourceProduct);
        Product szaSubProduct = sourceProduct;

        // subset might have set ptype to null, thus:
        if (szaSubProduct.getDescription() == null) szaSubProduct.setDescription("vgt product");

        // setup target product primarily as copy of sourceProduct
        final int rasterWidth = szaSubProduct.getSceneRasterWidth();
        final int rasterHeight = szaSubProduct.getSceneRasterHeight();
        targetProduct = new Product(szaSubProduct.getName(),
                                    szaSubProduct.getProductType(),
                                    rasterWidth, rasterHeight);
        targetProduct.setStartTime(szaSubProduct.getStartTime());
        targetProduct.setEndTime(szaSubProduct.getEndTime());
        targetProduct.setPointingFactory(szaSubProduct.getPointingFactory());
        ProductUtils.copyTiePointGrids(szaSubProduct, targetProduct);
        ProductUtils.copyGeoCoding(szaSubProduct, targetProduct);
        ProductUtils.copyFlagBands(szaSubProduct, targetProduct);
        Mask mask;
        for (int i=0; i<szaSubProduct.getMaskGroup().getNodeCount(); i++){
            mask = szaSubProduct.getMaskGroup().get(i);
            targetProduct.getMaskGroup().add(mask);
        }

        // create pixel calssification if missing in sourceProduct
        // and add flag band to targetProduct
        Product idepixProduct = null;
        if (needPixelClassif) {
            Map<String, Object> pixelClassParam = new HashMap<String, Object>(4);
            pixelClassParam.put("algorithm", CloudScreeningSelector.GlobAlbedo);
            pixelClassParam.put("gaCopyRadiances", false);
            pixelClassParam.put("gaCopyAnnotations", false);
            pixelClassParam.put("gaComputeFlagsOnly", true);
            pixelClassParam.put("gaCloudBufferWidth", 3);
            idepixProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ComputeChainOp.class), pixelClassParam, szaSubProduct);
            ProductUtils.copyFlagBands(idepixProduct, targetProduct);
            for (int i=0; i<idepixProduct.getMaskGroup().getNodeCount(); i++){
                mask = idepixProduct.getMaskGroup().get(i);
                targetProduct.getMaskGroup().add(mask);
            }
        }

        // create elevation product if band is missing in sourceProduct
        Product elevProduct = null;
        if (needElevation){
            elevProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CreateElevationBandOp.class), GPF.NO_PARAMS, szaSubProduct);
        }

        // create surface pressure estimate product if band is missing in sourceProduct
        VirtualBand surfPresBand = null;
        if (needSurfacePres){
            String presExpr = "(1013.25 * exp(-elevation/8400))";
            surfPresBand = new VirtualBand(instrC.getSurfPressureName("VGT"),
                                                       ProductData.TYPE_FLOAT32,
                                                       rasterWidth, rasterHeight, presExpr);
            surfPresBand.setDescription("estimated sea level pressure (p0=1013.25hPa, hScale=8.4km)");
            surfPresBand.setNoDataValue(0);
            surfPresBand.setNoDataValueUsed(true);
            surfPresBand.setUnit("hPa");
        }



        // copy all bands from sourceProduct
        Band tarBand;
        for (Band srcBand : szaSubProduct.getBands()){
            String srcName = srcBand.getName();
            if (srcBand.isFlagBand()){
                tarBand = targetProduct.getBand(srcName);
                tarBand.setSourceImage(srcBand.getSourceImage());
            }
            else {
                tarBand = ProductUtils.copyBand(srcName, szaSubProduct, targetProduct);
                tarBand.setSourceImage(srcBand.getSourceImage());
            }
        }

        // add idepix flag band data if needed
        if (needPixelClassif){
            Guardian.assertNotNull("idepixProduct", idepixProduct);
            Band srcBand = idepixProduct.getBand(instrC.getIdepixFlagBandName());
            Guardian.assertNotNull("idepix Band", srcBand);
            tarBand = targetProduct.getBand(srcBand.getName());
            tarBand.setSourceImage(srcBand.getSourceImage());
        }

        // add elevation band if needed
        if (needElevation){
            Guardian.assertNotNull("elevProduct", elevProduct);
            Band srcBand = elevProduct.getBand(instrC.getElevationBandName());
            Guardian.assertNotNull("elevation band", srcBand);
            tarBand = ProductUtils.copyBand(srcBand.getName(), elevProduct, targetProduct);
            tarBand.setSourceImage(srcBand.getSourceImage());
        }

        // add vitrual surface pressure band if needed
        if (needSurfacePres) {
            targetProduct.addBand(surfPresBand);
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
            super(VgtPrepOp.class);
        }
    }
}
