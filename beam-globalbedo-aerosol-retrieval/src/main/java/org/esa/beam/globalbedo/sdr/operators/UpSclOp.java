/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.BorderExtender;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

/**
 *
 * @author akheckel
 */
@OperatorMetadata(alias = "ga.UpSclOp",
                  description = "upscaling product",
                  authors = "Andreas Heckel",
                  version = "1.1",
                  copyright = "(C) 2010 by University Swansea (a.heckel@swansea.ac.uk)")
public class UpSclOp extends Operator {

    @SourceProduct
    private Product lowresProduct;
    @SourceProduct
    private Product hiresProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue="9")
    private int scale;
    private int offset;
    private int targetWidth;
    private int targetHeight;
    private int sourceRasterWidth;
    private int sourceRasterHeight;
    private String targetProductName;
    private String targetProductType;
    private Band validBand;

    @Override
    public void initialize() throws OperatorException {
        sourceRasterWidth = lowresProduct.getSceneRasterWidth();
        sourceRasterHeight = lowresProduct.getSceneRasterHeight();
        targetWidth = hiresProduct.getSceneRasterWidth();
        targetHeight = hiresProduct.getSceneRasterHeight();

        offset = scale/2;
        InstrumentConsts instrC = InstrumentConsts.getInstance();
        String instrument = instrC.getInstrument(hiresProduct);
        final String validExpression = instrC.getAotExpression(instrument);
        final BandMathsOp validBandOp = BandMathsOp.createBooleanExpressionBand(validExpression, hiresProduct);
        validBand = validBandOp.getTargetProduct().getBandAt(0);

        targetProductName = lowresProduct.getName();
        targetProductType = lowresProduct.getProductType();
        targetProduct = new Product(targetProductName, targetProductType, targetWidth, targetHeight);
        targetProduct.setStartTime(hiresProduct.getStartTime());
        targetProduct.setEndTime(hiresProduct.getEndTime());
        targetProduct.setPointingFactory(hiresProduct.getPointingFactory());
        ProductUtils.copyMetadata(lowresProduct, targetProduct);
        ProductUtils.copyTiePointGrids(hiresProduct, targetProduct);
        ProductUtils.copyGeoCoding(hiresProduct, targetProduct);
        copyBands(lowresProduct, targetProduct);
        Mask lowresMask;
        Mask hiresMask;
        for (int i=0; i<lowresProduct.getMaskGroup().getNodeCount(); i++){
            lowresMask = lowresProduct.getMaskGroup().get(i);
            targetProduct.getMaskGroup().add(lowresMask);
        }


        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle tarRec = targetTile.getRectangle();
//System.err.println("upscaling "+targetBand.getName()+" on tile: " + tarRec);
        String targetBandName = targetBand.getName();
        Band sourceBand;
        Tile sourceTile;
        Tile validTile;

        final Rectangle srcRec = calcSourceRectangle(tarRec);

        sourceBand = lowresProduct.getBand(targetBandName);
        sourceTile = getSourceTile(sourceBand, srcRec, ProgressMonitor.NULL);
        validTile = getSourceTile(validBand, tarRec, ProgressMonitor.NULL);

        if (!targetBand.isFlagBand()) {
            upscaleTileBilinear(sourceTile, validTile, targetTile, tarRec, ProgressMonitor.NULL);
        }
        else {
            upscaleFlagCopy(sourceTile, targetTile, tarRec, ProgressMonitor.NULL);
        }
    }

    private Rectangle calcSourceRectangle(Rectangle tarRec) {
        int srcX = (tarRec.x - offset) / scale;
        int srcY = (tarRec.y - offset) / scale;
        int srcWidth = tarRec.width / scale + 2;
        int srcHeight = tarRec.height / scale + 2;
        if (srcX >= sourceRasterWidth - 1) {
            srcX = sourceRasterWidth - 2;
            srcWidth = 2;
        }
        if (srcY >= sourceRasterHeight - 1) {
            srcY = sourceRasterHeight - 2;
            srcHeight = 2;
        }
        if (srcX + srcWidth > sourceRasterWidth) {
            srcWidth = sourceRasterWidth - srcX;
        }
        if (srcY + srcHeight > sourceRasterHeight) {
            srcHeight = sourceRasterHeight - srcY;
        }
        return new Rectangle(srcX, srcY, srcWidth, srcHeight);
    }

    private void copyBands(Product sourceProduct, Product targetProduct) {
        Guardian.assertNotNull("source", sourceProduct);
        Guardian.assertNotNull("target", targetProduct);
        Band sourceBand;
        Band targetBand;
        FlagCoding flgCoding;
        ProductNodeGroup<FlagCoding> targetFCG = targetProduct.getFlagCodingGroup();

        for (int iBand = 0; iBand < sourceProduct.getNumBands(); iBand++) {
            sourceBand = sourceProduct.getBandAt(iBand);
            if (!targetProduct.containsBand(sourceBand.getName())) {
                targetBand = copyBandScl(sourceBand.getName(), sourceProduct, sourceBand.getName(), targetProduct);
                if (sourceBand.isFlagBand()) {
                    flgCoding = sourceBand.getFlagCoding();
                    if (!targetFCG.contains(flgCoding.getName())) {
                        ProductUtils.copyFlagCoding(flgCoding, targetProduct);
                    }
                    targetBand.setSampleCoding(targetFCG.get(flgCoding.getName()));
                }
            }
        }
    }

    /**
     * Copies the named band from the source product to the target product.
     *
     * @param sourceBandName the name of the band to be copied.
     * @param sourceProduct  the source product.
     * @param targetBandName the name of the band copied.
     * @param targetProduct  the target product.
     *
     * @return the copy of the band, or <code>null</code> if the sourceProduct does not contain a band with the given name.
     */
    private Band copyBandScl(String sourceBandName, Product sourceProduct,
                                String targetBandName, Product targetProduct) {
        Guardian.assertNotNull("sourceProduct", sourceProduct);
        Guardian.assertNotNull("targetProduct", targetProduct);

        if (sourceBandName == null || sourceBandName.length() == 0) {
            return null;
        }
        final Band sourceBand = sourceProduct.getBand(sourceBandName);
        if (sourceBand == null) {
            return null;
        }
        Band targetBand = new Band(targetBandName,
                                   sourceBand.getDataType(),
                                   targetProduct.getSceneRasterWidth(),
                                   targetProduct.getSceneRasterHeight());
        ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
        targetProduct.addBand(targetBand);
        return targetBand;
    }

    private void upscaleTileBilinear(Tile srcTile, Tile validTile, Tile tarTile, Rectangle tarRec, ProgressMonitor pm) {

        final int tarX = tarRec.x;
        final int tarY = tarRec.y;
        final int tarWidth = tarRec.width;
        final int tarHeight = tarRec.height;
        float erg;
        float noData = (float) tarTile.getRasterDataNode().getGeophysicalNoDataValue();

        for (int iTarY = tarY; iTarY < tarY + tarHeight; iTarY++) {
            int iSrcY = (iTarY - offset) / scale;
            if (iSrcY >= srcTile.getMaxY()) iSrcY = srcTile.getMaxY() - 1;
            float yFac = (float) (iTarY - offset) / scale - iSrcY;
            for (int iTarX = tarX; iTarX < tarX + tarWidth; iTarX++) {
                checkForCancellation(pm);
                int iSrcX = (iTarX - offset) / scale;
                if (iSrcX >= srcTile.getMaxX()) iSrcX = srcTile.getMaxX() - 1;
                float xFrac = (float) (iTarX - offset) / scale - iSrcX;
                erg = noData;
                if (validTile.getSampleBoolean(iTarX, iTarY)){
                    try{
                        erg = (1.0f - xFrac) * (1.0f - yFac) * srcTile.getSampleFloat(iSrcX, iSrcY);
                        erg +=        (xFrac) * (1.0f - yFac) * srcTile.getSampleFloat(iSrcX+1, iSrcY);
                        erg += (1.0f - xFrac) *        (yFac) * srcTile.getSampleFloat(iSrcX, iSrcY+1);
                        erg +=        (xFrac) *        (yFac) * srcTile.getSampleFloat(iSrcX+1, iSrcY+1);
                    } catch (Exception ex) {
                        System.err.println(iTarX+" / "+iTarY);
                        System.err.println(ex.getMessage());
                    }
                }
                tarTile.setSample(iTarX, iTarY, erg);
            }
        }
    }

    private void upscaleFlagCopy(Tile srcTile, Tile tarTile, Rectangle tarRec, ProgressMonitor pm) {

        final int tarX = tarRec.x;
        final int tarY = tarRec.y;
        final int tarWidth = tarRec.width;
        final int tarHeight = tarRec.height;

        for (int iTarY = tarY; iTarY < tarY + tarHeight; iTarY++) {
            // int iSrcY = (iTarY - offset) / scale;
            int iSrcY = (iTarY) / scale;
            if (iSrcY >= srcTile.getMaxY()) iSrcY = srcTile.getMaxY() - 1;
            for (int iTarX = tarX; iTarX < tarX + tarWidth; iTarX++) {
                checkForCancellation(pm);
                // int iSrcX = (iTarX - offset) / scale;
                int iSrcX = (iTarX) / scale;
                if (iSrcX >= srcTile.getMaxX()) iSrcX = srcTile.getMaxX() - 1;
                tarTile.setSample(iTarX, iTarY, srcTile.getSampleInt(iSrcX, iSrcY));
            }
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
            super(UpSclOp.class);
        }
    }
}
