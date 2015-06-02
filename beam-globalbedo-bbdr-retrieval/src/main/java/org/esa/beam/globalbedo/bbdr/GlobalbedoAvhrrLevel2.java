package org.esa.beam.globalbedo.bbdr;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AVHRR LTDR Level 2 processor for BBDR and kernel parameter computation, and tile extraction
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.avhrr",
                  description = "Level 2 processor for BBDR and kernel parameter computation, and tile extraction",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2015 by Brockmann Consult")
public class GlobalbedoAvhrrLevel2 extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "")
    private String tile;

    private static final Sensor sensor = Sensor.AVHRR;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        if (sourceProduct.getPreferredTileSize() == null) {
            sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 45);
            logger.log(Level.ALL, "adjusting tile size to: " + sourceProduct.getPreferredTileSize());
        }

        BbdrAvhrrOp bbdrOp = new BbdrAvhrrOp();
        bbdrOp.setParameterDefaultValues();
        bbdrOp.setSourceProduct(sourceProduct);
        bbdrOp.setParameter("sensor", sensor);
        Product bbdrProduct = bbdrOp.getTargetProduct();
        if (tile != null && !tile.isEmpty()) {
            setTargetProduct(TileExtractor.reproject(bbdrProduct, tile));
        } else {
            setTargetProduct(bbdrProduct);
        }
        getTargetProduct().setProductType(sourceProduct.getProductType() + "_BBDR");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoAvhrrLevel2.class);
        }
    }
}
