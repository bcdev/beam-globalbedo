package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.BorderExtender;
import java.awt.*;

/**
 * Daily accumulation part (new implementation in QA4ECV).
 * The daily accumulations are still written to binary files.
 * This prototype only creates a 'dummy' target product which shall not be written to disk.
 * Performance is ~25% better than original version used in GA processing.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l3.outlierremoval",
        description = "Provides daily accumulation of single BBDR observations (new improved prototype)",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class OutlierRetrievalOp extends Operator {

    @SourceProduct(description = "BBDR source product")
    private Product sourceProduct;

    @SourceProduct(description = "BBDR source product")
    private Product priorProduct;

    @Parameter(defaultValue = "BRDF_Albedo_Parameters_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    @Parameter(defaultValue = "BRDF_Albedo_Parameters_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "4", description = "Outlier criterion threshold")
    private int outlierThresh;

    @Parameter(defaultValue = "6.0",
            valueSet = {"0.5", "1.0", "2.0", "4.0", "6.0", "10.0", "12.0", "20.0", "60.0"},
            description = "Scale factor with regard to MODIS default 1200x1200. Values > 1.0 reduce product size. For AVHRR/GEO set to 6.0")
    protected double modisTileScaleFactor;

    private Tile kvolVisTile;
    private Tile kvolNirTile;
    private Tile kvolSwTile;
    private Tile kgeoVisTile;
    private Tile kgeoNirTile;
    private Tile kgeoSwTile;

    private Tile[] priorVisAvrTiles;
    private Tile[] priorVisSdTiles;
    private Tile[] priorNirAvrTiles;
    private Tile[] priorNirSdTiles;
    private Tile[] priorSwAvrTiles;
    private Tile[] priorSwSdTiles;

    @Override
    public void initialize() throws OperatorException {

        final int sourceProductWidth = (int) (AlbedoInversionConstants.MODIS_TILE_WIDTH / modisTileScaleFactor);
        final int sourceProductHeight = (int) (AlbedoInversionConstants.MODIS_TILE_HEIGHT / modisTileScaleFactor);
        final Rectangle sourceRect = new Rectangle(0, 0, sourceProductWidth, sourceProductHeight);

        kvolVisTile = getSourceTile(sourceProduct.getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_VIS_NAME), sourceRect);
        kvolNirTile = getSourceTile(sourceProduct.getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_NIR_NAME), sourceRect);
        kvolSwTile = getSourceTile(sourceProduct.getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_SW_NAME), sourceRect);
        kgeoVisTile = getSourceTile(sourceProduct.getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_VIS_NAME), sourceRect);
        kgeoNirTile = getSourceTile(sourceProduct.getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_NIR_NAME), sourceRect);
        kgeoSwTile = getSourceTile(sourceProduct.getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_SW_NAME), sourceRect);

        priorVisAvrTiles = new Tile[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        priorVisSdTiles = new Tile[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        priorNirAvrTiles = new Tile[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        priorNirSdTiles = new Tile[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        priorSwAvrTiles = new Tile[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        priorSwSdTiles = new Tile[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            priorVisAvrTiles[i] = getSourceTile(priorProduct.getBand(priorMeanBandNamePrefix + "vis_f" + i + "_avr"), sourceRect);
            priorVisSdTiles[i] = getSourceTile(priorProduct.getBand(priorSdBandNamePrefix + "vis_f" + i + "_sd"), sourceRect);
            priorNirAvrTiles[i] = getSourceTile(priorProduct.getBand(priorMeanBandNamePrefix + "nir_f" + i + "_avr"), sourceRect);
            priorNirSdTiles[i] = getSourceTile(priorProduct.getBand(priorSdBandNamePrefix + "nir_f" + i + "_sd"), sourceRect);
            priorSwAvrTiles[i] = getSourceTile(priorProduct.getBand(priorMeanBandNamePrefix + "shortwave_f" + i + "_avr"), sourceRect);
            priorSwSdTiles[i] = getSourceTile(priorProduct.getBand(priorSdBandNamePrefix + "shortwave_f" + i + "_sd"), sourceRect);
        }

        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    private Product createTargetProduct() {
        Product targetProduct = new Product(sourceProduct.getName(),
                                            sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        targetProduct.addBand(AlbedoInversionConstants.BBDR_BB_VIS_NAME, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(AlbedoInversionConstants.BBDR_BB_NIR_NAME, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(AlbedoInversionConstants.BBDR_BB_SW_NAME, ProductData.TYPE_FLOAT32);

        // copy all bands except BBDR:
        for (Band band : sourceProduct.getBands()) {
            if (!targetProduct.containsBand(band.getName()) && !isBbdrBand(band.getName())) {
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

        for (Band band : targetProduct.getBands()) {
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        return targetProduct;
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        RasterDataNode sourceRaster = sourceProduct.getRasterDataNode(targetBand.getName());
        Rectangle rectangle = targetTile.getRectangle();
        Tile sourceTile = getSourceTile(sourceRaster, rectangle, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        Tile[] priorAvrTiles = getPriorAvrTiles(targetBand.getName());
        Tile[] priorSdTiles = getPriorSdTiles(targetBand.getName());
        float[] priorParms = new float[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        float[] priorSd = new float[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        Tile kvolTile = getKvolTile(targetBand.getName());
        Tile kgeoTile = getKgeoTile(targetBand.getName());

        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                final float bbdrValue = sourceTile.getSampleFloat(x, y);
                final float kvolValue = kvolTile.getSampleFloat(x, y);
                final float kgeoValue = kgeoTile.getSampleFloat(x, y);
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    priorParms[i] = priorAvrTiles[i].getSampleFloat(x, y);
                    priorSd[i] = priorSdTiles[i].getSampleFloat(x, y);
                }
                float outlierDelta =
                        checkForOutliers(bbdrValue, priorParms, priorSd, kvolValue, kgeoValue);
                if (outlierDelta > outlierThresh) {
                    targetTile.setSample(x, y, Float.NaN);
                } else {
                    targetTile.setSample(x, y, bbdrValue);
                }
//                targetTile.setSample(x, y, outlierDelta); // test!
            }
        }
    }

    static float checkForOutliers(float bbdrValue, float[] priorParms, float[] priorSd, float kvol, float kgeo) {
        //        ==================
        //        outlier pseudocode
        //        ==================
        //
        //
        //        cloud filtering
        //        ------------------
        //                assume we have obs avhrr
        //
        //        prior params = [f0,f1,f2]
        //        prior sd        = [sd0,sd1,sd2]
        //
        //
        //        fwd kernels = [1, k1, k2]
        //        for the AVHRR angles
        //
        //        operations:
        //        =========
        //
        //        # dot product -> scalar output
        //        obs prediction = (prior params) dot (fwd kernels)
        final float obsPrediction = priorParms[0] + priorParms[1]*kvol + priorParms[2]*kgeo;

        //
        //
        //        # scaled difference
        //        delta = (  (obs prediction) - (obs avhrr)) / (prior sd)
        //
        final float sdMean = (priorSd[0] + priorSd[1] + priorSd[2])/3.0f;   // todo: clarify with PL
        return Math.abs((obsPrediction - bbdrValue)/sdMean);
    }

    private Tile[] getPriorAvrTiles(String bbChannel) {
        if (bbChannel.endsWith("VIS")) {
            return priorVisAvrTiles;
        } else if (bbChannel.endsWith("NIR")) {
            return priorNirAvrTiles;
        }  else if (bbChannel.endsWith("SW")) {
            return priorSwAvrTiles;
        }
        return null;
    }

    private Tile[] getPriorSdTiles(String bbChannel) {
        if (bbChannel.endsWith("VIS")) {
            return priorVisSdTiles;
        } else if (bbChannel.endsWith("NIR")) {
            return priorNirSdTiles;
        }  else if (bbChannel.endsWith("SW")) {
            return priorSwSdTiles;
        }
        return null;
    }

    private Tile getKvolTile(String bbChannel) {
        if (bbChannel.endsWith("VIS")) {
            return kvolVisTile;
        } else if (bbChannel.endsWith("NIR")) {
            return kvolNirTile;
        }  else if (bbChannel.endsWith("SW")) {
            return kvolSwTile;
        }
        return null;
    }

    private Tile getKgeoTile(String bbChannel) {
        if (bbChannel.endsWith("VIS")) {
            return kgeoVisTile;
        } else if (bbChannel.endsWith("NIR")) {
            return kgeoNirTile;
        }  else if (bbChannel.endsWith("SW")) {
            return kgeoSwTile;
        }
        return null;
    }

    private boolean isBbdrBand(String bandName) {
        return bandName.equals(AlbedoInversionConstants.BBDR_BB_VIS_NAME) ||
                bandName.equals(AlbedoInversionConstants.BBDR_BB_NIR_NAME) ||
                bandName.equals(AlbedoInversionConstants.BBDR_BB_SW_NAME);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OutlierRetrievalOp.class);
        }
    }


}
