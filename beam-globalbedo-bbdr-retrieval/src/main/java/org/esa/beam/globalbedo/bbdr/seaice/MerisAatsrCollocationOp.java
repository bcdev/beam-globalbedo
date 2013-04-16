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
import org.esa.beam.globalbedo.bbdr.Sensor;
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
                  description = "Collocates MERIS/AATSR L1b products with fitting AATSR/MERIS L1b products.",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class MerisAatsrCollocationOp extends Operator {
    @Parameter(defaultValue = "", description = "Master sensor input data directory")
    private File masterInputDataDir;

    @Parameter(defaultValue = "", description = "Slave sensor input data directory")
    private File slaveInputDataDir;

    @Parameter(defaultValue = "", description = "Collocation output data directory")
    private File collocOutputDataDir;

    @Parameter(defaultValue = "MERIS", valueSet = {"MERIS", "AATSR"})
    private Sensor masterSensor;

    @Parameter(defaultValue = ProductIO.DEFAULT_FORMAT_NAME, valueSet = {"BEAM-DIMAP", "NetCDF4-BEAM"})
    private String formatName;

    private Product[] masterSourceProducts;
    private Product[] slaveSourceProducts;

    @Override
    public void initialize() throws OperatorException {

        final String  masterProductPrefix = masterSensor == Sensor.MERIS ? "MER_RR__1P" : "ATS_TOA_1P";
        final String  slaveProductPrefix  = masterSensor == Sensor.MERIS ? "ATS_TOA_1P" : "MER_RR__1P";

        masterSourceProducts = getInputProducts(masterInputDataDir, masterProductPrefix);
        slaveSourceProducts = getInputProducts(slaveInputDataDir, slaveProductPrefix);

        Arrays.sort(masterSourceProducts, new ProductNameComparator());
        Arrays.sort(slaveSourceProducts, new ProductNameComparator());

        for (Product masterSourceProduct : masterSourceProducts) {
            Product[] slaveSourceProducts = findSlaveProductsToCollocate(masterSourceProduct);

            for (Product slaveSourceProduct : slaveSourceProducts) {
                if (slaveSourceProduct != null) {
                    System.out.println("Collocating '" + masterSourceProduct.getName() + "', '" + slaveSourceProduct.getName() + "'...");
                    Map<String, Product> collocateInput = new HashMap<String, Product>(2);
                    collocateInput.put("masterProduct", masterSourceProduct);
                    collocateInput.put("slaveProduct", slaveSourceProduct);
                    Product collocateProduct =
                            GPF.createProduct(OperatorSpi.getOperatorAlias(CollocateOp.class), GPF.NO_PARAMS, collocateInput);

                    final String collocTargetFileName = getCollocTargetFileName(masterSourceProduct, slaveSourceProduct);
                    final String collocTargetFilePath = collocOutputDataDir + File.separator + collocTargetFileName;
                    final File collocTargetFile = new File(collocTargetFilePath);
                    final WriteOp collocWriteOp = new WriteOp(collocateProduct, collocTargetFile, formatName);
                    System.out.println("Writing collocated product '" + collocTargetFileName + "'...");
                    collocWriteOp.writeProduct(ProgressMonitor.NULL);
                }
            }
        }
        setTargetProduct(new Product("dummy", "dummy", 0, 0));
    }

    private String getCollocTargetFileName(Product masterSourceProduct, Product slaveSourceProduct) {
        final String year = String.format("%02d", masterSourceProduct.getStartTime().getAsCalendar().get(Calendar.YEAR));
        final String month = String.format("%02d", masterSourceProduct.getStartTime().getAsCalendar().get(Calendar.MONTH) + 1);
        final String day = String.format("%02d", masterSourceProduct.getStartTime().getAsCalendar().get(Calendar.DAY_OF_MONTH));

        final String masterStartHour = String.format("%02d", masterSourceProduct.getStartTime().getAsCalendar().get(Calendar.HOUR_OF_DAY));
        final String masterStartMin = String.format("%02d", masterSourceProduct.getStartTime().getAsCalendar().get(Calendar.MINUTE));
        final String masterStartSec = String.format("%02d", masterSourceProduct.getStartTime().getAsCalendar().get(Calendar.SECOND));
        final String masterEndHour = String.format("%02d", masterSourceProduct.getEndTime().getAsCalendar().get(Calendar.HOUR_OF_DAY));
        final String masterEndMin = String.format("%02d", masterSourceProduct.getEndTime().getAsCalendar().get(Calendar.MINUTE));
        final String masterEndSec = String.format("%02d", masterSourceProduct.getEndTime().getAsCalendar().get(Calendar.SECOND));

        final String slaveStartHour = String.format("%02d", slaveSourceProduct.getStartTime().getAsCalendar().get(Calendar.HOUR_OF_DAY));
        final String slaveStartMin = String.format("%02d", slaveSourceProduct.getStartTime().getAsCalendar().get(Calendar.MINUTE));
        final String slaveStartSec = String.format("%02d", slaveSourceProduct.getStartTime().getAsCalendar().get(Calendar.SECOND));
        final String slaveEndHour = String.format("%02d", slaveSourceProduct.getEndTime().getAsCalendar().get(Calendar.HOUR));
        final String slaveEndMin = String.format("%02d", slaveSourceProduct.getEndTime().getAsCalendar().get(Calendar.MINUTE));
        final String slaveEndSec = String.format("%02d", slaveSourceProduct.getEndTime().getAsCalendar().get(Calendar.SECOND));

        // name should be COLLOC_yyyyMMdd_MER_hhmmss_hhmmss_ATS_hhmmss_hhmmss.dim

        final String  masterSensorId = masterSensor == Sensor.MERIS ? "MER" : "ATS";
        final String  slaveSensorId = masterSensor == Sensor.MERIS ? "ATS" : "MER";
        final String  extension = formatName.equals(ProductIO.DEFAULT_FORMAT_NAME) ? ".dim" : ".nc";
        return "COLLOC_" + year + month + day +
                "_" + masterSensorId + "_" +
                masterStartHour + masterStartMin + masterStartSec + "_" + masterEndHour + masterEndMin + masterEndSec +
                "_" + slaveSensorId + "_" +
                slaveStartHour + slaveStartMin + slaveStartSec + "_" + slaveEndHour + slaveEndMin + slaveEndSec +
                extension;
    }


    private Product[] findSlaveProductsToCollocate(Product merisSourceProduct) {
        final ProductData.UTC merisStartTime = merisSourceProduct.getStartTime();
        final ProductData.UTC merisEndTime = merisSourceProduct.getEndTime();
        final long merisStartTimeSecs = merisStartTime.getAsCalendar().getTimeInMillis() / 1000;
        final long merisEndTimeSecs = merisEndTime.getAsCalendar().getTimeInMillis() / 1000;

        List<Product> aatsrCollocProductsList = new ArrayList<Product>();

        for (Product aatsrSourceProduct : slaveSourceProducts) {
            final ProductData.UTC aatsrStartTime = aatsrSourceProduct.getStartTime();
            final ProductData.UTC aatsrEndTime = aatsrSourceProduct.getEndTime();
            final long aatsrStartTimeSecs = aatsrStartTime.getAsCalendar().getTimeInMillis() / 1000;
            final long aatsrEndTimeSecs = aatsrEndTime.getAsCalendar().getTimeInMillis() / 1000;

            if ((merisStartTimeSecs < aatsrStartTimeSecs && aatsrStartTimeSecs < merisEndTimeSecs) ||
                    (merisStartTimeSecs < aatsrEndTimeSecs && aatsrEndTimeSecs < merisEndTimeSecs) ||
                    (aatsrStartTimeSecs < merisStartTimeSecs && merisStartTimeSecs < aatsrEndTimeSecs) ||
                    (aatsrStartTimeSecs < merisEndTimeSecs && merisEndTimeSecs < aatsrEndTimeSecs)) {
                aatsrCollocProductsList.add(aatsrSourceProduct);
            }
        }

        return aatsrCollocProductsList.toArray(new Product[aatsrCollocProductsList.size()]);
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
            System.out.println("No source products found in directory " + masterInputDataDir + " - nothing to do.");
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
