package org.esa.beam.dataio.mod35;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * PlugIn class which provides a MODIS MOD35 cloud mask HDF product reader to the framework.
 *
 * @author Olaf Danne
 */
public class Modis35ProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME_MODIS35 = "MOD35-L2";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "MODIS35 Format";
    private static final String FILE_EXTENSION = ".hdf";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_MODIS35};

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (isInputValid(input)) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    /**
     * Retrieves the input file from the input object passed in
     *
     * @param input the input object (File or String)
     * @return the File representing the product
     * @throws java.io.IOException on disk access failures
     */
    public static File getInput(Object input) throws IOException {
        return getFileInput(input);
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    private boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputHdfFileNameValid(inputFile.getName());
    }

    private boolean isInputHdfFileNameValid(String fileName) {
//        MOD35_L2.A2005180.1455.006.2012278173052.hdf
        return (fileName.matches("MOD35_L2.A[0-9]{7}.[0-9]{4}.006.[0-9]{13}.(?i)(hdf)"));
    }

    @Override
    public Class[] getInputTypes() {
        return SUPPORTED_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new Modis35ProductReader(this);
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
