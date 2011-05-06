package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
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

    private double[][][] sumMatrices;
    private double[][] mask;
    private double[] weight;

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

        bandNames = sourceProduct0.getBandNames();

        weight = new double[sourceFilenames.length];
        for (int j = 0; j < sourceFilenames.length; j++) {
            weight[j] = Math.exp(-1.0 * Math.abs(allDoys[j]) / HALFLIFE);
//            System.out.println("i, j, weight = " + j + ", " + weight[j]);
        }

        int[] bandDataTypes = new int[bandNames.length];
        for (int i = 0; i < bandDataTypes.length; i++) {
            bandDataTypes[i] = sourceProduct0.getBand(bandNames[i]).getDataType();
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

        sumMatrices = new double[bandNames.length][rasterWidth][rasterHeight];
        mask = new double[rasterWidth][rasterHeight];

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
        // we need the computeTile only for the 'daysToTheClosestSample' band. All other bands are done imagewise
        // using JAI in initialize().
        if (targetBand.getName().equals(AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME)) {
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    targetTile.setSample(x, y, daysToTheClosestSample[x][y]);
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
            while ((nRead = ch.read(bb)) != -1) {
                if (nRead == 0) {
                    continue;
                }
                bb.position(0);
                bb.limit(nRead);
                while (bb.hasRemaining()) {
                    nGet = Math.min(bb.remaining(), size);
                    // todo: find the right indices for sumMatrices array. Depends on tile size when product was written!
                    sumMatrices[0][0][0] += weight[fileIndex] * bb.getDouble();
                    // todo: last band is the mask. extract array mask[i][j] for determination of doyOfClosestSample
                    mask[0][0] = 1.0;
                    index++;
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

}