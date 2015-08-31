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

import javax.media.jai.JAI;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;

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

    @Parameter(valueSet = {"5", "6", "30", "60"},
               description = "Scaling (5 = 1/24deg, 6 = 1/20deg, 30 = 1/4deg, 60 = 1/2deg resolution", defaultValue = "60")
    private int scaling;

    @Parameter(defaultValue = "DIMAP", valueSet = {"DIMAP", "NETCDF"},
               description = "Input format, either DIMAP or NETCDF.")
    private String inputFormat;

    @Parameter(defaultValue = "Merge", valueSet = {"Merge", "Snow", "NoSnow"},
               description = "Input BRDF type, either Merge, Snow or NoSnow.")
    private String inputType;


    @TargetProduct
    private Product targetProduct;

    private String[] brdfModelBandNames;
    private String[][] uncertaintyBandNames;
    private double matrixNodataValue;
    private Band entropyBand;
    private Band relEntropyBand;
    private Band weightedNumSamplesBand;
    private Band mergeProportionNsamplesBand;
    private Band godnessOfFitBand;
    private Band closestSampleBand;
    private int height;

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

        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(refTile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + refTile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }

        setReprojectedProduct(mosaicProduct, MosaicConstants.MODIS_TILE_SIZE);

        final int width = MosaicConstants.MODIS_TILE_SIZE * MosaicConstants.NUM_H_TILES / scaling;
        final int height = MosaicConstants.MODIS_TILE_SIZE * MosaicConstants.NUM_V_TILES / scaling;
        final int tileWidth = MosaicConstants.MODIS_TILE_SIZE / scaling / 2;
        final int tileHeight = MosaicConstants.MODIS_TILE_SIZE / scaling / 2;

        setUpscaledProduct(mosaicProduct, width, height, tileWidth, tileHeight);

        for (Band srcBand : reprojectedProduct.getBands()) {
            Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        }

        attachUpscaleGeoCoding(mosaicProduct, scaling, width, height, reprojectToPlateCarre);

        brdfModelBandNames = IOUtils.getInversionParameterBandNames();
        matrixNodataValue = upscaledProduct.getBand(brdfModelBandNames[0]).getNoDataValue();
        uncertaintyBandNames = IOUtils.getInversionUncertaintyBandNames();
        entropyBand = reprojectedProduct.getBand(INV_ENTROPY_BAND_NAME);
        relEntropyBand = reprojectedProduct.getBand(INV_REL_ENTROPY_BAND_NAME);
        weightedNumSamplesBand = reprojectedProduct.getBand(INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME);
        mergeProportionNsamplesBand = reprojectedProduct.getBand(MERGE_PROPORTION_NSAMPLES_BAND_NAME);
        godnessOfFitBand = reprojectedProduct.getBand(INV_GOODNESS_OF_FIT_BAND_NAME);
        closestSampleBand = reprojectedProduct.getBand(ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME);

        targetProduct = upscaledProduct;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle(targetRect.x * scaling,
                                          targetRect.y * scaling,
                                          targetRect.width * scaling,
                                          targetRect.height * scaling);
//        System.out.println("calling computeTileStack: targetRect = " + targetRect);
//        System.out.println("calling computeTileStack: srcRect    = " + srcRect);
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);
        if (hasValidPixel(getSourceTile(entropyBand, srcRect))) {
            Map<String, Tile> srcTiles = getSourceTiles(srcRect);

            computeUncertainty(srcTiles, targetTiles, targetRect);
            computeNearest(srcTiles.get(INV_ENTROPY_BAND_NAME), targetTiles.get(INV_ENTROPY_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            computeNearest(srcTiles.get(INV_REL_ENTROPY_BAND_NAME), targetTiles.get(INV_REL_ENTROPY_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            computeNearest(srcTiles.get(INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME), targetTiles.get(INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            computeNearest(srcTiles.get(INV_GOODNESS_OF_FIT_BAND_NAME), targetTiles.get(INV_GOODNESS_OF_FIT_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            if (inputType.equals("Merge")) {
                computeNearest(srcTiles.get(MERGE_PROPORTION_NSAMPLES_BAND_NAME), targetTiles.get(MERGE_PROPORTION_NSAMPLES_BAND_NAME), srcTiles.get(INV_ENTROPY_BAND_NAME));
            }

            String closestSampleBandName = ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME;
            if (srcTiles.get(closestSampleBandName) == null || targetTiles.get(closestSampleBandName) == null) {
                // the originally processed files before 'netcdf polishing' have this band name
                closestSampleBandName = ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME_OLD;
            }
            computeMajority(srcTiles.get(closestSampleBandName), targetTiles.get(closestSampleBandName), srcTiles.get(INV_ENTROPY_BAND_NAME));
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

    private File findRefTile() {

        final FilenameFilter mergeFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String expectedFilenameExt = inputFormat.equals("DIMAP") ? ".dim" : ".nc";
                String expectedFilename;

                if (inputType.equals("Merge")) {
                    expectedFilename = "GlobAlbedo.brdf.merge." + year +
                            IOUtils.getDoyString(doy) + "." + dir.getName() + expectedFilenameExt;
                } else if (inputType.equals("Snow")) {
                    expectedFilename = "GlobAlbedo.brdf." + year +
                            IOUtils.getDoyString(doy) + "." + dir.getName() + ".Snow" + expectedFilenameExt;
                } else {
                    expectedFilename = "GlobAlbedo.brdf." + year +
                            IOUtils.getDoyString(doy) + "." + dir.getName() + ".NoSnow" + expectedFilenameExt;
                }

                return name.equals(expectedFilename);
            }
        };

        String mergeDirExt = "Inversion";
        String mergeDirString = gaRootDir + File.separator + mergeDirExt + File.separator + inputType + File.separator + year;

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

    private boolean hasValidPixel(Tile entropy) {
        double noDataValue = entropyBand.getNoDataValue();
        Rectangle rect = entropy.getRectangle();
        for (int y = rect.y; y < rect.y + rect.height; y++) {
            for (int x = rect.x; x < rect.x + rect.width; x++) {
                double sample = entropy.getSampleDouble(x, y);
                if (sample != 0.0 && sample != noDataValue && AlbedoInversionUtils.isValid(sample)) {
                    return true;
                }
            }
        }
        return false;
    }

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
                if (mInvSum.lu().isNonsingular() &&  mInvSum.det() != 0.0 && containsData(mInvSum)) {
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
        for (int i = 0; i < uncertaintyBandNames.length; i++) {
            for (int j = i; j < uncertaintyBandNames[i].length; j++) {
                Tile tile = tileMap.get(uncertaintyBandNames[i][j]);
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

    private void computeNearest(Tile src, Tile target, Tile mask) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                float sample = src.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                final float sampleMask = mask.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                if (sample == 0.0 || sampleMask == 0.0 || !AlbedoInversionUtils.isValid(sample)) {
                    sample = AlbedoInversionConstants.NO_DATA_VALUE;
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