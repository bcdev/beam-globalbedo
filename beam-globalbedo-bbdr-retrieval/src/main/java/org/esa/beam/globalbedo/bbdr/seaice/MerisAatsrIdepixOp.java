package org.esa.beam.globalbedo.bbdr.seaice;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.idepix.algorithms.globalbedo.GlobAlbedoOp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * Collocates MERIS L1b products with a fitting AATSR L1b products
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.seaice.merisaatsr.idepix",
                  description = "Applies Idepix to MERIS/AATSR collocated products.",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class MerisAatsrIdepixOp extends Operator {

    @Parameter(defaultValue = "", description = "Collocation output data directory")
    private File collocInputDataDir;

    @Parameter(defaultValue = "", description = "Idepix output data directory")
    private File idepixOutputDataDir;

    @Override
    public void initialize() throws OperatorException {

        Product[] collocationSourceProducts = getInputProducts(collocInputDataDir);

        Arrays.sort(collocationSourceProducts, new ProductNameComparator());

        for (Product collocSourceProduct : collocationSourceProducts) {
            System.out.println("Computing Idepix for product '" + collocSourceProduct.getName() + "'...");
            Map<String, Object> pixelClassParam = new HashMap<String, Object>(4);
            pixelClassParam.put("gaComputeFlagsOnly", true);
            pixelClassParam.put("gaCloudBufferWidth", 3);
            pixelClassParam.put("gaCopyRadiances", false);
            pixelClassParam.put("gaCopySubsetOfRadiances", true);
            Product idepixProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(GlobAlbedoOp.class), pixelClassParam, collocSourceProduct);
            idepixProduct.setProductType("MER_ATS_IDEPIX_");

            final String idepixTargetFileName = "IDEPIX_" + collocSourceProduct.getName();
            final String idepixTargetFilePath = idepixOutputDataDir + File.separator + idepixTargetFileName;
            final File idepixTargetFile = new File(idepixTargetFilePath);
            final WriteOp idepixWriteOp = new WriteOp(idepixProduct, idepixTargetFile, ProductIO.DEFAULT_FORMAT_NAME);
            System.out.println("Writing Idepix product '" + idepixTargetFileName + "'...");
            idepixWriteOp.writeProduct(ProgressMonitor.NULL);
        }
        setTargetProduct(new Product("dummy", "dummy", 0, 0));

    }

    private Product[] getInputProducts(File inputDataDir) {
        final FileFilter productsFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                // e.g. COLLOC_20070621_MER_134157_ATS_134155.dim
                return file.isFile() &&
                        file.getName().contains("COLLOC_") && file.getName().endsWith(".dim");
            }
        };

        final File[] sourceProductFiles = (new File(inputDataDir.getAbsolutePath())).listFiles(productsFilter);
        List<Product> sourceProductsList = new ArrayList<Product>();

        int productIndex = 0;
        if (sourceProductFiles != null && sourceProductFiles.length > 0) {
            for (File sourceProductFile : sourceProductFiles) {
                try {
                    final Product product = ProductIO.readProduct(sourceProductFile.getAbsolutePath());
                    if (product != null && product.getStartTime() != null && product.getEndTime() != null) {
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
            System.out.println("No source products found in directory " + collocInputDataDir + " - nothing to do.");
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
            super(MerisAatsrIdepixOp.class);
        }
    }
}
