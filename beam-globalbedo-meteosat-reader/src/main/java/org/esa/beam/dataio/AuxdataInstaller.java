package org.esa.beam.dataio;

import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * This class is responsible for installing Meteosat Reader auxiliary data
 *
 * @author olafd
 */
public class AuxdataInstaller {
    private AuxdataInstaller() {
    }

    static void installAuxdata(Class providerClass) {
        installAuxdata(ResourceInstaller.getSourceUrl(providerClass));
    }

    static void installAuxdata(URL sourceBaseUrl) {
        final String auxdataSrcPath = "org/esa/beam/dataio";
        final String auxdataDestPath = "beam-globalbedo/beam-globalbedo-meteosat-reader/" + auxdataSrcPath;

        installAuxdata(sourceBaseUrl, auxdataSrcPath, auxdataDestPath);
    }

    private static void installAuxdata(URL sourceBaseUrl, String sourceRelPath, String targetRelPath) {
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceBaseUrl, sourceRelPath, new File(SystemUtils.getApplicationDataDir(), targetRelPath));
        try {
            resourceInstaller.install(".*", com.bc.ceres.core.ProgressMonitor.NULL);
            BeamLogManager.getSystemLogger().info("Installed GlobAlbedo Meteosat Reader auxiliary data '" + sourceRelPath + "'");
        } catch (IOException ignore) {
            BeamLogManager.getSystemLogger().severe("Failed to install GlobAlbedo Meteosat Reader auxiliary data '" + sourceRelPath + "'");
        }
    }
}
