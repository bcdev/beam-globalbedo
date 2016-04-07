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

package org.esa.beam.globalbedo.bbdr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.BorderExtender;
import java.awt.Rectangle;

import static java.lang.Math.cos;
import static java.lang.StrictMath.toRadians;

/**
 * This function calculates the local-neighbourhood statistical variance.
 * I.e. for each array element a the variance of the neighbourhood of +-
 * halfwidth is calculated. The routine avoids any loops and so is fast
 * and "should" work for any dimension of array.
 */
public class ImageVarianceOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Override
    public void initialize() throws OperatorException {
        Product sourceProduct = getSourceProduct();
        Product targetProduct = new Product(getId(),
                getClass().getName(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

        Band[] bands = sourceProduct.getBands();
        for (Band band : bands) {
            if (sensor == Sensor.MERIS) {
                if (band.getName().startsWith("reflectance")) {
                    targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
                }
            } else if (sensor == Sensor.AATSR_NADIR) {
                if (band.getName().startsWith("reflec_nadir")) {
                    targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
                }
                if (band.getName().startsWith("reflec_fward")) {
                    targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
                }
            } else if (sensor == Sensor.AATSR_NADIR) {
                if (band.getName().startsWith("reflec_nadir")) {
                    targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
                }
            } else if (sensor == Sensor.AATSR_FWARD) {
                if (band.getName().startsWith("reflec_fward")) {
                    targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
                }
            } else if (sensor == Sensor.VGT) {
                if (band.getName().startsWith("B") || band.getName().equals("MIR")) {
                    targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
                }
            } else if (sensor == Sensor.PROBAV) {
                if (band.getName().startsWith("TOA_REFL")) {
                    targetProduct.addBand(band.getName(), ProductData.TYPE_FLOAT32);
                }
            }
        }
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        RasterDataNode sourceRaster = sourceProduct.getRasterDataNode(targetBand.getName());
        Rectangle rectangle = targetTile.getRectangle();
        rectangle.grow(1, 1);
        Tile sourceTile = getSourceTile(sourceRaster, rectangle, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        if (sensor == Sensor.AATSR_NADIR) {
            double cal2MerisCoeff = getAatsrCal2MerisCoeffByBandName(targetBand.getName());
            TiePointGrid sunElevTpg = sourceProduct.getTiePointGrid("sun_elev_nadir");
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    final float sza = 90.0f - sunElevTpg.getPixelFloat(x, y);
                    final double sza_r = toRadians(sza);
                    final double mus = cos(sza_r);
                    final double factor = 0.01 / (cal2MerisCoeff * mus);
                    targetTile.setSample(x, y, variance(sourceTile, x, y, factor));
                }
            }
        } else if (sensor == Sensor.AATSR_FWARD) {
            double cal2MerisCoeff = getAatsrCal2MerisCoeffByBandName(targetBand.getName());
            TiePointGrid sunElevTpg = sourceProduct.getTiePointGrid("sun_elev_fward");
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    final float sza = 90.0f - sunElevTpg.getPixelFloat(x, y);
                    final double sza_r = toRadians(sza);
                    final double mus = cos(sza_r);
                    final double factor = 0.01 / (cal2MerisCoeff * mus);
                    targetTile.setSample(x, y, variance(sourceTile, x, y, factor));
                }
            }
        } else {
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    targetTile.setSample(x, y, variance(sourceTile, x, y, 1.0));
                }
            }
        }
    }

    double variance(Tile sourceTile, int x0, int y0, double factor) {
        double sum = 0;
        double sumSq = 0;
        for (int y = y0 - 1; y <= y0 + 1; y++) {
            for (int x = x0 - 1; x <= x0 + 1; x++) {
                double v = sourceTile.getSampleDouble(x, y) * factor;
                sum += v;
                sumSq += v * v;
            }
        }
        return Math.sqrt((sumSq / 9) - (sum * sum / 81));
    }

    private double getAatsrCal2MerisCoeffByBandName(String bandName) {
        if (bandName.endsWith("550")) {
           return sensor.getCal2Meris()[0];
        } else if (bandName.endsWith("670")) {
           return sensor.getCal2Meris()[1];
        } else if (bandName.endsWith("870")) {
           return sensor.getCal2Meris()[2];
        } else if (bandName.endsWith("1600")) {
           return sensor.getCal2Meris()[3];
        }
        return 1.0;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ImageVarianceOp.class);
        }
    }
}
