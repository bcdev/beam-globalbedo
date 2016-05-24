package org.esa.beam.globalbedo.inversion.spectral;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.globalbedo.inversion.Accumulator;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.util.logging.Level;

/**
 * Pixel operator implementing the daily accumulation for spectral inversion.
 *
 * @author Olaf Danne
 */
@SuppressWarnings({"MismatchedReadAndWriteOfArray", "StatementWithEmptyBody"})
@OperatorMetadata(alias = "ga.inversion.dailyaccbinary.spectral",
        description = "Provides spectral daily accumulation of single SDR observations",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class SpectralDailyAccumulationOp extends Operator {


    @SourceProducts(description = "SDR source products")
    private Product[] sourceProducts;


    @Parameter(defaultValue = "7", description = "Number of spectral bands (7 for standard MODIS spectral mapping")
    private int numSdrBands;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Debug - run additional parts of code if needed.")
    private boolean debug;

    @Parameter(description = "Daily accumulator binary file")
    private File dailyAccumulatorBinaryFile;

    private static final int sourceSampleOffset = 100;  // this value must be >= number of bands in a source product

    private float[][][] resultArray;


    private Tile[][] sdrTiles;
    private Tile[][] sigmaSdrTiles;

    private Tile[] kvolVisTile;
    private Tile[] kvolNirTile;
    private Tile[] kvolSwTile;
    private Tile[] kgeoVisTile;
    private Tile[] kgeoNirTile;
    private Tile[] kgeoSwTile;
    private Tile[] snowMaskTile;

    private int currentSourceProductIndex;
    private int numSigmaSdrBands;

    private String[] sdrBandNames;
    private String[] sigmaSdrBandNames;
    private int[] sigmaSdrDiagonalIndices;
    private int[] sigmaSdrURIndices;


    @Override
    public void initialize() throws OperatorException {

        final Rectangle sourceRect = new Rectangle(0, 0,
                                                   AlbedoInversionConstants.MODIS_TILE_WIDTH,
                                                   AlbedoInversionConstants.MODIS_TILE_HEIGHT);

        sdrTiles = new Tile[sourceProducts.length][numSdrBands];
        // (7*7 - 7)/2  + 7 = 28 sigma bands default (UR matrix)
        numSigmaSdrBands = (numSdrBands * numSdrBands - numSdrBands)/2 + numSdrBands;
        sigmaSdrTiles = new Tile[sourceProducts.length][numSigmaSdrBands];

        sdrBandNames = SpectralInversionUtils.getSdrBandNames(numSdrBands);
        sigmaSdrBandNames = SpectralInversionUtils.getSigmaSdrBandNames(numSdrBands, numSigmaSdrBands);
        sigmaSdrDiagonalIndices = SpectralInversionUtils.getSigmaSdrDiagonalIndices(numSdrBands);
        sigmaSdrURIndices = SpectralInversionUtils.getSigmaSdrURIndices(numSdrBands, numSigmaSdrBands);

        kvolVisTile = new Tile[sourceProducts.length];
        kvolNirTile = new Tile[sourceProducts.length];
        kvolSwTile = new Tile[sourceProducts.length];
        kgeoVisTile = new Tile[sourceProducts.length];
        kgeoNirTile = new Tile[sourceProducts.length];
        kgeoSwTile = new Tile[sourceProducts.length];
        snowMaskTile = new Tile[sourceProducts.length];

        resultArray = new float[AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS]
                [AlbedoInversionConstants.MODIS_TILE_WIDTH]
                [AlbedoInversionConstants.MODIS_TILE_HEIGHT];



        for (int k = 0; k < sourceProducts.length; k++) {
            int sdrIndex = 0;
            for (int j = 0; j < numSdrBands; j++) {
                sdrTiles[k][j] = getSourceTile(sourceProducts[k].getBand(sdrBandNames[sdrIndex++]), sourceRect);
            }
            int sigmaSdrIndex = 0;
            for (int j = 0; j < numSigmaSdrBands; j++) {
                sigmaSdrTiles[k][j] = getSourceTile(sourceProducts[k].getBand(sigmaSdrBandNames[sigmaSdrIndex++]), sourceRect);
            }
            kvolVisTile[k] = getSourceTile(sourceProducts[k].getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_VIS_NAME), sourceRect);
            kvolNirTile[k] = getSourceTile(sourceProducts[k].getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_NIR_NAME), sourceRect);
            kvolSwTile[k] = getSourceTile(sourceProducts[k].getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_SW_NAME), sourceRect);
            kgeoVisTile[k] = getSourceTile(sourceProducts[k].getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_VIS_NAME), sourceRect);
            kgeoNirTile[k] = getSourceTile(sourceProducts[k].getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_NIR_NAME), sourceRect);
            kgeoSwTile[k] = getSourceTile(sourceProducts[k].getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_SW_NAME), sourceRect);
            snowMaskTile[k] = getSourceTile(sourceProducts[k].getBand(AlbedoInversionConstants.BBDR_SNOW_MASK_NAME), sourceRect);
        }

        // per pixel, accumulate the matrices from the single products...
        // Since we loop over all source products, we need a sequential and thread-safe approach, so we do not
        // implement computeTile.
        for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
            for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                accumulate(x, y);
            }
        }

        // accumulation done for all pixels, now write result to binary file...
        BeamLogManager.getSystemLogger().log(Level.INFO, "all pixels processed.");
        BeamLogManager.getSystemLogger().log(Level.INFO, "...writing accumulator result array...");
        IOUtils.writeFloatArrayToFile(dailyAccumulatorBinaryFile, resultArray);
        BeamLogManager.getSystemLogger().log(Level.INFO, "accumulator result array written.");

        setTargetProduct(new Product("n", "d", 1, 1));
    }

    private void accumulate(int x, int y) {
        Matrix M = new Matrix(3 * numSdrBands,
                              3 * numSdrBands);
        Matrix V = new Matrix(3 * numSdrBands, 1);
        Matrix E = new Matrix(1, 1);
        double mask = 0.0;

        for (int k = 0; k < sourceProducts.length; k++) {
            currentSourceProductIndex = k;
            final Accumulator accumulator = getMatricesPerBBDRDataset(x, y);
            M.plusEquals(accumulator.getM());
            V.plusEquals(accumulator.getV());
            E.plusEquals(accumulator.getE());
            mask += accumulator.getMask();
        }

        Accumulator finalAccumulator = new Accumulator(M, V, E, mask);
        // write pixel result into array for binary file...
        fillBinaryResultArray(finalAccumulator, x, y);
    }

    private Accumulator getMatricesPerBBDRDataset(int x, int y) {
        if (isSnowFilter(x, y) || isBBDRFilter(x, y) || isSDFilter(x, y)) {
            // do not consider non-land pixels (BBDR = NaN), non-snowfilter pixels,
            // pixels with BBDR == 0.0 or -9999.0, SD == 0.0
            return getZeroAccumulator();
        }

        // get kernels...
        Matrix kernels = getKernels(x, y);

        // compute C matrix...
        final double[] correlation = getCorrelation(x, y);
        final double[] SD = getSD(x, y);
        Matrix C = new Matrix(
                numSdrBands * numSdrBands, 1);
        Matrix thisC = new Matrix(numSdrBands,
                                  numSdrBands);

        int count = 0;
        int cCount = 0;
        for (int j = 0; j < numSdrBands; j++) {
            for (int k = j + 1; k < numSdrBands; k++) {
                if (k == j + 1) {
                    cCount++;
                }
                C.set(cCount, 0, correlation[count] * SD[j] * SD[k]);
                count++;
                cCount++;
            }
        }

        cCount = 0;
        for (int j = 0; j < numSdrBands; j++) {
            C.set(cCount, 0, SD[j] * SD[j]);
            cCount = cCount + numSdrBands - j;
        }

        count = 0;
        for (int j = 0; j < numSdrBands; j++) {
            for (int k = j; k < numSdrBands; k++) {
                thisC.set(j, k, C.get(count, 0));
                thisC.set(k, j, thisC.get(j, k));
                count++;
            }
        }

        // compute M, V, E matrices...
        final Matrix bbdr = getBBDR(x, y);
        final Matrix inverseC = thisC.inverse();
        final Matrix M = (kernels.transpose().times(inverseC)).times(kernels);
        final Matrix inverseCDiagFlat = AlbedoInversionUtils.getRectangularDiagonalMatrix(inverseC);
        final Matrix kernelTimesInvCDiag = kernels.transpose().times(inverseCDiagFlat);
        final Matrix V = kernelTimesInvCDiag.times(bbdr);
        final Matrix E = (bbdr.transpose().times(inverseC)).times(bbdr);

        // return result
        return new Accumulator(M, V, E, 1);
    }

    private Accumulator getZeroAccumulator() {
        final Matrix zeroM = new Matrix(3 * numSdrBands,
                                        3 * numSdrBands);
        final Matrix zeroV = new Matrix(3 * numSdrBands, 1);
        final Matrix zeroE = new Matrix(1, 1);

        return new Accumulator(zeroM, zeroV, zeroE, 0);
    }

    private Matrix getBBDR(int x, int y) {
        Matrix bbdr = new Matrix(numSdrBands, 1);
        for (int j = 0; j < numSdrBands; j++) {
            bbdr.set(0, 0, sdrTiles[currentSourceProductIndex][j].getSampleDouble(x, y));
        }
        return bbdr;
    }

    private double[] getSD(int x, int y) {
        double[] SD = new double[numSdrBands];
        // sigma_00 xx xx xx xx xx xx
        // sigma_   01 xx xx xx xx xx
        // sigma_      02 xx xx xx xx
        // sigma_         03 xx xx xx
        // sigma_            04 xx xx
        // sigma_               05 xx
        // sigma_                  06
        for (int j = 0; j < numSdrBands; j++) {
            SD[j] = sigmaSdrTiles[currentSourceProductIndex][sigmaSdrDiagonalIndices[j]].getSampleDouble(x, y);
        }
        return SD;
    }

    private double[] getCorrelation(int x, int y) {
        double[] correlation = new double[numSigmaSdrBands - numSdrBands];
        // sigma_xx 01 02 03 04 05 06
        // sigma_   xx 02 03 04 05 06
        // sigma_      xx 03 04 05 06
        // sigma_         xx 04 05 06
        // sigma_            xx 05 06
        // sigma_               xx 06
        // sigma_                  xx
        for (int j = 0; j < sigmaSdrURIndices.length; j++) {
            correlation[j] = sigmaSdrTiles[currentSourceProductIndex][sigmaSdrURIndices[j]].getSampleDouble(x, y);
        }
        return correlation;
    }

    private void fillBinaryResultArray(Accumulator accumulator, int x, int y) {
        int offset = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = 0; j < 3 * numSdrBands; j++) {
                resultArray[offset++][x][y] = (float) accumulator.getM().get(i, j);
            }
        }
        for (int i = 0; i < 3 * numSdrBands; i++) {
            resultArray[offset++][x][y] = (float) accumulator.getV().get(i, 0);
        }
        resultArray[offset++][x][y] = (float) accumulator.getE().get(0, 0);
        resultArray[offset][x][y] = (float) accumulator.getMask();
    }


    private Matrix getKernels(int x, int y) {
        Matrix kernels = new Matrix(numSdrBands,
                                    3 * numSdrBands);

        kernels.set(0, 0, 1.0);
        kernels.set(1, 3, 1.0);
        kernels.set(2, 6, 1.0);
        kernels.set(0, 1, kvolVisTile[currentSourceProductIndex].getSampleDouble(x, y));
        kernels.set(1, 4, kvolNirTile[currentSourceProductIndex].getSampleDouble(x, y));
        kernels.set(2, 7, kvolSwTile[currentSourceProductIndex].getSampleDouble(x, y));
        kernels.set(0, 2, kgeoVisTile[currentSourceProductIndex].getSampleDouble(x, y));
        kernels.set(1, 5, kgeoNirTile[currentSourceProductIndex].getSampleDouble(x, y));
        kernels.set(2, 8, kgeoSwTile[currentSourceProductIndex].getSampleDouble(x, y));

        return kernels;
    }

    private boolean isSnowFilter(int x, int y) {
        return ((computeSnow && !(snowMaskTile[currentSourceProductIndex].getSampleInt(x, y) == 1)) ||
                (!computeSnow && (snowMaskTile[currentSourceProductIndex].getSampleInt(x, y) == 1)));
    }

    private boolean isBBDRFilter(int x, int y) {
        for (int j = 0; j < numSdrBands; j++) {
            final double sdr = sdrTiles[currentSourceProductIndex][j].getSampleDouble(x, y);
            if (sdr == 0.0 || !AlbedoInversionUtils.isValid(sdr) || sdr == 9999.0) {
                return true;
            }
        }
        return false;
    }

    private boolean isSDFilter(int x, int y) {
        for (int j = 0; j < numSdrBands; j++) {
            final double sigmaSdr = sigmaSdrTiles[currentSourceProductIndex][sigmaSdrDiagonalIndices[j]].getSampleDouble(x, y);
            if (sigmaSdr == 0.0 || !AlbedoInversionUtils.isValid(sigmaSdr) || sigmaSdr == 9999.0) {
                return true;
            }
        }
        return false;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SpectralDailyAccumulationOp.class);
        }
    }

}
