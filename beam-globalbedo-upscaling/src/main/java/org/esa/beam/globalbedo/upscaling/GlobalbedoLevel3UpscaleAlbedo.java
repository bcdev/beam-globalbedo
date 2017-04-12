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
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
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

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
public class GlobalbedoLevel3UpscaleAlbedo extends GlobalbedoLevel3UpscaleBasisOp {

    @Parameter(valueSet = {"1200", "200"},
            description = "Input product tile size (default = 1200 (MODIS), 200 for AVHRR/GEO",
            defaultValue = "1200")
    private int inputProductTileSize;

    @Parameter(defaultValue = "NETCDF", valueSet = {"DIMAP", "NETCDF"}, description = "Input format, either DIMAP or NETCDF.")
    private String inputFormat;

    @Parameter(defaultValue = "Albedo", description = "Name of albedo subdirectory.")
    private String albedoSubdirName;

    @Parameter(defaultValue = "false",
            description = "If set, not all bands (i.e. no alphas/sigmas) are written. Set to true to save computation time and disk space.")
    private boolean reducedOutput;

    @TargetProduct
    private Product targetProduct;


    private String[] dhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrAlphaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrAlphaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private Band dataMaskBand;
    private Band szaBand;

    @Override
    public void initialize() throws OperatorException {
        final File refTile = findRefTile();
        if (refTile == null || !refTile.exists()) {
            throw new OperatorException("No albedo files for mosaicing found.");
        }

        final ProductReader productReader = ProductIO.getProductReader("GLOBALBEDO-L3-MOSAIC");
        if (productReader == null) {
            throw new OperatorException("No 'GLOBALBEDO-L3-MOSAIC' reader available.");
        }
        if (productReader instanceof GlobAlbedoMosaicProductReader) {
            ((GlobAlbedoMosaicProductReader) productReader).setTileSize(inputProductTileSize);
        }

        dhrBandNames = IOUtils.getAlbedoDhrBandNames();
        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        dhrAlphaBandNames = IOUtils.getAlbedoDhrAlphaBandNames();
        bhrAlphaBandNames = IOUtils.getAlbedoBhrAlphaBandNames();
        dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
        bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();

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

        attachUpscaleGeoCoding(mosaicProduct, scaling, width, height, reprojection);

        dataMaskBand = reprojectedProduct.getBand(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME);
        szaBand = reprojectedProduct.getBand(AlbedoInversionConstants.ALB_SZA_BAND_NAME);

        targetProduct = upscaledProduct;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetBandTiles, Rectangle targetRect, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRect = new Rectangle(targetRect.x * scaling,
                                          targetRect.y * scaling,
                                          targetRect.width * scaling,
                                          targetRect.height * scaling);
        Map<String, Tile> targetTiles = getTargetTiles(targetBandTiles);

        final Tile latTile = targetTiles.get(BRDF_ALBEDO_PRODUCT_LAT_NAME);
        final Tile lonTile = targetTiles.get(BRDF_ALBEDO_PRODUCT_LON_NAME);
        if (reprojection.equals("PC") && latTile != null && lonTile != null) {
            computeLatLon(latTile, lonTile, latTile);
        }

        final Tile szaSrcTile = getSourceTile(szaBand, srcRect);
        computeNearestAlbedo(szaSrcTile,
                             targetTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME),
                             getSourceTile(dataMaskBand, srcRect));

//        if (hasValidPixel(getSourceTile(relEntropyBand, srcRect), relEntropyBand.getNoDataValue())) {
        // todo: check why we ever switched to relEntropy as data mask?!
        if (hasValidPixel(getSourceTile(dataMaskBand, srcRect), dataMaskBand.getNoDataValue())) {
            Map<String, Tile> srcTiles = getSourceTiles(srcRect);

            for (String dhrBandName : dhrBandNames) {
                computeNearestAlbedo(srcTiles.get(dhrBandName), targetTiles.get(dhrBandName),
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            for (String bhrBandName : bhrBandNames) {
                computeNearestAlbedo(srcTiles.get(bhrBandName), targetTiles.get(bhrBandName),
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            if (!reducedOutput) {
                for (String dhrAlphaBandName : dhrAlphaBandNames) {
                    computeNearestAlbedo(srcTiles.get(dhrAlphaBandName), targetTiles.get(dhrAlphaBandName),
                                         srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                }
                for (String bhrAlphaBandName : bhrAlphaBandNames) {
                    computeNearestAlbedo(srcTiles.get(bhrAlphaBandName), targetTiles.get(bhrAlphaBandName),
                                         srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                }
                for (String dhrSigmaBandName : dhrSigmaBandNames) {
                    computeNearestAlbedo(srcTiles.get(dhrSigmaBandName), targetTiles.get(dhrSigmaBandName),
                                         srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                }
                for (String bhrSigmaBandName : bhrSigmaBandNames) {
                    computeNearestAlbedo(srcTiles.get(bhrSigmaBandName), targetTiles.get(bhrSigmaBandName),
                                         srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                }
            }

            computeMajority(srcTiles.get(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME),
                            targetTiles.get(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME),
                            srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME), scaling);

            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME),
                                 targetTiles.get(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME),
                                 srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
//            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME),
//                                 targetTiles.get(AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME),
//                                 srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME),
                                 targetTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME),
                                 srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            if (srcTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME) != null) {
                computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME),
                                     targetTiles.get(AlbedoInversionConstants.ALB_SZA_BAND_NAME),
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
            computeNearestAlbedo(srcTiles.get(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME),
                                 targetTiles.get(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME),
                                 srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            if (reprojection.equals("SIN") && latTile != null && lonTile != null) {
                computeNearestAlbedo(srcTiles.get(BRDF_ALBEDO_PRODUCT_LAT_NAME), latTile,
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
                computeNearestAlbedo(srcTiles.get(BRDF_ALBEDO_PRODUCT_LON_NAME), lonTile,
                                     srcTiles.get(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME));
            }
        } else {
            for (Map.Entry<String, Tile> tileEntry : targetTiles.entrySet()) {
                checkForCancellation();

                Tile targetTile = tileEntry.getValue();
                String bandName = tileEntry.getKey();
                if (!isLatLonBand(bandName) &&!bandName.equals(AlbedoInversionConstants.ALB_SZA_BAND_NAME)) {
                    double noDataValue = getTargetProduct().getBand(bandName).getNoDataValue();
                    for (int y = targetRect.y; y < targetRect.y + targetRect.height; y++) {
                        for (int x = targetRect.x; x < targetRect.x + targetRect.width; x++) {
                            targetTile.setSample(x, y, noDataValue);
                        }
                    }
                }
            }
        }
    }

    private void addTargetBand(Band srcBand) {
        boolean skipBand = reducedOutput && (srcBand.getName().contains("alpha") || srcBand.getName().contains("sigma"));
        if (!skipBand) {
            Band band = upscaledProduct.addBand(srcBand.getName(), srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, band);
        }
    }


    private File findRefTile() {

        final FilenameFilter albedoFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String expectedFilenameExt = inputFormat.equals("DIMAP") ? ".dim" : ".nc";
                String expectedFilename;

                if (isMonthlyAlbedo) {
                    expectedFilename = "GlobAlbedo.albedo." + year + IOUtils.getMonthString(monthIndex) + "." +
                            dir.getName() + expectedFilenameExt;
                } else {
                    expectedFilename = "GlobAlbedo.albedo." + year + IOUtils.getDoyString(doy) + "." +
                            dir.getName() + expectedFilenameExt;
                }

                return name.equals(expectedFilename);
            }
        };

        String albedoDirString = gaRootDir + File.separator + albedoSubdirName + File.separator + year;

        final File[] albedoFiles = IOUtils.getTileDirectories(albedoDirString);
        if (albedoFiles != null && albedoFiles.length > 0) {
            for (File albedoFile : albedoFiles) {
                File[] tileFiles = albedoFile.listFiles(albedoFilter);
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

    private void computeNearestAlbedo(Tile src, Tile target, Tile mask) {
        computeNearest(src, target, mask, scaling);
        applySouthPoleCorrection(src, target, mask);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3UpscaleAlbedo.class);
        }
    }
}
