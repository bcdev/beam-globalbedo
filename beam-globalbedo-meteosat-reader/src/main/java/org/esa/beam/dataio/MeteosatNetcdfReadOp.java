package org.esa.beam.dataio;

import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.io.IOException;

@OperatorMetadata(alias = "Meteosat.Netcdf.Read",
        description = "This operator reads a Meteosat MSG 'Euro' product with standard Beam Netcdf reader, " +
                      "attaches a Meteosat Geocoding, and reprojects to WGS84 lat/lon. " +
                      "Suitable lat/lon bands must be passed as parameters.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2014 by Brockmann Consult")
public class MeteosatNetcdfReadOp extends Operator {
    @SourceProduct
    private Product sourceProduct;

    @SourceProduct
    private Product latlonProduct;

    @Parameter(defaultValue = "lat",
            description = "Latitude band for setup of Meteosat geocoding")
    private String latBandName;

    @Parameter(defaultValue = "lon",
            description = "Longitude band for setup of Meteosat geocoding")
    private String lonBandName;

    @Parameter(defaultValue = "MSG_Euro", valueSet = {"MSG_Euro"},
               description = "Region ID for setup of Meteosat geocoding (only one so far)")
    private String regionID;

    private static final String MSG_EURO_REF_FILE_NAME = "MSG_Euro_WGS84_latlon_reference.dim";

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    private Product createTargetProduct() {
        Product targetProductOrigProj = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(sourceProduct, targetProductOrigProj);
        ProductUtils.copyFlagCodings(sourceProduct, targetProductOrigProj);
        ProductUtils.copyFlagBands(sourceProduct, targetProductOrigProj, true);
        ProductUtils.copyMasks(sourceProduct, targetProductOrigProj);
        targetProductOrigProj.setStartTime(sourceProduct.getStartTime());
        targetProductOrigProj.setEndTime(sourceProduct.getEndTime());

        // copy all source bands
        for (Band band : sourceProduct.getBands()) {
            if (!targetProductOrigProj.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProductOrigProj, true);
                ProductUtils.copyRasterDataNodeProperties(band, targetProductOrigProj.getBand(band.getName()));
            }
        }
        // copy lat/lon bands
        for (Band band : latlonProduct.getBands()) {
            if (!targetProductOrigProj.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), latlonProduct, targetProductOrigProj, true);
                ProductUtils.copyRasterDataNodeProperties(band, targetProductOrigProj.getBand(band.getName()));
            }
        }

        final Band latBand = latlonProduct.getBand(latBandName);
        latBand.setValidPixelExpression("lat != 90 && lon != 90");
        final Band lonBand = latlonProduct.getBand(lonBandName);
        lonBand.setValidPixelExpression("lat != 90 && lon != 90");
        try {
            targetProductOrigProj.setGeoCoding(new MeteosatGeoCoding(latBand, lonBand, regionID));
        } catch (IOException e) {
            throw new OperatorException
                    ("Cannot attach Meteosat geocoding to target product: " + e.getMessage());
        }

        // *** begin: this block needed to be done only once todo: document somewhere else
        // first step was to take an arbitrary global WGS84 latlon product
        // (e.g. a Globalbedo mosaic 'Globalbedo.albedo.200505.05.PC_reprojected.dim')
        // and reproject to 1/30 deg resolution (was done once with Visat)

        // second step was to subset this 1/30 deg resolution global WGS84 latlon ref product to 65N,40S/30W,65E; one band
        // Again we needed to do this only once:
//        SubsetOp subsetOp = new SubsetOp();
//        subsetOp.setSourceProduct(latlonReferenceProduct);
//        subsetOp.setParameterDefaultValues();
//        subsetOp.setRegion(new Rectangle(4500,750,2850,3150));
//        subsetOp.setBandNames(new String[]{latlonReferenceProduct.getBandAt(0).getName()});
//        return subsetOp.getTargetProduct();
        // This reference is provided as resource product (MSG_Euro_WGS84_latlon_reference.dim).
        // *** end: this block needed to be done only once

        // Collocate with reference product, which has no bands, but only WGS84 latlon CRS
        // and target region [65N,40S], [30W,65E] which sufficiently covers BRFs for MSG_Euro.
        // todo: simple reprojection of the Meteosat product would be more reasonable, but does not work. Check why!
        Product latlonReferenceProduct;
        final File latlonReferenceFile = getAuxFile(MSG_EURO_REF_FILE_NAME);
        try {
            latlonReferenceProduct = ProductIO.readProduct(latlonReferenceFile);
            CollocateOp collocateOp = new CollocateOp();
            collocateOp.setParameterDefaultValues();
            collocateOp.setMasterProduct(latlonReferenceProduct);
            collocateOp.setSlaveProduct(targetProductOrigProj);
            collocateOp.setRenameSlaveComponents(false);
            final Product finalProduct = collocateOp.getTargetProduct();
//            finalProduct.setGeoCoding(new PixelGeoCoding(finalProduct.getBand("lat"),
//                                                         finalProduct.getBand("lon"),
//                                                         null, 4));
            ProductUtils.copyMetadata(sourceProduct, finalProduct);

            return finalProduct;
        } catch (IOException e) {
            throw new OperatorException("Cannot find WGS84 latlon reference file: " + latlonReferenceFile.getAbsolutePath());
        }
    }

    private File getAuxFile(String filename) {
        String meteosatAuxdataSrcPath = "org/esa/beam/dataio";
        final String meteosatAuxdataDestPath = ".beam/beam-globalbedo/beam-globalbedo-meteosat-reader/" + meteosatAuxdataSrcPath;
        File meteosatAuxdataTargetDir = new File(SystemUtils.getUserHomeDir(), meteosatAuxdataDestPath);
        return new File(meteosatAuxdataTargetDir, filename);
    }


    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MeteosatNetcdfReadOp.class);
            AuxdataInstaller.installAuxdata(MeteosatNetcdfReadOp.class);
        }
    }
}
