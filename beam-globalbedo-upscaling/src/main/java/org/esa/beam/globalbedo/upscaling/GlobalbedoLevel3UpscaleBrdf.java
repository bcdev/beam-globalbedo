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

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.mosaic.GlobAlbedoMosaicProductReader;
import org.esa.beam.globalbedo.mosaic.MosaicConstants;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.*;

/**
 * Reprojects and upscales the GlobAlbedo product
 * that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.
 *
 * @author MarcoZ
 */
@OperatorMetadata(
        alias = "ga.l3.upscale.brdf",
        authors = "Marco Zuehlke",
        copyright = "2011 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Reprojects and upscales the GlobAlbedo product \n" +
                " that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.")
public class GlobalbedoLevel3UpscaleBrdf extends GlobalbedoLevel3UpscaleBasisOp {

    @Parameter(valueSet = {"1200", "200"},
            description = "Input product tile size (default = 1200 (MODIS), 200 for AVHRR/GEO",
            defaultValue = "1200")
    private int inputProductTileSize;

    @Parameter(valueSet = {"1", "6", "10", "60"},
            description = "Scaling: 1/20deg: 1 (AVHRR/GEO), 6 (MODIS); 1/2deg: 10 (AVHRR/GEO), 60 (MODIS)",
            defaultValue = "60")
    private int scaling;

    @Parameter(defaultValue = "NETCDF", valueSet = {"DIMAP", "NETCDF"},
            description = "Input format, either DIMAP or NETCDF.")
    private String inputFormat;

    @Parameter(defaultValue = "Merge", valueSet = {"Merge", "Snow", "NoSnow"},
            description = "Input BRDF type, either Merge, Snow or NoSnow.")
    private String inputType;

    @Parameter(defaultValue = "Inversion", description = "Name of inversion (BRDF) subdirectory.")
    private String brdfSubdirName;

    @Parameter(defaultValue = "false",
            description = "If set, not all bands (i.e. no uncertainties) are written. Set to true to save computation time and disk space.")
    private boolean reducedOutput;


    @TargetProduct
    private Product targetProduct;

    private String[] brdfModelBandNames;
    private String[][] brdfUncertaintyBandNames;
    private double matrixNodataValue;
    private Band entropyBand;
    private Band relEntropyBand;

    @Override
    public void initialize() throws OperatorException {

//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1);

        final File refTile = findRefTile();
        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No BRDF files for mosaicing found.");
        }

        final ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        if (productReader instanceof GlobAlbedoMosaicProductReader) {
            ((GlobAlbedoMosaicProductReader) productReader).setTileSize(inputProductTileSize);
        }
        brdfModelBandNames = IOUtils.getInversionParameterBandNames();
        brdfUncertaintyBandNames = IOUtils.getInversionUncertaintyBandNames();

        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(refTile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }

        setReprojectedProduct(mosaicProduct, inputProductTileSize);

        final int width = inputProductTileSize * MosaicConstants.NUM_H_TILES / scaling;
        final int height = inputProductTileSize * MosaicConstants.NUM_V_TILES / scaling;
        final int tileWidth = inputProductTileSize / scaling / 2;
        final int tileHeight = inputProductTileSize / scaling / 2;

        setUpscaledProduct(mosaicProduct, width, height, tileWidth, tileHeight);

        for (Band srcBand : reprojectedProduct.getBands()) {
            addTargetBand(srcBand);
        }

        attachUpscaleGeoCoding(mosaicProduct, scaling, width, height, reprojectToPlateCarre);

        matrixNodataValue = reprojectedProduct.getBand(brdfModelBandNames[0]).getNoDataValue();
        entropyBand = reprojectedProduct.getBand(INV_ENTROPY_BAND_NAME);

        targetProduct = upscaledProduct;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle(targetRect.x * scaling,
                                          targetRect.y * scaling,
                                          targetRect.width * scaling,
                                          targetRect.height * scaling);
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);
        final Tile entropyTile = getSourceTile(entropyBand, srcRect);
        boolean tileValid = hasValidPixel(entropyTile, entropyBand.getNoDataValue());
