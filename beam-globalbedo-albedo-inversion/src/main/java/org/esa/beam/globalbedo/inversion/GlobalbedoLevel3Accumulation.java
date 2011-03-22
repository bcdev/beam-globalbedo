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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
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
        List<Product> sourceProducts;
        try {
            sourceProducts = getInputProducts();
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        // STEP 2: optimal estimation, PER OBSERVATION FILE:
        Product[] singleOptimalEstimationProducts = new Product[sourceProducts.size()];
        // create matrices :
        //  M(obsDoy,i), V(obsDoy,i), E(obsDoy,i), mask(obsDoy,i)
        // todo: use OptimalEstimationOp (pixelwise operator)
        // parameter: sourceProducts[i]
        // result: product with matrices from a single observation file
        // --> this corresponds to breadboard method 'GetBBDR'

        // STEP 3: accumulate all optimal estimations M, V, E as obtained above:
        Product accumulationProduct = null;
        // M_tot += M(obsDoy[i])
        // V_tot += V(obsDoy[i])
        // E_tot += E(obsDoy[i])
        // parameter: singleOptimalEstimationProducts
        // todo: use OptimalEstimationAccumulator (pixelwise operator)
        // result: accumulation product with M_tot(i,j,x,y), V_tot(i,x,y), E_tot(x,y), mask(x,y)
        // --> this is done in breadboard method 'CreateAccumulatorFiles'

        setTargetProduct(accumulationProduct);

    }

    private List<Product> getInputProducts() throws IOException {
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

        List<Product> bbdrProducts = new ArrayList<Product>();

        for (Iterator<String> i = merisBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = merisBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts.add(product);
        }
        for (Iterator<String> i = aatsrBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = aatsrBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts.add(product);
        }
        for (Iterator<String> i = vgtBbdrFileList.iterator(); i.hasNext();) {
            String sourceProductFileName = vgtBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts.add(product);
        }

        if (bbdrProducts.size() == 0) {
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
