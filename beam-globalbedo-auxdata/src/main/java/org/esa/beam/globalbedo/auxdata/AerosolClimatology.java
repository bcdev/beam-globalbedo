package org.esa.beam.globalbedo.auxdata;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 03.02.2016
 * Time: 12:45
 *
 * @author olafd
 */
public class AerosolClimatology {

    private static AerosolClimatology instance = null;
    private Product aotProduct;

    public static AerosolClimatology getInstance() {
        if(instance == null) {
            instance = new AerosolClimatology();
        }
        return instance;
    }

    public Product getAotProduct() {
        return aotProduct;
    }

    private AerosolClimatology() {
        loadAerosolClimatologyProduct();
    }

    private void loadAerosolClimatologyProduct() {
        final String aotFilename = AuxdataConstants.AEROSOL_CLIMATOLOGY_FILE;
        final String aotFilePath = getClass().getResource(aotFilename).getPath();

        aotProduct = null;
        try {
            Logger.getGlobal().log(Level.INFO, "Reading AOT climatology file '" + aotFilePath + "'...");
            aotProduct = ProductIO.readProduct(new File(aotFilePath));
        } catch (IOException e) {
            Logger.getGlobal().log(Level.WARNING, "Warning: cannot open or read AOT climatology file.");
        }
    }
}
