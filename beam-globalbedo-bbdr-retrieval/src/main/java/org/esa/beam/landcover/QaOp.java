package org.esa.beam.landcover;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import java.io.IOException;

/**
 * todo: add comment
 *
 * @author olafd
 */
@OperatorMetadata(alias = "lc.l2.qa")
public class QaOp extends Operator {

    @SourceProduct(alias = "l1b", description = "MERIS L1b (N1) product")
    private Product sourceProduct;

    @Parameter(defaultValue = "l1_flags")
    private String l1FlagBandName;

    @Parameter(defaultValue = "radiance_13")
    private String radianceOutputBandName;

    @Parameter(defaultValue = "l1_flags.INVALID")
    private String invalidExpression;

    @Parameter(defaultValue = "7")
    private int invalidMaskBitIndex;

    // percentage of bad data values per row as criterion for QA failure (100% means that whole row must be 'bad')
    @Parameter(defaultValue = "100.0")
    private float percentBadDataValuesThreshold;

    // threshold of rows identified as 'bad' as criterion for QA failure
    @Parameter(defaultValue = "1")
    private int badDataRowsThreshold;

    private static final int START_ROW_OFFSET_MAX = 100;
    private static final int END_ROW_OFFSET_MAX = 100;

    @Override
    public void initialize() throws OperatorException {

        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();

        final Band b = sourceProduct.getBand(l1FlagBandName);
        if (b == null) {
            throw new OperatorException("Input product does not contain specified L1 flag band.");
        }

        boolean qaFailed = false;
        try {
            b.loadRasterData();
        } catch (IOException e) {
            System.out.println("Cannot load raster data for band '" + b.getName() + "' - product QA failed.");
            writeProductForQAFailure(true);
            return;
        }
        final RasterDataNode sourceRaster = sourceProduct.getRasterDataNode(b.getName());

        int yStart = 0;
        boolean firstGoodRowFound = false;
        while (!firstGoodRowFound && yStart < height-1) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, yStart, width, 1, pixels);
            int x = 0;
            while (!firstGoodRowFound && x < width - 1) {
                firstGoodRowFound = !isInvalidMaskBitSet(pixels[x]);
                x++;
            }
            yStart++;
        }

        if (yStart >= START_ROW_OFFSET_MAX) {
            System.out.println("Too many invalid rows (" + yStart + ") at start of product - QA failed.");
            writeProductForQAFailure(true);
            return;
        }

        int yEnd = height-1;
        boolean lastGoodRowFound = false;
        while (!lastGoodRowFound && yEnd > yStart) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, yEnd, width, 1, pixels);
            int x = 0;
            while (!lastGoodRowFound && x < width - 1) {
                lastGoodRowFound = !isInvalidMaskBitSet(pixels[x]);
                x++;
            }
            yEnd--;
        }

        if (yEnd <= height-END_ROW_OFFSET_MAX) {
            System.out.println("Too many invalid rows (" + yStart + ") at end of product - QA failed.");
            writeProductForQAFailure(true);
            return;
        }

        int y = yStart;
        int badDataRows = 0;
        while (!qaFailed && y < yEnd) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, y, width, 1, pixels);
            int x = 0;
            int badCount = 0;
            while (badCount * 100.0 / (width - 1) < percentBadDataValuesThreshold && x < width - 1) {
                final boolean isBadPixel = isInvalidMaskBitSet(pixels[x]);
                if (isBadPixel) {
                    badCount++;
                }
                x++;
            }
            final double percentBadDataValues = badCount * 100.0 / (width - 1);
            if (percentBadDataValues >= percentBadDataValuesThreshold) {
                // row is identified as 'bad'
                badDataRows++;
                if (badDataRows >= badDataRowsThreshold) {
                    qaFailed = true;
                }
            }
            y++;
        }

        writeProductForQAFailure(qaFailed);
    }

    private boolean isInvalidMaskBitSet(int pixel) {
        return ((pixel >> invalidMaskBitIndex) & 1) != 0;
    }

    private void writeProductForQAFailure(boolean qaFailed) {
        Product failedProduct = new Product(sourceProduct.getName(),
                                            sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());
        if (qaFailed) {
            failedProduct.setProductType(sourceProduct.getProductType() + "_QA_FAILED");
//            ProductUtils.copyMasks(sourceProduct, failedProduct);
//            ProductUtils.copyMetadata(sourceProduct, failedProduct);
            ProductUtils.copyGeoCoding(sourceProduct, failedProduct);
            ProductUtils.copyBand(radianceOutputBandName, sourceProduct, failedProduct, true);
//            ProductUtils.copySpectralBandProperties(sourceProduct.getBand(radianceOutputBandName),
//                                                    failedProduct.getBand(radianceOutputBandName));
            ProductUtils.copyBand(l1FlagBandName, sourceProduct, failedProduct, true);
            ProductUtils.copyFlagCoding(sourceProduct.getBand(l1FlagBandName).getFlagCoding(), failedProduct);
        }
        setTargetProduct(failedProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(QaOp.class);
        }
    }
}
