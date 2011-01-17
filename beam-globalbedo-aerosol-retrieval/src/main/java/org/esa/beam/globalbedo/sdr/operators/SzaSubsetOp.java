/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import com.bc.ceres.core.ProgressMonitor;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.util.Guardian;

/**
 *
 * @author akheckel
 */
@OperatorMetadata(alias = "ga.SzaSubsetOp",
                  description = "extracts subset by sza limit",
                  authors = "Andreas Heckel",
                  version = "1.1",
                  copyright = "(C) 2010 by University Swansea (a.heckel@swansea.ac.uk)")
public class SzaSubsetOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(defaultValue="SZA")
    private String szaBandName;
    @Parameter(defaultValue="false")
    private boolean hasSolarElevation;
    @Parameter(defaultValue="69.99")
    private double szaLimit;
    private ProductSubsetBuilder subsetReader;
    private ProductSubsetDef subsetDef;


    @Override
    public void initialize() throws OperatorException {
        Rectangle region = getSzaRegion();
        Guardian.assertGreaterThan("szaSubsetRegion.x >= 0", region.x, -1);
        Guardian.assertGreaterThan("szaSubsetRegion.y >= 0", region.y, -1);
        Guardian.assertGreaterThan("szaSubsetRegion.width > 0", region.width, 0);
        Guardian.assertGreaterThan("szaSubsetRegion.height > 0", region.height, 0);
        subsetReader = new ProductSubsetBuilder();
        subsetDef = new ProductSubsetDef();
        subsetDef.addNodeNames(sourceProduct.getTiePointGridNames());
        subsetDef.addNodeNames(sourceProduct.getBandNames());
        subsetDef.addNodeNames(sourceProduct.getMetadataRoot().getElementNames());
        subsetDef.addNodeNames(sourceProduct.getMetadataRoot().getAttributeNames());
        subsetDef.setRegion(region);
        subsetDef.setSubSampling(1,1);
        subsetDef.setSubsetName(sourceProduct.getName());
        subsetDef.setIgnoreMetadata(false);

        try {
            targetProduct = subsetReader.readProductNodes(sourceProduct, subsetDef);
        } catch (Throwable t) {
            throw new OperatorException(t);
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        ProductData destBuffer = targetTile.getRawSamples();
        Rectangle rectangle = targetTile.getRectangle();
        try {
            subsetReader.readBandRasterData(targetBand,
                                            rectangle.x,
                                            rectangle.y,
                                            rectangle.width,
                                            rectangle.height,
                                            destBuffer, pm);
            targetTile.setRawSamples(destBuffer);
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private Rectangle getSzaRegion() throws OperatorException {
        RasterDataNode szaBand = sourceProduct.getRasterDataNode(szaBandName);
        int srcWidth = sourceProduct.getSceneRasterWidth();
        int srcHeight = sourceProduct.getSceneRasterHeight();
        float[] sza0 = new float[srcHeight];
        float[] sza1 = new float[srcHeight];
        try {
            szaBand.readPixels(0, 0, 1, srcHeight, sza0, ProgressMonitor.NULL);
            szaBand.readPixels(srcWidth-1, 0, 1, srcHeight, sza1, ProgressMonitor.NULL);
        } catch (IOException ex) {
            throw new OperatorException(ex);
        }
        int start = 0;
        int end = srcHeight-1;
        if (hasSolarElevation) {
            while (start < srcHeight-1 && (90 - sza0[start]) > szaLimit && (90 - sza1[start]) > szaLimit){
                start++;
            }
            while (end > start && (90 - sza0[end]) > szaLimit && (90 - sza1[end]) > szaLimit){
                end--;
            }
        } else {
            while (start < srcHeight-1 && (sza0[start]) > szaLimit && (sza1[start]) > szaLimit){
                start++;
            }
            while (end > start && (sza0[end]) > szaLimit && (sza1[end]) > szaLimit){
                end--;
            }
        }
        Rectangle region = new Rectangle(0, start, srcWidth, end - start + 1);
        return region;
    }



    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SzaSubsetOp.class);
        }
    }
}
