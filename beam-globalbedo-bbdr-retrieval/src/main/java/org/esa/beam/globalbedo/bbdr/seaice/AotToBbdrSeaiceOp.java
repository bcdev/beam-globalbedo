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


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.bbdr.BbdrAatsrOp;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.gpf.operators.standard.MergeOp;
import org.esa.beam.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes SDR/BBDR from AOT input product.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.aot.bbdr",
        description = "Computes SDR/BBDR from AOT input product.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2013 by Brockmann Consult")
public class AotToBbdrSeaiceOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Parameter(defaultValue = "false")
    private boolean sdrOnly;

    @Override
    public void initialize() throws OperatorException {

        Map<String, Product> fillSourceProds = new HashMap<String, Product>(2);
        fillSourceProds.put("sourceProduct", sourceProduct);
        Map<String, Object> bbdrParams = new HashMap<String, Object>();
        bbdrParams.put("bbdrSeaIce", true);
        bbdrParams.put("seaiceWriteSdr", sdrOnly);

        Product bbdrProduct;
        if (sensor == Sensor.AATSR || sensor == Sensor.AATSR_FWARD) {
            bbdrParams.put("sensor", sensor);
            bbdrProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrAatsrOp.class), bbdrParams, fillSourceProds);
        } else if (sensor == Sensor.AATSR) {
            bbdrParams.put("sensor", Sensor.AATSR);
            Product bbdrNadirProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrAatsrOp.class), bbdrParams, fillSourceProds);
            bbdrParams.clear();
            bbdrParams.put("sensor", Sensor.AATSR_FWARD);
            Product bbdrFwardProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrAatsrOp.class), bbdrParams, fillSourceProds);
            // merge fward into nadir product...
            MergeOp mergeOp = new MergeOp();
            mergeOp.setParameterDefaultValues();
            mergeOp.setSourceProduct("masterProduct", bbdrNadirProduct);
            mergeOp.setSourceProduct("fward", bbdrFwardProduct);
            bbdrProduct = mergeOp.getTargetProduct();
        } else {
            bbdrParams.put("sensor", sensor);
            bbdrProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(BbdrSeaiceOp.class), bbdrParams, fillSourceProds);
        }

        ProductUtils.copyBand("reflec_nadir_1600", sourceProduct, bbdrProduct, true);
        ProductUtils.copyBand("reflec_fward_1600", sourceProduct, bbdrProduct, true);

        setTargetProduct(bbdrProduct);
        getTargetProduct().setProductType(sourceProduct.getProductType() + "_BBDR");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AotToBbdrSeaiceOp.class);
        }
    }
}
