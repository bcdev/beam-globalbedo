package org.esa.beam.globalbedo.inversion.util;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.BorderExtender;
import java.awt.*;

@OperatorMetadata(alias = "ga.upscale.southpole",
        description = "Operator to correct for lost pixels near south pole.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011 by Brockmann Consult")
public class SouthPoleCorrectionOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = new Product(getId(),
                getClass().getName(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);

        Band[] bands = sourceProduct.getBands();
        for (Band band : bands) {
            if (!targetProduct.containsBand(band.getName())) {
                Band targetBand = targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
                targetBand.setDescription(band.getDescription());
                targetBand.setGeophysicalNoDataValue(band.getGeophysicalNoDataValue());
                targetBand.setNoDataValue(band.getNoDataValue());
                targetBand.setNoDataValueUsed(band.isNoDataValueUsed());
                targetBand.setScalingFactor(band.getScalingFactor());
                targetBand.setScalingOffset(band.getScalingOffset());
                targetBand.setUnit(band.getUnit());
            }
        }

        setTargetProduct(targetProduct);

    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        RasterDataNode sourceRaster = sourceProduct.getRasterDataNode(targetBand.getName());
        Rectangle rectangle = targetTile.getRectangle();
        Tile sourceTile = getSourceTile(sourceRaster, rectangle, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                final float value = sourceTile.getSampleFloat(x, y);
                if (y > targetTile.getMinY()) {
                    final float prevRowValue = sourceTile.getSampleFloat(x, y - 1);
                    // if we detect a data discontinuity going down a column, we check if the rest
                    // of the column is zero or NaN. If so, we copy the last valid data pixel at the edge.
                    if ((value == 0.0 || !AlbedoInversionUtils.isValid(value)) &&
                            (prevRowValue != 0.0 && AlbedoInversionUtils.isValid(prevRowValue)) &&
                            isRestOfColumnZero(sourceTile, x, y)) {
                        targetTile.setSample(x, y, prevRowValue);
                    } else {
                        targetTile.setSample(x, y, value);
                    }
                } else {
                    targetTile.setSample(x, targetTile.getMinY(), value);
                }

            }
        }
    }

    private boolean isRestOfColumnZero(Tile tile, int x, int y) {
        for (int i = y; i < tile.getMaxY() - 1; i++) {
            final float value = tile.getSampleFloat(x, i);
            if (value != 0.0 && !Float.isNaN(value)) {
                return false;
            }
        }
        return true;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SouthPoleCorrectionOp.class);
        }
    }


}
