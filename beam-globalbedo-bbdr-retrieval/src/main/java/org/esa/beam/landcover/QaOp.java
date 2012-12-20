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

    private static final int START_ROW_OFFSET = 10;
    private static final int END_ROW_OFFSET = 10;

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
        int y = START_ROW_OFFSET;
        int badDataRows = 0;
        while (!qaFailed && y < height-END_ROW_OFFSET) {
            int[] pixels = new int[width];
            pixels = sourceRaster.getPixels(0, y, width, 1, pixels);
            int x = 0;
            int badCount = 0;
            while (badCount * 100.0 / (width - 1) < percentBadDataValuesThreshold && x < width - 1) {
                final boolean isBadPixel = ((pixels[x] >> invalidMaskBitIndex) & 1) != 0;
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

    private void writeProductForQAFailure(boolean qaFailed) {
        Product failedProduct = new Product(sourceProduct.getName(),
                                            sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());
        if (qaFailed) {
            System.out.println(sourceProduct.getName());
            ProductUtils.copyBand(radianceOutputBandName, sourceProduct, failedProduct, false);   // todo: discuss band(s) to write
            ProductUtils.copyBand(l1FlagBandName, sourceProduct, failedProduct, false);
        }
        setTargetProduct(failedProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(QaOp.class);
        }
    }
}
