package org.esa.beam.globalbedo.inversion.io.netcdf;

import org.esa.beam.dataio.netcdf.AbstractNetCdfWriterPlugIn;
import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartWriter;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartWriter;
import org.esa.beam.dataio.netcdf.metadata.profiles.beam.BeamGeocodingPart;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.nc.NWritableFactory;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.framework.datamodel.*;
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
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 27.11.2015
 * Time: 17:26
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
        return "AVHRR AC NetCDF4 products";
    }

    @Override
    public ProfilePartWriter createGeoCodingPartWriter() {
        return new BeamGeocodingPart();
    }

    @Override
    public ProfileInitPartWriter createInitialisationPartWriter() {
        return new AvhrrAcMainPart();
    }

    @Override
    public NFileWriteable createWritable(String outputPath) throws IOException {
        return NWritableFactory.create(outputPath, "netcdf4");
    }


    private class AvhrrAcMainPart implements ProfileInitPartWriter {

        private final String[] PARAMETER_BAND_NAMES = IOUtils.getInversionParameterBandNames();
        private final String[][] UNCERTAINTY_BAND_NAMES = IOUtils.getInversionUncertaintyBandNames();

        private final SimpleDateFormat COMPACT_ISO_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        private Dimension tileSize;

        AvhrrAcMainPart() {
            COMPACT_ISO_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public void writeProductBody(ProfileWriteContext ctx, Product product) throws IOException {
            NFileWriteable writeable = ctx.getNetcdfFileWriteable();
            tileSize = ImageManager.getPreferredTileSize(product);

            writeable.addDimension("time", 1);  // todo: do we want this? yes, for Alex Loew?! make configurable!
            writeable.addDimension("y", product.getSceneRasterHeight());
            writeable.addDimension("x", product.getSceneRasterWidth());

            addGlobalAttributes(writeable, product);

            for (String bandName : PARAMETER_BAND_NAMES) {
                Band b = product.getBand(bandName);
                addNc4BrdfMeanVariableAttribute(writeable, b);
            }

            for (int i = 0; i < 3 * NUM_BBDR_WAVE_BANDS; i++) {
                for (int j = i; j < 3 * NUM_BBDR_WAVE_BANDS; j++) {
                    Band b = product.getBand(UNCERTAINTY_BAND_NAMES[i][j]);
                    addNc4BrdfVarVariableAttribute(writeable, b);
                }
            }
        }

        private void addAcAncillaryVariableAttributes(NFileWriteable writeable, Band b) throws IOException {
            // todo
            if (b.getName().equals(NcConstants.LAT_BAND_NAME)) {
                addNc4BrdfAncillaryVariableAttribute(writeable, b);
            } else if (b.getName().equals(NcConstants.LON_BAND_NAME)) {
                addNc4BrdfAncillaryVariableAttribute(writeable, b);
            } else {
                // the optional debug bands
                addNc4BrdfAncillaryVariableAttribute(writeable, b);
            }
        }

        private void addGlobalAttributes(NFileWriteable writeable, Product product) throws IOException {
            // todo: clarify/discuss attribute entries
            writeable.addGlobalAttribute("Conventions", "CF-1.6");
            writeable.addGlobalAttribute("history", "QA4ECV Processing, 2014-2017");
            writeable.addGlobalAttribute("title", "QA4ECV BRDF Product");
            writeable.addGlobalAttribute("institution", "Mullard Space Science Laboratory, " +
                    "Department of Space and Climate Physics, University College London");
            writeable.addGlobalAttribute("source", "Satellite observations, BRDF/Albedo Inversion Model");
            writeable.addGlobalAttribute("references", "GlobAlbedo ATBD V3.1");
            writeable.addGlobalAttribute("comment", "none");
            // todo: extend if needed
        }

        private void addNc4BrdfMeanVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = writeable.addVariable(b.getName(),
                                                       DataTypeUtils.getNetcdfDataType(ProductData.TYPE_FLOAT32),
                                                       tileSize, writeable.getDimensions());

            // e.g. "mean_VIS_f0" --> "Mean of parameter F0 in VIS"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Mean of parameter " + bSplit[2].toUpperCase() + " in " + bSplit[1];
            variable.addAttribute("long_name", longName);
            variable.addAttribute("fill_value", b.getNoDataValue());
            variable.addAttribute("scale_factor", b.getScalingFactor());
            variable.addAttribute("add_offset", b.getScalingOffset());
            variable.addAttribute("units", b.getUnit());
        }

        private void addNc4BrdfVarVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
            NVariable variable = writeable.addVariable(b.getName(),
                                                       DataTypeUtils.getNetcdfDataType(ProductData.TYPE_FLOAT32),
                                                       tileSize, writeable.getDimensions());

            // e.g. "VAR_VIS_f0_NIR_f1" --> "Covariance(VIS_F0,NIR_F1)"
            final String[] bSplit = b.getName().split("_");
            final String longName = "Covariance(" + bSplit[1] + "_" + bSplit[2].toUpperCase() + "," +
                    bSplit[3] + "_" + bSplit[4].toUpperCase() + ")";
            variable.addAttribute("long_name", longName);
            variable.addAttribute("fill_value", b.getNoDataValue());
            variable.addAttribute("scale_factor", b.getScalingFactor());
            variable.addAttribute("add_offset", b.getScalingOffset());
            variable.addAttribute("units", b.getUnit());
        }

        private void addNc4BrdfAncillaryVariableAttribute(NFileWriteable writeable, Band b) throws IOException {
           // todo
        }

    }
}