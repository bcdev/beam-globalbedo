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
public class MisrProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME_MISR = "GLOBALBEDO-MISR";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "MISR Format";
    private static final String FILE_EXTENSION = ".hdf";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_MISR};

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
                    extension.contains("gz") || extension.contains("bz2"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputTarFileNameValid(inputFile.getName());
    }

    private boolean isInputTarFileNameValid(String fileName) {
//      e.g.  MISR_AM1_GRP_ELLIPSOID_GM_P119_O061188_AA_F03_0024.tar
        return (fileName.matches("MISR_AM1_GRP_ELLIPSOID_GM_P[0-9]{3}_O[0-9]{6}_AA_F[0-9]{2}_[0-9]{4}.(?i)(tar)"));
    }

    @Override
    public Class[] getInputTypes() {
        return SUPPORTED_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new MisrProductReader(this);
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
