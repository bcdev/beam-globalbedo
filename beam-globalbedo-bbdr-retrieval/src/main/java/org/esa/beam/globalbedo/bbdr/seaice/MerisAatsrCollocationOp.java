package org.esa.beam.globalbedo.bbdr.seaice;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.bbdr.BbdrConstants;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

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

    @Parameter(defaultValue = "false")
    private boolean doCoregistration;

    @Parameter(defaultValue = "false")
    private boolean coregAvailable;

    private Product[] masterSourceProducts;
    private Product[] slaveSourceProducts;

    @Override
    public void initialize() throws OperatorException {

        final String masterProductPrefix = masterSensor == Sensor.MERIS ? "MER_RR__1P" : "ATS_TOA_1P";
        final String slaveProductPrefix = masterSensor == Sensor.MERIS ? "ATS_TOA_1P" : "MER_RR__1P";

        masterSourceProducts = getInputProducts(masterInputDataDir, masterProductPrefix);
        slaveSourceProducts = getInputProducts(slaveInputDataDir, slaveProductPrefix);

        Arrays.sort(masterSourceProducts, new ProductNameComparator());
        Arrays.sort(slaveSourceProducts, new ProductNameComparator());

        for (Product masterSourceProduct : masterSourceProducts) {
            Product[] slaveSourceProducts = findSlaveProductsToCollocate(masterSourceProduct);

            for (Product slaveSourceProduct : slaveSourceProducts) {
                if (slaveSourceProduct != null) {
                    final String collocTargetFileName = getCollocTargetFileName(masterSourceProduct, slaveSourceProduct);
                    final String collocTargetFilePath = collocOutputDataDir + File.separator + collocTargetFileName;
                    final File collocTargetFile = new File(collocTargetFilePath);
                    Product collocateProduct = null;
                    if (doCoregistration) {
                        // create netcdf files for python input:
                        final Product masterNcProduct =
                                GPF.createProduct(OperatorSpi.getOperatorAlias(GaPassThroughOp.class), GPF.NO_PARAMS, masterSourceProduct);
                        final String masterNcDir = masterSourceProduct.getFileLocation().getParent();
                        final String masterNcFilename = FileUtils.getFilenameWithoutExtension(masterSourceProduct.getFileLocation()) + ".nc";
                        final File masterNcFile = new File(masterNcDir + File.separator + masterNcFilename);
                        final WriteOp masterWriteOp = new WriteOp(masterNcProduct, masterNcFile, "NetCDF-CF");
                        System.out.println("Writing master netcdf product '" + masterNcFile.getAbsolutePath() + "'...");
                        masterWriteOp.writeProduct(ProgressMonitor.NULL);

                        final Product slaveNcProduct =
                                GPF.createProduct(OperatorSpi.getOperatorAlias(GaPassThroughOp.class), GPF.NO_PARAMS, slaveSourceProduct);
                        final String slaveNcDir = slaveSourceProduct.getFileLocation().getParent();
                        final String slaveNcFilename = FileUtils.getFilenameWithoutExtension(slaveSourceProduct.getFileLocation()) + ".nc";
                        final File slaveNcFile = new File(slaveNcDir + File.separator + slaveNcFilename);
                        final WriteOp slaveWriteOp = new WriteOp(slaveNcProduct, slaveNcFile, "NetCDF-CF");
                        System.out.println("Writing slave netcdf product '" + slaveNcFile.getAbsolutePath() + "'...");
                        slaveWriteOp.writeProduct(ProgressMonitor.NULL);

                        // call Python coregistration
                        final String pythonCoregCall =
                                "python ./coregistered/python_scripts/AatsrMerisCoregisterNc4.py " +
                                        slaveNcFile.getParent() + File.separator + " " +      // todo: make sure AATSR is passed first in any case
                                        slaveNcFile.getName() + " " +
                                        masterNcFile.getParent() + File.separator + " " +
                                        masterNcFile.getName() + " " +
                                        "/tmp" + " " +
                                        slaveNcFile.getParent() + File.separator;
                        System.out.println("pythonCoregCall = " + pythonCoregCall);
                        try {
                            System.out.println("Starting Python coregistration...");
                            if (coregAvailable) {
                                System.out.println("...coregistration was done earlier - go ahead...");
                            } else {
                                final Process p = Runtime.getRuntime().exec(pythonCoregCall);
                                p.waitFor();
                                System.out.println("Finished Python coregistration.");
                            }
                            // todo: make sure correct slave is used
                            collocateProduct = getCollocFromCoregProduct(masterSourceProduct, slaveSourceProduct);
                        } catch (IOException e) {
                            // todo
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            // todo
                            e.printStackTrace();
                        }
                    } else {
                        // standard collocation
                        System.out.println("Collocating '" + masterSourceProduct.getName() + "', '" + slaveSourceProduct.getName() + "'...");
                        Map<String, Product> collocateInput = new HashMap<String, Product>(2);
                        collocateInput.put("masterProduct", masterSourceProduct);
                        collocateInput.put("slaveProduct", slaveSourceProduct);
                        collocateProduct =
                                GPF.createProduct(OperatorSpi.getOperatorAlias(CollocateOp.class), GPF.NO_PARAMS, collocateInput);

                    }
                    final WriteOp collocWriteOp = new WriteOp(collocateProduct, collocTargetFile, formatName);
                    System.out.println("Writing collocated product '" + collocTargetFileName + "'...");
                    collocWriteOp.writeProduct(ProgressMonitor.NULL);
                }
            }
        }
        setTargetProduct(new Product("dummy", "dummy", 0, 0));
    }

    private Product getCollocFromCoregProduct(Product merisL1bProduct,
                                              Product aatsrL1bProduct) throws IOException {

        final String coregFilepath = aatsrL1bProduct.getFileLocation().getParent() +
                File.separator + FileUtils.getFilenameWithoutExtension(aatsrL1bProduct.getFileLocation()) + "_warped.nc";
        System.out.println("Coregistration product filepath: '" + coregFilepath + "'...");
        Product coregProduct = getCoregProduct(new File(coregFilepath));
        Product collocProduct = new Product(coregProduct.getName(),
                                            "COLLOCATED",
                                            coregProduct.getSceneRasterWidth(),
                                            coregProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(coregProduct, collocProduct);
        ProductUtils.copyFlagCodings(merisL1bProduct, collocProduct);
        ProductUtils.copyFlagCodings(aatsrL1bProduct, collocProduct);
        ProductUtils.copyMasks(coregProduct, collocProduct);

        final ProductData.UTC startTime =
                masterSensor == Sensor.MERIS ? merisL1bProduct.getStartTime() : aatsrL1bProduct.getStartTime();
        final ProductData.UTC endTime =
                masterSensor == Sensor.MERIS ? merisL1bProduct.getEndTime() : aatsrL1bProduct.getEndTime();

        collocProduct.setStartTime(startTime);
        collocProduct.setEndTime(endTime);

        // copy all bands from collocation product which are no flag bands and no original MERIS tie points
        for (Band bCoreg : coregProduct.getBands()) {
            String bandNameNoExtension = bCoreg.getName().substring(0, bCoreg.getName().length() - 2);
            if (!isMerisTpg(bandNameNoExtension) &&
                    !bandNameNoExtension.contains("flag") &&
                    !bCoreg.getName().startsWith("lat_") &&
                    !bCoreg.getName().startsWith("lon_") &&
                    !bandNameNoExtension.contains("detector")) {
                if (!collocProduct.containsBand(bCoreg.getName())) {
                    System.out.println("copy: " + bCoreg.getName());
                    ProductUtils.copyBand(bCoreg.getName(), coregProduct, collocProduct, true);
                    ProductUtils.copyRasterDataNodeProperties(bCoreg, collocProduct.getBand(bCoreg.getName()));
                    if (bandNameNoExtension.startsWith("radiance")) {
                        final String spectralBandIndex = bandNameNoExtension.substring(9, bandNameNoExtension.length());
                        collocProduct.getBand(bCoreg.getName()).setSpectralBandIndex(Integer.parseInt(spectralBandIndex) - 1);
                    }
                }
            } else {
                final int width = collocProduct.getSceneRasterWidth();
                final int height = collocProduct.getSceneRasterHeight();
                if (bandNameNoExtension.contains("flag")) {
                    // restore flag bands with int values...
                    System.out.println("adding flag band: " + bCoreg.getName());
                    Band flagBand = collocProduct.addBand(bCoreg.getName(), ProductData.TYPE_INT32);
                    flagBand.setSampleCoding(collocProduct.getFlagCodingGroup().get(bandNameNoExtension));
                    DataBuffer dataBuffer = bCoreg.getSourceImage().getData().getDataBuffer();
                    final int[] flagData = new int[dataBuffer.getSize()];
                    for (int i = 0; i < dataBuffer.getSize(); i++) {
                        final float elemFloatAt = dataBuffer.getElemFloat(i);
                        flagData[i] = (int) elemFloatAt;
                    }
                    flagBand.setDataElems(flagData);
                    flagBand.getSourceImage();
                } else if (bandNameNoExtension.contains("detector")) {     // MERIS detector index band
                    // restore detector index band with short values...
                    System.out.println("adding detector index band: " + bCoreg.getName());
                    Band detectorIndexBand = collocProduct.addBand(bCoreg.getName(), ProductData.TYPE_INT16);
                    DataBuffer dataBuffer = bCoreg.getSourceImage().getData().getDataBuffer();
                    final short[] detectorIndexData = new short[dataBuffer.getSize()];
                    for (int i = 0; i < dataBuffer.getSize(); i++) {
                        final float elemFloatAt = dataBuffer.getElemFloat(i);
                        detectorIndexData[i] = (short) elemFloatAt;
                    }
                    detectorIndexBand.setDataElems(detectorIndexData);
                    detectorIndexBand.getSourceImage();
                } else if (isMerisTpg(bandNameNoExtension)) {
                    // restore tie point grids...
                    DataBuffer dataBuffer = bCoreg.getSourceImage().getData().getDataBuffer();
                    float[] tpgData = new float[dataBuffer.getSize()];
                    for (int i = 0; i < dataBuffer.getSize(); i++) {
                        tpgData[i] = dataBuffer.getElemFloat(i);
                    }
                    TiePointGrid tpg = new TiePointGrid(bandNameNoExtension,
                                                        width,
                                                        height,
                                                        0.0f, 0.0f, 1.0f, 1.0f, tpgData);
                    tpg.setSourceImage(bCoreg.getSourceImage());
                    System.out.println("adding tpg: " + bandNameNoExtension);
                    collocProduct.addTiePointGrid(tpg);
                }
            }
        }
        final TiePointGrid latTpg = collocProduct.getTiePointGrid("latitude");
        final TiePointGrid lonTpg = collocProduct.getTiePointGrid("longitude");
        if (latTpg == null || lonTpg == null) {
            throw new OperatorException("latitude or longitude tie point grid missing - cannot proceed.");
        }
        collocProduct.setGeoCoding(new TiePointGeoCoding(latTpg, lonTpg));

        return collocProduct;
    }

    private Product getCoregProduct(File coregFile) {
        Product coregProduct = null;
        try {
            coregProduct = ProductIO.readProduct(coregFile);
            if (coregProduct == null) {
                String msg = String.format("Could not read file '%s. No appropriate reader found.",
                                           coregFile.getName());
                System.out.println(msg);
            }
        } catch (IOException e) {
            String msg = String.format("Not able to read file '%s.", coregFile.getName());
            System.out.println(msg);
        }
        return coregProduct;
    }

    private static boolean isMerisTpg(String bandName) {
        for (String merisTpgName : BbdrConstants.MERIS_TIE_POINT_GRID_NAMES) {
            if (merisTpgName.equals(bandName)) {
                return true;
            }
        }
        return false;
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
        final String slaveEndHour = String.format("%02d", slaveSourceProduct.getEndTime().getAsCalendar().get(Calendar.HOUR_OF_DAY));
        final String slaveEndMin = String.format("%02d", slaveSourceProduct.getEndTime().getAsCalendar().get(Calendar.MINUTE));
        final String slaveEndSec = String.format("%02d", slaveSourceProduct.getEndTime().getAsCalendar().get(Calendar.SECOND));

        // name should be COLLOC_yyyyMMdd_MER_hhmmss_hhmmss_ATS_hhmmss_hhmmss.dim

        final String masterSensorId = masterSensor == Sensor.MERIS ? "MER" : "ATS";
        final String slaveSensorId = masterSensor == Sensor.MERIS ? "ATS" : "MER";
        final String extension = formatName.equals(ProductIO.DEFAULT_FORMAT_NAME) ? ".dim" : ".nc";
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
                        (file.getName().contains(productType) && file.getName().endsWith(".N1") ||
                                file.getName().contains(productType) && file.getName().endsWith(".dim"));
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
