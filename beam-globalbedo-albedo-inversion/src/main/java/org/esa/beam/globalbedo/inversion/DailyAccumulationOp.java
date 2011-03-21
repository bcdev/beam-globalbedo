package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class DailyAccumulationOp extends Operator {

    @Parameter(defaultValue = "", description="BBDR root directory")
    private String bbdrRootDir;

    @Parameter(defaultValue = "h18v04", description="MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description="Year")
    private int year;

    @Parameter(defaultValue = "001", description="Day of Year", interval="[1,366]")
    private int doy;

    @Override
    public void initialize() throws OperatorException {

        // get BBDR input product list...
        List<Product> sourceProducts = new ArrayList<Product>();
        try {
            sourceProducts = getInputProducts();
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        // PER OBSERVATION DAY:
        // create accumulator files:
        //  M(Doy), V(DoY), E(DoY), mask(DoY)
        // todo: use SingleDayAccumulator (pixelwise operator)
        // parameter: sourceProducts.toArray()

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

        for (Iterator<String> i = merisBbdrFileList.iterator(); i.hasNext();)
        {
            String sourceProductFileName = merisBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts.add(product);
        }
        for (Iterator<String> i = aatsrBbdrFileList.iterator(); i.hasNext();)
        {
            String sourceProductFileName = aatsrBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts.add(product);
        }
        for (Iterator<String> i = vgtBbdrFileList.iterator(); i.hasNext();)
        {
            String sourceProductFileName = vgtBbdrDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            bbdrProducts.add(product);
        }

        return bbdrProducts;
    }
}
