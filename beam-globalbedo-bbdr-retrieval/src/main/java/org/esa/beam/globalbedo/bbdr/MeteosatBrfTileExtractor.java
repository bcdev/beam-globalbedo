package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.dataio.MeteosatGeoCoding;
import org.esa.beam.framework.datamodel.*;
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

import java.io.IOException;

/**
 * Reads a Meteosat MVIRI BRF disk product, attaches a Meteosat Geocoding, and reprojects to specified MODIS SIN tile.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.brf.meteosat",
        description = "Reads a Meteosat MVIRI BRF (spectral and broadband) disk product with standard Beam Netcdf reader, " +
                "attaches a Meteosat Geocoding, and reprojects to specified MODIS SIN tile. " +
                "Suitable lat/lon bands must be passed as parameters.",
        authors = "Olaf Danne",
        internal = true,
        version = "1.0",
        copyright = "(c) 2016 by Brockmann Consult")
public class MeteosatBrfTileExtractor extends Operator {
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

        // todo: add angles
        Band szaBand = targetProductOrigProj.addBand("SZA", ProductData.TYPE_FLOAT32);
        Band vzaBand = targetProductOrigProj.addBand("VZA", ProductData.TYPE_FLOAT32);
        Band relaziBand = targetProductOrigProj.addBand("RAA", ProductData.TYPE_FLOAT32);

        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();


        final Band latBand = latlonProduct.getBand(latBandName);
        latBand.setValidPixelExpression("lat != 90 && lon != 90");
        final Band lonBand = latlonProduct.getBand(lonBandName);
        lonBand.setValidPixelExpression("lat != 90 && lon != 90");
        try {
            final MeteosatGeoCoding meteosatGeoCoding = new MeteosatGeoCoding(latBand, lonBand, regionID);
            targetProductOrigProj.setGeoCoding(meteosatGeoCoding);

            for (int i=0; i<modisTileCoordinates.getTileCount(); i++) {
                final String tileName = modisTileCoordinates.getTileName(i);
                checkIfModisTileIntersectsMeteosatDisk(tileName, meteosatGeoCoding);
            }

            // now reproject onto given MODIS SIN tile...
            if (checkIfModisTileIntersectsMeteosatDisk(tile, meteosatGeoCoding)) {
                return TileExtractor.reprojectToModisTile(targetProductOrigProj, tile);
            } else {
                throw new OperatorException
                        ("Tile '" + tile + "' has no ontersection with Meteosat disk.");
            }
        } catch (IOException e) {
            throw new OperatorException
                    ("Cannot attach Meteosat geocoding to target product: " + e.getMessage());
        }

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
            super(MeteosatBrfTileExtractor.class);
        }
    }
}
