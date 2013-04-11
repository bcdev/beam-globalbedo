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
import org.esa.beam.globalbedo.bbdr.BbdrAatsrOp;
import org.esa.beam.globalbedo.bbdr.BbdrConstants;
import org.esa.beam.globalbedo.bbdr.seaice.BbdrSeaiceOp;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.globalbedo.sdr.operators.GaMasterOp;
import org.esa.beam.gpf.operators.standard.MergeOp;
import org.esa.beam.idepix.algorithms.globalbedo.GlobAlbedoOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies IDEPIX to a MERIS/AATSR collocation product and then computes SDR/BBDR.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.colloc.bbdr",
                  description = "Applies IDEPIX to a MERIS/AATSR collocation product and then computes SDR/BBDR.",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class CollocToBbdrSeaiceOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Parameter(defaultValue = "false")
    private boolean sdrOnly;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        if (sourceProduct.getPreferredTileSize() == null) {
            sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 45);
            System.out.println("adjusting tile size to: " + sourceProduct.getPreferredTileSize());
        }

        Product aotProduct;
        Product aotSourceProduct;

        Product extendedCollocationProduct = getCollocationProductWithIdepix();

        aotSourceProduct = getCollocationMasterSubset(extendedCollocationProduct);

        GaMasterOp gaMasterOp = new GaMasterOp();
        gaMasterOp.setParameter("copyToaRadBands", false);
        gaMasterOp.setParameter("copyToaReflBands", true);
        gaMasterOp.setParameter("gaUseL1bLandWaterFlag", false);
        gaMasterOp.setSourceProduct(aotSourceProduct);
        aotProduct = gaMasterOp.getTargetProduct();

        if (aotProduct.equals(GaMasterOp.EMPTY_PRODUCT)) {
            logger.log(Level.ALL, "No AOT product generated for source product: " + extendedCollocationProduct.getName() +
                    " --> cannot create BBDR product.");
        } else {
            Map<String, Product> fillSourceProds = new HashMap<String, Product>(2);
            fillSourceProds.put("sourceProduct", aotProduct);
            Map<String, Object> bbdrParams = new HashMap<String, Object>();
            bbdrParams.put("bbdrSeaIce", true);
            bbdrParams.put("seaiceWriteSdr", sdrOnly);

            Product bbdrProduct;
            if (sensor == Sensor.AATSR_NADIR || sensor == Sensor.AATSR_FWARD) {
                bbdrParams.put("sensor", sensor);
                bbdrProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrAatsrOp.class), bbdrParams, fillSourceProds);
            } else if (sensor == Sensor.AATSR) {
                bbdrParams.put("sensor", Sensor.AATSR_NADIR);
                Product bbdrNadirProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrAatsrOp.class), bbdrParams, fillSourceProds);
                bbdrParams.clear();
                bbdrParams.put("sensor", Sensor.AATSR_FWARD);
                Product bbdrFwardProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrAatsrOp.class), bbdrParams, fillSourceProds);
                // merge fward into nadir product...
                MergeOp mergeOp = new MergeOp();
                mergeOp.setSourceProduct("masterProduct", bbdrNadirProduct);
                mergeOp.setSourceProduct("fward", bbdrFwardProduct);
                bbdrProduct = mergeOp.getTargetProduct();
            } else {
                bbdrParams.put("sensor", sensor);
                bbdrProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrSeaiceOp.class), bbdrParams, fillSourceProds);
            }

            if (extendedCollocationProduct != null) {
                ProductUtils.copyBand("reflec_nadir_1600", extendedCollocationProduct, bbdrProduct, true);
                ProductUtils.copyBand("reflec_fward_1600", extendedCollocationProduct, bbdrProduct, true);
            }

            setTargetProduct(bbdrProduct);
        }
    }

    private Product getCollocationMasterSubset(Product collocationProduct) {
        Product masterSubsetProduct = new Product(collocationProduct.getName(),
                                                  collocationProduct.getProductType(),
                                                  collocationProduct.getSceneRasterWidth(),
                                                  collocationProduct.getSceneRasterHeight());
        ProductUtils.copyMetadata(collocationProduct, masterSubsetProduct);
        ProductUtils.copyTiePointGrids(collocationProduct, masterSubsetProduct);
        ProductUtils.copyGeoCoding(collocationProduct, masterSubsetProduct);
        ProductUtils.copyFlagCodings(collocationProduct, masterSubsetProduct);
        ProductUtils.copyFlagBands(collocationProduct, masterSubsetProduct, true);
        ProductUtils.copyMasks(collocationProduct, masterSubsetProduct);
        masterSubsetProduct.setStartTime(collocationProduct.getStartTime());
        masterSubsetProduct.setEndTime(collocationProduct.getEndTime());
        // copy all  master bands from collocation product
        final String[] masterBandNames = sensor == Sensor.MERIS ? EnvisatConstants.MERIS_L1B_BAND_NAMES :
                EnvisatConstants.AATSR_L1B_BAND_NAMES;

        for (String bandName : masterBandNames) {
            if (!masterSubsetProduct.containsBand(bandName) &&
                    !masterSubsetProduct.containsTiePointGrid(bandName)) {
                ProductUtils.copyBand(bandName, collocationProduct, masterSubsetProduct, true);
                ProductUtils.copyRasterDataNodeProperties(collocationProduct.getBand(bandName),
                                                          masterSubsetProduct.getBand(bandName));
            }
        }

        return masterSubsetProduct;
    }

    private Product getCollocationProductWithIdepix() {
        // in any case, we need an input for Idepix with MERIS as master...
        Product collocateMerisMasterProduct;
        if (sensor == Sensor.MERIS) {
            collocateMerisMasterProduct = sourceProduct;
        } else {
            collocateMerisMasterProduct = getCollocationMerisMasterProduct(sourceProduct);
        }

        // now create the Idepix product...
        Map<String, Product> idepixInput = new HashMap<String, Product>(1);
        idepixInput.put("source", collocateMerisMasterProduct);
        Map<String, Object> idepixParameters = new HashMap<String, Object>(1);
        idepixParameters.put("gaComputeFlagsOnly", true);
        Product idepixProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(GlobAlbedoOp.class), idepixParameters, idepixInput);

        // copy Idepix 'cloud_classif_flags' to collocation product
        ProductUtils.copyFlagBands(idepixProduct, sourceProduct, true);

        final String  productType = sensor == Sensor.MERIS ? "MER_RR__1P" : "ATS_TOA_1P";
        sourceProduct.setProductType(productType);

        // band names must not have extensions!
        for (Band b : sourceProduct.getBands()) {
            if (b.getName().endsWith("_M") || b.getName().endsWith("_M")) {
                String bandNameNoExtension = b.getName().substring(0, b.getName().length() - 2);
                b.setName(bandNameNoExtension);
            }
        }

        return sourceProduct;
    }

    static Product getCollocationMerisMasterProduct(Product collocProduct) {
        Product merisMasterProduct = new Product(collocProduct.getName(),
                                                 collocProduct.getProductType(),
                                                 collocProduct.getSceneRasterWidth(),
                                                 collocProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(collocProduct, merisMasterProduct);
        ProductUtils.copyGeoCoding(collocProduct, merisMasterProduct);
        ProductUtils.copyFlagCodings(collocProduct, merisMasterProduct);
        ProductUtils.copyFlagBands(collocProduct, merisMasterProduct, true);
        ProductUtils.copyMasks(collocProduct, merisMasterProduct);
        merisMasterProduct.setStartTime(collocProduct.getStartTime());
        merisMasterProduct.setEndTime(collocProduct.getEndTime());
        // copy all  master bands from collocation product
        for (Band bColloc : collocProduct.getBands()) {
            String bandNameNoExtension = bColloc.getName().substring(0, bColloc.getName().length() - 2);
            String targetBandName;
            if (bColloc.getName().endsWith("_M")) {
                targetBandName = bandNameNoExtension + "_S";
            } else {
                targetBandName = bandNameNoExtension + "_M";
            }
            // only copy this band if is not originally a MERIS tie point grid...
            if (!isMerisTpg(bandNameNoExtension)) {
                ProductUtils.copyBand(bColloc.getName(), collocProduct, targetBandName, merisMasterProduct, true);
                ProductUtils.copyRasterDataNodeProperties(bColloc, merisMasterProduct.getBand(targetBandName));
            }
        }

        // MERIS tie point grids...
        for (int i = 0; i < EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES.length; i++) {
            String tiePointGridName = EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[i];
            for (Band bColloc : collocProduct.getBands()) {
                final String bandNameNoExtension = bColloc.getName().substring(0, bColloc.getName().length() - 2);
                if (bandNameNoExtension.equals(tiePointGridName)) {
                    final String tmpTpgName = "sun_elev_nadir";
                    if (collocProduct.containsTiePointGrid(tmpTpgName) &&
                            !merisMasterProduct.containsTiePointGrid(tmpTpgName) &&
                            !merisMasterProduct.containsTiePointGrid(tiePointGridName)) {
                        ProductUtils.copyTiePointGrid(tmpTpgName, collocProduct, merisMasterProduct);
                        merisMasterProduct.getTiePointGrid(tmpTpgName).setName(tiePointGridName);
                        merisMasterProduct.getTiePointGrid(tiePointGridName).setSourceImage(bColloc.getSourceImage());
                    }
                }
            }
        }

        return merisMasterProduct;
    }

    private static boolean isMerisTpg(String bandName) {
        for (String merisTpgName : BbdrConstants.MERIS_TIE_POINT_GRID_NAMES) {
            if (merisTpgName.equals(bandName)) {
                return true;
            }
        }
        return false;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CollocToBbdrSeaiceOp.class);
        }
    }
}
