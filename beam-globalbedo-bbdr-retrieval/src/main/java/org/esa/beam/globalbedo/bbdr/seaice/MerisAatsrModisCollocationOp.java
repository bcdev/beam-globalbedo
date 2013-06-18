package org.esa.beam.globalbedo.bbdr.seaice;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.io.IOUtils;
import com.vividsolutions.jts.geom.Geometry;
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
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.globalbedo.bbdr.TileExtractor;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * Collocates a MERIS/AATSR L1b product with fitting AATSR/MERIS L1b products
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.seaice.merisaatsrmodis.colloc",
                  description = "Collocates a MERIS/AATSR collocation product with fitting MOD29 products.",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class MerisAatsrModisCollocationOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "", description = "Slave sensor input data directory")
    private File slaveInputDataDir;

    @Parameter(defaultValue = "", description = "Collocation output data directory")
    private File collocOutputDataDir;

    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME, valueSet = {"BEAM-DIMAP", "NetCDF4-BEAM"})
    private String formatName;

    @Parameter(defaultValue = "false")
    private boolean coregAvailable;

    @Override
    public void initialize() throws OperatorException {

        final String slaveProductPrefix = "MOD29";

        Product[] slaveSourceProducts = getInputProducts(sourceProduct, slaveInputDataDir, slaveProductPrefix);
        Arrays.sort(slaveSourceProducts, new ProductNameComparator());

        Product[] slaveSourceProductsToCollocate = findSlaveProductsToCollocate(sourceProduct, slaveSourceProducts);

        for (Product slaveSourceProduct : slaveSourceProductsToCollocate) {
            if (slaveSourceProduct != null) {
                final String collocTargetFileName = getCollocTargetFileName(sourceProduct, slaveSourceProduct);
                final String collocTargetFilePath = collocOutputDataDir + File.separator + collocTargetFileName;
                final File collocTargetFile = new File(collocTargetFilePath);

                // standard collocation, no coregistration
                Product collocateProduct = doStandardCollocation(sourceProduct, slaveSourceProduct);
                collocateProduct.setProductType(sourceProduct.getProductType() + "_" + collocateProduct.getProductType());
                if (collocateProduct != null) {
                    final WriteOp collocWriteOp = new WriteOp(collocateProduct, collocTargetFile, formatName);
                    System.out.println("Writing collocated product '" + collocTargetFileName + "'...");
                    collocWriteOp.writeProduct(ProgressMonitor.NULL);
                }
            }
        }
        setTargetProduct(new Product("dummy", "dummy", 0, 0));
    }

    static String getCollocTargetFileName(Product masterSourceProduct, Product slaveSourceProduct) {
        final String slaveProductName = slaveSourceProduct.getName();
        final int slaveProductLength = slaveProductName.length();
        final String slaveProductTimestring = slaveProductName.substring(slaveProductLength - 8, slaveProductLength - 4);

        return FileUtils.getFilenameWithoutExtension(masterSourceProduct.getFileLocation().getName()) + "_MOD_" +
                slaveProductTimestring + ".dim";
    }

    static Product[] findSlaveProductsToCollocate(Product masterSourceProduct, Product[] slaveSourceProducts) {
        // todo: check that we collocate data from the same day!
        List<Product> slaveCollocProductsList = new ArrayList<Product>();
        for (Product slaveSourceProduct : slaveSourceProducts) {
            final Geometry masterGeometry = TileExtractor.computeProductGeometry(masterSourceProduct);
            final Geometry slaveGeometry = TileExtractor.computeProductGeometry(slaveSourceProduct);
            if (masterGeometry != null && slaveGeometry != null && masterGeometry.intersects(slaveGeometry)) {
                slaveCollocProductsList.add(slaveSourceProduct);
            }
        }
        return slaveCollocProductsList.toArray(new Product[slaveCollocProductsList.size()]);
    }

    static Product[] getInputProducts(final Product sourceProduct, File inputDataDir, final String productType) {
        final FileFilter productsFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && isInTimeRange(file) &&
                        (file.getName().contains(productType) && file.getName().endsWith(".N1") ||
                                file.getName().contains(productType) && file.getName().endsWith(".dim") ||
                                file.getName().contains(productType) && file.getName().endsWith(".tar"));
            }

            private boolean isInTimeRange(File file) {
                final String masterProductTimestring =
                        FileUtils.getFilenameWithoutExtension(sourceProduct.getFileLocation().getName()).substring(20, 24);
                final int masterHH = Integer.parseInt(masterProductTimestring.substring(0, 2));
                final int masterMM = Integer.parseInt(masterProductTimestring.substring(2, 4));
                final int masterMinutesOfDay = 60 * masterHH + masterMM;

                final String slaveProductName = file.getName();
                final int slaveProductLength = slaveProductName.length();
                final String slaveProductTimestring = slaveProductName.substring(slaveProductLength - 8, slaveProductLength - 4);
                final int slaveHH = Integer.parseInt(slaveProductTimestring.substring(0, 2));
                final int slaveMM = Integer.parseInt(slaveProductTimestring.substring(2, 4));
                final int slaveMinutesOfDay = 60 * slaveHH + slaveMM;
                return Math.abs(masterMinutesOfDay - slaveMinutesOfDay) < 60;
            }
        };

        final File[] sourceProductFiles = (new File(inputDataDir.getAbsolutePath())).listFiles(productsFilter);
        List<Product> sourceProductsList = new ArrayList<Product>();

        int productIndex = 0;
        if (sourceProductFiles != null && sourceProductFiles.length > 0) {
            for (File sourceProductFile : sourceProductFiles) {
                try {
                    final Product product = ProductIO.readProduct(sourceProductFile.getAbsolutePath());
                    if (product != null) {
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
            System.out.println("No source products found in directory " + inputDataDir + " - nothing to do.");
        }

        return sourceProductsList.toArray(new Product[sourceProductsList.size()]);
    }

    private Product doStandardCollocation(Product masterSourceProduct, Product slaveSourceProduct) {
        Product collocateProduct;
        System.out.println("Collocating '" + masterSourceProduct.getName() + "', '" + slaveSourceProduct.getName() + "'...");
        Map<String, Product> collocateInput = new HashMap<String, Product>(2);
        collocateInput.put("masterProduct", masterSourceProduct);
        collocateInput.put("slaveProduct", slaveSourceProduct);
        collocateProduct =
                GPF.createProduct(OperatorSpi.getOperatorAlias(CollocateOp.class), GPF.NO_PARAMS, collocateInput);
        return collocateProduct;
    }

    private class ProductNameComparator implements Comparator<Product> {
        @Override
        public int compare(Product o1, Product o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisAatsrModisCollocationOp.class);
        }
    }
}
