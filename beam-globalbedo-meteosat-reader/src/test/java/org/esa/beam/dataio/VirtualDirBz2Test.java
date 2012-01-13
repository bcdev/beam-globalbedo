package org.esa.beam.dataio;


import org.esa.beam.util.io.FileUtils;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class VirtualDirBz2Test {

//    private VirtualDirBz2 virtualDir;

    @Test
    public void testOpenBz2() throws IOException {
        final File testTgz = getTestFile("MSA_Albedo_test.tar.bz2");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testTgz);
        assertEquals(testTgz.getPath(), virtualDir.getBasePath());

        assertTrue(virtualDir.isCompressed());
        assertTrue(virtualDir.isArchive());
        virtualDir.close();
    }

    @Test
    public void testOpenTar() throws IOException {
        final File testTar = getTestFile("MSA_Albedo_test.tar");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testTar);
        assertEquals(testTar.getPath(), virtualDir.getBasePath());

        assertFalse(virtualDir.isCompressed());
        assertTrue(virtualDir.isArchive());
        virtualDir.close();
    }

    @SuppressWarnings({"UnusedDeclaration"})
    @Test
    public void testOpenNull() throws IOException {
        try {
            final VirtualDirBz2 vdBz2 = new VirtualDirBz2(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testBz2_getInputStream() throws IOException {
        final File testBz2 = getTestFile("MSA_Albedo_test.tar.bz2");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testBz2);
        assertExpectedInputStream(virtualDir);
        virtualDir.close();
    }

    @Test
    public void testBz2_getInputStream_invalidPath() throws IOException {
        final File testBz2 = getTestFile("MSA_Albedo_test.tar.bz2");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testBz2);
        assertInputStreamInvalidPath(virtualDir);
        virtualDir.close();
    }

    @Test
    public void testBz2_getFile() throws IOException {
        final File testBz2 = getTestFile("MSA_Albedo_test.tar.bz2");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testBz2);
        assertExpectedFile(virtualDir);
        virtualDir.close();
    }

    @Test
    public void testTar_getFile_invalidPath() throws IOException {
        final File testTar = getTestFile("MSA_Albedo_test.tar");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testTar);
        assertGetFileInvalidPath(virtualDir);
        virtualDir.close();
    }

    @Test
    public void testBz2_getFile_invalidPath() throws IOException {
        final File testBz2 = getTestFile("MSA_Albedo_test.tar.bz2");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testBz2);
        assertGetFileInvalidPath(virtualDir);
        virtualDir.close();
    }

    @Test
    public void testBz2_list() throws IOException {
        final File testBz2 = getTestFile("MSA_Albedo_test.tar.bz2");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testBz2);
        assertCorrectList(virtualDir);
        virtualDir.close();
    }

    @Test
    public void testTar_list_invalidPath() throws IOException {
        final File testTar = getTestFile("MSA_Albedo_test.tar");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testTar);
        assertListInvalidPath(virtualDir);
        virtualDir.close();
    }

    @Test
    public void testBz2_list_invalidPath() throws IOException {
        final File testBz2 = getTestFile("MSA_Albedo_test.tar.bz2");

        VirtualDirBz2 virtualDir = new VirtualDirBz2(testBz2);
        assertListInvalidPath(virtualDir);
        virtualDir.close();
    }

    @Test
    public void testCreateTargetDirInTemp_fromSystemPropertyTmpDir() throws IOException {
        final String tempDirName = System.getProperty("java.io.tmpdir");
        assertNotNull(tempDirName);

        File dirInTemp = null;
        try {
            dirInTemp = VirtualDirBz2.createTargetDirInTemp("wurst");
            assertNotNull(dirInTemp);
            assertTrue(dirInTemp.isDirectory());
            assertEquals(new File(tempDirName, "wurst").getAbsolutePath(), dirInTemp.getAbsolutePath());
        } finally {
            if (dirInTemp != null) {
                FileUtils.deleteTree(dirInTemp);
            }
        }
    }

    @Test
    public void testCreateTargetDirInTemp_fromSystemPropertyUserHome() throws IOException {
        final String oldTempDir = System.getProperty("java.io.tmpdir");
        System.clearProperty("java.io.tmpdir");
        final String userHome = System.getProperty("user.home");
        assertNotNull(userHome);

        File dirInTemp = null;
        try {
            dirInTemp = VirtualDirBz2.createTargetDirInTemp("Schneck");
            assertNotNull(dirInTemp);
            assertTrue(dirInTemp.isDirectory());
            assertEquals(new File(userHome, ".beam/temp/Schneck").getAbsolutePath(), dirInTemp.getAbsolutePath());
        } finally {
            System.setProperty("java.io.tmpdir", oldTempDir);
            if (dirInTemp != null) {
                FileUtils.deleteTree(dirInTemp);
            }
        }
    }

    @Test
    public void testGetFilenameFromPath_Windows() {
        final String fullPath = "C:\\bla\\blubber\\theFile.txt";
        assertEquals("theFile.txt", VirtualDirBz2.getFilenameFromPath(fullPath));

        final String relativePath = "bla\\schnuffi\\schnatter.txt";
        assertEquals("schnatter.txt", VirtualDirBz2.getFilenameFromPath(relativePath));
    }

    @Test
    public void testGetFilenameFromPath_Linux() {
        final String fullPath = "/bla/blubber/theFile.txt";
        assertEquals("theFile.txt", VirtualDirBz2.getFilenameFromPath(fullPath));

        final String relativePath = "bla/schnuffi/schnatter.txt";
        assertEquals("schnatter.txt", VirtualDirBz2.getFilenameFromPath(relativePath));
    }

    @Test
    public void testGetFilenameFromPath_notAPath() {
        final String file = "theFile.txt";
        assertEquals(file, VirtualDirBz2.getFilenameFromPath(file));
    }

    @Test
    public void testIsBz2() {
        assertTrue(VirtualDirBz2.isBz2("test_archive.tar.bz2"));

        assertFalse(VirtualDirBz2.isBz2("test_archive.tar"));
        assertFalse(VirtualDirBz2.isBz2("test_archive.exe"));
        assertFalse(VirtualDirBz2.isBz2("test_archive"));
    }

