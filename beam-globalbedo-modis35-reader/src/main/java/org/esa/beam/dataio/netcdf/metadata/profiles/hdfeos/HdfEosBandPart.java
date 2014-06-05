/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de) 
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

package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.esa.beam.dataio.netcdf.Modis35ProfileReadContext;
import org.esa.beam.dataio.netcdf.Modis35ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.Modis35ProfilePartIO;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.Modis35CfBandPart;
import org.esa.beam.dataio.netcdf.util.*;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.ForLoop;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import javax.media.jai.Interpolation;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;


public class HdfEosBandPart extends Modis35ProfilePartIO {

    @Override
    public void decode(final Modis35ProfileReadContext ctx, final Product p) throws IOException {
        Modis35RasterDigest rasterDigest = ctx.getRasterDigest();
        final Variable[] variables = rasterDigest.getRasterVariables();
        for (final Variable variable : variables) {
//            final int rasterDataType = DataTypeUtils.getRasterDataType(variable);
//            final Band band = p.addBand(variable.getShortName(), rasterDataType);
//            CfBandPart.readCfBandAttributes(variable, band);
//            band.setSourceImage(new NetcdfMultiLevelImage(band, variable, ctx));

            final List<ucar.nc2.Dimension> dimensions = variable.getDimensions();
            final int rank = dimensions.size();
            final String bandBasename = variable.getShortName();

            if (rank == 2) {
                final int rasterDataType = DataTypeUtils.getRasterDataType(variable);
                final Band band = p.addBand(variable.getShortName(), rasterDataType);
                Modis35CfBandPart.readCfBandAttributes(variable, band);
                band.setSourceImage(new Modis35NetcdfMultiLevelImage(band, variable, ctx));
            } else {
                final int[] sizeArray = new int[rank - 2];
                final int startIndexToCopy = Modis35DimKey.findStartIndexOfBandVariables(dimensions);
                System.arraycopy(variable.getShape(), startIndexToCopy, sizeArray, 0, sizeArray.length);
                ForLoop.execute(sizeArray, new ForLoop.Body() {
                    @Override
                    public void execute(int[] indexes, int[] sizes) {
                        final StringBuilder bandNameBuilder = new StringBuilder(bandBasename);
                        for (int i = 0; i < sizes.length; i++) {
                            final ucar.nc2.Dimension zDim = dimensions.get(i + startIndexToCopy);
                            String zName = zDim.getShortName();
                            final String skipPrefix = "n_";
                            if (zName.toLowerCase().startsWith(skipPrefix)
                                    && zName.length() > skipPrefix.length()) {
                                zName = zName.substring(skipPrefix.length());
                            }
                            if (zDim.getLength() > 1) {
                                bandNameBuilder.append(String.format("_%s%d", zName, (indexes[i] + 1)));
                            }

                        }
                        addBand(ctx, p, variable, indexes, bandNameBuilder.toString());
                    }
                });
            }
        }
        Modis35ScaledVariable[] scaledVariables = rasterDigest.getScaledVariables();
        for (Modis35ScaledVariable scaledVariable : scaledVariables) {
            Variable variable = scaledVariable.getVariable();
            final int rasterDataType = DataTypeUtils.getRasterDataType(variable);
            final Band band = p.addBand(variable.getShortName(), rasterDataType);
            Modis35CfBandPart.readCfBandAttributes(variable, band);
            band.setSourceImage(new ScaledMultiLevelImage(band, scaledVariable, ctx));
        }
    }

    @Override
    public void preEncode(Modis35ProfileWriteContext ctx, Product p) throws IOException {
        throw new IllegalStateException();
    }

    private static class ScaledMultiLevelImage extends AbstractNetcdfMultiLevelImage {

        private final Variable variable;
        private final float scaleFactor;
        private final int[] imageOrigin;
        private final Modis35ProfileReadContext ctx;

        public ScaledMultiLevelImage(RasterDataNode rdn, Modis35ScaledVariable scaledVariable, Modis35ProfileReadContext ctx) {
            super(rdn);
            this.variable = scaledVariable.getVariable();
            this.scaleFactor = scaledVariable.getScaleFactor();
            this.imageOrigin = new int[0];
            this.ctx = ctx;
        }

