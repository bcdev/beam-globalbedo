package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.util.ProductUtils;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Operator to attach Geocoding to a MODIS tile product. Target product will be in Dimap format
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.albedo.modis.geocoding",
                  description = "Attaches Geocoding to a MODIS tile product.",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2012 by Brockmann Consult")
public class ModisTileAttachGeocodingOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "1200", description = "Number of horizontal/vertical pixels in tile")
    private int numPixels;

    @Override
    public void initialize() throws OperatorException {

        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        // copy everything from source to target product
        for (Band sourceBand : sourceProduct.getBands()) {
            ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, true);
        }
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);

        // determine and attach the geocoding
        final ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        final int tileIndex = modisTileCoordinates.findTileIndex(tile);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tile + "");
        }
        final double easting = modisTileCoordinates.getUpperLeftX(tileIndex);
        final double northing = modisTileCoordinates.getUpperLeftY(tileIndex);
        final int imageWidth = numPixels;
        final int imageHeight = numPixels;
        final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X *
                1.0*AlbedoInversionConstants.MODIS_TILE_WIDTH/imageWidth;
        final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y *
                1.0*AlbedoInversionConstants.MODIS_TILE_HEIGHT/imageHeight;
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING);
            final CrsGeoCoding geoCoding = new CrsGeoCoding(crs, imageWidth, imageHeight, easting, northing, pixelSizeX, pixelSizeY);
            targetProduct.setGeoCoding(geoCoding);
        } catch (Exception e) {
            throw new OperatorException("Cannot attach geocoding for tileName= ''" + tile + " : ", e);
        }

        setTargetProduct(targetProduct);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ModisTileAttachGeocodingOp.class);
        }
    }
}
