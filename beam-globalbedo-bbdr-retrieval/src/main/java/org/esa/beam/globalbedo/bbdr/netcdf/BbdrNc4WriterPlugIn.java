package org.esa.beam.globalbedo.bbdr.netcdf;

import org.esa.beam.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.nc.NWritableFactory;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.globalbedo.bbdr.BbdrConstants;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.io.netcdf.AlbedoInversionGeocodingPart;
import org.esa.beam.globalbedo.inversion.io.netcdf.NcConstants;
import org.esa.beam.jai.ImageManager;

import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Writer for CF compliant Albedo NetCDF4 output
 *
 * @author olafd
 */
public class BbdrNc4WriterPlugIn extends AbstractNetCdfWriterPlugIn {

    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF4-GA-BBDR"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{Constants.FILE_EXTENSION_NC};
    }

    @Override
    public String getDescription(Locale locale) {
        return "QA4ECV BBDR NetCDF4 products";
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new AlbedoInversionGeocodingPart();
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new BbdrNc4MainPart();
    }

    @Override
    public NFileWriteable createWritable(String outputPath) throws IOException {
        return NWritableFactory.create(outputPath, "netcdf4");
    }


    private class BbdrNc4MainPart implements ProfileInitPartWriter {

        private final SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        private Dimension tileSize;

        BbdrNc4MainPart() {
            COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
            NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            tileSize = ImageManager.getPreferredTileSize(product);

            writeable.addDimension("y", product.getSceneRasterHeight());
            writeable.addDimension("x", product.getSceneRasterWidth());

            addGlobalAttributes(writeable);

            for (String bandName : BbdrConstants.BB_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4BBVariableWithAttributes(writeable, b);
                }
            }

            for (String bandName : BbdrConstants.BB_SIGMA_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4BBSigmaVariableWithAttributes(writeable, b);
                }

            }

            for (String bandName : BbdrConstants.BB_KERNEL_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4BBKernelVariableWithAttributes(writeable, b);
                }
            }


            addNc4AncillaryVariablesAttributes(writeable, product);
            addNc4SnowMaskVariableAttributes(writeable, product);
        }

        private void addNc4AncillaryVariablesAttributes(NFileWriteable writeable, Product p) throws IOException {
            final Band vzaBand = p.getBand("VZA");
            if (vzaBand != null) {
                addNc4BBAncillaryVariableWithAttributes(writeable, vzaBand,
                                                        "View zenith angle", "degrees");
            }
            final Band szaBand = p.getBand("SZA");
            if (szaBand != null) {
                addNc4BBAncillaryVariableWithAttributes(writeable, szaBand,
                                                        "Sun zenith angle", "degrees");
            }
            final Band raaBand = p.getBand("RAA");
            if (raaBand != null) {
                addNc4BBAncillaryVariableWithAttributes(writeable, raaBand,
                                                        "Relative azimuth angle", "degrees");
            }
            final Band latBand = p.getBand(NcConstants.LAT_BAND_NAME);
            if (latBand != null) {
                addNc4BBLatLonVariableWithAttributes(writeable, latBand, "latitude coordinate", "latitude", "degrees");
            }
            final Band lonBand = p.getBand(NcConstants.LON_BAND_NAME);
            if (lonBand != null) {
                addNc4BBLatLonVariableWithAttributes(writeable, lonBand, "longitude coordinate", "longitude", "degrees");
            }
        }

        private void addNc4SnowMaskVariableAttributes(NFileWriteable writeable, Product p) throws IOException {
            final Band snowMaskBand = p.getBand(AlbedoInversionConstants.BBDR_SNOW_MASK_NAME);
            if (snowMaskBand != null) {
                NVariable variable = addNc4Variable(writeable, ProductData.TYPE_INT8, snowMaskBand);
                variable.addAttribute("long_name", "snow_mask");
                variable.addAttribute("coordinates", "lat lon");
                addUnitAttribute("1", variable);
            }
        }

        private void addGlobalAttributes(NFileWriteable writeable) throws IOException {
            // todo: clarify/discuss attribute entries
            writeable.addGlobalAttribute("Conventions", "CF-1.6");
            writeable.addGlobalAttribute("history", "QA4ECV Processing, 2014-2017");
            writeable.addGlobalAttribute("title", "QA4ECV BBDR Product");
            writeable.addGlobalAttribute("institution", "Mullard Space Science Laboratory, " +
                    "Department of Space and Climate Physics, University College London");
            writeable.addGlobalAttribute("source", "Satellite observations, Atmospheric Correction and Broadband Conversion");
            writeable.addGlobalAttribute("references", "GlobAlbedo ATBD V4.12");
            writeable.addGlobalAttribute("comment", "none");
        }

        private void addNc4BBVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "BB_NIR" --> "Broadband Reflectance - NIR range"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Broadband Reflectance - " + bSplit[1] + " range band";
            addBBVariableAttributes(b, variable, longName);
        }

        private void addNc4BBSigmaVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "sig_BB_NIR_SW" --> "Uncertainty of Broadband Reflectance - (NIR,SW) term"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Uncertainty of Broadband Reflectance - (" + bSplit[2] + "," + bSplit[3] + ") term";
            addBBVariableAttributes(b, variable, longName);
        }

        private void addNc4BBKernelVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "Kvol_BRDF_NIR" --> "Volumetric kernel - NIR term"
            final String[] bSplit = b.getName().split("_");
            final String kernelType = b.getName().startsWith("Kvol") ? "Volumetric" : "Geometric";
            final String longName = kernelType + " kernel - " + bSplit[2] + " term";
            addBBVariableAttributes(b, variable, longName);
        }

        private void addBBVariableAttributes(Band b, NVariable variable, String longName) throws IOException {
            variable.addAttribute("long_name", longName);
            final Float _fillValue = (float) b.getNoDataValue();
            variable.addAttribute("_FillValue", _fillValue);
            variable.addAttribute("units", "1");
            variable.addAttribute("coordinates", "lat lon");
        }


        private void addNc4BBAncillaryVariableWithAttributes(NFileWriteable writeable, Band b,
                                                             String longName,
                                                             String unit) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            variable.addAttribute("long_name", longName);
//            final Float _fillValue = fillValue;
            final Float _fillValue = (float) b.getNoDataValue();
            variable.addAttribute("_FillValue", _fillValue);
            variable.addAttribute("coordinates", "lat lon");
            addUnitAttribute(unit, variable);
        }

        private void addNc4BBLatLonVariableWithAttributes(NFileWriteable writeable, Band b,
                                                          String longName,
                                                          String standardName,
                                                          String unit) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            variable.addAttribute("long_name", longName);
            variable.addAttribute("long_name", standardName);
            addUnitAttribute(unit, variable);
        }

        private void addUnitAttribute(String unit, NVariable variable) throws IOException {
            if (unit != null) {
                variable.addAttribute("units", unit);
            }
        }

        private NVariable addNc4Variable(NFileWriteable writeable, Band b) throws IOException {
            return addNc4Variable(writeable, ProductData.TYPE_FLOAT32, b);
        }

        private NVariable addNc4Variable(NFileWriteable writeable, int dataType, Band b) throws IOException {
            return writeable.addVariable(b.getName(),
                                         DataTypeUtils.getNetcdfDataType(dataType),
                                         tileSize, writeable.getDimensions());
        }

    }
}