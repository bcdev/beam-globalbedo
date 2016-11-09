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

package org.esa.beam.globalbedo.mosaic;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A product reader for the internal mosaic products of GlobAlbedo.
 * NEW: QA4ECV BRDF/albedo products only, but now also for latitude interval subsets of globe
 *
 * @author OlafD
 */
public class GlobAlbedoQa4ecvMosaicProductReader extends AbstractProductReader {

    private static final String PRODUCT_TYPE = "QA4ECV_Mosaic";

    private final Pattern pattern;
    private MosaicDefinition mosaicDefinition;
    private MosaicGrid mosaicGrid;

    private int tileSize;
    private int horizontalTileStartIndex;
    private int horizontalTileEndIndex;

    protected GlobAlbedoQa4ecvMosaicProductReader(GlobAlbedoMosaicReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.pattern = Pattern.compile("h(\\d\\d)v(\\d\\d)");
        this.mosaicDefinition = new MosaicDefinition(MosaicConstants.NUM_H_TILES, MosaicConstants.NUM_V_TILES,
                MosaicConstants.MODIS_TILE_SIZE);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final int numHorizontalTiles = horizontalTileEndIndex - horizontalTileStartIndex + 1;
        mosaicDefinition = new MosaicDefinition(numHorizontalTiles, MosaicConstants.NUM_V_TILES, tileSize);

        final File inputFile = getInputFile();
        Set<MosaicTile> mosaicTiles = createMosaicTiles(inputFile);
        mosaicGrid = new MosaicGrid(mosaicDefinition, mosaicTiles);
        int width = mosaicDefinition.getWidth();
        int height = mosaicDefinition.getHeight();

        Product firstMosaicProduct = mosaicTiles.iterator().next().getProduct();

        final String productName = getProductName(inputFile);
        final Product product;
        if (productName != null) {
            product = new Product(productName, PRODUCT_TYPE, width, height);

            product.setPreferredTileSize(mosaicDefinition.getTileSize() / 4, mosaicDefinition.getTileSize() / 4);
            product.setFileLocation(inputFile);
            product.setStartTime(firstMosaicProduct.getStartTime());
            product.setEndTime(firstMosaicProduct.getEndTime());

            ModisTileGeoCoding crsGeoCoding = getMosaicGeocoding();
            product.setGeoCoding(crsGeoCoding);

            Band[] bands = firstMosaicProduct.getBands();
            for (Band srcBand : bands) {
                Band band = product.addBand(srcBand.getName(), srcBand.getDataType());
                ProductUtils.copyRasterDataNodeProperties(srcBand, band);
            }
            ProductUtils.copyMetadata(firstMosaicProduct, product);

            return product;
        } else {
            return null;
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {

        Rectangle sourceRect = new Rectangle(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight);
        Point[] indexes = mosaicGrid.getAffectedSourceTiles(sourceRect);
        for (Point index : indexes) {
            Rectangle tileBounds = mosaicGrid.getTileBounds(index.x, index.y);
            Rectangle toRead = sourceRect.intersection(tileBounds);
            if (toRead.isEmpty()) {
                continue;
            }
            MosaicTile mosaicTile = mosaicGrid.getMosaicTile(index.x, index.y);

            Rectangle toWrite = (Rectangle) toRead.clone();
            int wIndex;

            if (mosaicTile != null) {
                toRead.translate(-tileBounds.x, -tileBounds.y);
                Band band = mosaicTile.getProduct().getBand(destBand.getName());
                if (band != null) {
                    ProductData buffer = ProductData.createInstance(band.getDataType(), toRead.width * toRead.height);
                    band.readRasterData(toRead.x, toRead.y, toRead.width, toRead.height, buffer);

                    int rIndex = 0;
                    for (int y = toWrite.y - destOffsetY; y < toWrite.y + toWrite.height - destOffsetY; y++) {
                        for (int x = toWrite.x - destOffsetX; x < toWrite.x + toWrite.width - destOffsetX; x++) {
                            wIndex = x + y * destWidth;
                            destBuffer.setElemDoubleAt(wIndex, buffer.getElemDoubleAt(rIndex++));
                        }
                    }
                } else {
                    System.out.println("WARNING: band '" + destBand.getName() + "' not found in product '" +
                            mosaicTile.getProduct().getName() + "'.");
                    double nodataValue = destBand.getNoDataValue();
                    for (int y = toWrite.y - destOffsetY; y < toWrite.y + toWrite.height - destOffsetY; y++) {
                        for (int x = toWrite.x - destOffsetX; x < toWrite.x + toWrite.width - destOffsetX; x++) {
                            wIndex = x + y * destWidth;
                            destBuffer.setElemDoubleAt(wIndex, nodataValue);
                        }
                    }
                }
            } else {
                double nodataValue = destBand.getNoDataValue();
                for (int y = toWrite.y - destOffsetY; y < toWrite.y + toWrite.height - destOffsetY; y++) {
                    for (int x = toWrite.x - destOffsetX; x < toWrite.x + toWrite.width - destOffsetX; x++) {
                        wIndex = x + y * destWidth;
                        destBuffer.setElemDoubleAt(wIndex, nodataValue);
                    }
                }
            }
        }

    }

    @Override
    public void close() throws IOException {
        mosaicGrid.dispose();
        super.close();
    }

    private Set<MosaicTile> createMosaicTiles(File refFile) throws IOException {
        HashSet<MosaicTile> mosaicTiles = new HashSet<>();
        File refTileDir = refFile.getParentFile();
        MosaicTile mosaicTile = createMosaicTile(refFile);
        if (refTileDir == null && mosaicTile != null) {
            mosaicTiles.add(mosaicTile);
            return mosaicTiles;
        }
        File rootDir = null;
        if (refTileDir != null) {
            rootDir = refTileDir.getParentFile();
        }
        if (rootDir == null && mosaicTile != null) {
            mosaicTiles.add(mosaicTile);  // todo: unclear. check what this does.
            return mosaicTiles;
        }

        String regex = getMosaicFileRegex(refFile.getName());
        final File[] tileDirs;
        if (rootDir != null) {
            tileDirs = rootDir.listFiles(new TileDirFilter());
            if (tileDirs != null) {
                for (File tileDir : tileDirs) {
                    final File[] mosaicFiles = tileDir.listFiles(new MosaicFileFilter(regex));
                    if (mosaicFiles != null && mosaicFiles.length == 1) {
                        mosaicTile = createMosaicTile(mosaicFiles[0]);
                        mosaicTiles.add(mosaicTile);
                    } else {
                        // TODO error
                    }
                }
            }
        }
        return mosaicTiles;
    }

    private MosaicTile createMosaicTile(File file) {
        if (isTileToProcess(file.getName(), horizontalTileStartIndex, horizontalTileEndIndex)) {
            Matcher matcher = pattern.matcher(file.getName());
            boolean found = matcher.find();
            if (found) {
                int x = Integer.parseInt(matcher.group(1));
                int y = Integer.parseInt(matcher.group(2));
                int index = mosaicDefinition.calculateIndex(x, y);
                return new MosaicTile(x, y, index, file);
            } else {
                return null;
            }
        } else {
            return null;
        }

    }

    private String getProductName(File file) {
        String fileName = file.getName();
        Matcher matcher = pattern.matcher(fileName);
        boolean found = matcher.find();
        if (found) {
            return fileName.substring(0, matcher.start() - 1);
        } else {
            return null;
        }
    }

    private String getMosaicFileRegex(String filename) {
        return pattern.matcher(filename).replaceFirst("h\\\\d\\\\dv\\\\d\\\\d");
    }

    public static boolean isTileToProcess(String tileName, int startIndex, int endIndex) {
        final int hIndex = Integer.parseInt(tileName.substring(1, 3));
        return startIndex <= hIndex && hIndex <= endIndex;
    }

    private File getInputFile() throws IOException {
        final Object input = getInput();

        if (!(input instanceof String || input instanceof File)) {
            throw new IOException("Input object must either be a string or a file.");
        }
        return new File(String.valueOf(input));
    }

    private ModisTileGeoCoding getMosaicGeocoding() {
        final int downscalingFactor = MosaicConstants.MODIS_TILE_SIZE / tileSize;
        final double easting = MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_X;
        final String crsString = MosaicConstants.MODIS_SIN_PROJECTION_CRS_STRING;
        final double pixelSizeX = MosaicConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * downscalingFactor;
        final double pixelSizeY = MosaicConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * downscalingFactor;
        final double northing = MosaicConstants.MODIS_UPPER_LEFT_TILE_UPPER_LEFT_Y -
                horizontalTileStartIndex * pixelSizeY * tileSize;
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            return new ModisTileGeoCoding(crs, easting, northing, pixelSizeX, pixelSizeY);
        } catch (Exception e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot attach mosaic geocoding : ", e);
            return null;
        }
    }


    ///// setters //////
    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }

    public void setHorizontalTileStartIndex(int horizontalTileStartIndex) {
        this.horizontalTileStartIndex = horizontalTileStartIndex;
    }

    public void setHorizontalTileEndIndex(int horizontalTileEndIndex) {
        this.horizontalTileEndIndex = horizontalTileEndIndex;
    }


    ///// private classes //////
    private class TileDirFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && pattern.matcher(file.getName()).matches();
        }
    }

    private static class MosaicFileFilter implements FileFilter {

        private final Pattern pattern;

        MosaicFileFilter(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        @Override
        public boolean accept(File file) {
            final boolean patternMatches = pattern.matcher(file.getName()).matches();
            final boolean isFile = file.isFile();

            return isFile && patternMatches;
        }
    }
}