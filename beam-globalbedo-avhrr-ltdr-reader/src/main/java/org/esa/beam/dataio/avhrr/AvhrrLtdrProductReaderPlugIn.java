package org.esa.beam.dataio.avhrr;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * PlugIn class which provides an AVHRR LTDR HDF product reader.
 *
 * @author Olaf Danne
 */
public class AvhrrLtdrProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME_AVHRR_LTDR = "AVHRR-LTDR";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "AVHRR LTDR Format";
    private static final String FILE_EXTENSION = ".hdf";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_AVHRR_LTDR};

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (isInputValid(input)) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    private boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputHdfFileNameValid(inputFile.getName());
    }

    private boolean isInputHdfFileNameValid(String fileName) {
        // e.g.
//        AVH02C1.A2005017.N16.004.2014115230321.hdf
//        AVH02C1.A2005183.N18.004.2014084122657.hdf
        return (fileName.matches("AVH[0-9]{2}C1.A[0-9]{7}.N[0-9]{2}.[0-9]{3}.[0-9]{13}.(?i)(hdf)"));
    }

    @Override
    public Class[] getInputTypes() {
        return SUPPORTED_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new AvhrrLtdrProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAMES[0], FILE_EXTENSION, DESCRIPTION);
    }

}
