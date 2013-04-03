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


import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.collocation.CollocateOp;
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
import org.esa.beam.globalbedo.bbdr.BbdrOp;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.globalbedo.bbdr.TileExtractor;
import org.esa.beam.globalbedo.sdr.operators.GaMasterOp;
import org.esa.beam.gpf.operators.standard.MergeOp;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.idepix.algorithms.globalbedo.GlobAlbedoOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@OperatorMetadata(alias = "ga.l2.bbdr.seaice")
public class MerisAatsrBbdrSeaiceOp extends Operator {

    // todo: add the case 'AATSR BBDR' with sea ice flag from MERIS/AATSR synergy (not from AATSR alone)

    @SourceProduct(alias = "master")
    private Product masterSourceProduct;

    @SourceProduct(alias = "slave", optional = true)
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
    private boolean tileBased;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        if (masterSourceProduct.getPreferredTileSize() == null) {
            masterSourceProduct.setPreferredTileSize(masterSourceProduct.getSceneRasterWidth(), 45);
            System.out.println("adjusting tile size to: " + masterSourceProduct.getPreferredTileSize());
        }

        Product aotProduct;
        Product collocationProduct = null;
        Product aotSourceProduct = null;
        tileBased = tile != null && !tile.isEmpty();

        if (computeAotToBbdrProductOnly) {
            aotProduct = masterSourceProduct;
        } else {
            if (tileBased) {
                Geometry geometry = TileExtractor.computeProductGeometry(TileExtractor.reproject(masterSourceProduct, tile));
                if (geometry == null) {
                    throw new OperatorException("Could not get geometry for product");
                }
                SubsetOp subsetOp = new SubsetOp();
                subsetOp.setGeoRegion(geometry);
                subsetOp.setSourceProduct(masterSourceProduct);

                if (slaveSourceProduct != null) {
                    // todo: test this case!!!
                    collocationProduct = getTargetProductFromCollocation(geometry, subsetOp);
                    aotSourceProduct = getCollocationMasterSubset(collocationProduct);
                } else {
                    aotSourceProduct = subsetOp.getTargetProduct();
                }
            } else {
                if (slaveSourceProduct != null) {
                    collocationProduct = getTargetProductFromCollocation(null, null);
                    aotSourceProduct = getCollocationMasterSubset(collocationProduct);
//                    throw new OperatorException("MERIS/AATSR synergy approach must be processed in tileBased mode!");
                } else {
                    aotSourceProduct = masterSourceProduct;
                }
            }

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

                final boolean isBbdsSeaIce = ((sensor == Sensor.AATSR_NADIR || sensor == Sensor.AATSR_FWARD) ||
                        slaveSourceProduct != null);
                bbdrParams.put("bbdrSeaIce", isBbdsSeaIce);

                Product bbdrProduct = null;
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
                    bbdrProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrOp.class), bbdrParams, fillSourceProds);
                }

                if (collocationProduct != null) {
                    ProductUtils.copyBand("reflec_nadir_1600", collocationProduct, bbdrProduct, true);
                    ProductUtils.copyBand("reflec_fward_1600", collocationProduct, bbdrProduct, true);
                }

                if (tileBased) {
                    setTargetProduct(TileExtractor.reproject(bbdrProduct, tile));
                } else {
                    setTargetProduct(bbdrProduct);
                }
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
            System.out.println("bColloc = " + bColloc.getName());
            if (!masterSubsetProduct.containsBand(bColloc.getName()) &&
                    !masterSubsetProduct.containsTiePointGrid(bColloc.getName())) {
                ProductUtils.copyBand(bColloc.getName(), collocationProduct, masterSubsetProduct, true);
                ProductUtils.copyRasterDataNodeProperties(bColloc, masterSubsetProduct.getBand(bColloc.getName()));
            }
        }

        return masterSubsetProduct;
    }

    private Product getTargetProductFromCollocation(Geometry geometry, SubsetOp subsetOp) {
        Map<String, Product> collocateInput = new HashMap<String, Product>(2);
        if (tileBased && geometry != null && subsetOp != null) {
            Product subsettedMasterProduct;
            Product subsettedSlaveProduct;
            subsettedMasterProduct = subsetOp.getTargetProduct();

            subsetOp = new SubsetOp();
            subsetOp.setGeoRegion(geometry);
            subsetOp.setSourceProduct(slaveSourceProduct);
            subsettedSlaveProduct = subsetOp.getTargetProduct();

            // create collocation product...
            collocateInput.put("masterProduct", subsettedMasterProduct);
            collocateInput.put("slaveProduct", subsettedSlaveProduct);
        } else {
            collocateInput.put("masterProduct", masterSourceProduct);
            collocateInput.put("slaveProduct", slaveSourceProduct);
        }
        Product collocateProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(CollocateOp.class), GPF.NO_PARAMS, collocateInput);

        // create Idepix product...
        Map<String, Product> idepixInput = new HashMap<String, Product>(1);
        idepixInput.put("source", collocateProduct);
        Map<String, Object> idepixParameters = new HashMap<String, Object>(1);
        idepixParameters.put("gaComputeFlagsOnly", true);
        Product idepixProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(GlobAlbedoOp.class), idepixParameters, idepixInput);

        // remove all other than master bands from collocation product
//        for (Band bColloc : collocateProduct.getBands()) {
//            boolean isMasterBand = false;
//            for (Band bMaster : masterSourceProduct.getBands()) {
//                if (bMaster.getName().equals(bColloc.getName())) {
//                    isMasterBand = true;
//                    break;
//                }
//            }
////            if (!isMasterBand) {
//            if (!isMasterBand &&
//                    !bColloc.getName().equalsIgnoreCase("reflec_nadir_1600") &&
//                    !bColloc.getName().equalsIgnoreCase("reflec_fward_1600")) {
//                collocateProduct.removeBand(bColloc);
//            }
//        }

        // copy Idepix 'cloud_classif_flags' to collocation product
        ProductUtils.copyFlagBands(idepixProduct, collocateProduct, true);

        // set product type of master source product
        collocateProduct.setProductType(masterSourceProduct.getProductType());

        return collocateProduct;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisAatsrBbdrSeaiceOp.class);
        }
    }
}
