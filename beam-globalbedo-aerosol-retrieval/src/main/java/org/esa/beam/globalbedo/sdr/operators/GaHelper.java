/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.util.Guardian;

/**
 *
 * @author akheckel
 */
public class GaHelper {
    private static GaHelper instance;
    private GaHelper() {}

    public synchronized static GaHelper getInstance() {
        if (instance == null) instance = new GaHelper();
        return instance;
    }

    public void createFlagMasks(Product targetProduct) {
        Guardian.assertNotNull("targetProduct", targetProduct);
        int w = targetProduct.getSceneRasterWidth();
        int h = targetProduct.getSceneRasterHeight();

        MyMaskColor mColor = new MyMaskColor();
        ProductNodeGroup<Mask> tarMG = targetProduct.getMaskGroup();
        ProductNodeGroup<FlagCoding> tarFCG = targetProduct.getFlagCodingGroup();
        for (int node=0; node<tarFCG.getNodeCount(); node++){
            FlagCoding fc = tarFCG.get(node);
            for (int i=0; i<fc.getNumAttributes(); i++){
                MetadataAttribute f = fc.getAttributeAt(i);
                String expr = fc.getName() + "." + f.getName();
                Mask m = Mask.BandMathsType.create(f.getName(), f.getDescription(), w, h, expr, mColor.next(), 0.5);
                tarMG.add(m);
            }
        }
    }

    public void createFlagMask(FlagCoding fc, int rasterWidth, int rasterHeight) {
        Guardian.assertNotNull("FlagCoding", fc);

        MyMaskColor mColor = new MyMaskColor();
        for (int i=0; i<fc.getNumAttributes(); i++){
            MetadataAttribute f = fc.getAttributeAt(i);
            String expr = fc.getName() + "." + f.getName();
            Mask m = Mask.BandMathsType.create(f.getName(), f.getDescription(), rasterWidth, rasterHeight, expr, mColor.next(), 0.5);
        }
    }

    public Band createTargetBand(AotConsts bandFeat, int rasterWidth, int rasterHeight) {
        Band targetBand = new Band(bandFeat.name,
                                   bandFeat.type,
                                   rasterWidth,
                                   rasterHeight);
        targetBand.setDescription(bandFeat.description);
        targetBand.setNoDataValue(bandFeat.noDataValue);
        targetBand.setNoDataValueUsed(bandFeat.noDataUsed);
        targetBand.setUnit(bandFeat.unit);
        targetBand.setScalingFactor(bandFeat.scale);
        targetBand.setScalingOffset(bandFeat.offset);
        return targetBand;
    }

}
