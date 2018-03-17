package org.esa.beam.globalbedo.inversion.io.netcdf;

import com.bc.ceres.binding.converters.DateFormatConverter;
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
import org.esa.beam.globalbedo.inversion.spectral.SpectralIOUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writer for CF compliant Albedo NetCDF4 output
 *
 * @author olafd
 */
public class SpectralAlbedoNc4WriterPlugIn extends AbstractNetCdfWriterPlugIn {

    @Override
    public String[] getFormatNames() {
        return new String[]{"NetCDF4-GA-SPECTRAL-ALBEDO"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{Constants.FILE_EXTENSION_NC};
    }

    @Override
    public String getDescription(Locale locale) {
        return "QA4ECV Spectral Albedo NetCDF4 products";
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new AlbedoInversionGeocodingPart();
//        return new CfGeocodingPart();
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new AlbedoNc4MainPart();
    }

    @Override
    public NFileWriteable createWritable(String outputPath) throws IOException {
        this.outputPath = outputPath;
        return NWritableFactory.create(outputPath, "netcdf4");
    }

    private String outputPath;

    private class AlbedoNc4MainPart implements ProfileInitPartWriter {

        private static final float NODATA = AlbedoInversionConstants.NO_DATA_VALUE;

        private static final int numSdrBands = 6;
        private String[] BHR_BAND_NAMES;

        private String[] DHR_BAND_NAMES;
        private String[] BHR_SIGMA_BAND_NAMES;
        private String[] DHR_SIGMA_BAND_NAMES;
        private final SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

        private Dimension tileSize;

        private Map<Integer, String> spectralWaveBandsMap = new HashMap<>();
        private int[] sigmaBandIndices;

        AlbedoNc4MainPart() {
            COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
            NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            tileSize = ImageManager.getPreferredTileSize(product);
            writeable.addDimension("y", product.getSceneRasterHeight());
            writeable.addDimension("x", product.getSceneRasterWidth());

            addGlobalAttributes(writeable, product, outputPath);

            setupSpectralWaveBandsMap(numSdrBands);
            sigmaBandIndices = new int[]{1, 2, 3, 4, 5, 6, 7};

            BHR_BAND_NAMES = SpectralIOUtils.getSpectralAlbedoBhrBandNames(numSdrBands + 1, spectralWaveBandsMap);
            DHR_BAND_NAMES = SpectralIOUtils.getSpectralAlbedoDhrBandNames(numSdrBands + 1, spectralWaveBandsMap);
            BHR_SIGMA_BAND_NAMES = SpectralIOUtils.getSpectralAlbedoBhrSigmaBandNames(sigmaBandIndices);
            DHR_SIGMA_BAND_NAMES = SpectralIOUtils.getSpectralAlbedoDhrSigmaBandNames(sigmaBandIndices);

            for (String bandName : BHR_BAND_NAMES) {
                if (bandName != null) {
                    BeamLogManager.getSystemLogger().log(Level.INFO, "bandName: " + bandName);
                    Band b = product.getBand(bandName);
                    if (b != null) {
                        addNc4BhrVariableWithAttributes(writeable, b);
                    }
                }
            }

            for (String bandName : DHR_BAND_NAMES) {
                if (bandName != null) {
                    Band b = product.getBand(bandName);
                    if (b != null) {
                        addNc4DhrVariableWithAttributes(writeable, b);
                    }
                }
            }

            for (String bandName : BHR_SIGMA_BAND_NAMES) {
                if (bandName != null) {
                    Band b = product.getBand(bandName);
                    if (b != null) {
                        addNc4BhrSigmaVariableWithAttributes(writeable, b);
                    }
                }
            }

            for (String bandName : DHR_SIGMA_BAND_NAMES) {
                if (bandName != null) {
                    Band b = product.getBand(bandName);
                    if (b != null) {
                        addNc4DhrSigmaVariableWithAttributes(writeable, b);
                    }
                }
            }


            addAcAncillaryVariableAttributes(writeable, product);
        }

        private void addAcAncillaryVariableAttributes(NFileWriteable writeable, Product p) throws IOException {
            final Band relEntropyBand = p.getBand(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME);
            if (relEntropyBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, relEntropyBand,
                                                          "Relative Entropy", NODATA, "1");
            }
            final Band weightedNumSamplesBand = p.getBand(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME);
            if (weightedNumSamplesBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, weightedNumSamplesBand,
                                                          "Weighted number of albedo samples", NODATA, "1");
            }
            final Band goodnessOfFitBand = p.getBand(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME);
            if (goodnessOfFitBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, goodnessOfFitBand,
                                                          "Goodness of Fit", NODATA, "1");
            }
            final Band snowFractionBand = p.getBand(AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME);
            if (snowFractionBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, snowFractionBand, "Snow Fraction", NODATA, "1");
            }
            final Band dataMaskBand = p.getBand(AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME);
            if (dataMaskBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, dataMaskBand, "Data Mask", NODATA, "1");
            }
            final Band szaBand = p.getBand(AlbedoInversionConstants.ALB_SZA_BAND_NAME);
            if (szaBand != null) {
                addNc4BrdfAncillaryVariableWithAttributes(writeable, szaBand, "Solar Zenith Angle", NODATA, "degrees");
            }
