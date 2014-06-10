package org.esa.beam.dataio;

import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

@OperatorMetadata(alias = "Meteosat.Netcdf.Read",
        description = "Reads a Meteosat product with standard Beam Netcdf reader and " +
                      "attaches a Meteosat Geocoding. Suitable lat/lon bands must be passed as parameters.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2014 by Brockmann Consult",
        internal = true)
public class MeteosatNetcdfReadOp extends Operator {
    @SourceProduct
    private Product sourceProduct;


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

        // copy all bands
        for (Band band : sourceProduct.getBands()) {
            if (!targetProductOrigProj.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProductOrigProj, true);
                ProductUtils.copyRasterDataNodeProperties(band, targetProductOrigProj.getBand(band.getName()));
            }
        }
        // tie point grids (there should be none)...
        for (TiePointGrid tpg : sourceProduct.getTiePointGrids()) {
            if (!targetProductOrigProj.containsTiePointGrid(tpg.getName())) {
                ProductUtils.copyTiePointGrid(tpg.getName(), sourceProduct, targetProductOrigProj);
            }
        }

        final Band latBand = targetProductOrigProj.getBand(latBandName);
        latBand.setValidPixelExpression("lat != 90 && lon != 90");
        final Band lonBand = targetProductOrigProj.getBand(lonBandName);
        lonBand.setValidPixelExpression("lat != 90 && lon != 90");
        try {
            targetProductOrigProj.setGeoCoding(new MeteosatGeoCoding(latBand, lonBand, regionID));
        } catch (IOException e) {
            throw new OperatorException
                    ("Cannot attach Meteosat geocoding to target product: " + e.getMessage());
        }

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

        // Collocate with reference product, which has no bands, but only WGS84 latlon CRS
        // and target region [65N,40S], [30W,65E] which sufficiently covers BRFs for MSG_Euro.
        Product latlonReferenceProduct;
        final File latlonReferenceFile = getLatlonReferenceFile(MSG_EURO_REF_FILE_NAME);
        try {
            latlonReferenceProduct = ProductIO.readProduct(latlonReferenceFile);
            CollocateOp collocateOp = new CollocateOp();
            collocateOp.setParameterDefaultValues();
            collocateOp.setMasterProduct(latlonReferenceProduct);
            collocateOp.setSlaveProduct(targetProductOrigProj);
            collocateOp.setRenameSlaveComponents(false);
            final Product finalProduct = collocateOp.getTargetProduct();
            finalProduct.setGeoCoding(new PixelGeoCoding(finalProduct.getBand("lat"),
                                                         finalProduct.getBand("lon"),
                                                         null, 4));
            ProductUtils.copyMetadata(sourceProduct, finalProduct);

            return finalProduct;
        } catch (IOException e) {
            throw new OperatorException("Cannot find WGS84 latlon reference file: " + latlonReferenceFile.getAbsolutePath());
        }
    }

    private File getLatlonReferenceFile(String name) {
        final URL url = getClass().getResource(name);
        if (url == null) {
            throw new OperatorException("Cannot find WGS84 latlon reference file: " + name);
        }
        String filePath;
        try {
            filePath = URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new OperatorException(e.getMessage());
        }
        return new File(filePath);
    }


    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MeteosatNetcdfReadOp.class);
        }
    }
}