        @Override
        protected RenderedImage createImage(int level) {
            RasterDataNode rdn = getRasterDataNode();
            NetcdfFile lock = ctx.getNetcdfFile();
            final Object object = ctx.getProperty(Modis35Constants.Y_FLIPPED_PROPERTY_NAME);
            boolean isYFlipped = object instanceof Boolean && (Boolean) object;
            int dataBufferType = ImageManager.getDataBufferType(rdn.getDataType());
            int sourceWidth = (int) (rdn.getSceneRasterWidth() / scaleFactor);
            int sourceHeight = (int) (rdn.getSceneRasterHeight() / scaleFactor);
            ResolutionLevel resolutionLevel = ResolutionLevel.create(getModel(), level);
            Dimension imageTileSize = new Dimension(getTileWidth(), getTileHeight());

            RenderedImage netcdfImg;
            if (variable.getDataType() == DataType.LONG) {
                if (rdn.getName().endsWith("_lsb")) {
                    netcdfImg = NetcdfOpImage.createLsbImage(variable, imageOrigin, isYFlipped, lock, dataBufferType,
                                                             sourceWidth, sourceHeight, imageTileSize, resolutionLevel);
                } else {
                    netcdfImg = NetcdfOpImage.createMsbImage(variable, imageOrigin, isYFlipped, lock, dataBufferType,
                                                             sourceWidth, sourceHeight, imageTileSize, resolutionLevel);
                }
            } else {
                netcdfImg = new NetcdfOpImage(variable, imageOrigin, isYFlipped, lock,
                                              dataBufferType, sourceWidth, sourceHeight, imageTileSize, resolutionLevel);
            }

            return ScaleDescriptor.create(netcdfImg, scaleFactor, scaleFactor, 0.5f, 0.5f,
                                                          Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
        }
    }