//        BeamLogManager.getSystemLogger().log(Level.INFO, "targetRect/hasValidPixels: " +
//                targetRect.x + "," + targetRect.y + " // " + tileValid);
        if (entropyTile != null && tileValid) {
            Map<String, Tile> srcTiles = getSourceTiles(srcRect);

            computeNearestBrdf(srcTiles.get(INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME), targetTiles.get(INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            computeNearestBrdf(srcTiles.get(INV_GOODNESS_OF_FIT_BAND_NAME), targetTiles.get(INV_GOODNESS_OF_FIT_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            computeNearestBrdf(srcTiles.get(INV_ENTROPY_BAND_NAME), targetTiles.get(INV_ENTROPY_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            if (!reducedOutput) {
                computeUncertainty(srcTiles, targetTiles, targetRect);
            }
            computeNearestBrdf(srcTiles.get(INV_REL_ENTROPY_BAND_NAME), targetTiles.get(INV_REL_ENTROPY_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            if (inputType.equals("Merge")) {
                computeNearestBrdf(srcTiles.get(MERGE_PROPORTION_NSAMPLES_BAND_NAME), targetTiles.get(MERGE_PROPORTION_NSAMPLES_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            }
            String closestSampleBandName = ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME;
            if (srcTiles.get(closestSampleBandName) == null || targetTiles.get(closestSampleBandName) == null) {
                // the originally processed files before 'netcdf polishing' have this band name:
                closestSampleBandName = ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME_OLD;
            }
            computeMajorityBrdf(srcTiles.get(closestSampleBandName), targetTiles.get(closestSampleBandName), srcTiles.get(INV_ENTROPY_BAND_NAME));
        } else {
            BeamLogManager.getSystemLogger().log(Level.FINEST, "targetRect/hasValidPixels: " +
                    targetRect.x + "," + targetRect.y + " //  NO VALID PIXELS");
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

    private void addTargetBand(Band srcBand) {
        boolean addBand = true;
        if (reducedOutput) {
            // to save resources, do not write uncertainties, relEntropy, proportionNSamples, daysToClosestSample
            for (String[] brdfUncertaintyBandName : brdfUncertaintyBandNames) {
                for (int j = 0; j < brdfUncertaintyBandNames[0].length; j++) {
                    if (srcBand.getName().equals(brdfUncertaintyBandName[j])) {
                        addBand = false;
                        break;
                    }
                }
            }
//            if (srcBand.getName().equals(INV_REL_ENTROPY_BAND_NAME) ||
//                    srcBand.getName().equals(MERGE_PROPORTION_NSAMPLES_BAND_NAME) ||
//                    srcBand.getName().equals(ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME)) {
//                addBand = false;
//            }
        }
        if (addBand) {
            Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        }
    }

    private File findRefTile() {

        final FilenameFilter mergeFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String expectedFilenameExt = inputFormat.equals("DIMAP") ? ".dim" : ".nc";
                String expectedFilename;

                switch (inputType) {
                    case "Merge":
                        expectedFilename = "GlobAlbedo.brdf.merge." + year +
                                IOUtils.getDoyString(doy) + "." + dir.getName() + expectedFilenameExt;
                        break;
                    case "Snow":
                        expectedFilename = "GlobAlbedo.brdf." + year +
                                IOUtils.getDoyString(doy) + "." + dir.getName() + ".Snow" + expectedFilenameExt;
                        break;
                    default:
                        expectedFilename = "GlobAlbedo.brdf." + year +
                                IOUtils.getDoyString(doy) + "." + dir.getName() + ".NoSnow" + expectedFilenameExt;
                        break;
                }

                return name.equals(expectedFilename);
            }
        };

        String mergeDirString = gaRootDir + File.separator + brdfSubdirName + File.separator + inputType + File.separator + year;

        final File[] mergeFiles = IOUtils.getTileDirectories(mergeDirString);
        if (mergeFiles != null && mergeFiles.length > 0) {
            for (File mergeFile : mergeFiles) {
                File[] tileFiles = mergeFile.listFiles(mergeFilter);
                if (tileFiles != null && tileFiles.length > 0) {
                    for (File tileFile : tileFiles) {
                        if (tileFile.exists()) {
                            return tileFile;
                        }
                    }
                }
            }
        }
        return null;
    }

//    private boolean hasValidPixel(Tile entropy) {
//        double noDataValue = entropyBand.getNoDataValue();
//        Rectangle rect = entropy.getRectangle();
//        for (int y = rect.y; y < rect.y + rect.height; y++) {
//            for (int x = rect.x; x < rect.x + rect.width; x++) {
//                double sample = entropy.getSampleDouble(x, y);
//                if (sample != 0.0 && sample != noDataValue && AlbedoInversionUtils.isValid(sample)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    private void computeUncertainty(Map<String, Tile> srcTiles, Map<String, Tile> targetTiles, Rectangle targetRectangle) {
        Tile[] fSrcTiles = getFTiles(srcTiles);
        Tile[][] mSrcTiles = getMTiles(srcTiles);
        Tile[] fTargetTiles = getFTiles(targetTiles);
        Tile[][] mTargetTiles = getMTiles(targetTiles);

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                Rectangle pixelSrc = new Rectangle(x * scaling, y * scaling, scaling, scaling);
                Matrix fmInvsum = new Matrix(1, 9);
                Matrix mInvSum = new Matrix(9, 9);
                for (int sy = pixelSrc.y; sy < pixelSrc.y + pixelSrc.height; sy++) {
                    for (int sx = pixelSrc.x; sx < pixelSrc.x + pixelSrc.width; sx++) {
                        Matrix m = getM(mSrcTiles, sx, sy);
                        if (m.lu().isNonsingular() && m.det() != 0.0 && containsData(m)) {
                            Matrix mInv = m.inverse();
                            Matrix f = getF(fSrcTiles, sx, sy);
                            Matrix fmInv = f.times(mInv);
                            fmInvsum.plusEquals(fmInv);
                            mInvSum.plusEquals(mInv);
                        }
                    }
                }
                if (mInvSum.lu().isNonsingular() && mInvSum.det() != 0.0 && containsData(mInvSum)) {
                    Matrix mt = mInvSum.inverse();
                    setM(mt, mTargetTiles, x, y);
                    Matrix ft = fmInvsum.times(mt);
                    setF(ft, fTargetTiles, x, y);
                } else {
                    setMNodata(mTargetTiles, x, y);
                    setFNodata(fTargetTiles, x, y);
                }
            }
        }
    }

    private static boolean containsData(Matrix m) {
        double[][] array = m.getArray();
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                if (array[i][j] != 0.0) {
                    return true;
                }
            }
        }
        return false;
    }

    private Tile[] getFTiles(Map<String, Tile> tileMap) {
        Tile[] fTiles = new Tile[9];
        for (int i = 0; i < brdfModelBandNames.length; i++) {
            fTiles[i] = tileMap.get(brdfModelBandNames[i]);
        }
        return fTiles;
    }

    private Tile[][] getMTiles(Map<String, Tile> tileMap) {
        Tile[][] mTiles = new Tile[9][9];
        for (int i = 0; i < brdfUncertaintyBandNames.length; i++) {
            for (int j = i; j < brdfUncertaintyBandNames[i].length; j++) {
                Tile tile = tileMap.get(brdfUncertaintyBandNames[i][j]);
                mTiles[i][j] = tile;
                if (i != j) {
                    mTiles[j][i] = tile;
                }
            }
        }
        return mTiles;
    }

    private Matrix getM(Tile[][] mTiles, int x, int y) {
        Matrix m = new Matrix(9, 9);
        for (int i = 0; i < mTiles.length; i++) {
            for (int j = 0; j < mTiles[i].length; j++) {
                double value = mTiles[i][j].getSampleDouble(x, y);
                m.set(i, j, value);
            }
        }
        return m;
    }

    private void setM(Matrix m, Tile[][] mTiles, int x, int y) {
        for (int i = 0; i < mTiles.length; i++) {
            for (int j = i; j < mTiles[i].length; j++) {
                double v = m.get(i, j);
                mTiles[i][j].setSample(x, y, v);
            }
        }
    }

    private void setMNodata(Tile[][] mTiles, int x, int y) {
        for (int i = 0; i < mTiles.length; i++) {
            for (int j = i; j < mTiles[i].length; j++) {
                mTiles[i][j].setSample(x, y, matrixNodataValue);
            }
        }
    }

    private Matrix getF(Tile[] fTiles, int x, int y) {
        Matrix f = new Matrix(1, 9);
        for (int i = 0; i < fTiles.length; i++) {
            f.set(0, i, fTiles[i].getSampleDouble(x, y));
        }
        return f;
    }

    private void setF(Matrix f, Tile[] fTiles, int x, int y) {
        for (int i = 0; i < fTiles.length; i++) {
            double v = f.get(0, i);
            fTiles[i].setSample(x, y, v);
        }
    }

    private void setFNodata(Tile[] fTiles, int x, int y) {
        for (Tile fTile : fTiles) {
            fTile.setSample(x, y, matrixNodataValue);
        }
    }

    private void computeNearestBrdf(Tile src, Tile target, Tile mask) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
//                final float sampleMask = mask.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
//                if (sample == 0.0 || sampleMask == 0.0 || !AlbedoInversionUtils.isValid(sample)) {
                float sample = src.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                if (AlbedoInversionUtils.isValid(sample)) {
                    target.setSample(x, y, sample);
                } else {
                    target.setSample(x, y, AlbedoInversionConstants.NO_DATA_VALUE);
                }
            }
        }
    }

    private void computeMajorityBrdf(Tile src, Tile target, Tile mask) {
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
//                if (sampleMask > 0.0) {
                if (AlbedoInversionUtils.isValid(sampleMask)) {
                    target.setSample(x, y, max);
                } else {
                    target.setSample(x, y, AlbedoInversionConstants.NO_DATA_VALUE);
                }
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleBrdf.class);
        }
    }
}