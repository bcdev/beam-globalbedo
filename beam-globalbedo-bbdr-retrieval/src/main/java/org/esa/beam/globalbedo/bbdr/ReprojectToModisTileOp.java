/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.globalbedo.bbdr;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.JAI;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * Creates subprodcuts for all intersecting tiles
 */
@OperatorMetadata(alias = "ga.reproject.tile")
public class ReprojectToModisTileOp extends Operator implements Output {

    @SourceProduct
    private Product sourceProduct;

    @Parameter
    private String tileName;


    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct.getPreferredTileSize() == null) {
            sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 45);
            System.out.println("adjusting tile size to: " + sourceProduct.getPreferredTileSize());
        }
        setTargetProduct(reprojectToModisTile(sourceProduct, tileName));
    }

    public static Product reprojectToModisTile(Product bbdrProduct, String tileName) {
        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        int tileIndex = modisTileCoordinates.findTileIndex(tileName);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tileName + "");
        }
        double easting = modisTileCoordinates.getUpperLeftX(tileIndex);
        double northing = modisTileCoordinates.getUpperLeftY(tileIndex);

        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameterDefaultValues();
        repro.setParameter("easting", easting);
        repro.setParameter("northing", northing);
        repro.setParameter("crs", AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING);
        repro.setParameter("resampling", "Nearest");
        repro.setParameter("includeTiePointGrids", false);
        repro.setParameter("referencePixelX", 0.0);
        repro.setParameter("referencePixelY", 0.0);
        repro.setParameter("orientation", 0.0);
        repro.setParameter("pixelSizeX", 926.6254330558);
        repro.setParameter("pixelSizeY", 926.6254330558);
        repro.setParameter("width", BbdrConstants.MODIS_TILE_WIDTH);
        repro.setParameter("height", BbdrConstants.MODIS_TILE_HEIGHT);
        repro.setParameter("orthorectify", true);
        repro.setParameter("noDataValue", 0.0);
        repro.setSourceProduct(bbdrProduct);

        return repro.getTargetProduct();
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ReprojectToModisTileOp.class);
        }
    }
}
