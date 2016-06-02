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
import org.esa.beam.dataio.MeteosatGeoCoding;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.globalbedo.auxdata.MVIRI.MeteosatDiskTiles;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * Reads and reprojects a Meteosat MVIRI BRF product onto MODIS SIN tiles.
 *
 * CURRENTLY NOT USED!!
 */
@OperatorMetadata(alias = "ga.tile.meteosat",
        description = "Reads a Meteosat MVIRI BRF (spectral and broadband) disk product with standard Beam Netcdf reader, " +
                "attaches a Meteosat Geocoding, and reprojects to intersecting MODIS SIN tiles. ",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2016 by Brockmann Consult")
public class MeteosatTileExtractor extends Operator implements Output {

    private static final String METEOSAT_LAT_BAND_NAME = "lat";
    private static final String METEOSAT_LON_BAND_NAME = "lon";

    @SourceProduct
    private Product sourceProduct;

    @SourceProduct
    private Product latlonProduct;    // make sure externally that the correct matching latlon product is submitted

    @Parameter
    private String bbdrDir;

    @Parameter(defaultValue = "0")   // to define a subset of 'vertical stripes' of tiles
    protected int horizontalTileStartIndex;

    @Parameter(defaultValue = "35")   // to define a subset of 'vertical stripes' of tiles
    protected int horizontalTileEndIndex;

    private int parallelism;
    private ModisTileCoordinates tileCoordinates;
    private Product extendedSourceProduct;

    @Override
    public void initialize() throws OperatorException {
        if (!isLatlonProductMatching()) {
            throw new OperatorException("MeteosatTileExtractor: latlonProduct '" +
                                                latlonProduct.getName() +
                                                "' does not contain correct disk identifier of source product '" +
                                                sourceProduct.getName() + "'.");
        }

        if (sourceProduct.getPreferredTileSize() == null) {
            sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 45);
            System.out.println("adjusting tile size to: " + sourceProduct.getPreferredTileSize());
        }
        parallelism = JAI.getDefaultInstance().getTileScheduler().getParallelism();
        tileCoordinates = ModisTileCoordinates.getInstance();
        extendedSourceProduct = createExtendedSourceProduct();
        doExtract_executor();
        setTargetProduct(new Product("n", "d", 1, 1));
    }

    private boolean isLatlonProductMatching() {
        // checks if given source product and latlon product are on the same disk (000, 057 or 063)

        // we have product filenames:
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_C_BRF_EUMP_20050501000000.nc
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET5+MVIRI_063_C_BRF_EUMP_20070420000000.nc
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_057_C_BRF_EUMP_20070420000000.nc
        // and the latlon products:
        // MET_000_VIS01_LatLon.nc (000 products from 1982-2006)
        // MET_057_VIS01_LatLon.nc (057 products from 2006-2010)
        // MET_063_VIS01_LatLon.nc (063 products from 1998-2007)
        final boolean matches000Disk = sourceProduct.getName().contains("VIRI_C_BRF") &&
                latlonProduct.getName().equals("MET_000_VIS01_LatLon");
        final boolean matches057Disk = sourceProduct.getName().contains("VIRI_057_C_BRF") &&
                latlonProduct.getName().equals("MET_057_VIS01_LatLon");
        final boolean matches063Disk = sourceProduct.getName().contains("VIRI_063_C_BRF") &&
                latlonProduct.getName().equals("MET_063_VIS01_LatLon");

        return matches000Disk || matches057Disk || matches063Disk;
    }

    private String getDiskId() {
        // MET_000_VIS01_LatLon.nc
        // MET_057_VIS01_LatLon.nc
        // MET_063_VIS01_LatLon.nc
        return latlonProduct.getName().substring(4,7);
    }

    private boolean checkIfModisTileIntersectsMeteosatDisk(String tile) {
        MeteosatDiskTiles diskTiles = MeteosatDiskTiles.getInstance(getDiskId());
        for (int i = 0; i < diskTiles.getTileCount(); i++) {
            if (tile.equals(diskTiles.getTileName(i))) {
                return true;
            }
        }

        return false;
    }

    private Product createExtendedSourceProduct() {
        // creates an extended source product with lat/lon from latlonProduct and a MeteosatGeoCoding

        Product extendedSourceProduct = new Product(sourceProduct.getName(),
                                                    sourceProduct.getProductType(),
                                                    sourceProduct.getSceneRasterWidth(),
                                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(sourceProduct, extendedSourceProduct);
        ProductUtils.copyFlagCodings(sourceProduct, extendedSourceProduct);
        ProductUtils.copyFlagBands(sourceProduct, extendedSourceProduct, true);
        ProductUtils.copyMasks(sourceProduct, extendedSourceProduct);
        extendedSourceProduct.setStartTime(sourceProduct.getStartTime());
        extendedSourceProduct.setEndTime(sourceProduct.getEndTime());

        // copy all source bands
        for (Band band : sourceProduct.getBands()) {
            if (!extendedSourceProduct.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), sourceProduct, extendedSourceProduct, true);
                ProductUtils.copyRasterDataNodeProperties(band, extendedSourceProduct.getBand(band.getName()));
            }
        }
        // copy lat/lon bands
        // todo: do we need this??
        for (Band band : latlonProduct.getBands()) {
            if (!extendedSourceProduct.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), latlonProduct, extendedSourceProduct, true);
                ProductUtils.copyRasterDataNodeProperties(band, extendedSourceProduct.getBand(band.getName()));
            }
        }

        final Band latBand = latlonProduct.getBand(METEOSAT_LAT_BAND_NAME);
        latBand.setValidPixelExpression("lat != 90 && lon != 90");
        final Band lonBand = latlonProduct.getBand(METEOSAT_LON_BAND_NAME);
        lonBand.setValidPixelExpression("lat != 90 && lon != 90");
        try {
            extendedSourceProduct.setGeoCoding(new MeteosatGeoCoding(latBand, lonBand));
            return extendedSourceProduct;
        } catch (IOException e) {
            throw new OperatorException
                    ("Cannot attach Meteosat geocoding to target product: " + e.getMessage());
        }

    }

    private void doExtract_executor() {
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        ExecutorCompletionService<TileProduct> ecs = new ExecutorCompletionService<>(executorService);

        for (int index = 0; index < tileCoordinates.getTileCount(); index++) {
            final String tileName = tileCoordinates.getTileName(index);
            Callable<TileProduct> callable = new Callable<TileProduct>() {
                @Override
                public TileProduct call() throws Exception {
                    if (isTileToProcess(tileName)) {
                        Product reprojected = TileExtractor.reprojectToModisTile(extendedSourceProduct, tileName);
                        return new TileProduct(reprojected, tileName);
                    } else {
                        return null;
                    }
                }
            };
            ecs.submit(callable);
        }
        for (int i = 0; i < tileCoordinates.getTileCount(); i++) {
            try {
                Future<TileProduct> future = ecs.take();
                TileProduct tileProduct = future.get();
                if (tileProduct != null && tileProduct.product != null && isTileToProcess(tileProduct.tileName)) {
                    writeTileProduct(tileProduct.product, tileProduct.tileName);
                }
            } catch (Exception e) {
                throw new OperatorException(e);
            }
        }
        executorService.shutdown();
    }

    private boolean isTileToProcess(String tileName) {
        final int hIndex = Integer.parseInt(tileName.substring(1, 3));
        final boolean isTileInSelectedSubset = horizontalTileStartIndex <= hIndex && hIndex <= horizontalTileEndIndex;

        boolean tileIntersectsMeteosatDisk = false;
        if (isTileInSelectedSubset) {
            tileIntersectsMeteosatDisk = checkIfModisTileIntersectsMeteosatDisk(tileName);
        }

        return isTileInSelectedSubset && tileIntersectsMeteosatDisk;
    }

    private void writeTileProduct(Product product, String tileName) {
        File dir = new File(bbdrDir, tileName);
        dir.mkdirs();
        File file = new File(dir, sourceProduct.getName() + "_" + tileName + ".nc");
        WriteOp writeOp = new WriteOp(product, file, "NetCDF4-BEAM");
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    private static class TileProduct {
        private final Product product;
        private final String tileName;

        private TileProduct(Product product, String tileName) {
            this.product = product;
            this.tileName = tileName;
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MeteosatTileExtractor.class);
        }
    }
}
