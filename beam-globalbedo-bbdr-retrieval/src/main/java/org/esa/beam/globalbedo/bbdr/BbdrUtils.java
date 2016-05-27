package org.esa.beam.globalbedo.bbdr;


import Jama.Matrix;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.globalbedo.auxdata.Luts;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.util.math.LookupTable;

import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AddConstDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.util.Calendar;

import static java.lang.Math.*;
import static java.lang.Math.PI;

/**
 * Utility class for BBDR retrieval
 *
 * @author Olaf Danne
 */
public class BbdrUtils {

    /**
     * reads an AOT LUT (BBDR breadboard procedure GA_read_LUT_AOD)
     * * This LUT is equivalent to the original IDL LUT:
     * for bd = 0, nm_bnd - 1 do  $
     * for jj = 0, nm_aot - 1 do $
     * for ii = 0, nm_hsf - 1 do $
     * for k = 0, nm_azm - 1 do $
     * for j = 0, nm_asl - 1 do $
     * for i = 0, nm_avs - 1 do begin
     * readu, 1, aux
     * lut[*, i, j, nm_azm - k - 1, ii, jj, bd] = aux
     * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, aotValue, hsfValue, aziValue, szaValue, vzaValue, parameterValue});
     *
     * @param sensor The sensor
     * @return LookupTable
     * @throws java.io.IOException when failing to real LUT data
     */
    public static AotLookupTable getAotLookupTable(Sensor sensor) throws IOException {
        String instrument;
        if (sensor.getInstrument().equals("PROBAV")) {
            instrument = "VGT";
        } else if (sensor.getInstrument().startsWith("AATSR")) {
            instrument = "AATSR";
        } else {
            instrument = sensor.getInstrument();
        }
        ImageInputStream iis = Luts.getAotLutData(instrument);
        try {
            // read LUT dimensions and values
            float[] vza = Luts.readDimension(iis);
            int nVza = vza.length;
            float[] sza = Luts.readDimension(iis);
            int nSza = sza.length;
            float[] azi = Luts.readDimension(iis);
            int nAzi = azi.length;
            float[] hsf = Luts.readDimension(iis);
            int nHsf = hsf.length;
            // conversion from surf.pressure to elevation ASL
            for (int i = 0; i < nHsf; i++) {
                if (hsf[i] != -1) {
                    // 1.e-3 * (1.d - (xnodes[3, wh_nod] / 1013.25)^(1./5.25588)) / 2.25577e-5
                    final double a = hsf[i] / 1013.25;
                    final double b = 1. / 5.25588;
                    hsf[i] = (float) (0.001 * (1.0 - Math.pow(a, b)) / 2.25577E-5);
                }
            }
            float[] aot = Luts.readDimension(iis);
            int nAot = aot.length;

            float[] parameters = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
            int nParameters = parameters.length;

            float[] wvl = sensor.getWavelength();
            final int nWvl = wvl.length;

            float[] tgLut = new float[nParameters * nVza * nSza * nAzi * nHsf * nAot * nWvl];

            for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                for (int iAot = 0; iAot < nAot; iAot++) {
                    for (int iHsf = 0; iHsf < nHsf; iHsf++) {
                        for (int iAzi = 0; iAzi < nAzi; iAzi++) {
                            for (int iSza = 0; iSza < nSza; iSza++) {
                                for (int iVza = 0; iVza < nVza; iVza++) {
                                    for (int iParams = 0; iParams < nParameters; iParams++) {
                                        int iAziTemp = nAzi - iAzi - 1;
                                        int i = iParams + nParameters * (iVza + nVza * (iSza + nSza * (iAziTemp + nAzi * (iHsf + nHsf * (iAot + nAot * iWvl)))));
                                        tgLut[i] = iis.readFloat();
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Luts.readDimension(iis, nWvl); // skip wavelengths
            float[] solarIrradiances = Luts.readDimension(iis, nWvl);

            // store in original sequence (see breadboard: loop over bd, jj, ii, k, j, i in GA_read_lut_AOD
            AotLookupTable aotLut = new AotLookupTable();
            aotLut.setLut(new LookupTable(tgLut, wvl, aot, hsf, azi, sza, vza, parameters));
            aotLut.setWvl(wvl);
            aotLut.setSolarIrradiance(solarIrradiances);
            return aotLut;
        } finally {
            iis.close();
        }
    }

    /**
     * reads an AOT Kx LUT (BBDR breadboard procedure GA_read_LUT_AOD)
     * <p/>
     * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, aotValue, hsfValue, aziValue, szaValue, vzaValue, parameterValue});
     *
     * @param sensor The sensor
     * @return LookupTable
     * @throws java.io.IOException when failing to real LUT data
     */
    public static LookupTable getAotKxLookupTable(Sensor sensor) throws IOException {
        String instrument;
        if (sensor.getInstrument().equals("PROBAV")) {
            instrument = "VGT";
        } else if (sensor.getInstrument().startsWith("AATSR")) {
            instrument = "AATSR";
        } else {
            instrument = sensor.getInstrument();
        }
        ImageInputStream iis = Luts.getAotKxLutData(instrument);
        try {
            // read LUT dimensions and values
            float[] vza = Luts.readDimension(iis);
            int nVza = vza.length;
            float[] sza = Luts.readDimension(iis);
            int nSza = sza.length;
            float[] azi = Luts.readDimension(iis);
            int nAzi = azi.length;
            float[] hsf = Luts.readDimension(iis);
            int nHsf = hsf.length;
            float[] aot = Luts.readDimension(iis);
            int nAot = aot.length;

            float[] kx = new float[]{1.0f, 2.0f};
            int nKx = kx.length;

            float[] wvl = sensor.getWavelength();
            final int nWvl = wvl.length;

            float[] lut = new float[nKx * nVza * nSza * nAzi * nHsf * nAot * nWvl];
            iis.readFully(lut, 0, lut.length);

            return new LookupTable(lut, wvl, aot, hsf, azi, sza, vza, kx);
        } finally {
            iis.close();
        }
    }


    /**
     * reads a NSky DW LUT (BBDR breadboard procedure GA_read_LUT_Nsky, first LUT)
     * * This LUT is equivalent to the original IDL LUT.
     * * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, ozoValue, cwvValue, angValue, kxValue, kxcaseValue});
     *
     * @param sensor The sensor
     * @return LookupTable
     * @throws java.io.IOException when failing to real LUT data
     */
    public static NskyLookupTable getNskyLookupTableDw(Sensor sensor) throws IOException {
        String instrument;
        if (sensor.getInstrument().equals("PROBAV")) {
            instrument = "VGT";
        } else if (sensor.getInstrument().startsWith("AATSR")) {
            instrument = "AATSR";
        } else {
            instrument = sensor.getInstrument();
        }
        ImageInputStream iis = Luts.getNskyDwLutData(instrument);
        try {
            // read LUT dimensions and values
            float[] sza = Luts.readDimension(iis);
            int nSza = sza.length;
            float[] hsf = Luts.readDimension(iis);
            int nHsf = hsf.length;
            float[] aot = Luts.readDimension(iis);
            int nAot = aot.length;

            int nSpec = iis.readInt();
            float[] spec = new float[nSpec];
            for (int i = 0; i < spec.length; i++) {
                spec[i] = (i + 1) * 1.0f;
            }

            double kppVol = iis.readDouble();
            double kppGeo = iis.readDouble();

            float[] values = new float[]{1.0f, 2.0f};
            int nValues = values.length;

            float[] lut = new float[nValues * nSza * nHsf * nAot * nSpec];
            iis.readFully(lut, 0, lut.length);

            // store in original sequence (see breadboard: GA_read_LUT_Nsky)
            NskyLookupTable nskyLut = new NskyLookupTable();
            nskyLut.setLut(new LookupTable(lut, spec, aot, hsf, sza, values));
            nskyLut.setKppGeo(kppGeo);
            nskyLut.setKppVol(kppVol);

            return nskyLut;
        } finally {
            iis.close();
        }
    }

    /**
     * reads a NSky DW LUT (BBDR breadboard procedure GA_read_LUT_Nsky, first LUT)
     * * This LUT is equivalent to the original IDL LUT.
     * * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, ozoValue, cwvValue, angValue, kxValue, kxcaseValue});
     *
     * @param sensor The sensor
     * @return LookupTable
     * @throws java.io.IOException when failing to real LUT data
     */
    public static NskyLookupTable getNskyLookupTableUp(Sensor sensor) throws IOException {
        String instrument;
        if (sensor.getInstrument().equals("PROBAV")) {
            instrument = "VGT";
        } else if (sensor.getInstrument().startsWith("AATSR")) {
            instrument = "AATSR";
        } else {
            instrument = sensor.getInstrument();
        }
        ImageInputStream iis = Luts.getNskyUpLutData(instrument);
        try {
            // read LUT dimensions and values
            float[] vza = Luts.readDimension(iis);
            int nVza = vza.length;
            float[] hsf = Luts.readDimension(iis);
            int nHsf = hsf.length;
            float[] aot = Luts.readDimension(iis);
            int nAot = aot.length;

            int nSpec = iis.readInt();
            float[] spec = new float[nSpec];
            for (int i = 0; i < spec.length; i++) {
                spec[i] = (i + 1) * 1.0f;
            }
            double kppVol = iis.readDouble();
            double kppGeo = iis.readDouble();

            float[] values = new float[]{1.0f, 2.0f};
            int nValues = values.length;

            float[] lut = new float[nValues * nVza * nHsf * nAot * nSpec];
            iis.readFully(lut, 0, lut.length);

            // store in original sequence (see breadboard: GA_read_LUT_Nsky)
            NskyLookupTable nskyLut = new NskyLookupTable();
            nskyLut.setLut(new LookupTable(lut, spec, aot, hsf, vza, values));
            nskyLut.setKppGeo(kppGeo);
            nskyLut.setKppVol(kppVol);

            return nskyLut;
        } finally {
            iis.close();
        }
    }

    public static void convertProbavToVgtReflectances(Product sourceProduct) {
        for (int i = 0; i < BbdrConstants.PROBAV_TOA_BAND_NAMES.length; i++) {
            Band specBand = sourceProduct.getBand(BbdrConstants.PROBAV_TOA_BAND_NAMES[i]);
            final double factor = BbdrConstants.PROBAV_TO_VGT_FACTORS[i];
            final double offset = BbdrConstants.PROBAV_TO_VGT_OFFSETS[i]/specBand.getScalingFactor();
            final MultiLevelImage sourceImage = specBand.getSourceImage();
            final RenderedOp multipliedImage = MultiplyConstDescriptor.create(sourceImage, new double[]{factor}, null);
            final RenderedOp offsetImage = AddConstDescriptor.create(multipliedImage, new double[]{offset}, null);
            specBand.setSourceImage(offsetImage);
        }
    }

    public static int getIndexBefore(float value, float[] array) {
        for (int i = 0; i < array.length; i++) {
            if (value < array[i]) {
                if (i != 0) {
                    return i - 1;
                } else {
                    return i;
                }
            }
        }
        return (array.length - 2);
    }

    public static float getImageMeanValue(PlanarImage image) {
        // Set up the parameter block for the source image and
        // the three parameters.
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);   // The source image
        pb.add(null);       // null ROI means whole image
        pb.add(1);          // check every pixel horizontally
        pb.add(1);          // check every pixel vertically

        // Perform the mean operation on the source image.
        RenderedImage meanImage = JAI.create("mean", pb, null);

        // Retrieve and report the mean pixel value.
        double[] mean = (double[]) meanImage.getProperty("mean");

        return (float) mean[0];
    }

    public static int getDoyFromYYYYMMDD(String yyyymmdd) {
        Calendar cal = Calendar.getInstance();
        int doy = -1;
        try {
            final int year = Integer.parseInt(yyyymmdd.substring(0, 4));
            final int month = Integer.parseInt(yyyymmdd.substring(4, 6)) - 1;
            final int day = Integer.parseInt(yyyymmdd.substring(6, 8));
            cal.set(year, month, day);
            doy = cal.get(Calendar.DAY_OF_YEAR);
        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
        }
        return doy;
    }

    public static void fillTargetSampleWithNoDataValue(WritableSample[] targetSamples) {
        for (WritableSample targetSample : targetSamples) {
            if (targetSample.getIndex() != -1) {
                targetSample.set(Float.NaN);
            }
        }
    }


    public static Matrix matrixSquare(double[] doubles) {
        Matrix matrix = new Matrix(doubles.length, doubles.length);
        for (int i = 0; i < doubles.length; i++) {
            for (int j = 0; j < doubles.length; j++) {
                matrix.set(i, j, doubles[i] * doubles[j]);
            }
        }
        return matrix;
    }

    public static ElevationModel getDem() {
        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor("GMTED2010_30");
        if (demDescriptor == null || !demDescriptor.isDemInstalled()) {
            demDescriptor = elevationModelRegistry.getDescriptor("GETASSE30");
            if (demDescriptor == null || !demDescriptor.isDemInstalled()) {
                throw new OperatorException(" No DEM installed (neither GETASSE30 nor GMTED2010_30).");
            }
        }
        return demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
    }

    public static double[] computeConstantKernels(double vzaRad, double szaRad, double phiRad) {
        final double muv = cos(vzaRad);
        final double mus = cos(szaRad);

        final double muPhi = cos(phiRad);
        final double muPhiAng = mus * muv + sin(vzaRad) * sin(szaRad) * muPhi;
        final double phAng = acos(muPhiAng);

        final double tanVzaRad = tan(vzaRad);
        final double tanSzaRad = tan(szaRad);
        final double secVza = 1. / muv;
        final double secSza = 1. / mus;

        final double d2 = tanVzaRad * tanVzaRad + tanSzaRad * tanSzaRad - 2 * tanVzaRad * tanSzaRad * muPhi;

        final double hb = 2.0;
        double cost = hb * (pow((d2 + pow((tanVzaRad * tanSzaRad * sin(phiRad)), 2)), 0.5)) / (secVza + secSza);
        cost = min(cost, 1.0);
        final double t = acos(cost);

        final double ocap = (t - sin(t) * cost) * (secVza + secSza) / PI;

        final double kvol = ((PI / 2.0 - phAng) * cos(phAng) + sin(phAng)) / (mus + muv) - PI / 4.0;
        final double kgeo = 0.5 * (1. + muPhiAng) * secSza * secVza + ocap - secVza - secSza;

        return new double[]{kvol, kgeo};
    }

}
