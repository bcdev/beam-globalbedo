package org.esa.beam.globalbedo.bbdr;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.dataio.MeteosatGeoCoding;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.util.ProductUtils;

import java.io.IOException;

/**
 * Reads a Meteosat MVIRI BRF disk product, attaches a Meteosat Geocoding, and reprojects to specified MODIS SIN tile.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.bbdr.meteosat",
        description = "Reads a Meteosat MVIRI BRF (spectral and broadband) disk product with standard Beam Netcdf reader, " +
                "attaches a Meteosat Geocoding, and reprojects to specified MODIS SIN tile. " +
                "Suitable lat/lon bands must be passed as parameters.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2014 by Brockmann Consult")
public class BbdrMeteosatOp extends Operator {
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

        final int tileIndex = ModisTileCoordinates.getInstance().findTileIndex(tile);
        final double upperLeftX = ModisTileCoordinates.getInstance().getUpperLeftX(tileIndex);
        final double upperLeftY = ModisTileCoordinates.getInstance().getUpperLeftY(tileIndex);
        final GeoPos geoPos =
                targetProductOrigProj.getGeoCoding().getGeoPos(new PixelPos((float) upperLeftX, (float) upperLeftY), null);

        // now reproject onto given MODIS SIN tile...
        return TileExtractor.reproject(targetProductOrigProj, tile);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(BbdrMeteosatOp.class);
        }
    }
}
