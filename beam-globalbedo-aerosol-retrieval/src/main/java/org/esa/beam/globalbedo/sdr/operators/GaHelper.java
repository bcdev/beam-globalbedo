/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.Guardian;

import java.awt.Rectangle;
import java.io.IOException;

/**
 *
 * @author akheckel
 */
class GaHelper {

    static void createFlagMasks(Product targetProduct) {
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

    static Band createTargetBand(AotConsts bandFeat, int rasterWidth, int rasterHeight) {
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
     static Rectangle getSzaRegion(RasterDataNode szaBand, boolean hasSolarElevation, double szaLimit) throws OperatorException {
        int srcWidth = szaBand.getSceneRasterWidth();
        int srcHeight = szaBand.getSceneRasterHeight();
        float[] sza0 = new float[srcHeight];
        float[] sza1 = new float[srcHeight];
        try {
            szaBand.readPixels(0, 0, 1, srcHeight, sza0, ProgressMonitor.NULL);
            szaBand.readPixels(srcWidth - 1, 0, 1, srcHeight, sza1, ProgressMonitor.NULL);
        } catch (IOException ex) {
            throw new OperatorException(ex);
        }
        int start = 0;
        int end = srcHeight - 1;
        if (hasSolarElevation) {
            while (start < srcHeight - 1 && (90 - sza0[start]) > szaLimit && (90 - sza1[start]) > szaLimit) {
                start++;
            }
            while (end > start && (90 - sza0[end]) > szaLimit && (90 - sza1[end]) > szaLimit) {
                end--;
            }
        } else {
            while (start < srcHeight - 1 && (sza0[start]) > szaLimit && (sza1[start]) > szaLimit) {
                start++;
            }
            while (end > start && (sza0[end]) > szaLimit && (sza1[end]) > szaLimit) {
                end--;
            }
        }
        return new Rectangle(0, start, srcWidth, end - start + 1);
    }
}
