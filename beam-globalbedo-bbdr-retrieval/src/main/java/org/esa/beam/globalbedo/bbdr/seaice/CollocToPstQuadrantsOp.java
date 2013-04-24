/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.globalbedo.bbdr.seaice;


import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.bbdr.Sensor;
import org.esa.beam.globalbedo.bbdr.TileExtractor;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.util.logging.Logger;

/**
 * Applies IDEPIX to a MERIS/AATSR collocation product and then computes AOT.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.colloc.aotpst",
        description = "Reprojects a MERIS/AATSR collocation product onto up to 4 PST 'quadrants' products " + " " +
                      "if the MERIS and AATSR data intersects with them.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2013 by Brockmann Consult")
public class CollocToPstQuadrantsOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Override
    public void initialize() throws OperatorException {

        // find PST quadrants with data...
        // 1. make product band subset: 1 band + lat/lon TPGs...
        Product subsetSourceProduct = getCollocationBandSubset();
        // 2. reproject this to each of the 4 PSTquadrants
        Product qZero90WSubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                                                                                     1125.0, 0.0,
                                                                                     1000.0, 1000.0,
                                                                                     2250, 2250);
        Product q90W180WSubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                                                                                     1125.0, 1125.0,
                                                                                     1000.0, 1000.0,
                                                                                     2250, 2250);
        Product qZero90ESubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                                                                                     0.0, 0.0,
                                                                                     1000.0, 1000.0,
                                                                                     2250, 2250);
        Product q90E180ESubsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                                                                                     0.0, 1125.0,
                                                                                     1000.0, 1000.0,
                                                                                     2250, 2250);

        final Geometry sourceGeometry = TileExtractor.computeProductGeometry(sourceProduct);
        final Geometry qZero90WGeometry = TileExtractor.computeProductGeometry(qZero90WSubsetProduct);
        final Geometry q90W180WGeometry = TileExtractor.computeProductGeometry(q90W180WSubsetProduct);
        final Geometry qZero90EGeometry = TileExtractor.computeProductGeometry(qZero90ESubsetProduct);
        final Geometry q90E180EGeometry = TileExtractor.computeProductGeometry(q90E180ESubsetProduct);

        // 3. check for each of the 4 PST quadrants if we have intersection with the collocation product
        // if so, reproject source product onto this quadrant and write output
        // write output e.g. to
        //  ../COLLOC/180W_90W
        //  ../COLLOC/90W_0
        //  ../COLLOC/0_90E
        //  ../COLLOC/90E_180E
        // todo:  maybe do this like in doExtract_executor() in TileExtractor
        if (sourceGeometry.intersects(qZero90WGeometry))  {
            // reproject full source product to qZero90W and write reprojected product
            final Product qZero90WProduct = PolarStereographicOp.reprojectToPolarStereographic(sourceProduct,
                                                                                               1125.0, 0.0,
                                                                                               1000.0, 1000.0,
                                                                                               2250, 2250);
            final String pstTargetFilePath = getPstTargetFileName(sourceProduct, "90W_0");
            final File pstTargetFile = new File(pstTargetFilePath);
            final WriteOp bbdrWriteOp = new WriteOp(qZero90WProduct, pstTargetFile, ProductIO.DEFAULT_FORMAT_NAME);

            System.out.println("Writing PST 0-90W product '" + pstTargetFile.getName() + "'...");
            bbdrWriteOp.writeProduct(ProgressMonitor.NULL);
        }
        if (sourceGeometry.intersects(q90W180WGeometry))  {
            // reproject full source product to q90W180W and write reprojected product
            // todo
        }
        if (sourceGeometry.intersects(qZero90EGeometry))  {
            // reproject full source product to qZero90E and write reprojected product
            // todo
        }
        if (sourceGeometry.intersects(q90E180EGeometry))  {
            // reproject full source product to q90E180E and write reprojected product
            // todo
        }

        setTargetProduct(new Product("dummy", "dummy", 0, 0));
    }

    private String getPstTargetFileName(Product sourceProduct, String quadrant) {
        return sourceProduct.getFileLocation().getParent() + File.separator + sourceProduct.getName() +
                "_" + quadrant + "_PST.dim";
    }

    private Product getCollocationBandSubset() {
        Product bandSubsetProduct = new Product(sourceProduct.getName(),
                                                  sourceProduct.getProductType(),
                                                  sourceProduct.getSceneRasterWidth(),
                                                  sourceProduct.getSceneRasterHeight());
        ProductUtils.copyMetadata(sourceProduct, bandSubsetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, bandSubsetProduct);
        bandSubsetProduct.setStartTime(sourceProduct.getStartTime());
        bandSubsetProduct.setEndTime(sourceProduct.getEndTime());
        // just copy 1 band and lat/lon tpgs...
        ProductUtils.copyBand(sourceProduct.getBandAt(0).getName(), sourceProduct, bandSubsetProduct, true);
        ProductUtils.copyTiePointGrid("latitude", sourceProduct, bandSubsetProduct);
        ProductUtils.copyTiePointGrid("longitude", sourceProduct, bandSubsetProduct);

        return bandSubsetProduct;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CollocToPstQuadrantsOp.class);
        }
    }
}
