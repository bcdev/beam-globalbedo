package org.esa.beam.globalbedo.inversion.io.netcdf;

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
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
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
public class AlbedoNc4WriterPlugIn extends AbstractNetCdfWriterPlugIn {

    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF4-GA-ALBEDO"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{Constants.FILE_EXTENSION_NC};
    }

    @Override
    public String getDescription(Locale locale) {
        return "QA4ECV Albedo NetCDF4 products";
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new AlbedoInversionGeocodingPart();
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new AlbedoNc4MainPart();
    }

    @Override
    public NFileWriteable createWritable(String outputPath) throws IOException {
        return NWritableFactory.create(outputPath, "netcdf4");
    }


    private class AlbedoNc4MainPart implements ProfileInitPartWriter {

        private final String[] BHR_BAND_NAMES = IOUtils.getAlbedoBhrBandNames();
        private final String[] DHR_BAND_NAMES = IOUtils.getAlbedoDhrBandNames();
        private final String[] BHR_ALPHA_BAND_NAMES = IOUtils.getAlbedoBhrAlphaBandNames();
        private final String[] DHR_ALPHA_BAND_NAMES = IOUtils.getAlbedoDhrAlphaBandNames();
        private final String[] BHR_SIGMA_BAND_NAMES = IOUtils.getAlbedoBhrSigmaBandNames();
        private final String[] DHR_SIGMA_BAND_NAMES = IOUtils.getAlbedoDhrSigmaBandNames();

        private final SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        private Dimension tileSize;

        AlbedoNc4MainPart() {
            COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
            NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            tileSize = ImageManager.getPreferredTileSize(product);

            writeable.addDimension("y", product.getSceneRasterHeight());
            writeable.addDimension("x", product.getSceneRasterWidth());

            addGlobalAttributes(writeable);

            for (String bandName : BHR_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4BhrVariableAttribute(writeable, b);
                }
            }

            for (String bandName : DHR_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4DhrVariableAttribute(writeable, b);
                }
            }

            for (String bandName : BHR_ALPHA_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4BhrAlphaVariableAttribute(writeable, b);
                }
            }

            for (String bandName : DHR_ALPHA_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4DhrAlphaVariableAttribute(writeable, b);
                }
            }

            for (String bandName : BHR_SIGMA_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4BhrSigmaVariableAttribute(writeable, b);
                }

            }

            for (String bandName : DHR_SIGMA_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4DhrSigmaVariableAttribute(writeable, b);
                }
            }


            addAcAncillaryVariableAttributes(writeable, product);
        }

        private void addAcAncillaryVariableAttributes(NFileWriteable writeable, Product p) throws IOException {
            final Band relEntropyBand = p.getBand(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME);
            if (relEntropyBand != null) {
                addNc4BrdfAncillaryVariableAttribute(writeable, relEntropyBand,
                                                     "Relative Entropy", Float.NaN, null);
            }
            final Band weightedNumSamplesBand = p.getBand(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME);
            if (weightedNumSamplesBand != null) {
                addNc4BrdfAncillaryVariableAttribute(writeable, weightedNumSamplesBand,
                                                     "Weighted number of albedo samples", Float.NaN, null);
            }
            final Band goodnessOfFitBand = p.getBand(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME);
            if (goodnessOfFitBand != null) {
                addNc4BrdfAncillaryVariableAttribute(writeable, goodnessOfFitBand,
                                                     "Goodness of Fit", Float.NaN, null);
            }
            final Band snowFractionBand = p.getBand(AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME);
            if (snowFractionBand != null) {
                addNc4BrdfAncillaryVariableAttribute(writeable, snowFractionBand, "Snow Fraction", Float.NaN, null);
            }
            final Band dataMaskBand = p.getBand(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME);
            if (dataMaskBand != null) {
                addNc4BrdfAncillaryVariableAttribute(writeable, dataMaskBand, "Data Mask", Float.NaN, null);
            }
            final Band szaBand = p.getBand(AlbedoInversionConstants.ALB_SZA_BAND_NAME);
            if (szaBand != null) {
                addNc4BrdfAncillaryVariableAttribute(writeable, szaBand, "Solar Zenith Angle", Float.NaN, null);
            }
            final Band latBand = p.getBand(NcConstants.LAT_BAND_NAME);
            if (latBand != null) {
                addNc4BrdfLatLonVariableAttribute(writeable, latBand, "latitude coordinate", "latitude", "degrees_north");
            }
            final Band lonBand = p.getBand(NcConstants.LON_BAND_NAME);
            if (lonBand != null) {
                addNc4BrdfLatLonVariableAttribute(writeable, lonBand, "longitude coordinate", "longitude", "degrees_east");
            }
        }

        private void addGlobalAttributes(NFileWriteable writeable) throws IOException {
            // todo: clarify/discuss attribute entries
            writeable.addGlobalAttribute("Conventions", "CF-1.6");
            writeable.addGlobalAttribute("history", "QA4ECV Processing, 2014-2017");
            writeable.addGlobalAttribute("title", "QA4ECV Albedo Product");
            writeable.addGlobalAttribute("institution", "Mullard Space Science Laboratory, " +
                    "Department of Space and Climate Physics, University College London");
            writeable.addGlobalAttribute("source", "Satellite observations, BRDF/Albedo Inversion Model");
            writeable.addGlobalAttribute("references", "GlobAlbedo ATBD V3.1");
            writeable.addGlobalAttribute("comment", "none");
        }

        private void addNc4BhrVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "BHR_NIR" --> "Bi-Hemisphere Reflectance albedo - NIR band"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Bi-Hemisphere Reflectance albedo - " + bSplit[1].toUpperCase() + " band";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addNc4DhrVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "DHR_NIR" --> "Directional Hemisphere Reflectance albedo - NIR band"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Directional Hemisphere Reflectance albedo - " + bSplit[1].toUpperCase() + " band";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addNc4BhrAlphaVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "BHR_alpha_NIR_SW" --> "Bi-Hemisphere Reflectance albedo - (NIR,SW) alpha correlation term"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Bi-Hemisphere Reflectance albedo - (" +
                    bSplit[2].toUpperCase() + "," + bSplit[3].toUpperCase() + ") alpha correlation term";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addNc4DhrAlphaVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "BHR_alpha_NIR_SW" --> "Bi-Hemisphere Reflectance albedo - (NIR,SW) alpha correlation term"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Directional Hemisphere Reflectance albedo - (" +
                    bSplit[2].toUpperCase() + "," + bSplit[3].toUpperCase() + ") alpha correlation term";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addNc4BhrSigmaVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "BHR_sigma_NIR" --> "Uncertainty of Bi-Hemisphere Reflectance albedo - NIR band"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Uncertainty of Bi-Hemisphere Reflectance albedo - " +
                    bSplit[2].toUpperCase() + " band";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addNc4DhrSigmaVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "DHR_sigma_NIR" --> "Uncertainty of Directional Hemisphere Reflectance albedo - NIR band"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Uncertainty of Directional Hemisphere Reflectance albedo - " +
                    bSplit[2].toUpperCase() + " band";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addAlbedoVariableAttributes(Band b, NVariable variable, String longName) throws IOException {
            variable.addAttribute("long_name", longName);
            variable.addAttribute("fill_value", b.getNoDataValue());
            variable.addAttribute("coordinates", "lat lon");
        }


        private void addNc4BrdfAncillaryVariableAttribute(NFileWriteable writeable, Band b,
                                                          String longName,
                                                          float fillValue,
                                                          String unit) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            variable.addAttribute("long_name", longName);
            variable.addAttribute("fill_value", fillValue);
            variable.addAttribute("coordinates", "lat lon");
            addUnitAttribute(unit, variable);
        }

        private void addNc4BrdfLatLonVariableAttribute(NFileWriteable writeable, Band b,
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
            return writeable.addVariable(b.getName(),
                                         DataTypeUtils.getNetcdfDataType(ProductData.TYPE_FLOAT32),
                                         tileSize, writeable.getDimensions());
        }

    }
}