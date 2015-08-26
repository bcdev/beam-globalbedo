package org.esa.beam.globalbedo.inversion.util;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.inversion.FullAccumulator;

import java.awt.*;

@OperatorMetadata(alias = "ga.inversion.fullacctodimap",
                  description = "Utility operator to write a full or daily accumulator into a Dimap product for debugging.",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class AccToDimapOp extends Operator {

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "Day of year")
    private int doy;

    @Parameter(description = "Full accumulator filename (full path)")
    private String fullAccumulatorFilePath;

    @Parameter(description = "raster width", defaultValue="1200")
    private int rasterWidth;

    @Parameter(description = "raster height", defaultValue="1200")
    private int rasterHeight;

    @Parameter(description = "set to true for full accumulator", defaultValue="true")
    private boolean isFullAcc;

    private FullAccumulator fullAcc;

    @Override
    public void initialize() throws OperatorException {
        System.out.println("Reading accumulator file: " + fullAccumulatorFilePath);
        int numBands =  IOUtils.getDailyAccumulatorBandNames().length;
        if (isFullAcc) {
            numBands++;
        }
        fullAcc = IOUtils.getAccumulatorFromBinaryFile
                (year, doy, fullAccumulatorFilePath,
                 numBands,
                 rasterWidth, rasterHeight, isFullAcc);

        System.out.println("Done reading full accumulator file.");

        final float[][][] sumMatrices = fullAcc.getSumMatrices();

        final int dim1 = sumMatrices.length;
        final int dim2 = sumMatrices[0].length;
        final int dim3 = sumMatrices[0][0].length;

        Product targetProduct = new Product("fullAcc", "fullAcc", dim2, dim3);
        targetProduct.setPreferredTileSize(50, 50);

        // write all accumulator matrix elements in Dimap product
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                targetProduct.addBand("M_" + i + j, ProductData.TYPE_FLOAT32);
            }
        }
        for (int j = 0; j < 9; j++) {
            targetProduct.addBand("V_" + j, ProductData.TYPE_FLOAT32);
        }
        targetProduct.addBand("E", ProductData.TYPE_FLOAT32);

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle rectangle = targetTile.getRectangle();

        int sumMatrixIndex;
        if (targetBand.getName().startsWith("M")) {
            final int i = Integer.parseInt(targetBand.getName().substring(2, 3));
            final int j = Integer.parseInt(targetBand.getName().substring(3, 4));
            sumMatrixIndex = 9 * i + j;
        } else if (targetBand.getName().startsWith("V")) {
            final int j = Integer.parseInt(targetBand.getName().substring(2, 3));
            sumMatrixIndex = 9 * j;
        } else {
            sumMatrixIndex = 90;
        }

        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                final float value = fullAcc.getSumMatrices()[sumMatrixIndex][x][y];
                targetTile.setSample(x, y, value);
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AccToDimapOp.class);
        }
    }


}
