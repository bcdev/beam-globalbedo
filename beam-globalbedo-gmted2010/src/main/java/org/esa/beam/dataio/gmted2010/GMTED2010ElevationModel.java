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

package org.esa.beam.dataio.gmted2010;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.resamp.Resampling;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.esa.beam.dataio.gmted2010.GMTED2010ElevationModelDescriptor.*;

/**
 * The elevation model for GMTED2010, 30 arcs
 */
public class GMTED2010ElevationModel implements ElevationModel, Resampling.Raster {

    private final GMTED2010ElevationModelDescriptor descriptor;
    private final Resampling resampling;
    private final Resampling.Index resamplingIndex;
    private final Resampling.Raster resamplingRaster;
    private final ElevationTile[][] elevationTiles;

    public GMTED2010ElevationModel(GMTED2010ElevationModelDescriptor descriptor, Resampling resampling) throws IOException {
        Assert.notNull(descriptor, "descriptor");
        Assert.notNull(resampling, "resampling");
        this.descriptor = descriptor;
        this.resampling = resampling;
        this.resamplingIndex = resampling.createIndex();
        this.resamplingRaster = this;
        this.elevationTiles = new ElevationTile[NUM_X_TILES][NUM_Y_TILES];
    }

    @Override
    public ElevationModelDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Resampling getResampling() {
        return resampling;
    }

    @Override
    public int getWidth() {
        return RASTER_WIDTH;
    }

    @Override
    public int getHeight() {
        return RASTER_HEIGHT;
    }

    @Override
    public void dispose() {
        //'TODO
    }

    @Override
    public float getElevation(GeoPos geoPos) throws Exception {
        float pixelX = (geoPos.lon + 180.0f) / DEGREE_RES_X * TILE_WIDTH; // todo (nf) - consider 0.5
        float pixelY = RASTER_HEIGHT - (geoPos.lat + 90.0f) / DEGREE_RES_Y * TILE_HEIGHT; // todo (nf) - consider 0.5, y = (90 - lon) / DEGREE_RES * NUM_PIXELS_PER_TILE;
        final float elevation;
        synchronized (resampling) {
            resampling.computeIndex(pixelX, pixelY,
                                    RASTER_WIDTH,
                                    RASTER_HEIGHT,
                                    resamplingIndex);
            elevation = resampling.resample(resamplingRaster, resamplingIndex);
        }
        if (Float.isNaN(elevation)) {
            return descriptor.getNoDataValue();
        }
        return elevation;
    }

    @Override
    public float getSample(int pixelX, int pixelY) throws IOException {
        final int tileXIndex = pixelX / TILE_WIDTH;
        final int tileYIndex = pixelY / TILE_HEIGHT;
        final ElevationTile tile = getElevationTile(tileXIndex, tileYIndex);
        final int tileX = pixelX - tileXIndex * TILE_WIDTH;
        final int tileY = pixelY - tileYIndex * TILE_HEIGHT;
        final float sample = tile.getSample(tileX, tileY);
        if (sample == descriptor.getNoDataValue()) {
            return Float.NaN;
        }
        return sample;
    }

    private synchronized ElevationTile getElevationTile(final int lonIndex, final int latIndex) throws IOException {
        ElevationTile elevationTile = elevationTiles[lonIndex][latIndex];
        if (elevationTile == null) {
            elevationTile = createTile(lonIndex, latIndex);
            elevationTiles[lonIndex][latIndex] = elevationTile;
        }
        return elevationTile;
    }

    private ElevationTile createTile(int lonIndex, int latIndex) throws IOException {
        final ProductReaderPlugIn geotiffPlugIn = getGeoTiffReaderPlugIn();
        final ProductReader productReader = geotiffPlugIn.createReaderInstance();
        File tileFile = descriptor.getTileFile(lonIndex, latIndex);
        final Product product = productReader.readProductNodes(tileFile, null);
        return new ElevationTile(product);
    }

    private static ProductReaderPlugIn getGeoTiffReaderPlugIn() {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        final Iterator<ProductReaderPlugIn> readerPlugIns = plugInManager.getReaderPlugIns("GeoTIFF");
        return readerPlugIns.next();
    }
}
