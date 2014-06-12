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
package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.Modis35ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.Modis35ProfilePartIO;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.Modis35Constants;
import org.esa.beam.dataio.netcdf.util.Modis35DimKey;
import org.esa.beam.dataio.netcdf.util.Modis35NetcdfMultiLevelImage;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ForLoop;
import org.esa.beam.util.StringUtils;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Modis35CfBandPart extends Modis35ProfilePartIO {

    private static final Modis35DataTypeWorkarounds dataTypeWorkarounds = new Modis35DataTypeWorkarounds();

    @Override
    public void decode(final Modis35ProfileReadContext ctx, final Product p) throws IOException {
        for (final Variable variable : ctx.getRasterDigest().getRasterVariables()) {
            addBandsToProduct(ctx, p, variable);
        }
        p.setAutoGrouping(getAutoGrouping(ctx));
    }

    private void addBandsToProduct(final Modis35ProfileReadContext ctx, final Product p, final Variable variable) {
        final List<Dimension> dimensions = variable.getDimensions();
        final int rank = dimensions.size();
        final String bandBasename = variable.getShortName();

        if (rank == 2) {
            addBand(ctx, p, variable, new int[]{}, bandBasename);
        } else {
            final int[] sizeArray = new int[rank - 2];
            final int startIndexToCopy = Modis35DimKey.findStartIndexOfBandVariables(dimensions);
            System.arraycopy(variable.getShape(), startIndexToCopy, sizeArray, 0, sizeArray.length);
            ForLoop.execute(sizeArray, new ForLoop.Body() {
                @Override
                public void execute(int[] indexes, int[] sizes) {
                    final StringBuilder bandNameBuilder = new StringBuilder(bandBasename);
                    for (int i = 0; i < sizes.length; i++) {
                        final Dimension zDim = dimensions.get(i + startIndexToCopy);
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

    private static void addBand(Modis35ProfileReadContext ctx, Product p, Variable variable, int[] origin,
                                String bandBasename) {
        final int rasterDataType = getRasterDataType(variable, dataTypeWorkarounds);
        if (variable.getDataType() == DataType.LONG) {
            final Band lowerBand = p.addBand(bandBasename + "_lsb", rasterDataType);
            readCfBandAttributes(variable, lowerBand);
            if (lowerBand.getDescription() != null) {
                lowerBand.setDescription(lowerBand.getDescription() + "(least significant bytes)");
            }
            lowerBand.setSourceImage(new Modis35NetcdfMultiLevelImage(lowerBand, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, lowerBand, variable, variable.getFullName() + "_lsb", false);

            final Band upperBand = p.addBand(bandBasename + "_msb", rasterDataType);
            readCfBandAttributes(variable, upperBand);
            if (upperBand.getDescription() != null) {
                upperBand.setDescription(upperBand.getDescription() + "(most significant bytes)");
            }
            upperBand.setSourceImage(new Modis35NetcdfMultiLevelImage(upperBand, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, upperBand, variable, variable.getFullName() + "_msb", true);
        } else {
            final Band band = p.addBand(bandBasename, rasterDataType);
            readCfBandAttributes(variable, band);
            band.setSourceImage(new Modis35NetcdfMultiLevelImage(band, variable, origin, ctx));
            addSampleCodingOrMasksIfApplicable(p, band, variable, variable.getFullName(), false);
        }
    }

    private String getAutoGrouping(Modis35ProfileReadContext ctx) {
        ArrayList<String> bandNames = new ArrayList<>();
        for (final Variable variable : ctx.getRasterDigest().getRasterVariables()) {
            final List<Dimension> dimensions = variable.getDimensions();
            int rank = dimensions.size();
            for (int i = 0; i < rank - 2; i++) {
                Dimension dim = dimensions.get(i);
                if (dim.getLength() > 1) {
                    bandNames.add(variable.getFullName());
                    break;
                }
            }
        }
        return StringUtils.join(bandNames, ":");
    }

    static String replaceNonWordCharacters(String flagName) {
        return flagName.replaceAll("\\W+", "_");
    }

    public static void readCfBandAttributes(Variable variable, RasterDataNode rasterDataNode) {
        rasterDataNode.setDescription(variable.getDescription());
        rasterDataNode.setUnit(variable.getUnitsString());

        rasterDataNode.setScalingFactor(getScalingFactor(variable));
        rasterDataNode.setScalingOffset(getAddOffset(variable));

        final Number noDataValue = getNoDataValue(variable);
        if (noDataValue != null) {
            rasterDataNode.setNoDataValue(noDataValue.doubleValue());
            rasterDataNode.setNoDataValueUsed(true);
        }
    }

    private static double getScalingFactor(Variable variable) {
        Attribute attribute = variable.findAttribute(Modis35Constants.SCALE_FACTOR_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Modis35Constants.SLOPE_ATT_NAME);
        }
        if (attribute == null) {
            attribute = variable.findAttribute("scaling_factor");
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 1.0;
    }

    private static double getAddOffset(Variable variable) {
        Attribute attribute = variable.findAttribute(Modis35Constants.ADD_OFFSET_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Modis35Constants.INTERCEPT_ATT_NAME);
        }
        if (attribute != null) {
            return getAttributeValue(attribute).doubleValue();
        }
        return 0.0;
    }

    private static Number getNoDataValue(Variable variable) {
        Attribute attribute = variable.findAttribute(Modis35Constants.FILL_VALUE_ATT_NAME);
        if (attribute == null) {
            attribute = variable.findAttribute(Modis35Constants.MISSING_VALUE_ATT_NAME);
        }
        if (attribute != null) {
            return getAttributeValue(attribute);
        }
        return null;
    }

    private static Number getAttributeValue(Attribute attribute) {
        if (attribute.isString()) {
            String stringValue = attribute.getStringValue();
            if (stringValue.endsWith("b")) {
                // Special management for bytes; Can occur in e.g. ASCAT files from EUMETSAT
                return Byte.parseByte(stringValue.substring(0, stringValue.length() - 1));
            } else {
                return Double.parseDouble(stringValue);
            }
        } else {
            return attribute.getNumericValue();
        }
    }

    private static int getRasterDataType(Variable variable, Modis35DataTypeWorkarounds workarounds) {
        if (workarounds != null && workarounds.hasWorkaround(variable.getFullName(), variable.getDataType())) {
            return workarounds.getRasterDataType(variable.getFullName(), variable.getDataType());
        }
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
}
