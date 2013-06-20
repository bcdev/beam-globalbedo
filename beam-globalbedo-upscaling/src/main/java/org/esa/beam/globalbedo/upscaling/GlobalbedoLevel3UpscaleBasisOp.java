package org.esa.beam.globalbedo.upscaling;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.mosaic.MosaicConstants;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;
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

    @Parameter(defaultValue = "true", description = "If True product will be reprojected")
    boolean reprojectToPlateCarre;


    Product reprojectedProduct;
    Product upscaledProduct;


    protected void setReprojectedProduct(Product mosaicProduct, int tileSize) {
        if (reprojectToPlateCarre) {
            ReprojectionOp reprojection = new ReprojectionOp();
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
                                        boolean plateCarree) {
        if (plateCarree) {
            final AffineTransform modelTransform = ImageManager.getImageToModelTransform(reprojectedProduct.getGeoCoding());
            final double pixelSizeX = modelTransform.getScaleX();
            final double pixelSizeY = modelTransform.getScaleY();
            ReprojectionOp reprojectionUpscaleGeoCoding = new ReprojectionOp();
            reprojectionUpscaleGeoCoding.setParameter("crs", MosaicConstants.WGS84_CODE);
            reprojectionUpscaleGeoCoding.setParameter("pixelSizeX", pixelSizeX * scaling);
            reprojectionUpscaleGeoCoding.setParameter("pixelSizeY", -pixelSizeY * scaling);
            reprojectionUpscaleGeoCoding.setParameter("width", width);
            reprojectionUpscaleGeoCoding.setParameter("height", height);
            reprojectionUpscaleGeoCoding.setSourceProduct(reprojectedProduct);
            Product targetGeoCodingProduct = reprojectionUpscaleGeoCoding.getTargetProduct();
            ProductUtils.copyGeoCoding(targetGeoCodingProduct, upscaledProduct);
        } else {
            final CoordinateReferenceSystem mapCRS = mosaicProduct.getGeoCoding().getMapCRS();
            try {
                final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * scaling;
                final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * scaling;
                CrsGeoCoding geoCoding = new CrsGeoCoding(mapCRS, width, height,
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
                    sample = Float.NaN;
                }
                target.setSample(x, y, sample);
            }
        }
    }

    protected void computeMajority(Tile src, Tile target, Tile mask, double scaling) {
        Rectangle targetRectangle = target.getRectangle();

        final PixelPos pixelPos = new PixelPos((int) (targetRectangle.x * scaling),
                                               (int)((targetRectangle.y + targetRectangle.height) * scaling));
        final GeoPos geoPos = reprojectedProduct.getGeoCoding().getGeoPos(pixelPos, null);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                Rectangle pixelSrc = new Rectangle((int) (x * scaling), (int) (y * scaling), (int) scaling, (int) scaling);
                int max = -1;
                for (int sy = pixelSrc.y; sy < pixelSrc.y + pixelSrc.height; sy++) {
                    for (int sx = pixelSrc.x; sx < pixelSrc.x + pixelSrc.width; sx++) {
                        max = Math.max(max, src.getSampleInt(sx, sy));
                    }
                }
                final float sampleMask = mask.getSampleFloat((int) (x * scaling + scaling / 2), (int) (y * scaling + scaling / 2));
                if (sampleMask > 0.0) {
                    target.setSample(x, y, max);
                } else {
                    // south pole correction
                    if (reprojectToPlateCarre && geoPos.getLat() < -86.0) {
                        target.setSample(x, y, 0.0);
                    } else {
                        target.setSample(x, y, Float.NaN);
                    }
                }
            }
        }
    }

    protected Map<String, Tile> getSourceTiles(Rectangle srcRect) {
        Band[] srcBands = reprojectedProduct.getBands();
        Map<String, Tile> srcTiles = new HashMap<String, Tile>(srcBands.length);
        for (Band band : srcBands) {
            srcTiles.put(band.getName(), getSourceTile(band, srcRect));
        }
        return srcTiles;
    }

    protected Map<String, Tile> getTargetTiles(Map<Band, Tile> targetBandTiles) {
        Map<String, Tile> targetTiles = new HashMap<String, Tile>();
        for (Map.Entry<Band, Tile> entry : targetBandTiles.entrySet()) {
            targetTiles.put(entry.getKey().getName(), entry.getValue());
        }
        return targetTiles;
    }

    protected boolean hasValidPixel(Tile entropy, double noDataValue) {
        Rectangle rect = entropy.getRectangle();
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                double sample = entropy.getSampleDouble(x, y);
                if (sample != 0.0 && sample != noDataValue && !Double.isNaN(sample)) {
                    return true;
                }
            }
        }
        return false;
    }


}
