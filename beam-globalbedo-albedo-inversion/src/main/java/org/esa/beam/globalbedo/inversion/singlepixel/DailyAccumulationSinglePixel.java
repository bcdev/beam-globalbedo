package org.esa.beam.globalbedo.inversion.singlepixel;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.globalbedo.inversion.Accumulator;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.DailyAccumulationUtils;

import java.awt.image.Raster;

/**
 * Daily accumulation for single pixel mode.
 *
 * @author olafd
 */
public class DailyAccumulationSinglePixel {

    private Product[] sourceProducts;

    private boolean computeSnow;

    private int pixelX;
    private int pixelY;

    /**
     * DailyAccumulationSinglePixel constructor
     *
     * @param sourceProducts - the input products to accumulate
     * @param computeSnow    - if true, only snowpixels will be considered
     */
    public DailyAccumulationSinglePixel(Product[] sourceProducts, boolean computeSnow) {
        this.sourceProducts = sourceProducts;
        this.computeSnow = computeSnow;
        this.pixelX = 0;
        this.pixelY = 0;
    }

    public DailyAccumulationSinglePixel(Product[] sourceProducts, boolean computeSnow, int pixelX, int pixelY) {
        this.sourceProducts = sourceProducts;
        this.computeSnow = computeSnow;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
    }

    public void setPixelX(int pixelX) {
        this.pixelX = pixelX;
    }

    public void setPixelY(int pixelY) {
        this.pixelY = pixelY;
    }

    /**
     * Performs the daily accumulation
     *
     * @return daily accumulator
     */
    public Accumulator accumulate() {

        Matrix M = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                              3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        Matrix V = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        Matrix E = new Matrix(1, 1);
        double mask = 0.0;

        // accumulate the matrices from the single products...
        for (int i = 0; i < sourceProducts.length; i++) {
            final long t1 = System.currentTimeMillis();
            final Accumulator accumulator = getMatricesPerBBDRDataset(i);
            M.plusEquals(accumulator.getM());
            V.plusEquals(accumulator.getV());
            E.plusEquals(accumulator.getE());
            mask += accumulator.getMask();
        }

        return new Accumulator(M, V, E, mask);
    }

    private Accumulator getMatricesPerBBDRDataset(int sourceProductIndex) {
        final long t1 = System.currentTimeMillis();
        final Matrix bbdr = getBBDR(sourceProductIndex);
        final double[] SD = getSD(sourceProductIndex);
        final long t2 = System.currentTimeMillis();
        final long timeGetBBDRandSD = t2 - t1;
        System.out.println("timeGetBBDRandSD [ms] = " + timeGetBBDRandSD);

        if (isLandFilter(sourceProductIndex) || isSnowFilter(sourceProductIndex) ||
                isBBDRFilter(bbdr) || isSDFilter(SD)) {
            // do not consider non-land pixels, non-snowfilter pixels, pixels with BBDR == 0.0 or -9999.0, SD == 0.0
            return Accumulator.createZeroAccumulator();
        }

        // get kernels...
        Matrix kernels = getKernels(sourceProductIndex);

        // compute C matrix...
        final double[] correlation = getCorrelation(sourceProductIndex);
        Matrix C = new Matrix(
                AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        Matrix thisC = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                                  AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);

        int count = 0;
        int cCount = 0;
        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
            for (int k = j + 1; k < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                if (k == j + 1) {
                    cCount++;
                }
                C.set(cCount, 0, correlation[count] * SD[j] * SD[k]);
                count++;
                cCount++;
            }
        }

