package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Operator for the daily accumulation part in Albedo Inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.dailyacc")
public class GlobalbedoLevel3Accumulation extends Operator {

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

    @Override
    public void initialize() throws OperatorException {

        //        JAI.getDefaultInstance().getTileScheduler().setParallelism(4);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get BBDR input product list...
        Product[] inputProducts;
        try {
            inputProducts = getInputProducts();
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        // STEP 2: accumulate all optimal estimations M, V, E as obtained from single observation files:
        // M_tot(DoY) = sum(M(obs[i]))
        // V_tot(DoY) = sum(V(obs[i]))
        // E_tot(DoY) = sum(E(obs[i]))
        OptimalEstimationOp optimalEstimationOp = new OptimalEstimationOp();
        optimalEstimationOp.setSourceProducts(inputProducts);
//        Product[] testProducts = new Product[]{inputProducts[0]};
//        optimalEstimationOp.setSourceProducts(testProducts);
        optimalEstimationOp.setParameter("computeSnow", computeSnow);
        Product accumulationProduct = optimalEstimationOp.getTargetProduct();

        setTargetProduct(accumulationProduct);
    }

    private Product[] getInputProducts() throws IOException {
        String daystring = AlbedoInversionUtils.getDateFromDoy(year, doy);

        String merisBbdrDir = bbdrRootDir + File.separator + "MERIS" + File.separator + year + File.separator + tile;
        String[] merisBbdrFiles = (new File(merisBbdrDir)).list();
        List<String> merisBbdrFileList = AlbedoInversionUtils.getDailyBBDRFilenames(merisBbdrFiles, daystring);

        String aatsrBbdrDir = bbdrRootDir + File.separator + "AATSR" + File.separator + year + File.separator + tile;
        String[] aatsrBbdrFiles = (new File(aatsrBbdrDir)).list();
        List<String> aatsrBbdrFileList = AlbedoInversionUtils.getDailyBBDRFilenames(aatsrBbdrFiles, daystring);

        String vgtBbdrDir = bbdrRootDir + File.separator + "VGT" + File.separator + year + File.separator + tile;
        String[] vgtBbdrFiles = (new File(vgtBbdrDir)).list();
        List<String> vgtBbdrFileList = AlbedoInversionUtils.getDailyBBDRFilenames(vgtBbdrFiles, daystring);

        final int numberOfInputProducts = merisBbdrFileList.size() + aatsrBbdrFileList.size() + vgtBbdrFileList.size();
//        final int numberOfInputProducts = merisBbdrFileList.size() + aatsrBbdrFileList.size();
        Product[] bbdrProducts = new Product[numberOfInputProducts];

        int productIndex = 0;
        for (Iterator<String> i = merisBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = merisBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }
        for (Iterator<String> i = aatsrBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = aatsrBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }
        for (Iterator<String> i = vgtBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = vgtBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts[productIndex] = product;
            productIndex++;
        }

        if (productIndex == 0) {
            throw new OperatorException("No source products found - check contents of BBDR directory!");
        }

        return bbdrProducts;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Accumulation.class);
        }
    }
}
