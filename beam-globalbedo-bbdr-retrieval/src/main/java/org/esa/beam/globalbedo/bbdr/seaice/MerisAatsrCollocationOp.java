package org.esa.beam.globalbedo.bbdr.seaice;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.gpf.operators.standard.WriteOp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * Collocates MERIS L1b products with a fitting AATSR L1b products
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.seaice.merisaatsr.colloc",
                  description = "Collocates MERIS L1b products with a fitting AATSR L1b products.",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class MerisAatsrCollocationOp extends Operator {
    @Parameter(defaultValue = "", description = "MERIS input data directory")
    private File merisInputDataDir;

    @Parameter(defaultValue = "", description = "AATSR input data directory")
    private File aatsrInputDataDir;

    @Parameter(defaultValue = "", description = "Collocation output data directory")
    private File collocOutputDataDir;

    private Product[] aatsrSourceProducts;

    @Override
    public void initialize() throws OperatorException {

        Product[] merisSourceProducts = getInputProducts(merisInputDataDir, "MER_RR__1P");
        aatsrSourceProducts = getInputProducts(aatsrInputDataDir, "ATS_TOA_1P");

        Arrays.sort(merisSourceProducts, new ProductNameComparator());
        Arrays.sort(aatsrSourceProducts, new ProductNameComparator());

        for (Product merisSourceProduct : merisSourceProducts) {
            Product aatsrSourceProduct = findAatsrProductToCollocate(merisSourceProduct);

            if (aatsrSourceProduct != null) {
                System.out.println("Collocating '" + merisSourceProduct.getName() + "', '" + aatsrSourceProduct.getName() + "'...");
                Map<String, Product> collocateInput = new HashMap<String, Product>(2);
                collocateInput.put("masterProduct", merisSourceProduct);
                collocateInput.put("slaveProduct", aatsrSourceProduct);
                Product collocateProduct =
                        GPF.createProduct(OperatorSpi.getOperatorAlias(CollocateOp.class), GPF.NO_PARAMS, collocateInput);

                // name should be MER_ATS_COLLOC_yyyyMMdd_hhmmss.dim
                final String collocTargetFileName = "MER_ATS_COLLOC_" + merisSourceProduct.getName().substring(14, 29) + ".dim";
                final String collocTargetFilePath = collocOutputDataDir + File.separator + collocTargetFileName;
                final File collocTargetFile = new File(collocTargetFilePath);
                final WriteOp collocWriteOp = new WriteOp(collocateProduct, collocTargetFile, ProductIO.DEFAULT_FORMAT_NAME);
                System.out.println("Writing collocated product '" + collocTargetFileName + "'...");
                collocWriteOp.writeProduct(ProgressMonitor.NULL);
            }
        }
    }

    private Product findAatsrProductToCollocate(Product merisSourceProduct) {
        final ProductData.UTC merisStartTime = merisSourceProduct.getStartTime();
        final ProductData.UTC merisEndTime = merisSourceProduct.getEndTime();
        final long merisStartTimeSecs = merisStartTime.getAsCalendar().getTimeInMillis() / 1000;
        final long merisendTimeSecs = merisEndTime.getAsCalendar().getTimeInMillis() / 1000;

        // a corresponding AATSR product is selected if
        // start time difference not more than 60sec
        // end time difference not more than 180sec

        for (Product aatsrSourceProduct : aatsrSourceProducts) {
            final ProductData.UTC aatsrStartTime = aatsrSourceProduct.getStartTime();
            final ProductData.UTC aatsrEndTime = aatsrSourceProduct.getEndTime();
            final long aatsrStartTimeSecs = aatsrStartTime.getAsCalendar().getTimeInMillis() / 1000;
            final long aatsrendTimeSecs = aatsrEndTime.getAsCalendar().getTimeInMillis() / 1000;
            if (Math.abs(merisStartTimeSecs - aatsrStartTimeSecs) < 60 &&
                    Math.abs(merisendTimeSecs - aatsrendTimeSecs) < 180) {
                return aatsrSourceProduct;
            }
        }

        return null;
    }

    private Product[] getInputProducts(File inputDataDir, final String productType) {
        final FileFilter productsFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() &&
                        file.getName().startsWith(productType) && file.getName().endsWith(".N1");
            }
        };

        final File[] sourceProductFiles = (new File(inputDataDir.getAbsolutePath())).listFiles(productsFilter);
        List<Product> sourceProductsList = new ArrayList<Product>();

        int productIndex = 0;
        if (sourceProductFiles != null && sourceProductFiles.length > 0) {
            for (File sourceProductFile : sourceProductFiles) {
                try {
                    final Product product = ProductIO.readProduct(sourceProductFile.getAbsolutePath());
                    if (product != null && product.getProductType().equals(productType) &&
                            product.getStartTime() != null && product.getEndTime() != null) {
                        sourceProductsList.add(product);
                        productIndex++;
                    }
                } catch (IOException e) {
                    System.err.println("WARNING: Source file '" +
                                               sourceProductFile.getName() + "' could not be read - skipping.");
                }
            }
        }
        if (productIndex == 0) {
            System.out.println("No source products found in directory " + merisInputDataDir + " - nothing to do.");
        }

        return sourceProductsList.toArray(new Product[sourceProductsList.size()]);
    }

    private class ProductNameComparator implements Comparator<Product> {
        @Override
        public int compare(Product o1, Product o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisAatsrCollocationOp.class);
        }
    }
}
