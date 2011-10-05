/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.upscaling;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reprojects and upscales the GlobAlbedo product
 * that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.
 *
 * @author MarcoZ
 */
@OperatorMetadata(
        alias = "ga.l3.upscale.albedo",
        authors = "Marco Zuehlke",
        copyright = "2011 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Reprojects and upscales the GlobAlbedo product \n" +
                " that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.")
public class GlobalbedoLevel3UpscaleAlbedo extends Operator {

    private static final String WGS84_CODE = "EPSG:4326";
    private static final int TILE_SIZE = 1200;

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "001", description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "01", description = "MonthIndex", interval = "[1,12]")
    private int monthIndex;

    @Parameter(valueSet = {"5", "60"}, description = "Scaling (5 = 5km, 60 = 60km resolution", defaultValue = "60")
    private int scaling;

    @Parameter(defaultValue = "false", description = "True if monthly albedo to upscale")
    private boolean isMonthlyAlbedo;

    @Parameter(defaultValue = "true", description = "If True product will be reprojected")
    private boolean reprojectToPlateCarre;


    @TargetProduct
    private Product targetProduct;

    private Product reprojectedProduct;

    private File refTile;

    private String[] dhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private Band relEntropyBand;

    @Override
    public void initialize() throws OperatorException {

        refTile = findRefTile();

        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No albedo files for mosaicing found.");
        }
        ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(refTile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }
        if (reprojectToPlateCarre) {
            ReprojectionOp reprojection = new ReprojectionOp();
            reprojection.setParameter("crs", WGS84_CODE);
            reprojection.setSourceProduct(mosaicProduct);
            reprojectedProduct = reprojection.getTargetProduct();
            reprojectedProduct.setPreferredTileSize(TILE_SIZE / 4, TILE_SIZE / 4);
        } else {
            reprojectedProduct = mosaicProduct;
        }

        int width = reprojectedProduct.getSceneRasterWidth() / scaling;
        int height = reprojectedProduct.getSceneRasterHeight() / scaling;
        Product upscaledProduct = new Product(mosaicProduct.getName() + "_upscaled", "GA_UPSCALED", width, height);
        for (Band srcBand : reprojectedProduct.getBands()) {
            Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        }
        upscaledProduct.setStartTime(reprojectedProduct.getStartTime());
        upscaledProduct.setEndTime(reprojectedProduct.getEndTime());
        ProductUtils.copyMetadata(reprojectedProduct, upscaledProduct);
        upscaledProduct.setPreferredTileSize(TILE_SIZE / scaling / 4, TILE_SIZE / scaling / 4);

        if (reprojectToPlateCarre) {
            final AffineTransform modelTransform = ImageManager.getImageToModelTransform(reprojectedProduct.getGeoCoding());
            final double pixelSizeX = modelTransform.getScaleX();
            final double pixelSizeY = modelTransform.getScaleY();
            ReprojectionOp reprojectionUpscaleGeoCoding = new ReprojectionOp();
            reprojectionUpscaleGeoCoding.setParameter("crs", WGS84_CODE);
            reprojectionUpscaleGeoCoding.setParameter("pixelSizeX", pixelSizeX * scaling);
            reprojectionUpscaleGeoCoding.setParameter("pixelSizeY", -pixelSizeY * scaling);
            reprojectionUpscaleGeoCoding.setParameter("width", width);
            reprojectionUpscaleGeoCoding.setParameter("height", height);
            reprojectionUpscaleGeoCoding.setSourceProduct(reprojectedProduct);
            Product targetGeoCodingProduct = reprojectionUpscaleGeoCoding.getTargetProduct();
            ProductUtils.copyGeoCoding(targetGeoCodingProduct, upscaledProduct);
        } else {
            ProductUtils.copyGeoCoding(mosaicProduct, upscaledProduct);
        }

        dhrBandNames = IOUtils.getAlbedoDhrBandNames();
        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
        bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();

        relEntropyBand = reprojectedProduct.getBand(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME);

        targetProduct = upscaledProduct;
    }

    private File findRefTile() {

        final FilenameFilter albedoFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String expectedFilename;
                String expectedFilenameOld;
                if (isMonthlyAlbedo) {
                    expectedFilename = "GlobAlbedo.albedo." + year + IOUtils.getMonthString(monthIndex) + "." + dir.getName() + ".dim";
                    expectedFilenameOld = "GlobAlbedo." + year + IOUtils.getMonthString(monthIndex) + "." + dir.getName() + ".dim";
                } else {
                    expectedFilename = "GlobAlbedo.albedo." + year + IOUtils.getDoyString(doy) + "." + dir.getName() + ".dim";
                    expectedFilenameOld = "GlobAlbedo." + year + IOUtils.getDoyString(doy) + "." + dir.getName() + ".dim";
                }
                return (name.equals(expectedFilename) || name.equals(expectedFilenameOld));
            }
        };

        String albedoDirString;
        if (isMonthlyAlbedo) {
            albedoDirString = gaRootDir + File.separator + "MonthlyAlbedo";
        } else {
            albedoDirString = gaRootDir + File.separator + "Albedo";
        }
        final File[] albedoFiles = IOUtils.getTileDirectories(albedoDirString);
        for (File albedoFile : albedoFiles) {
            File[] tileFiles = albedoFile.listFiles(albedoFilter);
            for (File tileFile : tileFiles) {
                if (tileFile.exists()) {
                    return tileFile;
                }
            }
        }
        return null;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
