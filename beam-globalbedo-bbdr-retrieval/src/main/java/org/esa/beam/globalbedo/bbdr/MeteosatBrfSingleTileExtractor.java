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
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.util.ProductUtils;

import java.io.IOException;

/**
 * Reads a Meteosat MVIRI/SEVIRI BRF disk product, attaches a Meteosat Geocoding, and reprojects to specified MODIS SIN tile.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.brf.meteosat",
        description = "Reads a Meteosat MVIRI/SEVIRI BRF (spectral and broadband) disk product with standard Beam Netcdf reader, " +
                "attaches a Meteosat Geocoding, and reprojects to specified MODIS SIN tile. " +
                "Suitable lat/lon bands must be passed as parameters.",
        authors = "Olaf Danne",
        internal = true,
        version = "1.0",
        copyright = "(c) 2016 by Brockmann Consult")
public class MeteosatBrfSingleTileExtractor extends Operator {
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

    @Parameter(defaultValue = "6.0",
            valueSet = {"0.5", "1.0", "2.0", "4.0", "6.0", "10.0", "12.0", "20.0", "60.0"},
            description = "Scale factor with regard to MODIS default 1200x1200. Values > 1.0 reduce product size.")
    protected double modisTileScaleFactor;

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
                checkIfModisTileIntersectsMeteosatDisk(tileName, meteosatGeoCoding, modisTileScaleFactor);
            }

            // now reproject onto given MODIS SIN tile...
            if (checkIfModisTileIntersectsMeteosatDisk(tile, meteosatGeoCoding, modisTileScaleFactor)) {
                return TileExtractor.reprojectToModisTile(targetProductOrigProj, tile, modisTileScaleFactor);
            } else {
                throw new OperatorException
                        ("Tile '" + tile + "' has no ontersection with Meteosat disk.");
            }
        } catch (IOException e) {
            throw new OperatorException
                    ("Cannot attach Meteosat geocoding to target product: " + e.getMessage());
        }

    }

    static boolean checkIfModisTileIntersectsMeteosatDisk(String tile, MeteosatGeoCoding meteosatGeoCoding, double scaleFactor) {
        final ModisTileGeoCoding tileGeoCoding = IOUtils.getSinusoidalTileGeocoding(tile, scaleFactor);

        final float startPixelX = 0.5f;
        final float startPixelY = 0.5f;
        final float endPixelX = (float) (0.5 + AlbedoInversionConstants.MODIS_TILE_WIDTH / scaleFactor);
        final float endPixelY = (float) (0.5 + AlbedoInversionConstants.MODIS_TILE_HEIGHT / scaleFactor);
        final PixelPos ulPixelPos = new PixelPos(startPixelX, startPixelY);
        final PixelPos urPixelPos = new PixelPos(endPixelX, startPixelY);
        final PixelPos llPixelPos = new PixelPos(startPixelX, endPixelY);
        final PixelPos lrPixelPos = new PixelPos(endPixelX, endPixelY);

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
            super(MeteosatBrfSingleTileExtractor.class);
        }
    }
}