//            final Band latBand = p.getBand(NcConstants.LAT_BAND_NAME);
//            if (latBand != null) {
//                addNc4BrdfLatLonVariableWithAttributes(writeable, latBand, "latitude coordinate", "latitude", "degrees");
//            }
//            final Band lonBand = p.getBand(NcConstants.LON_BAND_NAME);
//            if (lonBand != null) {
//                addNc4BrdfLatLonVariableWithAttributes(writeable, lonBand, "longitude coordinate", "longitude", "degrees");
//            }
        }

        private void addGlobalAttributes(NFileWriteable writeable, Product product, String outputPath) throws IOException {
            // this follows CCI_Data_Requirements_Iss1.2_Mar2015.pdf as closely as possible, as we are forced
            // to do this like the CCIs
            writeable.addGlobalAttribute("title", "QA4ECV Spectral Albedo Product");
            final String institution = "Mullard Space Science Laboratory, " +
                    "Department of Space and Climate Physics, University College London";
            writeable.addGlobalAttribute("institution", institution);
            writeable.addGlobalAttribute("source", "Satellite observations, BRDF/Albedo Inversion Model");
            writeable.addGlobalAttribute("history", "QA4ECV Processing, 2014-2017");
            writeable.addGlobalAttribute("references", "www.qa4ecv.eu");
            writeable.addGlobalAttribute("tracking_id", UUID.randomUUID().toString());
            writeable.addGlobalAttribute("Conventions", "CF-1.6");
            writeable.addGlobalAttribute("product_version", "1.0");
            writeable.addGlobalAttribute("summary", "This dataset contains Level-3 daily surface spectral albedo " +
                    "products. Level-3 data are raw observations processed to geophysical quantities, and placed onto" +
                    " a regular grid.");
            writeable.addGlobalAttribute("keywords", "Albedo, MODIS, spectral mapping");
            writeable.addGlobalAttribute("id", product.getName() + ".nc");
            writeable.addGlobalAttribute("naming_authority", "ucl.ac.uk");
            writeable.addGlobalAttribute("keywords_vocabulary", "");
            writeable.addGlobalAttribute("cdm_data_type", "Alb");

            writeable.addGlobalAttribute("comment", "These data were produced in the frame of the QA4ECV " +
                    "(Quality Assurance for Essential Climate Variables) project, funded under theme 9 (Space) " +
                    "of the European Union Framework Program 7.");

            final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            final Date creationDate = calendar.getTime();
            final DateFormatConverter dateFormatConverter = new DateFormatConverter(COMPACT_ISO_FORMAT);
            writeable.addGlobalAttribute("date_created", dateFormatConverter.format(creationDate));
            writeable.addGlobalAttribute("creator_name", institution);
            writeable.addGlobalAttribute("creator_url", "www.ucl.ac.uk/mssl");
            writeable.addGlobalAttribute("creator_email", "qa4ecv-land-all@ucl.ac.uk");
            writeable.addGlobalAttribute("project", "QA4ECV - European Union Framework Program 7");
            writeable.addGlobalAttribute("geospatial_lat_min", "30.0");
            writeable.addGlobalAttribute("geospatial_lat_max", "70.0");
            writeable.addGlobalAttribute("geospatial_lon_min", "-10.0");
            writeable.addGlobalAttribute("geospatial_lon_max", "30.0");
            writeable.addGlobalAttribute("geospatial_vertical_min", "0.0");
            writeable.addGlobalAttribute("geospatial_vertical_max", "0.0");

            setProductTime(writeable, outputPath, calendar, dateFormatConverter);

            writeable.addGlobalAttribute("time_coverage_duration", "P1D");
            writeable.addGlobalAttribute("time_coverage_resolution", "P1D");
            writeable.addGlobalAttribute("standard_name_vocabulary",
                                         "NetCDF Climate and Forecast (CF) Metadata Convention version 18");
            writeable.addGlobalAttribute("license", "QA4ECV Data Policy: free and open access");
            writeable.addGlobalAttribute("platform", "Envisat, Spot-VGT");
            writeable.addGlobalAttribute("sensor", "MERIS, HRVIR");
            writeable.addGlobalAttribute("spatial_resolution", "0.5/0.05");
            writeable.addGlobalAttribute("geospatial_lat_units", "degrees_north");
            writeable.addGlobalAttribute("geospatial_lon_units", "degrees_east");
            writeable.addGlobalAttribute("geospatial_lat_resolution", "0.5/0.05");
            writeable.addGlobalAttribute("geospatial_lon_resolution", "0.5/0.05");
        }

        private void setProductTime(NFileWriteable writeable, String outputPath, Calendar calendar, DateFormatConverter dateFormatConverter) throws IOException {
            final Pattern pattern = Pattern.compile(".[0-9]{7}.");
            Matcher matcher = pattern.matcher(outputPath);
            if (matcher.find()) {
                final int start = matcher.start() + 1;
                final int end = matcher.end() - 1;
                final int year = Integer.parseInt(outputPath.substring(start, start + 4));
                final int doy = Integer.parseInt(outputPath.substring(start + 4, end));
//                final int zoneOffset = -3600 * 1000;
                final int zoneOffset = 0;
                calendar.set(GregorianCalendar.YEAR, year);
                calendar.set(GregorianCalendar.DAY_OF_YEAR, doy);
                calendar.set(GregorianCalendar.HOUR_OF_DAY, 0);
                calendar.set(GregorianCalendar.MINUTE, 0);
                calendar.set(GregorianCalendar.SECOND, 0);
                calendar.set(GregorianCalendar.ZONE_OFFSET, zoneOffset);
                final String productStart = dateFormatConverter.format(calendar.getTime());
                writeable.addGlobalAttribute("time_coverage_start", productStart);
                calendar.set(GregorianCalendar.HOUR_OF_DAY, 23);
                calendar.set(GregorianCalendar.MINUTE, 59);
                calendar.set(GregorianCalendar.SECOND, 59);
                calendar.set(GregorianCalendar.ZONE_OFFSET, zoneOffset);
                final String productEnd = dateFormatConverter.format(calendar.getTime());
                writeable.addGlobalAttribute("time_coverage_end", productEnd);
            }
        }

        private void addNc4BhrVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "BHR_NIR" --> "Bi-Hemisphere Reflectance albedo - NIR band"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Bi-Hemisphere Reflectance albedo - " + bSplit[1].toUpperCase() + " band";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addNc4DhrVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "DHR_NIR" --> "Directional Hemisphere Reflectance albedo - NIR band"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Directional Hemisphere Reflectance albedo - " + bSplit[1].toUpperCase() + " band";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addNc4BhrSigmaVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "BHR_sigma_NIR" --> "Uncertainty of Bi-Hemisphere Reflectance albedo - NIR band"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Uncertainty of Bi-Hemisphere Reflectance albedo - " +
                    bSplit[2].toUpperCase() + " band";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addNc4DhrSigmaVariableWithAttributes(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = addNc4Variable(writeable, b);
            // e.g. "DHR_sigma_NIR" --> "Uncertainty of Directional Hemisphere Reflectance albedo - NIR band"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Uncertainty of Directional Hemisphere Reflectance albedo - " +
                    bSplit[2].toUpperCase() + " band";
            addAlbedoVariableAttributes(b, variable, longName);
        }

        private void addAlbedoVariableAttributes(Band b, NVariable variable, String longName) throws IOException {
            variable.addAttribute("long_name", longName);
            final Float _fillValue = (float) b.getNoDataValue();
            variable.addAttribute("_FillValue", _fillValue);
            variable.addAttribute("units", "1");
            variable.addAttribute("coordinates", "lat lon");
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

        private void setupSpectralWaveBandsMap(int numSdrBands) {
            for (int i = 0; i < numSdrBands; i++) {
                spectralWaveBandsMap.put(i, "b" + (i + 1));
            }
        }
    }
}