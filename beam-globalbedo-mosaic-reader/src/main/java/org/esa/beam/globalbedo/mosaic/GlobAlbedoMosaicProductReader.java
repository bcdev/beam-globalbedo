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
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.ProductUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A product reader for the internal mosaic products of GlobAlbedo.
 *
 * @author MarcoZ
 */
public class GlobAlbedoMosaicProductReader extends AbstractProductReader {

    private static final String PRODUCT_TYPE = "GlobAlbedo_Mosaic";

    private final Pattern pattern;
    private final MosaicDefinition mosaicDefinition;
    private MosaicGrid mosaicGrid;
    private boolean mosaicPriors;
    private boolean mosaicAdam;

    protected GlobAlbedoMosaicProductReader(GlobAlbedoMosaicReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.pattern = Pattern.compile("h(\\d\\d)v(\\d\\d)");
        this.mosaicDefinition = new MosaicDefinition(36, 18, 1200);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = getInputFile();
        Set<MosaicTile> mosaicTiles;
        if (mosaicPriors) {
            mosaicTiles = createPriorMosaicTiles(inputFile);
        } else if (mosaicAdam) {
            mosaicDefinition.setTileSize(120);
            mosaicTiles = createMosaicTiles(inputFile);
        } else {
            mosaicTiles = createMosaicTiles(inputFile);
        }
        mosaicGrid = new MosaicGrid(mosaicDefinition, mosaicTiles);
        int width = mosaicDefinition.getWidth();
        int height = mosaicDefinition.getHeight();

        Product firstMosaicProduct = mosaicTiles.iterator().next().getProduct();

        final String productName = getProductName(inputFile);
        final Product product = new Product(productName, PRODUCT_TYPE, width, height);
        product.setPreferredTileSize(mosaicDefinition.getTileSize() / 4, mosaicDefinition.getTileSize() / 4);
        product.setFileLocation(inputFile);
        product.setStartTime(firstMosaicProduct.getStartTime());
        product.setEndTime(firstMosaicProduct.getEndTime());

        if (firstMosaicProduct.getGeoCoding() != null && !mosaicPriors && !mosaicAdam) {
            addGeoCoding(firstMosaicProduct, product);
        } else {
            CrsGeoCoding crsGeoCoding = null;
            if (mosaicAdam) {
                crsGeoCoding = getMosaicGeocoding(10);
            } else if (mosaicPriors) {
                crsGeoCoding = getMosaicGeocoding(1);
            }
            product.setGeoCoding(crsGeoCoding);
        }

        Band[] bands = firstMosaicProduct.getBands();
        for (Band srcBand : bands) {
            Band band = product.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        }
        ProductUtils.copyMetadata(firstMosaicProduct, product);

        return product;
    }

    Set<MosaicTile> createMosaicTiles(File refFile) throws IOException {
        HashSet<MosaicTile> mosaicTiles = new HashSet<MosaicTile>();
        File refTileDir = refFile.getParentFile();
        if (refTileDir == null) {
            mosaicTiles.add(createMosaicTile(refFile));
            return mosaicTiles;
        }
        File rootDir = refTileDir.getParentFile();
        if (rootDir == null) {
            mosaicTiles.add(createMosaicTile(refFile));
            return mosaicTiles;
        }
        String regex = getMosaicFileRegex(refFile.getName());
        final File[] tileDirs = rootDir.listFiles(new TileDirFilter());
        for (File tileDir : tileDirs) {
            final File[] mosaicFiles = tileDir.listFiles(new MosaicFileFilter(regex, mosaicPriors));
            if (mosaicFiles.length == 1) {
                mosaicTiles.add(createMosaicTile(mosaicFiles[0]));
            } else {
                // TODO error
            }
        }
        return mosaicTiles;
    }

    Set<MosaicTile> createPriorMosaicTiles(File refFile) throws IOException {
        HashSet<MosaicTile> mosaicTiles = new HashSet<MosaicTile>();
        File refTileDir = refFile.getParentFile().getParentFile().getParentFile();
        if (refTileDir == null) {
            mosaicTiles.add(createMosaicTile(refFile));
            return mosaicTiles;
        }
        File rootDir = refTileDir.getParentFile();
        if (rootDir == null) {
            mosaicTiles.add(createMosaicTile(refFile));
            return mosaicTiles;
        }
        String regex = getMosaicFileRegex(refFile.getName());
        final File[] tileDirs = getPriorTileDirectories(rootDir.getAbsolutePath());
        for (File tileDir : tileDirs) {
            final File[] mosaicFiles = tileDir.listFiles(new MosaicFileFilter(regex, mosaicPriors));
            if (mosaicFiles.length == 1) {
                mosaicTiles.add(createMosaicTile(mosaicFiles[0]));
            } else {
                // TODO error
            }
        }
        return mosaicTiles;
    }

