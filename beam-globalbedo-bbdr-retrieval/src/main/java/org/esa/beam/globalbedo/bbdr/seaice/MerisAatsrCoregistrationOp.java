package org.esa.beam.globalbedo.bbdr.seaice;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.bbdr.BbdrConstants;
import org.esa.beam.globalbedo.bbdr.GaPassThroughOp;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Coregistrates and collocates a MERIS L1b product with fitting AATSR L1b products.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.seaice.merisaatsr.coreg",
        description = "Coregistrates and collocates a MERIS L1b product with fitting AATSR L1b products.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2013 by Brockmann Consult")
public class MerisAatsrCoregistrationOp extends Operator {

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

        final String slaveProductPrefix = "ATS_TOA_1P";
        Product[] slaveSourceProducts = MerisAatsrCollocationOp.getInputProducts(slaveInputDataDir, slaveProductPrefix);
        Arrays.sort(slaveSourceProducts, new ProductNameComparator());

        Product[] slaveProductsToCollocate = MerisAatsrCollocationOp.findSlaveProductsToCollocate(sourceProduct, slaveSourceProducts);

        for (Product slaveSourceProduct : slaveProductsToCollocate) {
            if (slaveSourceProduct != null) {
                final String collocTargetFileName = MerisAatsrCollocationOp.getCollocTargetFileName(sourceProduct,
                                                                                                    slaveSourceProduct,
                                                                                                    Sensor.MERIS,
                                                                                                    formatName);
                final String collocTargetFilePath = collocOutputDataDir + File.separator + collocTargetFileName;
                final File collocTargetFile = new File(collocTargetFilePath);
                Product collocateProduct = null;
                if (!(EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(sourceProduct.getProductType()).matches())) {
                    throw new OperatorException("MERIS must be master sensor to apply coregistration.");
                }
                // create netcdf files for python input:
                final Product masterNcProduct =
                        GPF.createProduct(OperatorSpi.getOperatorAlias(GaPassThroughOp.class), GPF.NO_PARAMS, sourceProduct);
                final String masterNcDir = sourceProduct.getFileLocation().getParent();
                final String masterNcFilename = FileUtils.getFilenameWithoutExtension(sourceProduct.getFileLocation()) + ".nc";
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
                // (on Linux machine! On windows, take coregistration product (*_warped.nc) and set coregAvailable=true)
                final String pythonCoregCall =
                        "python ./coregistered/python_scripts/AatsrMerisCoregisterNc4.py " +
                                slaveNcFile.getParent() + File.separator + " " +      // AATSR is passed first
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
                    collocateProduct = getCollocFromCoregProduct(sourceProduct, slaveSourceProduct);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Coregistration failed for " +
                            sourceProduct.getName() + " / " +
                            slaveSourceProduct.getName() + " / " +
                            " - no collocation output written.");
                }

                if (collocateProduct != null) {
                    final WriteOp collocWriteOp = new WriteOp(collocateProduct, collocTargetFile, formatName);
                    System.out.println("Writing collocated product '" + collocTargetFileName + "'...");
                    collocWriteOp.writeProduct(ProgressMonitor.NULL);
                }
            }
        }
        setTargetProduct(new Product("dummy", "dummy", 0, 0));
    }

    private Product getCollocFromCoregProduct(Product merisL1bProduct,
                                              Product aatsrL1bProduct) throws IOException, OperatorException {

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

        collocProduct.setStartTime(merisL1bProduct.getStartTime());
        collocProduct.setEndTime(merisL1bProduct.getEndTime());

        // copy all bands from collocation product which are no flag bands and no original MERIS tie points
        for (Band bCoreg : coregProduct.getBands()) {
            String bandNameNoExtension = bCoreg.getName().substring(0, bCoreg.getName().length() - 2);
            if (!isMerisTpg(bandNameNoExtension) &&
                    !bandNameNoExtension.contains("flag") &&
                    !bCoreg.getName().startsWith("lat_") &&
                    !bCoreg.getName().startsWith("lon_") &&
                    !bandNameNoExtension.contains("detector")) {
                if (!collocProduct.containsBand(bCoreg.getName())) {
                    if (bandNameNoExtension.startsWith("radiance")) {
                        // MERIS
                        ProductUtils.copyBand(bandNameNoExtension, merisL1bProduct, bCoreg.getName(), collocProduct, true);
                        collocProduct.getBand(bCoreg.getName()).setValidPixelExpression("!(l1_flags_M.INVALID)");
                    } else {
                        ProductUtils.copyBand(bCoreg.getName(), coregProduct, collocProduct, true);
                        ProductUtils.copyRasterDataNodeProperties(bCoreg, collocProduct.getBand(bCoreg.getName()));
                    }
                }
            } else {
                final int width = collocProduct.getSceneRasterWidth();
                final int height = collocProduct.getSceneRasterHeight();
                if (bandNameNoExtension.contains("flag")) {
                    if (bandNameNoExtension.equals("l1_flags")) {
                        // take source image from L1b product
                        ProductUtils.copyBand(bandNameNoExtension, merisL1bProduct, bCoreg.getName(), collocProduct, true);
                        collocProduct.getFlagCodingGroup().get("l1_flags").setName("l1_flags_M");
                    } else {
                        // restore flag bands with int values...
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
                    }
                } else if (bandNameNoExtension.equals("detector_index")) {     // MERIS detector index band
                    // take source image from L1b product
                    ProductUtils.copyBand(bandNameNoExtension, merisL1bProduct, bCoreg.getName(), collocProduct, true);
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

    private class ProductNameComparator implements Comparator<Product> {
        @Override
        public int compare(Product o1, Product o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MerisAatsrCoregistrationOp.class);
        }
    }
}
