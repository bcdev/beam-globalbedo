package org.esa.beam.dataio;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * PlugIn class which provides a Meteosat First Generation MSA product reader to the framework.
 *
 * @author Olaf Danne
 */
public class Modis29ProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME_METEOSAT_MSA = "GLOBALBEDO-MODIS29-SEA-ICE";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "MODIS29.5 Format";
    private static final String FILE_EXTENSION = ".hdf";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_METEOSAT_MSA};

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (isInputValid(input)) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    /**
     * Retrieves the VirtualDir for input from the input object passed in
     *
     * @param input the input object (File or String)
     * @return the VirtualDir representing the product
     * @throws java.io.IOException on disk access failures
     */
    public static VirtualDir getInput(Object input) throws IOException {
        File inputFile = getFileInput(input);

        if (inputFile.isFile() && !isCompressedFile(inputFile)) {
            inputFile = inputFile.getParentFile();
        }

        VirtualDir virtualDir = VirtualDir.create(inputFile);
        if (virtualDir == null) {
            // todo: move VirtualDirBz2 into a utility package! appears now several times!!!
            virtualDir = new VirtualDirBz2(inputFile);
        }
        return virtualDir;
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    static boolean isCompressedFile(File file) {
        final String extension = FileUtils.getExtension(file);
        if (!StringUtils.isNullOrEmpty(extension)) {
            if ((extension.contains("zip") || extension.contains("tar") || extension.contains("tgz") ||
                    extension.contains("gz"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputTarFileNameValid(inputFile.getName());
    }

    private boolean isInputHdfFileNameValid(String fileName) {
//        MOD29.2002076.0140.hdf
        return (fileName.matches("MOD29.[0-9]{7}.[0-9]{4}.(?i)(hdf)"));
    }

    private boolean isInputZipFileNameValid(String fileName) {
//        MOD29.2002076.0140.zip
        return (fileName.matches("MOD29.[0-9]{7}.[0-9]{4}.(?i)(zip)"));
    }

    private boolean isInputTarFileNameValid(String fileName) {
//        MOD29.2002076.0140.zip
        return (fileName.matches("MOD29.[0-9]{7}.[0-9]{4}.(?i)(tar)"));
    }


    @Override
    public Class[] getInputTypes() {
        return SUPPORTED_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new Modis29ProductReader(this);
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