//        System.out.println("targetRectangle = " + targetRect);
        Rectangle srcRect = new Rectangle(targetRect.x * scaling,
                targetRect.y * scaling,
                targetRect.width * scaling,
                targetRect.height * scaling);
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);
        if (hasValidPixel(getSourceTile(relEntropyBand, srcRect))) {
            Map<String, Tile> srcTiles = getSourceTiles(srcRect);

            for (int i = 0; i < dhrBandNames.length; i++) {
                computeNearest(srcTiles.get(dhrBandNames[i]), targetTiles.get(dhrBandNames[i]),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            for (int i = 0; i < bhrBandNames.length; i++) {
                computeNearest(srcTiles.get(bhrBandNames[i]), targetTiles.get(bhrBandNames[i]),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            for (int i = 0; i < dhrSigmaBandNames.length; i++) {
                computeNearest(srcTiles.get(dhrSigmaBandNames[i]), targetTiles.get(dhrSigmaBandNames[i]),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            for (int i = 0; i < bhrSigmaBandNames.length; i++) {
                computeNearest(srcTiles.get(bhrSigmaBandNames[i]), targetTiles.get(bhrSigmaBandNames[i]),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }

            computeMajority(srcTiles.get(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME),
                    targetTiles.get(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            computeNearest(srcTiles.get(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME),
                    targetTiles.get(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            computeNearest(srcTiles.get(AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME),
                    targetTiles.get(AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            computeNearest(srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME),
                    targetTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            if (!isMonthlyAlbedo) {
                computeNearest(srcTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME),
                        targetTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME),
                        srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            computeNearest(srcTiles.get(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME),
                    targetTiles.get(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME),
                    srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
        } else {
            for (Map.Entry<String, Tile> tileEntry : targetTiles.entrySet()) {
                checkForCancellation();

                Tile targetTile = tileEntry.getValue();
                String bandName = tileEntry.getKey();
                double noDataValue = getTargetProduct().getBand(bandName).getNoDataValue();
                for (int y = targetRect.y; y < targetRect.y + targetRect.height; y++) {
                    for (int x = targetRect.x; x < targetRect.x + targetRect.width; x++) {
                        targetTile.setSample(x, y, noDataValue);
                    }
                }
            }
        }
    }

    private Map<String, Tile> getSourceTiles(Rectangle srcRect) {
        Band[] srcBands = reprojectedProduct.getBands();
        Map<String, Tile> srcTiles = new HashMap<String, Tile>(srcBands.length);
        for (Band band : srcBands) {
            srcTiles.put(band.getName(), getSourceTile(band, srcRect));
        }
        return srcTiles;
    }

    private Map<String, Tile> getTargetTiles(Map<Band, Tile> targetBandTiles) {
        Map<String, Tile> targetTiles = new HashMap<String, Tile>();
        for (Map.Entry<Band, Tile> entry : targetBandTiles.entrySet()) {
            targetTiles.put(entry.getKey().getName(), entry.getValue());
        }
        return targetTiles;
    }

    private boolean hasValidPixel(Tile entropy) {
        double noDataValue = relEntropyBand.getNoDataValue();
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

    private void computeNearest(Tile src, Tile target, Tile mask) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                float sample = src.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                final float sampleMask = mask.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                if (sample == 0.0 || sampleMask == 0.0 || Float.isNaN(sample)) {
                    sample = Float.NaN;
                }
                target.setSample(x, y, sample);
            }
        }
    }

    private void computeMajority(Tile src, Tile target, Tile mask) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                Rectangle pixelSrc = new Rectangle(x * scaling, y * scaling, scaling, scaling);
                int max = -1;
                for (int sy = pixelSrc.y; sy < pixelSrc.y + pixelSrc.height; sy++) {
                    for (int sx = pixelSrc.x; sx < pixelSrc.x + pixelSrc.width; sx++) {
                        max = Math.max(max, src.getSampleInt(sx, sy));
                    }
                }
                final float sampleMask = mask.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                if (sampleMask > 0.0) {
                    target.setSample(x, y, max);
                } else {
                    target.setSample(x, y, Float.NaN);
                }
            }
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleAlbedo.class);
        }
    }
}