        cCount = 0;
        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
            C.set(cCount, 0, SD[j] * SD[j]);
            cCount = cCount + AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS - j;
        }

        count = 0;
        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
            for (int k = j; k < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                thisC.set(j, k, C.get(count, 0));
                thisC.set(k, j, thisC.get(j, k));
                count++;
            }
        }

        // compute M, V, E matrices...
        final Matrix inverseC = thisC.inverse();
        final Matrix M = (kernels.transpose().times(inverseC)).times(kernels);
        final Matrix inverseCDiagFlat = DailyAccumulationUtils.getRectangularDiagonalMatrix(inverseC);
        final Matrix kernelTimesInvCDiag;
        if (inverseCDiagFlat != null) {
            kernelTimesInvCDiag = kernels.transpose().times(inverseCDiagFlat);
        } else {
            return Accumulator.createZeroAccumulator();
        }
        final Matrix V = kernelTimesInvCDiag.times(bbdr);
        final Matrix E = (bbdr.transpose().times(inverseC)).times(bbdr);

        // return result
        return new Accumulator(M, V, E, 1);
    }

    private Matrix getBBDR(int sourceProductIndex) {
        Matrix bbdr = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        final Product product = sourceProducts[sourceProductIndex];

        final Band bbVisBand = product.getBand(AlbedoInversionConstants.BBDR_BB_VIS_NAME);
        final Raster bbVisData = bbVisBand.getSourceImage().getData();
        final double bbVis = bbVisData.getSampleDouble(pixelX, pixelY, 0);
        bbdr.set(0, 0, bbVis);

        final Band bbNirBand = product.getBand(AlbedoInversionConstants.BBDR_BB_NIR_NAME);
        final Raster bbNirData = bbNirBand.getSourceImage().getData();
        final double bbNir = bbNirData.getSampleDouble(pixelX, pixelY, 0);
        bbdr.set(1, 0, bbNir);

        final Band bbSwBand = product.getBand(AlbedoInversionConstants.BBDR_BB_SW_NAME);
        final Raster bbSwData = bbSwBand.getSourceImage().getData();
        final double bbSw = bbSwData.getSampleDouble(pixelX, pixelY, 0);
        bbdr.set(2, 0, bbSw);
        return bbdr;
    }

    private double[] getSD(int sourceProductIndex) {
        double[] SD = new double[3];
        final Product product = sourceProducts[sourceProductIndex];

        final Band sigBbVisVisBand = product.getBand(AlbedoInversionConstants.BBDR_SIG_BB_VIS_VIS_NAME);
        final Raster sigBbVisVisData = sigBbVisVisBand.getSourceImage().getData();
        SD[0] = sigBbVisVisData.getSampleDouble(pixelX, pixelY, 0);

        final Band sigBbNirNirBand = product.getBand(AlbedoInversionConstants.BBDR_SIG_BB_NIR_NIR_NAME);
        final Raster sigBbNirNirData = sigBbNirNirBand.getSourceImage().getData();
        SD[1] = sigBbNirNirData.getSampleDouble(pixelX, pixelY, 0);

        final Band sigBbSwSwBand = product.getBand(AlbedoInversionConstants.BBDR_SIG_BB_SW_SW_NAME);
        final Raster sigBbSwSwData = sigBbSwSwBand.getSourceImage().getData();
        SD[2] = sigBbSwSwData.getSampleDouble(pixelX, pixelY, 0);

        return SD;
    }

    private double[] getCorrelation(int sourceProductIndex) {
        double[] correlation = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        final Product product = sourceProducts[sourceProductIndex];

        final Band sigBbVisNirBand = product.getBand(AlbedoInversionConstants.BBDR_SIG_BB_VIS_NIR_NAME);
        final Raster sigBbVisNirData = sigBbVisNirBand.getSourceImage().getData();
        correlation[0] = sigBbVisNirData.getSampleDouble(pixelX, pixelY, 0);

        final Band sigBbVisSwBand = product.getBand(AlbedoInversionConstants.BBDR_SIG_BB_VIS_SW_NAME);
        final Raster sigBbVisSwData = sigBbVisSwBand.getSourceImage().getData();
        correlation[1] = sigBbVisSwData.getSampleDouble(pixelX, pixelY, 0);

        final Band sigBbNirSwBand = product.getBand(AlbedoInversionConstants.BBDR_SIG_BB_NIR_SW_NAME);
        final Raster sigBbNirSwData = sigBbNirSwBand.getSourceImage().getData();
        correlation[2] = sigBbNirSwData.getSampleDouble(pixelX, pixelY, 0);

        return correlation;
    }

    private Matrix getKernels(int sourceProductIndex) {
        Matrix kernels = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                                    3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        final Product product = sourceProducts[sourceProductIndex];

        kernels.set(0, 0, 1.0);
        kernels.set(1, 3, 1.0);
        kernels.set(2, 6, 1.0);

        final Band kvolBrdfVisBand = product.getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_VIS_NAME);
        final Raster kvolBrdfVisData = kvolBrdfVisBand.getSourceImage().getData();
        kernels.set(0, 1, kvolBrdfVisData.getSampleDouble(pixelX, pixelY, 0));

        final Band kvolBrdfNirBand = product.getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_NIR_NAME);
        final Raster kvolBrdfNirData = kvolBrdfNirBand.getSourceImage().getData();
        kernels.set(1, 4, kvolBrdfNirData.getSampleDouble(pixelX, pixelY, 0));

        final Band kvolBrdfSwBand = product.getBand(AlbedoInversionConstants.BBDR_KVOL_BRDF_SW_NAME);
        final Raster kvolBrdfSwData = kvolBrdfSwBand.getSourceImage().getData();
        kernels.set(2, 7, kvolBrdfSwData.getSampleDouble(pixelX, pixelY, 0));

        final Band kgeoBrdfVisBand = product.getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_VIS_NAME);
        final Raster kgeoBrdfVisData = kgeoBrdfVisBand.getSourceImage().getData();
        kernels.set(0, 2, kgeoBrdfVisData.getSampleDouble(pixelX, pixelY, 0));

        final Band kgeoBrdfNirBand = product.getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_NIR_NAME);
        final Raster kgeoBrdfNirData = kgeoBrdfNirBand.getSourceImage().getData();
        kernels.set(1, 5, kgeoBrdfNirData.getSampleDouble(pixelX, pixelY, 0));

        final Band kgeoBrdfSwBand = product.getBand(AlbedoInversionConstants.BBDR_KGEO_BRDF_SW_NAME);
        final Raster kgeoBrdfSwData = kgeoBrdfSwBand.getSourceImage().getData();
        kernels.set(2, 8, kgeoBrdfSwData.getSampleDouble(pixelX, pixelY, 0));

        return kernels;
    }

    private boolean isLandFilter(int sourceProductIndex) {
        final Product product = sourceProducts[sourceProductIndex];
        if (product.getProductType().startsWith("MER") || product.getName().startsWith("MER")) {
            final Band l1FlagBand = product.getBand("l1_flags");
            final Raster l1FlagData = l1FlagBand.getSourceImage().getData();
            int l1 = (int) l1FlagData.getSampleDouble(pixelX, pixelY, 0);
            return (l1 & 1 << 4) == 0;   // LAND_OCEAN not set todo: test!
        } else if (product.getProductType().startsWith("VGT") || product.getName().startsWith("VGT")) {
            final Band smFlagBand = product.getBand("SM");
            final Raster smFlagData = smFlagBand.getSourceImage().getData();
            int sm = (int) smFlagData.getSampleDouble(pixelX, pixelY, 0);
            return (sm & 1 << 3) == 0;   // LAND not set todo: test!
        }

        return false; // do not filter
    }

    private boolean isSnowFilter(int sourceProductIndex) {
        final Product product = sourceProducts[sourceProductIndex];
        final Band snowMaskBand = product.getBand("snow_mask");
        final Raster snowMaskData = snowMaskBand.getSourceImage().getData();
        int snowValue = (int) snowMaskData.getSampleDouble(pixelX, pixelY, 0);

        return (!computeSnow && snowValue != 0) || (computeSnow && snowValue == 0);
    }

    private boolean isBBDRFilter(Matrix bbdr) {
        final double bbVis = bbdr.get(0, 0);
        final double bbNir = bbdr.get(1, 0);
        final double bbSw = bbdr.get(2, 0);

        return (bbVis == 0.0 || !AlbedoInversionUtils.isValid(bbVis) || bbVis == 9999.0 ||
                bbNir == 0.0 || !AlbedoInversionUtils.isValid(bbNir) || bbNir == 9999.0 ||
                bbSw == 0.0 || !AlbedoInversionUtils.isValid(bbSw) || bbSw == 9999.0);
    }

    private boolean isSDFilter(double[] SD) {
        return (SD[0] == 0.0 && SD[1] == 0.0 && SD[2] == 0.0);
    }

}
