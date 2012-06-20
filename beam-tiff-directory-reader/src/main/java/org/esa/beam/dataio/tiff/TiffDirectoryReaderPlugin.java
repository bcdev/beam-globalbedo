/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.tiff;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.tiff.tgz.VirtualDirTgz;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * Plugin class for the {@link TiffDirectoryReader} reader.
 *
 * @author Olaf Danne
 */
public class TiffDirectoryReaderPlugin implements ProductReaderPlugIn {

    public static final String FORMAT_NAME_TIFF_DIR = "TIFF_DIRECTORY";

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_TIFF_DIR};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".txt", ".TXT"};
    private static final String READER_DESCRIPTION = "Directory of TIFF Products";
    private static final BeamFileFilter FILE_FILTER = new TiffDirFileFilter();

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        VirtualDir virtualDir;
        try {
            virtualDir = getInput(input);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        if (virtualDir == null) {
            return DecodeQualification.UNABLE;
        }

        if (virtualDir.isCompressed() || virtualDir.isArchive()) {
            if (isMatchingArchiveFileName(getFileInput(input).getName())) {
                // special case
                return DecodeQualification.INTENDED;
            }
            return DecodeQualification.SUITABLE;
        }

        String[] list;
        try {
            list = virtualDir.list("");
            if (list == null || list.length == 0) {
                return DecodeQualification.UNABLE;
            }
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        for (String fileName : list) {
            FileReader fileReader = null;
            try {
                File file = virtualDir.getFile(fileName);
                if (isMetadataFile(file)) {
                    fileReader = new FileReader(file);
                    TiffDirectoryMetadata hcMetadata = new TiffDirectoryMetadata(fileReader);
                    if (hcMetadata.isHyperspectralCamera()) {
                        return DecodeQualification.INTENDED;
                    }
                }
            } catch (IOException ignore) {
            } finally {
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }

        int tifCounter = 0;
        for (String fileName : list) {
            try {
                File file = virtualDir.getFile(fileName);
                if (isTifFile(file)) {
                    tifCounter++;
                }
            } catch (IOException ignore) {
            }
        }
        if (tifCounter > 1) {
            return DecodeQualification.INTENDED;
        }

        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new TiffDirectoryReader(this);
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
        return READER_DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return FILE_FILTER;
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
            virtualDir = new VirtualDirTgz(inputFile);
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

    static boolean isMetadataFile(File file) {
        if (file == null) {
            return false;
        }
        final String filename = file.getName().toLowerCase();
        return filename.toLowerCase().endsWith("_meta.txt");
    }

    static boolean isTifFile(File file) {
        if (file == null || FileUtils.getExtension(file) == null) {
            return false;
        }
        return FileUtils.getExtension(file).equalsIgnoreCase(".tif") ||
                FileUtils.getExtension(file).equalsIgnoreCase(".tiff");
    }

    static boolean isCompressedFile(File file) {
        final String extension = FileUtils.getExtension(file);
        return !StringUtils.isNullOrEmpty(extension) &&
                (extension.contains("zip") ||
                        extension.contains("tar") ||
                        extension.contains("tgz") ||
                        extension.contains("gz"));
    }

    static boolean isMatchingArchiveFileName(String fileName) {
        // todo: confirm
        return StringUtils.isNotNullAndNotEmpty(fileName) && fileName.startsWith("Sample");
    }

    private static class TiffDirFileFilter extends BeamFileFilter {

        public TiffDirFileFilter() {
            super();
            setFormatName(FORMAT_NAMES[0]);
            setDescription(READER_DESCRIPTION);
        }

        @Override
        public boolean accept(final File file) {
            return file.isDirectory() || isCompressedFile(file) || isTifFile(file) || isMetadataFile(file);
        }

        @Override
        public FileSelectionMode getFileSelectionMode() {
            return FileSelectionMode.FILES_AND_DIRECTORIES;
        }
    }
}