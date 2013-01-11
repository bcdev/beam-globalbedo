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

package org.esa.beam.globalbedo.bbdr;


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
import org.esa.beam.globalbedo.sdr.operators.GaMasterOp;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.idepix.algorithms.globalbedo.GlobAlbedoOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@OperatorMetadata(alias = "ga.l2")
public class GlobalbedoLevel2 extends Operator {

    @SourceProduct(alias = "master")
    private Product masterSourceProduct;

    @SourceProduct(alias = "slave", optional = true)
    // this shall be an AATSR L1b for MERIS/AATSR synergy Idepix approach
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
                    aotSourceProduct = getTargetProductFromCollocation(geometry, subsetOp);
                } else {
                    aotSourceProduct = subsetOp.getTargetProduct();
                }
            } else {
                if (slaveSourceProduct != null) {
                    aotSourceProduct = getTargetProductFromCollocation(null, null);
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
                BbdrOp bbdrOp = new BbdrOp();
                bbdrOp.setSourceProduct(aotProduct);
                bbdrOp.setParameter("sensor", sensor);
                Product bbdrProduct = bbdrOp.getTargetProduct();
                if (tileBased) {
                    setTargetProduct(TileExtractor.reproject(bbdrProduct, tile));
                } else {
                    setTargetProduct(bbdrProduct);
                }
            }
        }
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
        }  else {
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
        for (Band bColloc : collocateProduct.getBands()) {
            boolean isMasterBand = false;
            for (Band bMaster : masterSourceProduct.getBands()) {
                if (bMaster.getName().equals(bColloc.getName())) {
                    isMasterBand = true;
                    break;
                }
            }
            if (!isMasterBand) {
                collocateProduct.removeBand(bColloc);
            }
        }

        // copy Idepix 'cloud_classif_flags' to collocation product
        ProductUtils.copyFlagBands(idepixProduct, collocateProduct, true);

        // set product type of master source product
        collocateProduct.setProductType(masterSourceProduct.getProductType());

        return collocateProduct;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel2.class);
        }
    }
}
