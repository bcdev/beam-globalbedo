package org.esa.beam.globalbedo.inversion.io.netcdf;

import org.esa.beam.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfGeocodingPart;
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

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS;

/**
 * Writer for CF compliant BRDF NetCDF4 output
 *
 * @author olafd
 */
public class BrdfNc4WriterPlugIn extends AbstractNetCdfWriterPlugIn {

    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF4-GA-BRDF"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{Constants.FILE_EXTENSION_NC};
    }

    @Override
    public String getDescription(Locale locale) {
        return "QA4ECV BRDF NetCDF4 products";
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        //        return new AlbedoInversionGeocodingPart();
        return new CfGeocodingPart();
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new BrdfNc4MainPart();
    }

    @Override
    public NFileWriteable createWritable(String outputPath) throws IOException {
        return NWritableFactory.create(outputPath, "netcdf4");
    }


    private class BrdfNc4MainPart implements ProfileInitPartWriter {

        private static final float NODATA = AlbedoInversionConstants.NO_DATA_VALUE;

        private final String[] PARAMETER_BAND_NAMES = IOUtils.getInversionParameterBandNames();
        private final String[][] UNCERTAINTY_BAND_NAMES = IOUtils.getInversionUncertaintyBandNames();

        private final SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        private Dimension tileSize;

        BrdfNc4MainPart() {
            COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
            NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            tileSize = ImageManager.getPreferredTileSize(product);

            writeable.addDimension("y", product.getSceneRasterHeight());
            writeable.addDimension("x", product.getSceneRasterWidth());

            addGlobalAttributes(writeable);

            for (String bandName : PARAMETER_BAND_NAMES) {
                Band b = product.getBand(bandName);
                if (b != null) {
                    addNc4BrdfMeanVariableWithAttributes(writeable, b);
                }
            }

            for (int i = 0; i < 3 * NUM_BBDR_WAVE_BANDS; i++) {
                for (int j = i; j < 3 * NUM_BBDR_WAVE_BANDS; j++) {
                    Band b = product.getBand(UNCERTAINTY_BAND_NAMES[i][j]);
                    if (b != null) {
                        addNc4BrdfVarVariableWithAttributes(writeable, b);
                    }
                }
            }

            addAcAncillaryVariableAttributes(writeable, product);
        }

        private void addAcAncillaryVariableAttributes(NFileWriteable writeable, Product p) throws IOException {
            final Band entropyBand = p.getBand(AlbedoInversionConstants.INV_ENTROPY_BAND_NAME);
            if (entropyBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, entropyBand,
                                                          "Entropy", NODATA, "1");
            }
            final Band relEntropyBand = p.getBand(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME);
            if (relEntropyBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, relEntropyBand,
                                                          "Relative Entropy", NODATA, "1");
            }
            final Band weightedNumSamplesBand = p.getBand(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME);
            if (weightedNumSamplesBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, weightedNumSamplesBand,
                                                          "Weighted number of BRDF samples", NODATA, "1");
            }
            final Band goodnessOfFitBand = p.getBand(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME);
            if (goodnessOfFitBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, goodnessOfFitBand,
                                                          "Goodness of Fit", NODATA, "1");
            }
            final Band proportionNSamplesBand = p.getBand(AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME);
            if (proportionNSamplesBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, proportionNSamplesBand, "Snow Fraction", NODATA, "1");
            }
            final Band daysClosestSampleBand = p.getBand(AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME);
            if (daysClosestSampleBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, daysClosestSampleBand,
                                                          "Number of days to the closest sample", NODATA, "days");
            }
            final Band latBand = p.getBand(NcConstants.LAT_BAND_NAME);
            if (latBand != null) {
                addNc4BrdfLatLonVariableWithAttributes(writeable, latBand, "latitude coordinate", "latitude", "degrees");
            }
            final Band lonBand = p.getBand(NcConstants.LON_BAND_NAME);
            if (lonBand != null) {
                addNc4BrdfLatLonVariableWithAttributes(writeable, lonBand, "longitude coordinate", "longitude", "degrees");
            }
        }

        private void addGlobalAttributes(NFileWriteable writeable) throws IOException {
            // todo: clarify/discuss attribute entries
            writeable.addGlobalAttribute("Conventions", "CF-1.6");
            writeable.addGlobalAttribute("history", "QA4ECV Processing, 2014-2017");
            writeable.addGlobalAttribute("title", "QA4ECV BRDF Product");
            writeable.addGlobalAttribute("institution", "Mullard Space Science Laboratory, " +
                    "Department of Space and Climate Physics, University College London");
            writeable.addGlobalAttribute("source", "Satellite observations, BRDF/Albedo Inversion Model");
            writeable.addGlobalAttribute("references", "GlobAlbedo ATBD V3.1");
            writeable.addGlobalAttribute("comment", "none");
        }

        private void addNc4BrdfMeanVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "mean_VIS_f0" --> "Mean of parameter F0 in VIS"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Mean of parameter " + bSplit[2].toUpperCase() + " in " + bSplit[1];
            variable.addAttribute("long_name", longName);
            final Float _fillValue = (float) b.getNoDataValue();
            variable.addAttribute("_FillValue", _fillValue);
            variable.addAttribute("coordinates", "lat lon");
            variable.addAttribute("units", "1");
        }

        private void addNc4BrdfVarVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "VAR_VIS_f0_NIR_f1" --> "Covariance(VIS_F0,NIR_F1)"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Covariance(" + bSplit[1] + "_" + bSplit[2].toUpperCase() + "," +
                    bSplit[3] + "_" + bSplit[4].toUpperCase() + ")";
            variable.addAttribute("long_name", longName);
            final Float _fillValue = (float) b.getNoDataValue();
            variable.addAttribute("_FillValue", _fillValue);
            variable.addAttribute("coordinates", "lat lon");
            variable.addAttribute("units", "1");
        }

        private void addNc4BrdfAncillaryVariableWithAttributes(NFileWriteable writeable, Band b,
                                                               String longName,
                                                               float fillValue,
                                                               String unit) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            variable.addAttribute("long_name", longName);
            final Float _fillValue = fillValue;
            variable.addAttribute("_FillValue", _fillValue);
            variable.addAttribute("coordinates", "lat lon");
            addUnitAttribute(unit, variable);
        }

        private void addNc4BrdfLatLonVariableWithAttributes(NFileWriteable writeable, Band b,
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