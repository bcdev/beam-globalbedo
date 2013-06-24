package org.esa.beam.dataio;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.util.io.FileUtils;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;
import ucar.unidata.io.bzip2.CBZip2InputStream;

import java.io.*;


/**
 * A read-only directory that can either be a directory in the file system, a .bz2-file, or a tar.bz2-file.
 * Files having '.gz' or '.tar.gz' extensions are automatically decompressed.
 *
 * @author Olaf Danne
 */
public class VirtualDirBz2 extends VirtualDir {
    private final File archiveFile;
    private File extractDir;

    public VirtualDirBz2(File bz2) throws IOException {
        if (bz2 == null) {
            throw new IllegalArgumentException("Input file shall not be null");
        }
        archiveFile = bz2;
        extractDir = null;
    }

    @Override
    public String getBasePath() {
        return archiveFile.getPath();
    }

    @Override
    public InputStream getInputStream(String path) throws IOException {
        final File file = getFile(path);
        return new FileInputStream(file);
    }

    @Override
    public File getFile(String path) throws IOException {
        if (isTarBz2(archiveFile.getName())) {
            ensureUnpackedTarBz2();
        } else if (isTarOnly(archiveFile.getName())) {
            ensureUnpackedTar();
        } else if (isBz2Only(archiveFile.getName())) {
            ensureUnpackedBz2();
        } else {
            throw new IllegalArgumentException("Input file " + archiveFile.getName() + " has wrong type.");
        }
        final File file = new File(extractDir, path);
        if (!(file.isFile() || file.isDirectory())) {
            throw new IOException();
        }
        return file;
    }

    @Override
    public String[] list(String path) throws IOException {
        final File file = getFile(path);
        return file.list();
    }

    @Override
    public void close() {
        if (extractDir != null) {
            FileUtils.deleteTree(extractDir);
            extractDir = null;
        }
    }

    @Override
    public boolean isCompressed() {
        return (isTarBz2(archiveFile.getName()) || isBz2Only(archiveFile.getName()));
    }

    @Override
    public boolean isArchive() {
        return true;
    }

    // todo: this code is almost completely duplicated from com.bc.ceres.core.VirtualDir
    // it might make sense to move it to a file utility class
    static File createTargetDirInTemp(String name) throws IOException {
        File tempDir = null;
        String tempDirName = System.getProperty("java.io.tmpdir");
        if (tempDirName != null) {
            tempDir = new File(tempDirName);
        }
        if (tempDir == null) {
            tempDir = new File(new File(System.getProperty("user.home", ".")), ".beam/temp");
            if (!tempDir.exists()) {
                if (!tempDir.mkdirs()) {
                    throw new IOException("unable to create directory: " + tempDir.getAbsolutePath());
                }
            }
        }

        File targetDir = new File(tempDir, name);
        if (!targetDir.exists()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("unable to create directory: " + targetDir.getAbsolutePath());
            }
        }

        return targetDir;
    }

    static String getFilenameFromPath(String path) {
        int lastSepIndex = path.lastIndexOf("/");
        if (lastSepIndex == -1) {
            lastSepIndex = path.lastIndexOf("\\");
            if (lastSepIndex == -1) {
                return path;
            }
        }

        return path.substring(lastSepIndex + 1, path.length());
    }

    static boolean isBz2Only(String filename) {
        return (filename.endsWith(".bz2") && !(filename.endsWith(".tar.bz2")));
    }

    static boolean isTarOnly(String filename) {
        return (filename.endsWith(".tar"));
    }

    static boolean isTarBz2(String filename) {
        return (filename.endsWith(".tar.bz2"));
    }

    private void ensureUnpackedTar() throws IOException {
        // unpacks *.tar
        if (extractDir == null) {
            extractDir = createTargetDirInTemp(archiveFile.getName());
            final TarInputStream tis = new TarInputStream(new BufferedInputStream(new FileInputStream(archiveFile)));
            writeTempFilesFromTarInputStream(tis);
        }
    }

    private void ensureUnpackedBz2() throws IOException {
        // unpacks (decompresses) *.bz2
        if (extractDir == null) {
            extractDir = createTargetDirInTemp(archiveFile.getName());
            BufferedInputStream bis = getBufferedInputStreamFromArchiveFile();
            final BufferedInputStream bisUnzipped = new BufferedInputStream(new CBZip2InputStream(bis));

            try {
                String archiveFileNameWithoutBz2 = FileUtils.getFilenameWithoutExtension(archiveFile.getName());
                final String fileNameFromPath = archiveFileNameWithoutBz2.concat(".hdf");

                final File targetDir = extractDir;
                writeTempHdfFile(bisUnzipped, fileNameFromPath, targetDir);

            } finally {
                bisUnzipped.close();
            }
        }
    }

    private void ensureUnpackedTarBz2() throws IOException {
        // unpack *.tar.bz2
        if (extractDir == null) {
            extractDir = createTargetDirInTemp(archiveFile.getName());
            BufferedInputStream bis = getBufferedInputStreamFromArchiveFile();
            // note that this will take quite a while for large datasets
            // in this case, bunzip2'ing the products externally in advance might be more appropriate...
            final TarInputStream tis = new TarInputStream(new BufferedInputStream(new CBZip2InputStream(bis)));
            writeTempFilesFromTarInputStream(tis);
        }
    }

    private void writeTempHdfFile(InputStream is, String targetFileName, File targetDir) throws IOException {
        final File targetFile = new File(targetDir, targetFileName);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (!targetFile.createNewFile()) {
            throw new IOException("Unable to create file: " + targetFile.getAbsolutePath());
        }

        final OutputStream outStream = new BufferedOutputStream(new FileOutputStream(targetFile));
        try {
            final byte data[] = new byte[1024 * 1024];
            int count;
            while ((count = is.read(data)) > 0) {
                outStream.write(data, 0, count);
            }
        } finally {
            outStream.flush();
            outStream.close();
        }
    }

    private void writeTempFilesFromTarInputStream(TarInputStream tis) throws IOException {
        TarEntry entry;
        while ((entry = tis.getNextEntry()) != null) {
            final String entryName = entry.getName();
            if (entry.isDirectory()) {
                final File directory = new File(extractDir, entryName);
                ensureDirectory(directory);
                continue;
            }

            final String fileNameFromPath = getFilenameFromPath(entryName);
            final int pathIndex = entryName.indexOf(fileNameFromPath);
            String tarPath = null;
            if (pathIndex > 0) {
                tarPath = entryName.substring(0, pathIndex - 1);
            }

            File targetDir;
            if (tarPath != null) {
                targetDir = new File(extractDir, tarPath);
            } else {
                targetDir = extractDir;
            }

            ensureDirectory(targetDir);
            writeTempHdfFile(tis, fileNameFromPath, targetDir);
        }
    }

    private BufferedInputStream getBufferedInputStreamFromArchiveFile() throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(archiveFile));
        // check the first two bytes before passing input stream to CBZip2InputStream
        // if they are 'B' and 'Z', skip them
        // (see http://www.kohsuke.org/bzip2/)
        int b1 = bis.read();
        int b2 = bis.read();
        // 'B' is 66 and 'Z' is 90
        if (!(b1 == 66) || !(b2 == 90)) {
            bis.reset();
        }
        return bis;
    }

    private void ensureDirectory(File targetDir) throws IOException {
        if (!targetDir.isDirectory()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("unable to create directory: " + targetDir.getAbsolutePath());
            }
        }
    }
}
