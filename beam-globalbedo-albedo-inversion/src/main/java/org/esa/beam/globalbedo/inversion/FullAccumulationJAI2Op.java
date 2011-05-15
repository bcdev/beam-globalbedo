package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Alternative operator implementing the full accumulation part of python breadboard:
 * Reads from daily accumulator binary file since reading so many (540?) dimap products seems too slow...
 * <p/>
 * The breadboard file is 'AlbedoInversion_multisensor_FullAccum_MultiProcessing.py' provided by Gerardo López Saldaña.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.fullaccjai2",
        description = "Provides full accumulation of daily accumulators",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011 by Brockmann Consult")
public class FullAccumulationJAI2Op extends Operator {

    private static final double HALFLIFE = 11.54;

    private int[][] daysToTheClosestSample;

    private int rasterWidth;
    private int rasterHeight;

    @Parameter(description = "Source filenames")
    private String[] sourceFilenames;

    @Parameter(description = "Source binary filenames")
    private String[] sourceBinaryFilenames;

    @Parameter(description = "All DoYs for full accumulation")
    private int[] allDoys;

    private float[][][] sumMatrices;
    private float[][] mask;
    private float[] weight;

    private Map<String, Integer> sourceBandMap = new HashMap();
    private String[] bandNames;


    @Override
    public void initialize() throws OperatorException {

        // todo: if we get the data from the binary files, we do not need the dimap source products any more.
        // --> set band names etc. as constants
        // -- BUT where to get TPG and geocoding from?? maybe from first BBDR file of that day, or sth.
        Product sourceProduct0 = null;
        try {
            sourceProduct0 = ProductIO.readProduct(new File(sourceFilenames[0]));
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }

        bandNames = IOUtils.getDailyAccumulatorBandNames();

        weight = new float[sourceFilenames.length];
        for (int j = 0; j < sourceFilenames.length; j++) {
            weight[j] = (float) Math.exp(-1.0 * Math.abs(allDoys[j]) / HALFLIFE);
        }

        int[] bandDataTypes = new int[bandNames.length];
        for (int i = 0; i < bandDataTypes.length; i++) {
            bandDataTypes[i] = ProductData.TYPE_FLOAT32;
        }
        rasterWidth = sourceProduct0.getSceneRasterWidth();
        rasterHeight = sourceProduct0.getSceneRasterHeight();
        Product targetProduct = new Product(getId(),
                getClass().getName(),
                rasterWidth,
                rasterHeight);
        targetProduct.setStartTime(sourceProduct0.getStartTime());
        targetProduct.setEndTime(sourceProduct0.getEndTime());
        targetProduct.setPreferredTileSize(100, 100);
        ProductUtils.copyTiePointGrids(sourceProduct0, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct0, targetProduct);
        sourceProduct0.dispose();

        int fileIndex = 0;
        daysToTheClosestSample = new int[rasterWidth][rasterHeight];
        int[][] dayOfClosestSampleOld;

        sumMatrices = new float[bandNames.length][rasterWidth][rasterHeight];
        mask = new float[rasterWidth][rasterHeight];

        for (String sourceFileName : sourceBinaryFilenames) {
            System.out.println("sourceFileName = " + sourceFileName);

            getDailyAccFromBinaryFileAndAccumulate(sourceFileName, fileIndex); // accumulates matrices and extracts mask array
            BeamLogManager.getSystemLogger().log(Level.ALL, "Accumulating file " + sourceFileName + " ...");
            dayOfClosestSampleOld = daysToTheClosestSample;
            daysToTheClosestSample = updateDoYOfClosestSampleArray(dayOfClosestSampleOld, fileIndex);

            fileIndex++;
        }

        for (int i = 0; i < bandNames.length; i++) {
            Band targetBand = targetProduct.addBand(bandNames[i], bandDataTypes[i]);
            sourceBandMap.put(bandNames[i], i);
        }

        Band daysToTheClosestSampleBand = targetProduct.addBand(
                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME,
                ProductData.TYPE_INT16);
        daysToTheClosestSampleBand.setNoDataValue(-1);
        daysToTheClosestSampleBand.setNoDataValueUsed(true);

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (targetBand.getName().equals(AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME)) {
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    targetTile.setSample(x, y, daysToTheClosestSample[x][y]);
                }
            }
        } else if (targetBand.getName().equals(AlbedoInversionConstants.ACC_MASK_NAME)) {
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    targetTile.setSample(x, y, mask[x][y]);
                }
            }
        } else {
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    final int bandIndex = sourceBandMap.get(targetBand.getName());
                    targetTile.setSample(x, y, sumMatrices[bandIndex][x][y]);
                }
            }
        }
    }

    private int[][] updateDoYOfClosestSampleArray(int[][] doyOfClosestSampleOld,
                                                  int productIndex) {
        // this is done at the end of 'Accumulator' routine in breadboard...
        int[][] doyOfClosestSample = new int[rasterWidth][rasterHeight];
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                int doy = 0;
                final int bbdrDaysToDoY = Math.abs(allDoys[productIndex]) + 1;
                if (productIndex == 0) {
                    if (mask[i][j] > 0.0) {
                        doy = bbdrDaysToDoY;
                    } else {
                        doy = doyOfClosestSampleOld[i][j];
                    }
                } else {
                    if (mask[i][j] > 0 && doyOfClosestSampleOld[i][j] == 0) {
                        doy = bbdrDaysToDoY;
                    } else {
                        doy = doyOfClosestSampleOld[i][j];
                    }
                    if (mask[i][j] > 0 && doy > 0) {
                        doy = Math.min(bbdrDaysToDoY, doy);
                    } else {
                        doy = doyOfClosestSampleOld[i][j];
                    }
                }
                doyOfClosestSample[i][j] = doy;
            }
        }
        return doyOfClosestSample;
    }

    public void getDailyAccFromBinaryFileAndAccumulate(String filename, int fileIndex) {
        int size = bandNames.length * rasterWidth * rasterHeight;
        final File dailyAccumulatorBinaryFile = new File(filename);
        FileInputStream f = null;
        try {
            f = new FileInputStream(dailyAccumulatorBinaryFile);
        } catch (FileNotFoundException e) {
            // todo
            e.printStackTrace();
        }
        FileChannel ch = f.getChannel();
        ByteBuffer bb = ByteBuffer.allocateDirect(size);

        int nRead, nGet;
        try {
            int index = 0;
            int ii = 0;
            int jj = 0;
            int kk = 0;
            while ((nRead = ch.read(bb)) != -1) {
                if (nRead == 0) {
                    continue;
                }
                bb.position(0);
                bb.limit(nRead);
                while (bb.hasRemaining()) {
                    nGet = Math.min(bb.remaining(), size);
                    // last band is the mask. extract array mask[jj][kk] for determination of doyOfClosestSample...
//                    if (ii == bandNames.length - 1) {
//                        mask[jj][kk] += weight[fileIndex] * bb.getFloat();
//                    } else {
//                        sumMatrices[ii][jj][kk] += weight[fileIndex] * bb.getFloat();
//                    }
                    sumMatrices[ii][jj][kk] += weight[fileIndex] * bb.getFloat();
                    mask[jj][kk] = sumMatrices[bandNames.length - 1][jj][kk];
                    // find the right indices for sumMatrices array...
                    kk++;
                    if (kk == rasterHeight) {
                        jj++;
                        kk = 0;
                        if (jj == rasterWidth) {
                            ii++;
                            jj = 0;
                        }
                    }

                }
                bb.clear();
            }
            ch.close();
            f.close();
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FullAccumulationJAI2Op.class);
        }
    }

}