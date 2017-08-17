package org.esa.beam.globalbedo.upscaling;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.globalbedo.mosaic.MosaicConstants;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

/**
 * Basis Operator for Level3 upscaling/mosaicing of various products
 *
 * @author olafd
 */
public abstract class GlobalbedoLevel3UpscaleBasisOp extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory")
    String gaRootDir;

    @Parameter(defaultValue = "2005", description = "Year")
    int year;

    @Parameter(defaultValue = "001", description = "Day of Year", interval = "[1,366]")
    int doy;

    @Parameter(defaultValue = "01", description = "MonthIndex", interval = "[1,12]")
    int monthIndex;

    @Parameter(defaultValue = "false", description = "True if monthly albedo to upscale")
    boolean isMonthlyAlbedo;

    @Parameter(defaultValue = "false", description = "If True, a 'Prior' product will be processed ('NR' product otherwise")
    boolean isPriors;

    @Parameter(defaultValue = "", description = "Prior root directory")
    String priorRootDir;

    @Parameter(valueSet = {"1", "2", "3"}, description = "Stage of priors to mosaic", defaultValue = "1")
    int priorStage;

    @Parameter(defaultValue = "false", description = "Compute snow priors")
    private boolean computeSnow;

    @Parameter(valueSet = {"SIN", "PC"},
            defaultValue = "SIN",
            description = "Final projection (Sinusoidal or Plate-Carree)")
    String reprojection;

    @Parameter(valueSet = {"1", "6", "10", "60"},
            description = "Scaling: 1/20deg: 1 (AVHRR/GEO), 6 (MODIS); 1/2deg: 10 (AVHRR/GEO), 60 (MODIS)",
            defaultValue = "60")
    int scaling;


    public static final String BRDF_ALBEDO_PRODUCT_LAT_NAME = "lat";  // lat name in BRDF of Albedo tile nc files
    public static final String BRDF_ALBEDO_PRODUCT_LON_NAME = "lon";  // lon name in BRDF of Albedo tile nc files

    Product reprojectedProduct;
    Product upscaledProduct;


    protected void setReprojectedProduct(Product mosaicProduct, int tileSize) {
        if (reprojection.equals("PC")) {
            ReprojectionOp reprojection = new ReprojectionOp();
            reprojection.setParameterDefaultValues();
            reprojection.setParameter("crs", MosaicConstants.WGS84_CODE);
            reprojection.setSourceProduct(mosaicProduct);
            reprojectedProduct = reprojection.getTargetProduct();
            reprojectedProduct.setPreferredTileSize(tileSize / 4, tileSize / 4);
        } else {
            reprojectedProduct = mosaicProduct;
        }
    }

    protected void setUpscaledProduct(Product mosaicProduct, int width, int height, int tileWidth, int tileHeight) {
        upscaledProduct = new Product(mosaicProduct.getName() + "_upscaled", "GA_UPSCALED", width, height);
        upscaledProduct.setStartTime(reprojectedProduct.getStartTime());
        upscaledProduct.setEndTime(reprojectedProduct.getEndTime());
        ProductUtils.copyMetadata(reprojectedProduct, upscaledProduct);
        upscaledProduct.setPreferredTileSize(tileWidth,tileHeight);
    }

    protected void attachUpscaleGeoCoding(Product mosaicProduct,
                                          double scaling,
                                          int width, int height,
                                          String reprojection) {
        if (reprojection.equals("PC")) {
            final AffineTransform modelTransform = ImageManager.getImageToModelTransform(reprojectedProduct.getGeoCoding());
            final double pixelSizeX = modelTransform.getScaleX();
            final double pixelSizeY = modelTransform.getScaleY();
            ReprojectionOp reprojectionUpscale = new ReprojectionOp();
            reprojectionUpscale.setParameterDefaultValues();
            reprojectionUpscale.setParameter("crs", MosaicConstants.WGS84_CODE);
            reprojectionUpscale.setParameter("pixelSizeX", pixelSizeX * scaling);
            reprojectionUpscale.setParameter("pixelSizeY", -pixelSizeY * scaling);
            reprojectionUpscale.setParameter("width", width);
            reprojectionUpscale.setParameter("height", height);
            reprojectionUpscale.setSourceProduct(reprojectedProduct);
            Product targetGeoCodingProduct = reprojectionUpscale.getTargetProduct();
            ProductUtils.copyGeoCoding(targetGeoCodingProduct, upscaledProduct);
        } else {
            final CoordinateReferenceSystem mapCRS = mosaicProduct.getGeoCoding().getMapCRS();
            try {
                final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * scaling;
                final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * scaling;
                ModisTileGeoCoding geoCoding = new ModisTileGeoCoding(mapCRS,
                                                                MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_X,
                                                                MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_Y,
                                                                pixelSizeX,
                                                                pixelSizeY);
                upscaledProduct.setGeoCoding(geoCoding);
            } catch (Exception e) {
                throw new OperatorException("Cannot attach geocoding for mosaic: ", e);
            }
        }
    }

    protected void attachQa4ecvUpscaleGeoCoding(Product mosaicProduct,
                                          double scaling,
                                          int hStartIndex, int vStartIndex,
                                          int width, int height,
                                          String reprojection) {
        if (reprojection.equals("PC")) {
            final CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
            final double pixelSizeX = 0.05; // todo
            final double pixelSizeY = 0.05;
            final double northing = 90.0 - 10.*vStartIndex;
            final double easting = -180.0 + 10.*hStartIndex;
            final CrsGeoCoding crsGeoCoding;
            try {
                crsGeoCoding = new CrsGeoCoding(crs, width, height, easting, northing, pixelSizeX, pixelSizeY);
                upscaledProduct.setGeoCoding(crsGeoCoding);
            } catch (Exception e) {
                e.printStackTrace();
                throw new OperatorException("Cannot attach geocoding for mosaic: ", e);
            }
        } else {
            final CoordinateReferenceSystem mapCRS = mosaicProduct.getGeoCoding().getMapCRS();
            try {
                final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * scaling;
                final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * scaling;
                ModisTileGeoCoding geoCoding = new ModisTileGeoCoding(mapCRS,
                                                                      MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_X,
                                                                      MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_Y,
                                                                      pixelSizeX,
                                                                      pixelSizeY);
                upscaledProduct.setGeoCoding(geoCoding);
            } catch (Exception e) {
                throw new OperatorException("Cannot attach geocoding for mosaic: ", e);
            }
        }
    }


    protected void computeNearest(Tile src, Tile target, Tile mask, double scaling) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                float sample = src.getSampleFloat((int) (x * scaling + scaling / 2), (int) (y * scaling + scaling / 2));
                float sampleMask = 1.0f;
                if (mask != null) {
                    sampleMask = mask.getSampleFloat((int) (x * scaling + scaling / 2), (int) (y * scaling + scaling / 2));
                }
                if (sample == 0.0 || sampleMask == 0.0 || Float.isNaN(sample)) {
                    sample = AlbedoInversionConstants.NO_DATA_VALUE;
                }
                target.setSample(x, y, sample);
            }
        }
    }

    protected void computeMajority(Tile src, Tile target, Tile mask, double scaling) {
        if (src != null && target != null && mask != null) {
            Rectangle targetRectangle = target.getRectangle();

            final PixelPos pixelPos = new PixelPos((int) (targetRectangle.x * scaling),
                                                   (int) ((targetRectangle.y + targetRectangle.height) * scaling));
            final GeoPos geoPos = reprojectedProduct.getGeoCoding().getGeoPos(pixelPos, null);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    Rectangle pixelSrc = new Rectangle((int) (x * scaling), (int) (y * scaling), (int) scaling, (int) scaling);
                    float max = -1;
                    for (int sy = pixelSrc.y; sy < pixelSrc.y + pixelSrc.height; sy++) {
                        for (int sx = pixelSrc.x; sx < pixelSrc.x + pixelSrc.width; sx++) {
                            max = Math.max(max, src.getSampleFloat(sx, sy));
                        }
                    }
                    final float sampleMask = mask.getSampleFloat((int) (x * scaling + scaling / 2), (int) (y * scaling + scaling / 2));
                    if (sampleMask > 0.0) {
                        target.setSample(x, y, max);
                    } else {
                        // south pole correction
                        if (reprojection.equals("PC") && geoPos.getLat() < -86.0) {
                            target.setSample(x, y, 0.0);
                        } else {
                            target.setSample(x, y, AlbedoInversionConstants.NO_DATA_VALUE);
                        }
                    }
                }
            }
        }
    }

    protected void computeLatLon(Tile latTile, Tile lonTile, Tile target) {
        // computes a simple lat/lon grid in given tile
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                float lon = -180.0f + 360.0f*(x+0.5f)/upscaledProduct.getSceneRasterWidth();
                float lat = 90.0f - 180.0f*(y+0.5f)/upscaledProduct.getSceneRasterHeight();
                latTile.setSample(x, y, lat);
                lonTile.setSample(x, y, lon);
            }
        }
    }

    protected boolean isLatLonBand(String bandName) {
        return bandName.equals(BRDF_ALBEDO_PRODUCT_LAT_NAME) || bandName.equals(BRDF_ALBEDO_PRODUCT_LON_NAME);
    }

    protected Map<String, Tile> getSourceTiles(Rectangle srcRect) {
        Band[] srcBands = reprojectedProduct.getBands();
        Map<String, Tile> srcTiles = new HashMap<>(srcBands.length);
        for (Band band : srcBands) {
            srcTiles.put(band.getName(), getSourceTile(band, srcRect));
        }
        return srcTiles;
    }

    protected Map<String, Tile> getTargetTiles(Map<Band, Tile> targetBandTiles) {
        Map<String, Tile> targetTiles = new HashMap<>();
        for (Map.Entry<Band, Tile> entry : targetBandTiles.entrySet()) {
            targetTiles.put(entry.getKey().getName(), entry.getValue());
        }
        return targetTiles;
    }

    protected boolean hasValidPixel(Tile dataMask, double noDataValue) {
        Rectangle rect = dataMask.getRectangle();
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                double sample = dataMask.getSampleDouble(x, y);
                if (sample != noDataValue && AlbedoInversionUtils.isValid(sample)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void applySouthPoleCorrection(Tile src, Tile target, Tile mask) {
        Rectangle targetRectangle = target.getRectangle();
//        final PixelPos pixelPos = new PixelPos(targetRectangle.x * scaling,
//                                               (targetRectangle.y + targetRectangle.height) * scaling);
        final PixelPos pixelPos = new PixelPos(targetRectangle.x * scaling, targetRectangle.y * scaling);
        final GeoPos geoPos = reprojectedProduct.getGeoCoding().getGeoPos(pixelPos, null);

        // correct for projection failures near south pole...
        if (reprojection.equals("PC") && geoPos.getLat() < -86.0) {
            float[][] correctedSampleWest = null;    // i.e. tile h17v17
            if (geoPos.getLon() < 0.0) {
                correctedSampleWest = correctFromWest(target);    // i.e. tile h17v17
            }
            float[][] correctedSampleEast = null;    // i.e. tile h18v17
            if (geoPos.getLon() >= 0.0) {
                correctedSampleEast = correctFromEast(target);    // i.e. tile h17v17
            }

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                checkForCancellation();
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    float sample = src.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                    final float sampleMask = mask.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                    if (sample == 0.0 || sampleMask == 0.0 || Float.isNaN(sample)) {
                        sample = AlbedoInversionConstants.NO_DATA_VALUE;
                    }
                    final int xCorr = x - targetRectangle.x;
                    final int yCorr = y - targetRectangle.y;
                    if (geoPos.getLon() < 0.0) {
                        if (correctedSampleWest != null) {
                            if (correctedSampleWest[xCorr][yCorr] != 0.0) {
                                target.setSample(x, y, correctedSampleWest[xCorr][yCorr]);
                            } else {
                                target.setSample(x, y, sample);
                            }
                        }
                    } else {
                        if (correctedSampleEast != null) {
                            if (correctedSampleEast[xCorr][yCorr] != 0.0) {
                                target.setSample(x, y, correctedSampleEast[xCorr][yCorr]);
                            } else {
                                target.setSample(x, y, sample);
                            }
                        }
                    }
                }
            }
        }
    }

    protected float[][] correctFromWest(Tile src) {
        final Rectangle sourceRectangle = src.getRectangle();
        float[][] correctedSample = new float[sourceRectangle.width][sourceRectangle.height];
        for (int y = sourceRectangle.y; y < sourceRectangle.y + sourceRectangle.height; y++) {
            // go east direction
            int xIndex = sourceRectangle.x;
            float corrValue;
            float value = src.getSampleFloat(xIndex, y);
            while ((value == 0.0 || !AlbedoInversionUtils.isValid(value))
                    && xIndex < sourceRectangle.x + sourceRectangle.width - 1) {
                xIndex++;
                value = src.getSampleFloat(xIndex, y);
            }
            if (value == 0.0 || !AlbedoInversionUtils.isValid(value)) {
                // go north direction, we WILL find a non-zero value
                int yIndex = y;
                float value2 = src.getSampleFloat(xIndex, yIndex);
                while ((value2 == 0.0 || !AlbedoInversionUtils.isValid(value2))
                        && yIndex > sourceRectangle.y + 1) {
                    yIndex--;
                    value2 = src.getSampleFloat(xIndex, yIndex);
                }
                corrValue = value2;
            } else {
                corrValue = value;
            }
            for (int i = sourceRectangle.x; i <= xIndex; i++) {
                correctedSample[i - sourceRectangle.x][y - sourceRectangle.y] = corrValue;
            }
        }
        return correctedSample;
    }

    protected float[][] correctFromEast(Tile src) {
        final Rectangle sourceRectangle = src.getRectangle();
        float[][] correctedSample = new float[sourceRectangle.width][sourceRectangle.height];
        for (int y = sourceRectangle.y; y < sourceRectangle.y + sourceRectangle.height; y++) {
            // go west direction
            int xIndex = sourceRectangle.x + sourceRectangle.width - 1;
            float corrValue;
            float value = src.getSampleFloat(xIndex, y);
            while ((value == 0.0 || !AlbedoInversionUtils.isValid(value))
                    && xIndex > sourceRectangle.x) {
                xIndex--;
                value = src.getSampleFloat(xIndex, y);
            }
            if (value == 0.0 || !AlbedoInversionUtils.isValid(value)) {
                // go north direction, we WILL find a non-zero value
                int yIndex = y;
                float value2 = src.getSampleFloat(xIndex, yIndex);
                while ((value2 == 0.0 || !AlbedoInversionUtils.isValid(value2))
                        && yIndex > sourceRectangle.y + 1) {
                    yIndex--;
                    value2 = src.getSampleFloat(xIndex, yIndex);
                }
                corrValue = value2;
            } else {
                corrValue = value;
            }
            for (int i = sourceRectangle.x + sourceRectangle.width - 1; i >= xIndex; i--) {
                correctedSample[i - sourceRectangle.x][y - sourceRectangle.y] = corrValue;
            }
        }
        return correctedSample;
    }

}
