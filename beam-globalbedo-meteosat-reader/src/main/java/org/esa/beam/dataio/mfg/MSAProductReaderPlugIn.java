package org.esa.beam.dataio.mfg;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.VirtualDirBz2;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * PlugIn class which provides a Level 1 Sentinel 3 product reader to the framework.
 *
 * @author Marco Peters
 * @since 1.0
 */
public class MSAProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME_METEOSAT_MSA = "GLOBALBEDO-METEOSAT-SURFACE-ALBEDO";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "METEOSAT MSA Format";
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
        if (StringUtils.isNullOrEmpty(extension)) {
            return false;
        }

        return extension.contains("zip")
                || extension.contains("tar")
                || extension.contains("tgz")
                || extension.contains("gz")
                || extension.contains("bz2");
    }



    private boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputZipFileNameValid(inputFile.getName());
    }

    private boolean isInputZipFileNameValid(String fileName) {
//        return fileName.matches("METEOSAT7-MVIRI-MTPMSA1-NA-1-[0-9]{14}.[0-9]{9}Z-[0-9]{7}.(?i)(zip)");
        return (fileName.matches("METEOSAT7-MVIRI-MTPMSA1-NA-1-[0-9]{14}.[0-9]{9}Z-[0-9]{7}.tar.(?i)(bz2)") ||
                fileName.matches("METEOSAT7-MVIRI-MTPMSA1-NA-1-[0-9]{14}.[0-9]{9}Z-[0-9]{7}.tar"));
    }

    @Override
    public Class[] getInputTypes() {
        return SUPPORTED_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new MSAProductReader(this);
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
