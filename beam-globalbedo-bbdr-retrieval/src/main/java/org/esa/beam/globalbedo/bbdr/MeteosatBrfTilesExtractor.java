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
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.globalbedo.auxdata.MVIRI.MeteosatDiskTiles;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Reads and reprojects a Meteosat MVIRI BRF product onto MODIS SIN tiles and optionally converts to BBDRs.
 */
@OperatorMetadata(alias = "ga.tile.meteosat",
        description = "Reads a Meteosat MVIRI BRF (spectral and broadband) disk product with standard " +
                "Beam Netcdf reader, attaches a Meteosat Geocoding, reprojects to intersecting MODIS SIN tiles, " +
                "and optionally converts to BBDRs. ",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(c) 2016 by Brockmann Consult")
public class MeteosatBrfTilesExtractor extends Operator implements Output {
//public class MeteosatBrfTilesExtractor extends Operator {

    private static final String METEOSAT_LAT_BAND_NAME = "lat";
    private static final String METEOSAT_LON_BAND_NAME = "lon";

    @SourceProduct
    private Product sourceProduct;

    @Parameter(description = "Year")
    private int year;

    @Parameter(defaultValue = "", description = "MSSL AVHRR mask root directory")
    private String avhrrMaskRootDir;

    @SourceProduct
    private Product latlonProduct;    // make sure externally that the correct matching latlon product is submitted

    @Parameter(description = "Sensor", valueSet = {"MVIRI", "SEVIRI", "GMS", "GOES_W", "GOES_E"}, defaultValue = "MVIRI")
    protected MeteosatSensor sensor;

    @Parameter
    private String bbdrDir;

    @Parameter(defaultValue = "0")   // to define a subset of 'vertical stripes' of tiles
    protected int horizontalTileStartIndex;

    @Parameter(defaultValue = "35")   // to define a subset of 'vertical stripes' of tiles
    protected int horizontalTileEndIndex;

    @Parameter(defaultValue = "6.0",
            valueSet = {"0.5", "1.0", "2.0", "4.0", "6.0", "10.0", "12.0", "20.0", "60.0"},
            description = "Scale factor with regard to MODIS default 1200x1200. Values > 1.0 reduce product size.")
    protected double modisTileScaleFactor;

    @Parameter(defaultValue = "true")   // apply conversion to BBDR here
    private boolean convertToBbdr;


    private int parallelism;
    private ModisTileCoordinates tileCoordinates;
    private Product extendedSourceProduct;

    @Override
    public void initialize() throws OperatorException {
        if (!isMeteosatLatLonProductMatching()) {
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

    private boolean isMeteosatLatLonProductMatching() {
        return sensor == MeteosatSensor.MVIRI && isMviriLatlonProductMatching() ||
                sensor == MeteosatSensor.SEVIRI && isSeviriLatlonProductMatching() ||
                sensor == MeteosatSensor.GMS && isGmsLatlonProductMatching() ||
                sensor == MeteosatSensor.GOES_W && isGoesWLatlonProductMatching() ||
                sensor == MeteosatSensor.GOES_E && isGoesELatlonProductMatching();
    }

    private boolean isMviriLatlonProductMatching() {
        // checks if given source product and latlon product are on the same disk (000, 057 or 063)

        // we have product filenames:
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_C_BRF_EUMP_20050501000000.nc
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET5+MVIRI_063_C_BRF_EUMP_20070420000000.nc
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_057_C_BRF_EUMP_20070420000000.nc
        // and the latlon products:
        // MET_000_VIS01_LatLon.nc (000 products from 1982-2006)
        // MET_057_VIS01_LatLon.nc (057 products from 2006-2010)
        // MET_063_VIS01_LatLon.nc (063 products from 1998-2007)
        final boolean matchesMviri000Disk = sourceProduct.getName().contains("VIRI_C_BRF") &&
                latlonProduct.getName().equals("MET_000_VIS01_LatLon");
        final boolean matchesMviri057Disk = sourceProduct.getName().contains("VIRI_057_C_BRF") &&
                latlonProduct.getName().equals("MET_057_VIS01_LatLon");
        final boolean matchesMviri063Disk = sourceProduct.getName().contains("VIRI_063_C_BRF") &&
                latlonProduct.getName().equals("MET_063_VIS01_LatLon");

        return matchesMviri000Disk || matchesMviri057Disk || matchesMviri063Disk;
    }

    private boolean isSeviriLatlonProductMatching() {
        // checks if given source product and latlon product are on the same disk (000, 057 or 063)

        // we have product filenames:
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET8+SEVIRI_HRVIS_000_C_BRF_EUMP_20060701000000.nc
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET8+SEVIRI_HRVIS_057_C_BRF_EUMP_20060701000000.nc
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET8+SEVIRI_HRVIS_063_C_BRF_EUMP_20060701000000.nc

        // and the latlon products:
        // MSG_000_RES01_LatLon.nc
        // MSG_057_RES01_LatLon.nc
        // MSG_063_RES01_LatLon.nc
        final boolean matchesSeviri000Disk = sourceProduct.getName().contains("SEVIRI_HRVIS_000_C_BRF") &&
                latlonProduct.getName().equals("MSG_000_RES01_LatLon");
        final boolean matchesSeviri057Disk = sourceProduct.getName().contains("SEVIRI_HRVIS_057_C_BRF") &&
                latlonProduct.getName().equals("MSG_057_RES01_LatLon");
        final boolean matchesSeviri063Disk = sourceProduct.getName().contains("SEVIRI_HRVIS_063_C_BRF") &&
                latlonProduct.getName().equals("MSG_063_RES01_LatLon");

        return matchesSeviri000Disk || matchesSeviri057Disk || matchesSeviri063Disk;
    }

    private boolean isGmsLatlonProductMatching() {
        // checks if given source product and latlon product are on the same disk (140)

        // we have product filenames:
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,GMS5+VISSR_VIS02_140_C_BRF_EUMP_20030108000000.nc

        // and the latlon products:
        // GMS_140_VIS02_LatLon.nc

        return sourceProduct.getName().contains("GMS5+VISSR_VIS02_140_C_BRF") &&
                latlonProduct.getName().equals("GMS_140_VIS02_LatLon");
    }

    private boolean isGoesELatlonProductMatching() {
        // checks if given source product and latlon product are on the same disk (-75 deg)

        // we have product filenames:
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,GO08+IMAGER_VIS02_-75_C_BRF_EUMP_20030113000000.nc

        // and the latlon products:
        // GOES_075_VIS02_LatLon.nc
        final boolean matchesGoes075Disk = sourceProduct.getName().contains("IMAGER_VIS02_-75_C_BRF") &&
                latlonProduct.getName().equals("GOES_075_VIS02_LatLon");

        return matchesGoes075Disk;
    }

    private boolean isGoesWLatlonProductMatching() {
        // checks if given source product and latlon product are on the same disk (-135 deg)

        // we have product filenames:
        // W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,GO10+IMAGER_VIS02_-135_C_BRF_EUMP_20030628000000.nc

        // and the latlon products:
        // GOES_135_VIS02_LatLon.nc
        final boolean matchesGoes135Disk = sourceProduct.getName().contains("IMAGER_VIS02_-135_C_BRF") &&
                latlonProduct.getName().equals("GOES_135_VIS02_LatLon");

        return matchesGoes135Disk;
    }

    private String getDiskId() {
        // MET_000_VIS01_LatLon.nc
        // MET_057_VIS01_LatLon.nc
        // MET_063_VIS01_LatLon.nc
        // GMS_140_VIS02_LatLon.nc
        // GOES_075_VIS02_LatLon.nc
        // GOES_135_VIS02_LatLon.nc
        final int start = latlonProduct.getName().indexOf("_");
        return latlonProduct.getName().substring(start+1, start+4);
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
        ExecutorCompletionService<TileExtractor.TileProduct> ecs = new ExecutorCompletionService<>(executorService);

        for (int index = 0; index < tileCoordinates.getTileCount(); index++) {
            final String tileName = tileCoordinates.getTileName(index);
            Callable<TileExtractor.TileProduct> callable = new Callable<TileExtractor.TileProduct>() {
                @Override
                public TileExtractor.TileProduct call() throws Exception {
                    if (isTileToProcess(tileName)) {
                        Product reprojected = TileExtractor.reprojectToModisTile(extendedSourceProduct, tileName, modisTileScaleFactor);
                        return new TileExtractor.TileProduct(reprojected, tileName);
                    } else {
                        return null;
                    }
                }
            };
            ecs.submit(callable);
        }
        for (int i = 0; i < tileCoordinates.getTileCount(); i++) {
            try {
                Future<TileExtractor.TileProduct> future = ecs.take();
                TileExtractor.TileProduct tileProduct = future.get();
                if (tileProduct != null && tileProduct.getProduct() != null && isTileToProcess(tileProduct.getTileName())) {
                    Product productToWrite = null;
                    if (convertToBbdr) {

                        Map<String, Product> meteosatBbdrInputProducts = new HashMap<>();
                        meteosatBbdrInputProducts.put("brf", tileProduct.getProduct());
                        final Product avhrrMaskProduct =
                                IOUtils.getAvhrrMaskProduct(avhrrMaskRootDir, sourceProduct.getName(), year, tileProduct.getTileName());
                        // todo: we will need the AVHRR BRF tile product instead
                        // such as: ../GlobAlbedoTest/BBDR/AVHRR/1989/h18v04/
                        //             AVHRR2_NOAA11_19891124_19891124_L1_BRF_900S900N1800W1800E_PLC_0005D_v03_h18v04.nc
                        // instead of
                        //          ../GlobAlbedoTest/MsslAvhrrMask/1989/h18v04/
                        //             msslFlag_v2__AVHRR-Land_v004_AVH09C1_NOAA-11_19891124_c20130905130918_h18v04.nc
                        if (avhrrMaskProduct != null) {
                            meteosatBbdrInputProducts.put("avhrrmask", avhrrMaskProduct);
                            productToWrite = GPF.createProduct(OperatorSpi.getOperatorAlias(MeteosatBbdrFromBrfOp.class),
                                                               GPF.NO_PARAMS, meteosatBbdrInputProducts);
                            productToWrite.setGeoCoding(tileProduct.getProduct().getGeoCoding());
                        }

                        // will save computation time and disk space in case of mass production...
//                        MeteosatBbdrFromBrfOp toBbdrOp = new MeteosatBbdrFromBrfOp();
//                        toBbdrOp.setParameterDefaultValues();
//                        toBbdrOp.setParameter("sensor", sensor);
//                        toBbdrOp.setSourceProduct(tileProduct.getProduct());
//                        productToWrite = toBbdrOp.getTargetProduct();
//                        productToWrite.setGeoCoding(tileProduct.getProduct().getGeoCoding());
                    } else {
                        productToWrite = tileProduct.getProduct();
                    }
                    if (productToWrite != null) {
                        writeTileProduct(productToWrite, tileProduct.getTileName());
                    }
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
        if (!dir.exists()) {
            final boolean madeDir = dir.mkdirs();
            if (!madeDir) {
                throw new OperatorException("Could not create directory '" + dir + "'.");
            }
        }
        File file;
        String writeFormat;
        if (convertToBbdr) {
            file = new File(dir, sourceProduct.getName().replace("_BRF_", "_BBDR_") + "_" + tileName + ".nc");
//            writeFormat = "NetCDF4-GA-BBDR";
        } else {
            file = new File(dir, sourceProduct.getName() + "_" + tileName + ".nc");
//            writeFormat = "NetCDF4-BEAM";
        }
        writeFormat = "NetCDF4-BEAM";
        WriteOp writeOp = new WriteOp(product, file, writeFormat);
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MeteosatBrfTilesExtractor.class);
        }
    }
}
