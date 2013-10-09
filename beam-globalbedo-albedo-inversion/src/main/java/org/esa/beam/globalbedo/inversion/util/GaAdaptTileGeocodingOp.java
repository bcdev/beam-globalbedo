package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.datamodel.*;
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
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 09.10.13
 * Time: 11:23
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.adaptgeocoding",
                  description = "Adapts misaligned MODIS geocoding.")
public class GaAdaptTileGeocodingOp extends Operator {

    @Parameter(defaultValue = "", description = "MODIS tile")
    private String tile;

    @SourceProduct
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {

        if (tile == null || tile.length() != 6) {
            throw new OperatorException("Invalid tile name - exiting.");
        }

        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    private Product createTargetProduct() {
        Product targetProduct = new Product(sourceProduct.getName(),
                                            sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());

        GeoCoding geoCoding = getModisTileGeocoding(tile);
        targetProduct.setGeoCoding(geoCoding);

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        // copy all bands
        for (Band band : sourceProduct.getBands()) {
            if (!targetProduct.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
                ProductUtils.copyRasterDataNodeProperties(band, targetProduct.getBand(band.getName()));
            }
        }
        // tie point grids...
        for (TiePointGrid tpg : sourceProduct.getTiePointGrids()) {
            if (!targetProduct.containsTiePointGrid(tpg.getName())) {
                ProductUtils.copyTiePointGrid(tpg.getName(), sourceProduct, targetProduct);
            }
        }


        return targetProduct;
    }

    private static CrsGeoCoding getModisTileGeocoding(String tile) {
        // todo: after testing, adapt public method in IOUtils accordingly
        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        int tileIndex = modisTileCoordinates.findTileIndex(tile);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tile + "");
        }
        final double easting = modisTileCoordinates.getUpperLeftX(tileIndex);
        final double northing = modisTileCoordinates.getUpperLeftY(tileIndex);
        final double referencePixelX =  0.0;
        final double referencePixelY =  0.0;
        final String crsString = AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING;
        final int imageWidth = AlbedoInversionConstants.MODIS_TILE_WIDTH;
        final int imageHeight = AlbedoInversionConstants.MODIS_TILE_HEIGHT;
        final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X;
        final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y;
        CrsGeoCoding geoCoding;
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            geoCoding = new CrsGeoCoding(crs,
                                         imageWidth, imageHeight,
                                         easting, northing,
                                         pixelSizeX, pixelSizeY,
                                         referencePixelX, referencePixelY);
        } catch (Exception e) {
            throw new OperatorException("Cannot attach geocoding for tileName= ''" + tile + " : ", e);
        }
        return geoCoding;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GaAdaptTileGeocodingOp.class);
        }
    }
}
