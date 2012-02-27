/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import org.esa.beam.aatsrrecalibration.operators.RecalibrateAATSRReflectancesOp;
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
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.idepix.operators.CloudScreeningSelector;
import org.esa.beam.idepix.operators.ComputeChainOp;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * Create Aatsr input product for Globalbedo aerosol retrieval and BBDR processor
 * @author akheckel
 */
@OperatorMetadata(alias = "ga.AatsrPrepOp",
                  description = "Create Aatsr product for input to Globalbedo aerosol retrieval and BBDR processor",
                  authors = "Andreas Heckel",
                  version = "1.0",
                  copyright = "(C) 2010 by University Swansea (a.heckel@swansea.ac.uk)")
public class AatsrPrepOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        InstrumentConsts instrC = InstrumentConsts.getInstance();
        final boolean needPixelClassif = (!sourceProduct.containsBand(instrC.getIdepixFlagBandName()));
        final boolean needElevation = (!sourceProduct.containsBand(instrC.getElevationBandName()));
        final boolean needSurfacePres = (!sourceProduct.containsBand(instrC.getSurfPressureName("AATSR")));


        //general SzaSubset to less 70 degree
        Product szaSubProduct;
        Rectangle szaRegion = GaHelper.getSzaRegion(sourceProduct.getRasterDataNode("sun_elev_nadir"), true, 69.99);
        if (szaRegion.x == 0 && szaRegion.y == 0 &&
                szaRegion.width == sourceProduct.getSceneRasterWidth() &&
                szaRegion.height == sourceProduct.getSceneRasterHeight()) {
            szaSubProduct = sourceProduct;
        } else {
            Map<String,Object> subsetParam = new HashMap<String, Object>(3);
            subsetParam.put("region", szaRegion);
            szaSubProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SubsetOp.class), subsetParam, sourceProduct);
        }

        // subset might have set ptype to null, thus:
        if (szaSubProduct.getDescription() == null) szaSubProduct.setDescription("aatsr product");

        // recalibrate AATSR
        Product recalProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(RecalibrateAATSRReflectancesOp.class), GPF.NO_PARAMS, szaSubProduct);

        // setup target product primarily as copy of source product
        final int rasterWidth = szaSubProduct.getSceneRasterWidth();
        final int rasterHeight = szaSubProduct.getSceneRasterHeight();
        targetProduct = new Product(szaSubProduct.getName(),
                                    szaSubProduct.getProductType(),
                                    rasterWidth, rasterHeight);
        targetProduct.setStartTime(szaSubProduct.getStartTime());
        targetProduct.setEndTime(szaSubProduct.getEndTime());
        targetProduct.setPointingFactory(szaSubProduct.getPointingFactory());
        ProductUtils.copyMetadata(szaSubProduct, targetProduct);
        ProductUtils.copyTiePointGrids(szaSubProduct, targetProduct);
        ProductUtils.copyGeoCoding(szaSubProduct, targetProduct);
        ProductUtils.copyFlagBands(szaSubProduct, targetProduct);
        Mask mask;
        for (int i=0; i<szaSubProduct.getMaskGroup().getNodeCount(); i++){
            mask = szaSubProduct.getMaskGroup().get(i);
            targetProduct.getMaskGroup().add(mask);
        }

        // create pixel calssification if missing in szaSubProduct
        // and add flag band to targetProduct
        Product idepixNadirProduct = null;
        Product idepixFwardProduct = null;
        if (needPixelClassif) {
            Map<String, Object> pixelClassParam = new HashMap<String, Object>(4);
            pixelClassParam.put("algorithm", CloudScreeningSelector.GlobAlbedo);
            pixelClassParam.put("gaCopyRadiances", false);
            pixelClassParam.put("gaCopyAnnotations", false);
            pixelClassParam.put("gaComputeFlagsOnly", true);
            pixelClassParam.put("gaUseAatsrFwardForClouds", false);
            pixelClassParam.put("gaCloudBufferWidth", 3);
            idepixNadirProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ComputeChainOp.class), pixelClassParam, szaSubProduct);
            ProductUtils.copyFlagBands(idepixNadirProduct, targetProduct);
            for (int i=0; i<szaSubProduct.getMaskGroup().getNodeCount(); i++){
                mask = szaSubProduct.getMaskGroup().get(i);
                targetProduct.getMaskGroup().add(mask);
            }
            pixelClassParam.put("gaUseAatsrFwardForClouds", true);
            idepixFwardProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(ComputeChainOp.class), pixelClassParam, szaSubProduct);
            ProductUtils.copyBand(instrC.getIdepixFlagBandName(), idepixFwardProduct, instrC.getIdepixFwardFlagBandName(), targetProduct);
        }

        // create elevation product if band is missing in szaSubProduct
        Product elevProduct = null;
        if (needElevation){
            elevProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CreateElevationBandOp.class), GPF.NO_PARAMS, szaSubProduct);
        }

        // create surface pressure estimate product if band is missing in szaSubProduct
        VirtualBand surfPresBand = null;
        if (needSurfacePres){
            String presExpr = "(1013.25 * exp(-elevation/8400))";
            surfPresBand = new VirtualBand(instrC.getSurfPressureName("AATSR"),
                                                       ProductData.TYPE_FLOAT32,
                                                       rasterWidth, rasterHeight, presExpr);
            surfPresBand.setDescription("estimated sea level pressure (p0=1013.25hPa, hScale=8.4km)");
            surfPresBand.setNoDataValue(0);
            surfPresBand.setNoDataValueUsed(true);
            surfPresBand.setUnit("hPa");
        }


        // copy all non-reflec bands from szaSubProduct and
        // copy reflectance bands from recalProduct
        Band tarBand;
        for (Band srcBand : szaSubProduct.getBands()){
            String srcName = srcBand.getName();
            if (srcBand.isFlagBand()){
                tarBand = targetProduct.getBand(srcName);
                tarBand.setSourceImage(srcBand.getSourceImage());
            }
            else if (srcName.startsWith("reflec")){
                ProductUtils.copyBand(srcName, recalProduct, targetProduct);
            }
            else {
                ProductUtils.copyBand(srcName, szaSubProduct, targetProduct);

            }
        }

        // add idepix flag band data if needed
        if (needPixelClassif){
            Guardian.assertNotNull("idepixProduct", idepixNadirProduct);
            Band srcBand = idepixNadirProduct.getBand(instrC.getIdepixFlagBandName());
            Guardian.assertNotNull("idepix Band", srcBand);
            tarBand = targetProduct.getBand(srcBand.getName());
            tarBand.setSourceImage(srcBand.getSourceImage());

            Guardian.assertNotNull("idepixProduct", idepixFwardProduct);
            srcBand = idepixFwardProduct.getBand(instrC.getIdepixFlagBandName());
            Guardian.assertNotNull("idepix Band", srcBand);
            tarBand = targetProduct.getBand(instrC.getIdepixFwardFlagBandName());
            tarBand.setSourceImage(srcBand.getSourceImage());
        }

        // add elevation band if needed
        if (needElevation){
            Guardian.assertNotNull("elevProduct", elevProduct);
            Band srcBand = elevProduct.getBand(instrC.getElevationBandName());
            Guardian.assertNotNull("elevation band", srcBand);
            ProductUtils.copyBand(srcBand.getName(), elevProduct, targetProduct);
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
            super(AatsrPrepOp.class);
        }
    }
}
