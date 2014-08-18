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
 * Class providing a QA for MERIS L1b products: quality check fails if
 * - more than either the first or last 100 rows of the product are invalid, where 'invalid' means that more
 *   than a certain percentage (user parameter) of pixels in a row is flagged as INVALID
 * - the product contains more than a certain number (user parameter) of invalid rows elsewhere (not at top or bottom)
 *
 * The target product will be an 'empty' product (no bands) if the quality check passed. If the quality check failed,
 * the L1 flag band and one of the radiance bands (user parameter) will be written to the target product. In this case,
 * the extension '_QA_FAILED' is added to the product type for a later identification.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "lc.l2.qa")
public class LcQaOp extends Operator {

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

        // check first rows of product
        int yStart = 0;
        boolean firstGoodRowFound = false;
        while (!firstGoodRowFound && yStart < height-1) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, yStart, width, 1, pixels);
            int x = 0;
            while (!firstGoodRowFound && x < width) {
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

        // check last rows of product
        int yEnd = height-1;
        boolean lastGoodRowFound = false;
        while (!lastGoodRowFound && yEnd > yStart) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, yEnd, width, 1, pixels);
            int x = 0;
            while (!lastGoodRowFound && x < width) {
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

        // valid start and end row found in first and last 100 rows - now check all rows in between
        int y = yStart;
        int badDataRows = 0;
        while (!qaFailed && y < yEnd) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, y, width, 1, pixels);
            int x = 0;
            int badCount = 0;
            while (badCount * 100.0 / width < percentBadDataValuesThreshold && x < width) {
                final boolean isBadPixel = isInvalidMaskBitSet(pixels[x]);
                if (isBadPixel) {
                    badCount++;
                }
                x++;
            }
            final double percentBadDataValues = badCount * 100.0 / width;
            if (percentBadDataValues >= percentBadDataValuesThreshold) {
                // row is identified as 'bad'
                badDataRows++;
                if (badDataRows >= badDataRowsThreshold) {
                    // limit of allowed 'bad' rows is exceeded - QA failed
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
        Product qaResultProduct;
        if (qaFailed) {
            // in this case, modify product type, copy geocoding, L1 flags, and one radiance band
            System.out.println("QA FAILED - writing target product with flag band and one radiance band...");
            qaResultProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());
            qaResultProduct.setProductType(sourceProduct.getProductType() + "_QA_FAILED");
            ProductUtils.copyGeoCoding(sourceProduct, qaResultProduct);
            ProductUtils.copyBand(radianceOutputBandName, sourceProduct, qaResultProduct, true);
            if (!qaResultProduct.containsBand(l1FlagBandName)) {
                ProductUtils.copyBand(l1FlagBandName, sourceProduct, qaResultProduct, true);
            }
            ProductUtils.copyFlagCoding(sourceProduct.getBand(l1FlagBandName).getFlagCoding(), qaResultProduct);
        } else {
            System.out.println("QA PASSED - writing empty target product...");
            qaResultProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        0, 0);
        }
        setTargetProduct(qaResultProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(LcQaOp.class);
        }
    }
}

