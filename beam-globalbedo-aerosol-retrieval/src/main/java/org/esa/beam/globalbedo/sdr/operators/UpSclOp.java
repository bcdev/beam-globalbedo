/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
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

import java.awt.Rectangle;

/**
 * @author akheckel
 */
@OperatorMetadata(alias = "ga.UpSclOp",
                  description = "upscaling product",
                  authors = "Andreas Heckel",
                  version = "1.1",
                  copyright = "(C) 2010 by University Swansea (a.heckel@swansea.ac.uk)",
                  internal = true)
public class UpSclOp extends Operator {

    @SourceProduct
    private Product lowresProduct;
    @SourceProduct
    private Product hiresProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "9")
    private int scale;
    private int offset;
    private int sourceRasterWidth;
    private int sourceRasterHeight;
    private Band validBand;

    @Override
    public void initialize() throws OperatorException {
        sourceRasterWidth = lowresProduct.getSceneRasterWidth();
        sourceRasterHeight = lowresProduct.getSceneRasterHeight();
        int targetWidth = hiresProduct.getSceneRasterWidth();
        int targetHeight = hiresProduct.getSceneRasterHeight();

        offset = scale / 2;
        InstrumentConsts instrC = InstrumentConsts.getInstance();
        String instrument = instrC.getInstrument(hiresProduct);
        final String validExpression = instrC.getValAotOutExpression(instrument);
        final BandMathsOp validBandOp = BandMathsOp.createBooleanExpressionBand(validExpression, hiresProduct);
        validBand = validBandOp.getTargetProduct().getBandAt(0);

        String targetProductName = lowresProduct.getName();
        String targetProductType = lowresProduct.getProductType();
        targetProduct = new Product(targetProductName, targetProductType, targetWidth, targetHeight);
        targetProduct.setStartTime(hiresProduct.getStartTime());
        targetProduct.setEndTime(hiresProduct.getEndTime());
        targetProduct.setPointingFactory(hiresProduct.getPointingFactory());
        ProductUtils.copyMetadata(lowresProduct, targetProduct);
        ProductUtils.copyTiePointGrids(hiresProduct, targetProduct);
        ProductUtils.copyGeoCoding(hiresProduct, targetProduct);
        copyBands(lowresProduct, targetProduct);
        for (int i = 0; i < lowresProduct.getMaskGroup().getNodeCount(); i++) {
            Mask lowresMask = lowresProduct.getMaskGroup().get(i);
            targetProduct.getMaskGroup().add(lowresMask);
        }

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle tarRec = targetTile.getRectangle();
        String targetBandName = targetBand.getName();
        final Rectangle srcRec = calcSourceRectangle(tarRec);

        Band sourceBand = lowresProduct.getBand(targetBandName);
        Tile sourceTile = getSourceTile(sourceBand, srcRec);
        Tile validTile = getSourceTile(validBand, tarRec);

        if (!targetBand.isFlagBand()) {
            upscaleTileBilinear(sourceTile, validTile, targetTile, tarRec);
        } else {
            upscaleFlagCopy(sourceTile, targetTile, tarRec);
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
        ProductNodeGroup<FlagCoding> targetFCG = targetProduct.getFlagCodingGroup();

        for (int iBand = 0; iBand < sourceProduct.getNumBands(); iBand++) {
            Band sourceBand = sourceProduct.getBandAt(iBand);
            if (!targetProduct.containsBand(sourceBand.getName())) {
                Band targetBand = copyBandScl(sourceBand.getName(), sourceProduct, sourceBand.getName(), targetProduct);
                if (sourceBand.isFlagBand()) {
                    FlagCoding flgCoding = sourceBand.getFlagCoding();
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

    private void upscaleTileBilinear(Tile srcTile, Tile validTile, Tile tarTile, Rectangle tarRec) {

        final int tarX = tarRec.x;
        final int tarY = tarRec.y;
        final int tarWidth = tarRec.width;
        final int tarHeight = tarRec.height;
        final float noData = (float) tarTile.getRasterDataNode().getGeophysicalNoDataValue();

        for (int iTarY = tarY; iTarY < tarY + tarHeight; iTarY++) {
            int iSrcY = (iTarY - offset) / scale;
            if (iSrcY >= srcTile.getMaxY()) {
                iSrcY = srcTile.getMaxY() - 1;
            }
            float yFac = (float) (iTarY - offset) / scale - iSrcY;
            for (int iTarX = tarX; iTarX < tarX + tarWidth; iTarX++) {
                checkForCancellation();
                int iSrcX = (iTarX - offset) / scale;
                if (iSrcX >= srcTile.getMaxX()) {
                    iSrcX = srcTile.getMaxX() - 1;
                }
                float xFrac = (float) (iTarX - offset) / scale - iSrcX;

                float erg = noData;
                try {
                    if (validTile.getSampleBoolean(iTarX, iTarY)) {
                        erg = interpolatBilinear(srcTile, noData, xFrac, yFac, iSrcX, iSrcY);
                    }
                } catch (Exception ex) {
                    System.err.println(iTarX + " / " + iTarY);
                    System.err.println(ex.getMessage());
                } finally {
                    tarTile.setSample(iTarX, iTarY, erg);
                }
            }
        }
    }

    private float interpolatBilinear(Tile srcTile, float nodataValue, float xFrac, float yFac, int x, int y) {
        float value = srcTile.getSampleFloat(x, y);
        if (Double.compare(nodataValue, value) == 0) {
            return nodataValue;
        }
        float erg = (1.0f - xFrac) * (1.0f - yFac) * value;

        value = srcTile.getSampleFloat(x + 1, y);
        if (Double.compare(nodataValue, value) == 0) {
            return nodataValue;
        }
        erg += (xFrac) * (1.0f - yFac) * value;

        value = srcTile.getSampleFloat(x, y + 1);
        if (Double.compare(nodataValue, value) == 0) {
            return nodataValue;
        }
        erg += (1.0f - xFrac) * (yFac) * value;

        value = srcTile.getSampleFloat(x + 1, y + 1);
        if (Double.compare(nodataValue, value) == 0) {
            return nodataValue;
        }
        erg += (xFrac) * (yFac) * value;

        return erg;
    }

    private void upscaleFlagCopy(Tile srcTile, Tile tarTile, Rectangle tarRec) {

        final int tarX = tarRec.x;
        final int tarY = tarRec.y;
        final int tarWidth = tarRec.width;
        final int tarHeight = tarRec.height;

        for (int iTarY = tarY; iTarY < tarY + tarHeight; iTarY++) {
            // int iSrcY = (iTarY - offset) / scale;
            int iSrcY = (iTarY) / scale;
            if (iSrcY >= srcTile.getMaxY()) {
                iSrcY = srcTile.getMaxY() - 1;
            }
            for (int iTarX = tarX; iTarX < tarX + tarWidth; iTarX++) {
                checkForCancellation();
                // int iSrcX = (iTarX - offset) / scale;
                int iSrcX = (iTarX) / scale;
                if (iSrcX >= srcTile.getMaxX()) {
                    iSrcX = srcTile.getMaxX() - 1;
                }
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
