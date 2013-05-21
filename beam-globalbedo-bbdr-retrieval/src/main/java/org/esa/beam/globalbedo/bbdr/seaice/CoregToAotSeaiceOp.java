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


import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.bbdr.BbdrConstants;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.globalbedo.sdr.operators.GaMasterOp;
import org.esa.beam.idepix.algorithms.globalbedo.GlobAlbedoOp;
import org.esa.beam.idepix.util.IdepixUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.image.DataBuffer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Applies IDEPIX to a MERIS/AATSR collocation product and then computes AOT.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.coreg.aot",
                  description = "Applies IDEPIX to a MERIS/AATSR collocation product and then computes AOT.",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class CoregToAotSeaiceOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @SourceProduct
    private Product merisL1bProduct;

    @SourceProduct
    private Product aatsrL1bProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Parameter(defaultValue = "false")
    private boolean sdrOnly;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        Product aotProduct;
        Product aotSourceProduct;

        Product extendedCollocationProduct = getCollocationProductWithIdepix();

//        setTargetProduct(extendedCollocationProduct); // test!!!

        aotSourceProduct = getCollocationMasterSubset(extendedCollocationProduct);

        GaMasterOp gaMasterOp = new GaMasterOp();
        gaMasterOp.setParameter("copyToaRadBands", false);
        gaMasterOp.setParameter("copyToaReflBands", true);
        gaMasterOp.setParameter("gaUseL1bLandWaterFlag", false);
        gaMasterOp.setParameter("isBbdrSeaice", true);
        gaMasterOp.setSourceProduct(aotSourceProduct);
        aotProduct = gaMasterOp.getTargetProduct();

        if (aotProduct.equals(GaMasterOp.EMPTY_PRODUCT)) {
            logger.log(Level.ALL, "No AOT product generated for source product: " + extendedCollocationProduct.getName() +
                    " --> cannot create BBDR product.");
        } else {
            if (extendedCollocationProduct != null && !(sensor == Sensor.AATSR)) {
                ProductUtils.copyBand("reflec_nadir_1600", extendedCollocationProduct, aotProduct, true);
                ProductUtils.copyBand("reflec_fward_1600", extendedCollocationProduct, aotProduct, true);
            }
            setTargetProduct(aotProduct);
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
        Product collocFromCoregProduct = null;
        try {
            collocFromCoregProduct = getCollocFromCoregProduct(sourceProduct);
        } catch (IOException e) {
            // todo
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        Product collocateMerisMasterProduct;
        if (sensor == Sensor.MERIS) {
            collocateMerisMasterProduct = collocFromCoregProduct;
        } else {
            collocateMerisMasterProduct = getCollocationMerisMasterProduct(collocFromCoregProduct);
        }

        // now create the Idepix product...
        Map<String, Product> idepixInput = new HashMap<String, Product>(1);
        idepixInput.put("source", collocateMerisMasterProduct);
        Map<String, Object> idepixParameters = new HashMap<String, Object>(1);
        idepixParameters.put("gaComputeFlagsOnly", true);
//        idepixParameters.put("gaUseL1bLandWaterFlag", true);
        Product idepixProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(GlobAlbedoOp.class), idepixParameters, idepixInput);

        // copy Idepix 'cloud_classif_flags' to collocation product
        ProductUtils.copyFlagBands(idepixProduct, collocFromCoregProduct, true);

        final String productType = sensor == Sensor.MERIS ? "MER_RR__1P" : "ATS_TOA_1P";
        collocFromCoregProduct.setProductType(productType);

        // band names must not have extensions!
        for (Band b : collocFromCoregProduct.getBands()) {
            if (b.getName().endsWith("_M") || b.getName().endsWith("_M")) {
                String bandNameNoExtension = b.getName().substring(0, b.getName().length() - 2);
                b.setName(bandNameNoExtension);
            }
        }

        return collocFromCoregProduct;
    }

    private Product getCollocFromCoregProduct(Product coregProduct) throws IOException {
        Product collocProduct = new Product(coregProduct.getName(),
                                            "COLLOCATED",
                                            coregProduct.getSceneRasterWidth(),
                                            coregProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(coregProduct, collocProduct);
        ProductUtils.copyFlagCodings(merisL1bProduct, collocProduct);
        ProductUtils.copyFlagCodings(aatsrL1bProduct, collocProduct);
        ProductUtils.copyMasks(coregProduct, collocProduct);

        final ProductData.UTC startTime =
                sensor == Sensor.MERIS ? merisL1bProduct.getStartTime() : aatsrL1bProduct.getStartTime();
        final ProductData.UTC endTime =
                sensor == Sensor.MERIS ? merisL1bProduct.getEndTime() : aatsrL1bProduct.getEndTime();

        collocProduct.setStartTime(startTime);
        collocProduct.setEndTime(endTime);

        // copy all bands from collocation product which are no flag bands and no original MERIS tie points
        for (Band bCoreg : coregProduct.getBands()) {
            String bandNameNoExtension = bCoreg.getName().substring(0, bCoreg.getName().length() - 2);
            if (!isMerisTpg(bandNameNoExtension) &&
                    !bandNameNoExtension.contains("flag") &&
                    !bCoreg.getName().startsWith("lat_") &&
                    !bCoreg.getName().startsWith("lon_") &&
                    !bandNameNoExtension.contains("detector")) {
                if (!collocProduct.containsBand(bCoreg.getName())) {
                    System.out.println("copy: " + bCoreg.getName());
                    ProductUtils.copyBand(bCoreg.getName(), coregProduct, collocProduct, true);
                    ProductUtils.copyRasterDataNodeProperties(bCoreg, collocProduct.getBand(bCoreg.getName()));
                    if (bandNameNoExtension.startsWith("radiance")) {
                        final String spectralBandIndex = bandNameNoExtension.substring(9, bandNameNoExtension.length());
                        collocProduct.getBand(bCoreg.getName()).setSpectralBandIndex(Integer.parseInt(spectralBandIndex)-1);
                    }
                }
            } else {
                final int width = collocProduct.getSceneRasterWidth();
                final int height = collocProduct.getSceneRasterHeight();
                if (bandNameNoExtension.contains("flag")) {
                    // restore flag bands with int values...
                    System.out.println("adding flag band: " + bCoreg.getName());
                    Band flagBand = collocProduct.addBand(bCoreg.getName(), ProductData.TYPE_INT32);
                    flagBand.setSampleCoding(collocProduct.getFlagCodingGroup().get(bandNameNoExtension));
                    DataBuffer dataBuffer = bCoreg.getSourceImage().getData().getDataBuffer();
                    final int[] flagData = new int[dataBuffer.getSize()];
                    for (int i = 0; i < dataBuffer.getSize(); i++) {
                        final float elemFloatAt = dataBuffer.getElemFloat(i);
                        flagData[i] = (int) elemFloatAt;
                    }
                    flagBand.setDataElems(flagData);
                    flagBand.getSourceImage();
                } else if (bandNameNoExtension.contains("detector")) {     // MERIS detector index band
                    // restore detector index band with short values...
                    System.out.println("adding detector index band: " + bCoreg.getName());
                    Band detectorIndexBand = collocProduct.addBand(bCoreg.getName(), ProductData.TYPE_INT16);
                    DataBuffer dataBuffer = bCoreg.getSourceImage().getData().getDataBuffer();
                    final short[] detectorIndexData = new short[dataBuffer.getSize()];
                    for (int i = 0; i < dataBuffer.getSize(); i++) {
                        final float elemFloatAt = dataBuffer.getElemFloat(i);
                        detectorIndexData[i] = (short) elemFloatAt;
                    }
                    detectorIndexBand.setDataElems(detectorIndexData);
                    detectorIndexBand.getSourceImage();
                }else if (isMerisTpg(bandNameNoExtension)) {
                    // restore tie point grids...
                    DataBuffer dataBuffer = bCoreg.getSourceImage().getData().getDataBuffer();
                    float[] tpgData = new float[dataBuffer.getSize()];
                    for (int i = 0; i < dataBuffer.getSize(); i++) {
                        tpgData[i] = dataBuffer.getElemFloat(i);
                    }
                    TiePointGrid tpg = new TiePointGrid(bandNameNoExtension,
                                                        width,
                                                        height,
                                                        0.0f, 0.0f, 1.0f, 1.0f, tpgData);
                    tpg.setSourceImage(bCoreg.getSourceImage());
                    System.out.println("adding tpg: " + bandNameNoExtension);
                    collocProduct.addTiePointGrid(tpg);
                }
            }
        }
        final TiePointGrid latTpg = collocProduct.getTiePointGrid("latitude");
        final TiePointGrid lonTpg = collocProduct.getTiePointGrid("longitude");
        if (latTpg == null || lonTpg == null) {
            throw new OperatorException("latitude or longitude tie point grid missing - cannot proceed.");
        }
        collocProduct.setGeoCoding(new TiePointGeoCoding(latTpg, lonTpg));

        return collocProduct;
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

    private static boolean isAatsrTpg(String bandName) {
        for (String aatsrTpgName : BbdrConstants.AATSR_TIE_POINT_GRID_NAMES) {
            if (aatsrTpgName.equals(bandName)) {
                return true;
            }
        }
        return false;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CoregToAotSeaiceOp.class);
        }
    }
}
