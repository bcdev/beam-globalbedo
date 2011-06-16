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
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
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
        alias = "ga.upscale",
        authors = "Marco Zuehlke",
        copyright = "2011 Brockmann Consult",
        version = "0.1",
        internal = true,
        description = "Reprojects and upscales the GlobAlbedo product \n" +
                " that exist in multiple Sinusoidal tiles into a 0.5 or 0.05 degree  Plate Caree product.")
public class Upscale extends Operator {

    private static final String WGS84_CODE = "EPSG:4326";
    private static final int TILE_SIZE = 1200;

    @Parameter
    private File tile;

    @Parameter(valueSet = {"6", "60"}, defaultValue = "60")
    private int scaling;

    @TargetProduct
    private Product targetProduct;

    private Product reprojectedProduct;
    private String[] brdfModelBandNames;
    private String[][] uncertaintyBandNames;
    private double matrixNodataValue;

    @Override
    public void initialize() throws OperatorException {
        if (tile == null || !tile.exists()) {
            throw new OperatorException("No tile paramter given.");
        }
        ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        Product mosaicProduct;
        try {
            mosaicProduct = productReader.readProductNodes(tile, null);
        } catch (IOException e) {
            throw new OperatorException("Could not read mosaic product: '" + tile.getAbsolutePath() + "'. " + e.getMessage(), e);
        }
        ReprojectionOp reprojection = new ReprojectionOp();
        reprojection.setParameter("crs", WGS84_CODE);
        reprojection.setSourceProduct(mosaicProduct);
        reprojectedProduct = reprojection.getTargetProduct();
        reprojectedProduct.setPreferredTileSize(TILE_SIZE / 4, TILE_SIZE / 4);

        /////////////////////////////////////////////////////////////////////////////////////////////////
        SubsetOp subsetOp = new SubsetOp();
        subsetOp.setSourceProduct(reprojectedProduct);
        subsetOp.setRegion(new Rectangle(18* TILE_SIZE, 4* TILE_SIZE, 2* TILE_SIZE, 1* TILE_SIZE));
        reprojectedProduct = subsetOp.getTargetProduct();
        /////////////////////////////////////////////////////////////////////////////////////////////////

        int width = reprojectedProduct.getSceneRasterWidth() / scaling;
        int height = reprojectedProduct.getSceneRasterHeight() / scaling;
        Product upscaledProduct = new Product(mosaicProduct.getName()+"_upscaled", "GA_UPSCALED", width, height);
        for (Band srcBand : reprojectedProduct.getBands()) {
            Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        }
        upscaledProduct.setStartTime(reprojectedProduct.getStartTime());
        upscaledProduct.setEndTime(reprojectedProduct.getEndTime());
        ProductUtils.copyMetadata(reprojectedProduct, upscaledProduct);
        upscaledProduct.setPreferredTileSize(TILE_SIZE / scaling / 4, TILE_SIZE / scaling / 4);


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

        brdfModelBandNames = IOUtils.getInversionParameterBandNames();
        matrixNodataValue = upscaledProduct.getBand(brdfModelBandNames[0]).getNoDataValue();
        uncertaintyBandNames = IOUtils.getInversionUncertaintyBandNames();

        targetProduct = upscaledProduct;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        System.out.println("targetRectangle = " + targetRectangle);
        Rectangle srcRect = new Rectangle(targetRectangle.x * scaling,
                                          targetRectangle.y * scaling,
                                          targetRectangle.width * scaling,
                                          targetRectangle.height * scaling);
        Band[] srcBands = reprojectedProduct.getBands();
        Map<String, Tile> srcTiles = new HashMap<String, Tile>(srcBands.length);
        for (Band band : srcBands) {
            srcTiles.put(band.getName(), getSourceTile(band, srcRect));
        }
        Map<String, Tile> targetTiles = new HashMap<String, Tile>(srcBands.length);
        for (Map.Entry<Band, Tile> entry : targetBandTiles.entrySet()) {
            targetTiles.put(entry.getKey().getName(), entry.getValue());
        }
        computeUncertainty(srcTiles, targetTiles, targetRectangle);

        computeMajority(AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, srcTiles, targetTiles);

        computeNearest(AlbedoInversionConstants.INV_ENTROPY_BAND_NAME, srcTiles, targetTiles);
        computeNearest(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME, srcTiles, targetTiles);
        computeNearest(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME, srcTiles, targetTiles);
        computeNearest(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME, srcTiles, targetTiles);
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
                        if (containsData(m)) {
                            Matrix mInv = m.inverse();
                            Matrix f = getF(fSrcTiles, sx ,sy);
                            Matrix fmInv = f.times(mInv);
                            fmInvsum.plusEquals(fmInv);
                            mInvSum.plusEquals(mInv);
                        }
                    }
                }
                if (containsData(mInvSum)) {
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
        for (int i = 0; i < fTiles.length; i++) {
            fTiles[i].setSample(x, y, matrixNodataValue);
        }
    }

    private void computeNearest(String bandName, Map<String, Tile> srcTiles, Map<String, Tile> targetTiles) {
        computeNearest(srcTiles.get(bandName), targetTiles.get(bandName));
    }

    private void computeNearest(Tile src, Tile target) {
        Rectangle targetRectangle = target.getRectangle();
        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                float sample = src.getSampleFloat(x * scaling + scaling / 2, y * scaling + scaling / 2);
                target.setSample(x, y, sample);
            }
        }
    }

    private void computeMajority(String bandName, Map<String, Tile> srcTiles, Map<String, Tile> targetTiles) {
        computeMajority(srcTiles.get(bandName), targetTiles.get(bandName));
    }

    private void computeMajority(Tile src, Tile target) {
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
                target.setSample(x, y, max);
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Upscale.class);
        }
    }
}