//    @After
//    public void tearDown() {
//        if (virtualDir != null) {
//            virtualDir.close();
//        }
//    }

    /* ---- helper methods called from the tests ---- */

    private void assertExpectedInputStream(VirtualDirBz2 virtualDir) throws IOException {
        final InputStream inputStream = virtualDir.getInputStream("MSA_Albedo_test2.hdf");
        try {
            assertNotNull(inputStream);

            final byte[] buffer = new byte[512];
            int bytesRead = inputStream.read(buffer);
            assertEquals(10, bytesRead);
            assertEquals("123456789", new String(buffer).trim());
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void assertInputStreamInvalidPath(VirtualDirBz2 virtualDir) {
        try {
            virtualDir.getInputStream("invalid_dir/no.file");
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    private void assertExpectedFile(VirtualDirBz2 virtualDir) throws IOException {
        final File file = virtualDir.getFile("MSA_Albedo_test1.hdf");
        assertNotNull(file);
        assertTrue((file.isFile()));

        final FileInputStream inputStream = new FileInputStream(file);
        try {
            final byte[] buffer = new byte[512];
            int bytesRead = inputStream.read(buffer);
            assertEquals(4, bytesRead);
            assertEquals("123", new String(buffer).trim());
        } finally {
            inputStream.close();
        }
    }

    private void assertGetFileInvalidPath(VirtualDirBz2 virtualDir) {
        try {
            virtualDir.getFile("invalid_dir/missing.file");
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    private void assertCorrectList(VirtualDirBz2 virtualDir) throws IOException {
        String[] list = virtualDir.list("");
        List<String> dirList = Arrays.asList(list);
        assertEquals(2, dirList.size());
        assertTrue(dirList.contains("MSA_Albedo_test1.hdf"));
        assertTrue(dirList.contains("MSA_Albedo_test2.hdf"));
    }

    private void assertListInvalidPath(VirtualDirBz2 virtualDir) {
        try {
            virtualDir.list("in/valid/path");
            fail("IOException expected");
        } catch (IOException expected) {
        }
    }

    private static File getTestFile(String name) {
        final URL url = VirtualDirBz2.class.getResource(name);
        if (url == null) {
            fail("Cannot find resource file: " + name);
        }
        String filePath = null;
        try {
            filePath = URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            fail(e.getMessage());
        }
        return new File(filePath);
    }

}
