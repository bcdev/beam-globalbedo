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


import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.bbdr.BbdrAatsrOp;
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

@OperatorMetadata(alias = "ga.l2.bbdr.seaice")
public class MerisAatsrBbdrSeaiceOp extends Operator {

    @SourceProduct(alias = "master")
    private Product masterSourceProduct;

    @SourceProduct(alias = "slave")
    // this shall be a MERIS OR AATSR L1b for MERIS/AATSR synergy Idepix approach
    private Product slaveSourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Parameter(defaultValue = "false")
    private boolean computeL1ToAotProductOnly;

    @Parameter(defaultValue = "false")
    private boolean computeAotToBbdrProductOnly;

    @Parameter(defaultValue = "")
    private String tile;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        if (masterSourceProduct.getPreferredTileSize() == null) {
            masterSourceProduct.setPreferredTileSize(masterSourceProduct.getSceneRasterWidth(), 45);
            System.out.println("adjusting tile size to: " + masterSourceProduct.getPreferredTileSize());
        }

        Product aotProduct;
        Product collocationProduct = null;
        Product aotSourceProduct;

        if (computeAotToBbdrProductOnly) {
            aotProduct = masterSourceProduct;
        } else {
            collocationProduct = getTargetProductFromCollocation();

            aotSourceProduct = getCollocationMasterSubset(collocationProduct);

            GaMasterOp gaMasterOp = new GaMasterOp();
            gaMasterOp.setParameter("copyToaRadBands", false);
            gaMasterOp.setParameter("copyToaReflBands", true);
            gaMasterOp.setParameter("gaUseL1bLandWaterFlag", false);
            gaMasterOp.setSourceProduct(aotSourceProduct);
            aotProduct = gaMasterOp.getTargetProduct();
        }

        if (aotProduct.equals(GaMasterOp.EMPTY_PRODUCT)) {
            logger.log(Level.ALL, "No AOT product generated for source product: " + masterSourceProduct.getName() +
                    " --> cannot create BBDR product.");
        } else {
            if (computeL1ToAotProductOnly) {
                setTargetProduct(aotProduct);
            } else {
                Map<String, Product> fillSourceProds = new HashMap<String, Product>(2);
                fillSourceProds.put("sourceProduct", aotProduct);
                Map<String, Object> bbdrParams = new HashMap<String, Object>();
                bbdrParams.put("bbdrSeaIce", true);

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

                if (collocationProduct != null) {
                    ProductUtils.copyBand("reflec_nadir_1600", collocationProduct, bbdrProduct, true);
                    ProductUtils.copyBand("reflec_fward_1600", collocationProduct, bbdrProduct, true);
                }

                setTargetProduct(bbdrProduct);
            }
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
        for (Band bColloc : masterSourceProduct.getBands()) {
            if (!masterSubsetProduct.containsBand(bColloc.getName()) &&
                    !masterSubsetProduct.containsTiePointGrid(bColloc.getName())) {
                ProductUtils.copyBand(bColloc.getName(), collocationProduct, masterSubsetProduct, true);
                ProductUtils.copyRasterDataNodeProperties(bColloc, masterSubsetProduct.getBand(bColloc.getName()));
            }
        }

        return masterSubsetProduct;
    }

    private Product getTargetProductFromCollocation() {
        Map<String, Product> collocateInput = new HashMap<String, Product>(2);

        collocateInput.put("masterProduct", masterSourceProduct);
        collocateInput.put("slaveProduct", slaveSourceProduct);
        Product collocateProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(CollocateOp.class), GPF.NO_PARAMS, collocateInput);

        // in any case, we need an input for Idepix with MERIS as master...
        Product collocateMerisMasterProduct;
        if (sensor == Sensor.MERIS) {
            collocateMerisMasterProduct = collocateProduct;
        } else {
            collocateMerisMasterProduct = getCollocateMerisMasterProduct(collocateProduct, slaveSourceProduct);
        }

