package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.dataio.MeteosatGeoCoding;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.TransposeDescriptor;
import java.io.IOException;

/**
 * Reads a Meteosat MSA Albedo disk product, attaches a Meteosat Geocoding, and reprojects to specified MODIS SIN tile.
 * TODO: lot of duplications from MeteosatBrfTileExtractor. Merge and cleanup!
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.msaalbedo.meteosat",
        description = "Reads a Meteosat MVIRI MSA albedo disk product with standard Beam Netcdf reader, " +
                "attaches a Meteosat Geocoding, and reprojects to specified MODIS SIN tile. " +
                "Suitable lat/lon bands must be passed as parameters.",
        authors = "Olaf Danne",
        internal = true,
        version = "1.0",
        copyright = "(c) 2016 by Brockmann Consult")
public class MeteosatMsaAlbedoTileExtractor extends Operator {
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

    @Parameter(description = "MODIS tile")
    private String tile;

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
                final Band targetBand = targetProductOrigProj.getBand(band.getName());
                ProductUtils.copyRasterDataNodeProperties(band, targetBand);
                RenderedOp flippedImage = flipImage(band);
                targetBand.setSourceImage(flippedImage);
            }
        }
        // copy lat/lon bands
        for (Band band : latlonProduct.getBands()) {
            if (!targetProductOrigProj.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), latlonProduct, targetProductOrigProj, true);
                final Band targetBand = targetProductOrigProj.getBand(band.getName());
                ProductUtils.copyRasterDataNodeProperties(band, targetBand);
                RenderedOp flippedImage = flipImage(band);
                targetBand.setSourceImage(flippedImage);
            }
        }

        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();

        final Band latBand = latlonProduct.getBand(latBandName);
        latBand.setValidPixelExpression(latBandName + " != -9999.0 && " + lonBandName + " != -9999.0");
        RenderedOp flippedImage = flipImage(latBand);
        latBand.setSourceImage(flippedImage);
        final Band lonBand = latlonProduct.getBand(lonBandName);
        lonBand.setValidPixelExpression(latBandName + " != -9999.0 && " + lonBandName + " != -9999.0");
        flippedImage = flipImage(lonBand);
        lonBand.setSourceImage(flippedImage);
        lonBand.setScalingFactor(1.0);   // current static data file contains a weird scaling factor for longitude band
        try {
            final MeteosatGeoCoding meteosatGeoCoding = new MeteosatGeoCoding(latBand, lonBand, regionID);
            targetProductOrigProj.setGeoCoding(meteosatGeoCoding);

            for (int i=0; i<modisTileCoordinates.getTileCount(); i++) {
                final String tileName = modisTileCoordinates.getTileName(i);
                checkIfModisTileIntersectsMeteosatDisk(tileName, meteosatGeoCoding);
            }

            // now reproject onto given MODIS SIN tile...
            if (checkIfModisTileIntersectsMeteosatDisk(tile, meteosatGeoCoding)) {
                return TileExtractor.reprojectToModisTile(targetProductOrigProj, tile, "Bilinear");
            } else {
                throw new OperatorException
                        ("Tile '" + tile + "' has no ontersection with Meteosat disk.");
            }
        } catch (IOException e) {
            throw new OperatorException
                    ("Cannot attach Meteosat geocoding to target product: " + e.getMessage());
        }

    }

    private RenderedOp flipImage(Band sourceBand) {
        final RenderedOp verticalFlippedImage = TransposeDescriptor.create(sourceBand.getSourceImage(), TransposeDescriptor.FLIP_VERTICAL, null);
        return TransposeDescriptor.create(verticalFlippedImage, TransposeDescriptor.FLIP_HORIZONTAL, null);
    }

    static boolean checkIfModisTileIntersectsMeteosatDisk(String tile, MeteosatGeoCoding meteosatGeoCoding) {
        final ModisTileGeoCoding tileGeoCoding = IOUtils.getSinusoidalTileGeocoding(tile);

        final PixelPos ulPixelPos = new PixelPos(0.5f, 0.5f);
        final PixelPos urPixelPos = new PixelPos(1200.5f, 0.5f);
        final PixelPos llPixelPos = new PixelPos(0.5f, 1200.5f);
        final PixelPos lrPixelPos = new PixelPos(1200.5f, 1200.5f);

        final GeoPos ulGeoPos = tileGeoCoding.getGeoPos(ulPixelPos, null);
        PixelPos meteosatPixelPos = meteosatGeoCoding.getPixelPos(ulGeoPos, null);
        if (!Float.isNaN(meteosatPixelPos.x) && !Float.isNaN(meteosatPixelPos.y)) {
            System.out.println(tile);
            return true;
        }
        final GeoPos urGeoPos = tileGeoCoding.getGeoPos(urPixelPos, null);
        meteosatPixelPos = meteosatGeoCoding.getPixelPos(urGeoPos, null);
        if (!Float.isNaN(meteosatPixelPos.x) && !Float.isNaN(meteosatPixelPos.y)) {
            System.out.println(tile);
            return true;
        }
        final GeoPos llGeoPos = tileGeoCoding.getGeoPos(llPixelPos, null);
        meteosatPixelPos = meteosatGeoCoding.getPixelPos(llGeoPos, null);
        if (!Float.isNaN(meteosatPixelPos.x) && !Float.isNaN(meteosatPixelPos.y)) {
            System.out.println(tile);
            return true;
        }
        final GeoPos lrGeoPos = tileGeoCoding.getGeoPos(lrPixelPos, null);
        meteosatPixelPos = meteosatGeoCoding.getPixelPos(lrGeoPos, null);
        if (!Float.isNaN(meteosatPixelPos.x) && !Float.isNaN(meteosatPixelPos.y)) {
            System.out.println(tile);
            return true;
        }

        return false;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MeteosatMsaAlbedoTileExtractor.class);
        }
    }
}