    private static void addBand(Modis35ProfileReadContext ctx, Product p, Variable variable, int[] origin,
                                String bandBasename) {
        final int rasterDataType = getRasterDataType(variable);
        if (variable.getDataType() == DataType.LONG) {
            final Band lowerBand = p.addBand(bandBasename + "_lsb", rasterDataType);
            Modis35CfBandPart.readCfBandAttributes(variable, lowerBand);
            if (lowerBand.getDescription() != null) {
                lowerBand.setDescription(lowerBand.getDescription() + "(least significant bytes)");
            }
            lowerBand.setSourceImage(new Modis35NetcdfMultiLevelImage(lowerBand, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, lowerBand, variable, variable.getFullName() + "_lsb", false);

            final Band upperBand = p.addBand(bandBasename + "_msb", rasterDataType);
            Modis35CfBandPart.readCfBandAttributes(variable, upperBand);
            if (upperBand.getDescription() != null) {
                upperBand.setDescription(upperBand.getDescription() + "(most significant bytes)");
            }
            upperBand.setSourceImage(new Modis35NetcdfMultiLevelImage(upperBand, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, upperBand, variable, variable.getFullName() + "_msb", true);
        } else {
            final Band band = p.addBand(bandBasename, rasterDataType);
            Modis35CfBandPart.readCfBandAttributes(variable, band);
            band.setSourceImage(new Modis35NetcdfMultiLevelImage(band, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, band, variable, variable.getFullName(), false);
        }
    }

    private static int getRasterDataType(Variable variable) {
        int rasterDataType = DataTypeUtils.getRasterDataType(variable);
        if (rasterDataType == -1) {
            if (variable.getDataType() == DataType.LONG) {
                rasterDataType = variable.isUnsigned() ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
            }
        }
        return rasterDataType;
    }

    private static void addSampleCodingOrMasksIfApplicable(Product p, Band band, Variable variable,
                                                           String sampleCodingName,
                                                           boolean msb) {
        Attribute flagMeanings = variable.findAttribute("flag_meanings");
        if (flagMeanings == null) {
            flagMeanings = variable.findAttribute("flag_meaning");
        }
        if (flagMeanings == null) {
            return;
        }
        final Attribute flagMasks = variable.findAttribute("flag_masks");
        final Attribute flagValues = variable.findAttribute("flag_values");

        if (flagMasks != null && flagValues == null) {
            if (!p.getFlagCodingGroup().contains(sampleCodingName)) {
                final FlagCoding flagCoding = new FlagCoding(sampleCodingName);
                addSamples(flagCoding, flagMeanings, flagMasks, msb);
                p.getFlagCodingGroup().add(flagCoding);
            }
            band.setSampleCoding(p.getFlagCodingGroup().get(sampleCodingName));
        } else if (flagMasks == null && flagValues != null) {
            if (!p.getIndexCodingGroup().contains(sampleCodingName)) {
                final IndexCoding indexCoding = new IndexCoding(sampleCodingName);
                addSamples(indexCoding, flagMeanings, flagValues, msb);
                p.getIndexCodingGroup().add(indexCoding);
            }
            band.setSampleCoding(p.getIndexCodingGroup().get(sampleCodingName));
        } else if (flagMasks != null && flagMasks.getLength() == flagValues.getLength()) {
            addMasks(p, band, flagMeanings, flagMasks, flagValues, msb);
        }
    }

    private static void addMasks(Product p, Band band, Attribute flagMeanings, Attribute flagMasks,
                                 Attribute flagValues, boolean msb) {
        final String[] meanings = getSampleMeanings(flagMeanings);
        final int sampleCount = Math.min(meanings.length, flagMasks.getLength());

        for (int i = 0; i < sampleCount; i++) {
            final String flagName = replaceNonWordCharacters(meanings[i]);
            final Number a = flagMasks.getNumericValue(i);
            final Number b = flagValues.getNumericValue(i);

            switch (flagMasks.getDataType()) {
                case BYTE:
                    addMask(p, band, flagName,
                            DataType.unsignedByteToShort(a.byteValue()),
                            DataType.unsignedByteToShort(b.byteValue()));
                    break;
                case SHORT:
                    addMask(p, band, flagName,
                            DataType.unsignedShortToInt(a.shortValue()),
                            DataType.unsignedShortToInt(b.shortValue()));
                    break;
                case INT:
                    addMask(p, band, flagName, a.intValue(), b.intValue());
                    break;
                case LONG:
                    final long flagMask = a.longValue();
                    final long flagValue = b.longValue();
                    if (msb) {
                        final long flagMaskMsb = flagMask >>> 32;
                        final long flagValueMsb = flagValue >>> 32;
                        addMask(p, band, flagName, flagMaskMsb, flagValueMsb);
                    } else {
                        final long flagMaskLsb = flagMask & 0x00000000FFFFFFFFL;
                        final long flagValueLsb = flagValue & 0x00000000FFFFFFFFL;
                        addMask(p, band, flagName, flagMaskLsb, flagValueLsb);
                    }
                    break;
            }
        }
    }

    private static void addMask(Product p, Band band, String flagName, long flagMask, long flagValue) {
        p.addMask(band.getName() + "_" + flagName,
                  "(" + band.getName() + " & " + flagMask + ") == " + flagValue, null, Color.RED, 0.5);
    }

    private static void addSamples(SampleCoding sampleCoding, Attribute sampleMeanings, Attribute sampleValues,
                                   boolean msb) {
        final String[] meanings = getSampleMeanings(sampleMeanings);
        final int sampleCount = Math.min(meanings.length, sampleValues.getLength());

        for (int i = 0; i < sampleCount; i++) {
            final String sampleName = replaceNonWordCharacters(meanings[i]);
            switch (sampleValues.getDataType()) {
                case BYTE:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedByteToShort(
                                                   sampleValues.getNumericValue(i).byteValue()), null);
                    break;
                case SHORT:
                    sampleCoding.addSample(sampleName,
                                           DataType.unsignedShortToInt(
                                                   sampleValues.getNumericValue(i).shortValue()), null);
                    break;
                case INT:
                    sampleCoding.addSample(sampleName, sampleValues.getNumericValue(i).intValue(), null);
                    break;
                case LONG:
                    final long sampleValue = sampleValues.getNumericValue(i).longValue();
                    if (msb) {
                        final long sampleValueMsb = sampleValue >>> 32;
                        if (sampleValueMsb > 0) {
                            sampleCoding.addSample(sampleName, (int) sampleValueMsb, null);
                        }
                    } else {
                        final long sampleValueLsb = sampleValue & 0x00000000FFFFFFFFL;
                        if (sampleValueLsb > 0 || sampleValue == 0L) {
                            sampleCoding.addSample(sampleName, (int) sampleValueLsb, null);
                        }
                    }
                    break;
            }
        }
    }

    private static String[] getSampleMeanings(Attribute sampleMeanings) {
        final int sampleMeaningsCount = sampleMeanings.getLength();
        if (sampleMeaningsCount > 1) {
            // handle a common misunderstanding of CF conventions, where flag meanings are stored as array of strings
            final String[] strings = new String[sampleMeaningsCount];
            for (int i = 0; i < strings.length; i++) {
                strings[i] = sampleMeanings.getStringValue(i);
            }
            return strings;
        }
        return sampleMeanings.getStringValue().split(" ");
    }

    static String replaceNonWordCharacters(String flagName) {
        return flagName.replaceAll("\\W+", "_");
    }
}
