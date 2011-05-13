package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import java.awt.*;
import java.io.*;

/**
 * 'Master' operator for the daily accumulation part in Albedo Inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.dailyacc")
public class GlobalbedoLevel3DailyAccumulation extends Operator {

    @Parameter(defaultValue = "", description = "BBDR root directory")
    private String bbdrRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "001", description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Write also binary accumulator files besides dimap (to be decided)")
    private boolean writeBinaryAccumulators;

    @Override
    public void initialize() throws OperatorException {

        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose  // todo: change back

        // STEP 1: get BBDR input product list...
        Product[] inputProducts;
        try {
            inputProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, tile, year, doy);
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        // STEP 2: accumulate all optimal estimations M, V, E as obtained from single observation files:
        // M_tot(DoY) = sum(M(obs[i]))
        // V_tot(DoY) = sum(V(obs[i]))
        // E_tot(DoY) = sum(E(obs[i]))
        DailyAccumulationOp accumulationOp = new DailyAccumulationOp();
        accumulationOp.setSourceProducts(inputProducts);
        accumulationOp.setParameter("computeSnow", computeSnow);
        final Product accumulationProduct = accumulationOp.getTargetProduct();

        // Create an output stream to the file.
        String dailyAccumulatorDir = bbdrRootDir + File.separator + "AccumulatorFiles"
                + File.separator + year + File.separator + tile;
        if (computeSnow) {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "Snow" + File.separator);
        } else {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
        }

        // todo: make final decision
        if (writeBinaryAccumulators) {
            String dailyAccumulatorBinaryFilename = "matrices_" + year + doy + ".bin";
            final File dailyAccumulatorBinaryFile = new File(dailyAccumulatorDir + dailyAccumulatorBinaryFilename);

            long time1 = System.currentTimeMillis();
            System.out.println("writing binary file...");
            writeDailyAccumulatorBinaryFile(accumulationProduct, dailyAccumulatorBinaryFile);
            long time2 = System.currentTimeMillis();
            System.out.println("time needed for writing binary file: " + (time2 - time1) / 1000.0);
            System.out.println("done!!");
        }

        setTargetProduct(accumulationProduct);
    }

    private void writeDailyAccumulatorBinaryFile(Product dailyAccProduct, File outputFile) {

        // extracts raster data (double arrays) from all bands of dailyAccProduct and writes to binary file
        // todo: try to make it faster...

        FileOutputStream fos = null;
        System.out.println("outputFile = " + outputFile.getAbsolutePath());
        try {
            if (!outputFile.exists()) {
                outputFile.createNewFile();
            }
            fos = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();  // todo
        } catch (IOException e) {
            e.printStackTrace();  // todo
        }

        DataOutputStream dos = new DataOutputStream(fos);

        for (Band band : dailyAccProduct.getBands()) {
            RasterDataNode rasterDataNode = dailyAccProduct.getRasterDataNode(band.getName());
            final int rasterWidth = rasterDataNode.getRasterWidth();
            final int rasterHeight = rasterDataNode.getRasterHeight();
            final int tileSize = 200;   // seems to be a good value
            System.out.println("band: " + band.getName());

            try {
                for (int w = 0; w < rasterWidth / tileSize; w++) {
                    for (int h = 0; h < rasterHeight / tileSize; h++) {
                        final Rectangle rect = new Rectangle(w * tileSize, h * tileSize, tileSize, tileSize);
                        // this is the expensive call... :-((
                        final Tile tileToProcess = getSourceTile(rasterDataNode, rect, (BorderExtender) null);
                        for (int i = rect.x; i < rect.x + rect.width; i++) {
                            for (int j = rect.y; j < rect.y + rect.height; j++) {
                                final double value = tileToProcess.getSampleDouble(i, j);
                                dos.writeDouble(value);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();  // todo
            }
        }

        try {
            fos.close();
            dos.close();
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3DailyAccumulation.class);
        }
    }
}