        // now create the Idepix product...
        Map<String, Product> idepixInput = new HashMap<String, Product>(1);
        idepixInput.put("source", collocateMerisMasterProduct);
        Map<String, Object> idepixParameters = new HashMap<String, Object>(1);
        idepixParameters.put("gaComputeFlagsOnly", true);
        Product idepixProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(GlobAlbedoOp.class), idepixParameters, idepixInput);

        // copy Idepix 'cloud_classif_flags' to collocation product
        ProductUtils.copyFlagBands(idepixProduct, collocateProduct, true);

        collocateProduct.setProductType(masterSourceProduct.getProductType());

        // band names must not have extensions!
        for (Band b : collocateProduct.getBands()) {
            if (b.getName().endsWith("_M") || b.getName().endsWith("_M")) {
                String bandNameNoExtension = b.getName().substring(0, b.getName().length() - 2);
                b.setName(bandNameNoExtension);
            }
        }

        return collocateProduct;
    }

    static Product getCollocateMerisMasterProduct(Product collocationProduct, Product merisSlaveProduct) {
        Product merisMasterProduct = new Product(collocationProduct.getName(),
                                                 collocationProduct.getProductType(),
                                                 collocationProduct.getSceneRasterWidth(),
                                                 collocationProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(collocationProduct, merisMasterProduct);
        ProductUtils.copyGeoCoding(collocationProduct, merisMasterProduct);
        ProductUtils.copyFlagCodings(collocationProduct, merisMasterProduct);
        ProductUtils.copyFlagBands(collocationProduct, merisMasterProduct, true);
        ProductUtils.copyMasks(collocationProduct, merisMasterProduct);
        merisMasterProduct.setStartTime(collocationProduct.getStartTime());
        merisMasterProduct.setEndTime(collocationProduct.getEndTime());
        // copy all  master bands from collocation product
        for (Band bColloc : collocationProduct.getBands()) {
            String bandNameNoExtension = bColloc.getName().substring(0, bColloc.getName().length() - 2);
            String targetBandName;
            if (bColloc.getName().endsWith("_M")) {
                targetBandName = bandNameNoExtension + "_S";
            } else {
                targetBandName = bandNameNoExtension + "_M";
            }
            // only copy this band if is not originally a MERIS tie point grid...
            if (merisSlaveProduct.getTiePointGrid(bandNameNoExtension) == null) {
                ProductUtils.copyBand(bColloc.getName(), collocationProduct, targetBandName, merisMasterProduct, true);
                ProductUtils.copyRasterDataNodeProperties(bColloc, merisMasterProduct.getBand(targetBandName));
            }
        }

        // MERIS tie point grids...
        for (int i = 0; i < merisSlaveProduct.getTiePointGrids().length; i++) {
            String tiePointGridName = merisSlaveProduct.getTiePointGrids()[i].getName();
            for (Band bColloc : collocationProduct.getBands()) {
                final String bandNameNoExtension = bColloc.getName().substring(0, bColloc.getName().length() - 2);
                if (bandNameNoExtension.equals(tiePointGridName)) {
                    final String tmpTpgName = "sun_elev_nadir";
                    if (collocationProduct.containsTiePointGrid(tmpTpgName) &&
                            !merisMasterProduct.containsTiePointGrid(tmpTpgName) &&
                            !merisMasterProduct.containsTiePointGrid(tiePointGridName)) {
                        ProductUtils.copyTiePointGrid(tmpTpgName, collocationProduct, merisMasterProduct);
                        merisMasterProduct.getTiePointGrid(tmpTpgName).setName(tiePointGridName);
                        merisMasterProduct.getTiePointGrid(tiePointGridName).setSourceImage(bColloc.getSourceImage());
                    }
                }
            }
        }

        return merisMasterProduct;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisAatsrBbdrSeaiceOp.class);
        }
    }
}