    public static File[] getPriorTileDirectories(String rootDirString) {
        final Pattern pattern = Pattern.compile("h(\\d\\d)v(\\d\\d)");
        FileFilter tileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && pattern.matcher(file.getName()).matches();
            }
        };

        File rootDir = new File(rootDirString);
        File[] priorRootDirs = rootDir.listFiles(tileFilter);
        File[] priorDirs = new File[priorRootDirs.length];
        int index = 0;
        for (File priorDir : priorRootDirs) {
            final String priorDirName = priorDir + File.separator +
                    "background" + File.separator +
                    "processed.p1.0.618034.p2.1.00000";
            priorDirs[index++] = new File(priorDirName);
        }
        return priorDirs;
    }

    public static File[] getAdamTileDirectories(String rootDirString) {
        final Pattern pattern = Pattern.compile("h(\\d\\d)v(\\d\\d)");
        FileFilter tileFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && pattern.matcher(file.getName()).matches();
            }
        };

        File rootDir = new File(rootDirString);
        return rootDir.listFiles(tileFilter);
    }

    public void setMosaicPriors(boolean mosaicPriors) {
        this.mosaicPriors = mosaicPriors;
    }

    public void setMosaicAdam(boolean mosaicAdam) {
        this.mosaicAdam = mosaicAdam;
    }

    String getProductName(File file) {
        String fileName = file.getName();
        Matcher matcher = pattern.matcher(fileName);
        matcher.find();
        return fileName.substring(0, matcher.start() - 1);
    }

    String getTileName(File file) {
        String fileName = file.getName();
        Matcher matcher = pattern.matcher(fileName);
        matcher.find();
        return fileName.substring(matcher.start(), matcher.start() + 6);
    }

    MosaicTile createMosaicTile(File file) {
        Matcher matcher = pattern.matcher(file.getName());
        matcher.find();
        int x = Integer.parseInt(matcher.group(1));
        int y = Integer.parseInt(matcher.group(2));
        int index = mosaicDefinition.calculateIndex(x, y);
        return new MosaicTile(x, y, index, file);

    }

    String getMosaicFileRegex(String filename) {
        return pattern.matcher(filename).replaceFirst("h\\\\d\\\\dv\\\\d\\\\d");
    }

    private void addGeoCoding(Product tileProduct, Product product) throws IOException {
        GeoCoding tileProductGeoCoding = tileProduct.getGeoCoding();
        CoordinateReferenceSystem mapCRS = tileProductGeoCoding.getMapCRS();

        DefaultEllipsoid ellipsoid = (DefaultEllipsoid) DefaultGeographicCRS.WGS84.getDatum().getEllipsoid();

        double lonExtendMeters = ellipsoid.orthodromicDistance(0.0, 0.0, 180.0, 0.0) * 2;
        int rasterWidth = product.getSceneRasterWidth();
        double easting = -lonExtendMeters / 2;
        double pixelSizeX = lonExtendMeters / rasterWidth;

        double latExtendMeters = ellipsoid.orthodromicDistance(0.0, 90.0, 0.0, -90);
        int rasterHeight = product.getSceneRasterHeight();
        double pixelSizeY = latExtendMeters / rasterHeight;
        double northing = latExtendMeters / 2;

        try {
            GeoCoding geocoding = new CrsGeoCoding(mapCRS,
                    rasterWidth,
                    rasterHeight,
                    easting,
                    northing,
                    pixelSizeX,
                    pixelSizeY);
            product.setGeoCoding(geocoding);
        } catch (Exception e) {
            throw new IOException("Cannot create GeoCoding: ", e);
        }
    }

    private static CrsGeoCoding getMosaicGeocoding(int downscalingFactor) {
        final double easting = AlbedoInversionConstants.UPPER_LEFT_TILE_UPPER_LEFT_X;
        final double northing = AlbedoInversionConstants.UPPER_LEFT_TILE_UPPER_LEFT_Y;
        final String crsString = AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING;
        final int imageWidth = AlbedoInversionConstants.MODIS_TILE_WIDTH *36 / downscalingFactor;
        final int imageHeight = AlbedoInversionConstants.MODIS_TILE_HEIGHT *18 / downscalingFactor;
        final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * downscalingFactor;
        final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * downscalingFactor;
        try {
            final CoordinateReferenceSystem crs = CRS.parseWKT(crsString);
            return new CrsGeoCoding(crs, imageWidth, imageHeight, easting, northing, pixelSizeX, pixelSizeY);
        } catch (Exception e) {
            throw new OperatorException("Cannot attach mosaic geocoding : ", e);
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

    private File getInputFile() throws IOException {
        final Object input = getInput();

        if (!(input instanceof String || input instanceof File)) {
            throw new IOException("Input object must either be a string or a file.");
        }
        return new File(String.valueOf(input));
    }

    private class TileDirFilter implements FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() && pattern.matcher(file.getName()).matches();
        }
    }

    private static class MosaicFileFilter implements FileFilter {

        private final Pattern pattern;
        private boolean mosaicPriors;

        private MosaicFileFilter(String regex, boolean mosaicPriors) {
            pattern = Pattern.compile(regex);
            this.mosaicPriors = mosaicPriors;
        }

        @Override
        public boolean accept(File file) {
            final boolean patternMatches = pattern.matcher(file.getName()).matches();
            final boolean isFile = file.isFile();

            boolean complete = true;
            if (patternMatches && isFile && mosaicPriors) {
                // check if binary file exists for give hdr file...
                final String fileRootPath = file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4);
                final String binFilePath = fileRootPath + ".bin";
                complete = new File(binFilePath).exists();
            }

            return isFile && patternMatches && complete;
        }
    }
}