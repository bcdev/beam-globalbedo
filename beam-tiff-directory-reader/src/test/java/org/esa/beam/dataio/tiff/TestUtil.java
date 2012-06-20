package org.esa.beam.dataio.tiff;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class TestUtil {

    public static File getTestFile(String file) {
        final File testTgz = getTestFileOrDirectory(file);
        assertTrue(testTgz.isFile());
        return testTgz;
    }

    public static File getTestDirectory(String file) {
        final File testTgz = getTestFileOrDirectory(file);
        assertTrue(testTgz.isDirectory());
        return testTgz;
    }

    private static File getTestFileOrDirectory(String file) {
        File testTgz = new File("./beam-tiff-directory-reader/src/test/resources/org/esa/beam/dataio/tiff/" + file);
        if (!testTgz.exists()) {
            testTgz = new File("./src/test/resources/org/esa/beam/tiff/" + file);
        }
        return testTgz;
    }
}
