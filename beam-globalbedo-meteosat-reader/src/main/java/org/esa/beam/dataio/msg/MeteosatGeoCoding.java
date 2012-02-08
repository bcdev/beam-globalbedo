package org.esa.beam.dataio.msg;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.math.MathUtils;

import java.awt.*;
import java.awt.image.Raster;
import java.io.IOException;

/**
 * Special geocoding for Meteosat First and Second Generation Albedo products
 *
 * @author olafd
 * @author norman
 */
public class MeteosatGeoCoding extends AbstractGeoCoding {

    private static final int LUT_SIZE = 10;
    //    public static final double MIN_DIST = 0.375;
    public static final double MIN_DIST = 0.025;
    public static final double INTERMEDIATE_DIST = 0.025;

    private final Band latBand;
    private final Band lonBand;
    private final float[] latData;
    private final float[] lonData;
    private final int width;
    private final int height;
    private final PixelBoxLut[][] latSuperLut = new PixelBoxLut[180][360];
    private final PixelBoxLut[][] lonSuperLut = new PixelBoxLut[180][360];
    private boolean initialized;

    public MeteosatGeoCoding(Band latitude, Band longitude) throws IOException {
        this.latBand = latitude;
        this.lonBand = longitude;
        width = latBand.getSceneRasterWidth();
        height = latBand.getSceneRasterHeight();

        latData = readDataFully(latBand);
        lonData = readDataFully(lonBand);
    }

    private float[] readDataFully(Band band) throws IOException {
        final int w = width;
        final int h = height;
        final float[] data = new float[w * h];
        band.readPixels(0, 0, w, h, data);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!band.isPixelValid(x, y)) {
                    data[y * w + x] = Float.NaN;
                }
            }
        }
        return data;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        final int x = (int) Math.floor(pixelPos.x);
        final int y = (int) Math.floor(pixelPos.y);
        if (x >= 0 && y >= 0 && x < width && y < height) {
            int index = width * y + x;
            geoPos.setLocation(latData[index], lonData[index]);
        } else {
            geoPos.setInvalid();
        }
        return geoPos;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }

//        System.out.println("entering getPixelPos: geoPos = " + geoPos);

        if (!initialized) {
            initialized = true;
            initialize();
        }

        int si = getSuperLutI(geoPos.lon);
        int sj = getSuperLutJ(geoPos.lat);
        PixelBoxLut latLut = latSuperLut[sj][si];
        PixelBoxLut lonLut = lonSuperLut[sj][si];
        if (latLut == null || lonLut == null) {
            pixelPos.setInvalid();
            return pixelPos;
        }

        final PixelBox latPixelBox = latLut.getPixelBox(geoPos.lat);
        final PixelBox lonPixelBox = lonLut.getPixelBox(geoPos.lon);
        if (latPixelBox == null || lonPixelBox == null) {
            pixelPos.setInvalid();
            return pixelPos;
        }
        // System.out.println("geoPos = " + geoPos);
        // System.out.println("latPixelBox = " + latPixelBox + ", index = " + latLut.getIndex(geoPos.lat));
        // System.out.println("lonPixelBox = " + lonPixelBox + ", index = " + lonLut.getIndex(geoPos.lon));

//        // intersection of latPixelBox and lonPixelBox
//        int x1 = Math.max(latPixelBox.minX, lonPixelBox.minX);
//        int y1 = Math.max(latPixelBox.minY, lonPixelBox.minY);
//        int x2 = Math.min(latPixelBox.maxX, lonPixelBox.maxX);
//        int y2 = Math.min(latPixelBox.maxY, lonPixelBox.maxY);

        // combination of latPixelBox and lonPixelBox
        int x1 = Math.min(latPixelBox.minX, lonPixelBox.minX);
        int y1 = Math.min(latPixelBox.minY, lonPixelBox.minY);
        int x2 = Math.max(latPixelBox.maxX, lonPixelBox.maxX);
        int y2 = Math.max(latPixelBox.maxY, lonPixelBox.maxY);

        if (x1 == x2 && y1 == y2) {
            pixelPos.setLocation(x1 + 0.5F, y1 + 0.5F);
            return pixelPos;
        }

        int w = x2 - x1 + 1;
        int h = y2 - y1 + 1;
        if (w <= 0 || h <= 0) {
            pixelPos.setInvalid();
            return pixelPos;
        }

        double minDist = Double.MAX_VALUE;
        int bestX = -1;
        int bestY = -1;

        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                int index = width * y + x;
                float lat = latData[index];
                float lon = lonData[index];
                if (isValid(lat, lon)) {
                    double dist = MathUtils.sphereDistanceDeg(1.0, lon, lat, geoPos.lon, geoPos.lat) * MathUtils.RTOD;
                    if (dist < minDist && dist < MIN_DIST) {
                        minDist = dist;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }

        if (bestX != -1 && bestY != -1) {
            pixelPos.setLocation(bestX + 0.5F, bestY + 0.5F);
        } else {
//            if (geoPos.getLat() > 70.89 && geoPos.getLat() < 70.91 && geoPos.getLon() > -10.13 && geoPos.getLon() < -10.1) {
//                System.out.println("pixel INVALID for geoPos = " + geoPos);
//                final Result result = new Result();
//                boolean pixelFound = quadTreeSearch(0,
//                                            geoPos.lat, geoPos.lon,
//                                            x1, y1,
//                                            w,
//                                            h,
//                                            result);
//            }
            final Result result = new Result();
            boolean pixelFound = quadTreeSearch(0,
                                                geoPos.lat, geoPos.lon,
                                                x1, y1,
                                                w,
                                                h,
                                                result);
            if (pixelFound) {
                pixelPos.setLocation(result.x + 0.5f, result.y + 0.5f);
            } else {
                pixelPos.setInvalid();
            }
        }

        return pixelPos;
    }

    private boolean isValid(float lat, float lon) {
        return !Float.isNaN(lat) && !Float.isNaN(lon);
    }

    private void initialize() {

        final long t0 = System.currentTimeMillis();

        final int w = width;
        final int h = height;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int i = w * y + x;
                final float lat = latData[i];
                final float lon = lonData[i];
                fillSuperLut(w, h, y, x, lat, lon);

//                if (x > 0) {
//                    final float latXminus = latData[w * y + (x - 1)];
//                    final float lonXminus = lonData[w * y + (x - 1)];
//                    for (int k = 0; k < INTERMEDIATE_VALUES; k++) {
//                        final float intermediateLatLeft = lat - k * (lat - latXminus) / INTERMEDIATE_VALUES;
//                        final float intermediateLonLeft = lon - k * (lon - lonXminus) / INTERMEDIATE_VALUES;
//                        fillSuperLut(w, h, y, x, intermediateLatLeft, intermediateLonLeft);
//                    }
//                }
//
//                if (x < w - 1) {
//                    final float latXplus = latData[w * y + (x + 1)];
//                    final float lonXplus = lonData[w * y + (x + 1)];
//                    for (int k = 0; k < INTERMEDIATE_VALUES; k++) {
//                        final float intermediateLatRight = lat + k * (latXplus - lat) / INTERMEDIATE_VALUES;
//                        final float intermediateLonRight = lon + k * (lonXplus - lon) / INTERMEDIATE_VALUES;
//                        fillSuperLut(w, h, y, x, intermediateLatRight, intermediateLonRight);
//                    }
//                }
//
//                if (y > 0) {
//                    final float latYminus = latData[w * (y - 1) + x];
//                    final float lonYminus = lonData[w * (y - 1) + x];
//                    for (int k = 0; k < INTERMEDIATE_VALUES; k++) {
//                        final float intermediateLatDown = lat - k * (lat - latYminus) / INTERMEDIATE_VALUES;
//                        final float intermediateLonDown = lon - k * (lon - lonYminus) / INTERMEDIATE_VALUES;
//                        fillSuperLut(w, h, y, x, intermediateLatDown, intermediateLonDown);
//                    }
//                }
//                if (y < h - 1) {
//                    final float latYplus = latData[w * (y + 1) + x];
//                    final float lonYplus = lonData[w * (y + 1) + x];
//                    for (int k = 0; k < INTERMEDIATE_VALUES; k++) {
//                        final float intermediateLatUp = lat + k * (latYplus - lat) / INTERMEDIATE_VALUES;
//                        final float intermediateLonUp = lon + k * (lonYplus - lon) / INTERMEDIATE_VALUES;
//                        fillSuperLut(w, h, y, x, intermediateLatUp, intermediateLonUp);
//                    }
//                }
            }
        }
        for (int sj = 0; sj < 180; sj++) {
            for (int si = 0; si < 360; si++) {
                PixelBoxLut latLut = latSuperLut[sj][si];
                PixelBoxLut lonLut = lonSuperLut[sj][si];
                if (latLut != null) {
                    latLut.complete();
                    lonLut.complete();
                }
            }
        }

        printMap();

        final long t1 = System.currentTimeMillis();
        System.out.println("initialize:  " + (t1 - t0) + "ms");
    }

    private void fillSuperLut(int w, int h, int y, int x, float lat, float lon) {
        if (isValid(lat, lon)) {
            int si = getSuperLutI(lon);
            int sj = getSuperLutJ(lat);

            PixelBoxLut latLut = latSuperLut[sj][si];
            PixelBoxLut lonLut = lonSuperLut[sj][si];
            if (latLut == null) {
                latLut = new PixelBoxLut(90.0 - (sj + 1), 90.0 - sj, LUT_SIZE);
                lonLut = new PixelBoxLut(si - 180.0, (si + 1) - 180.0, LUT_SIZE);
                latSuperLut[sj][si] = latLut;
                lonSuperLut[sj][si] = lonLut;
            }
            latLut.add(lat, x, y);
            lonLut.add(lon, x, y);

            if (x > 0) {
                latLut.add(lat, x - 1, y);
                lonLut.add(lon, x - 1, y);
            }
            if (x < w - 1) {
                latLut.add(lat, x + 1, y);
                lonLut.add(lon, x + 1, y);
            }
            if (y > 0) {
                latLut.add(lat, x, y - 1);
                lonLut.add(lon, x, y - 1);
            }
            if (y < h - 1) {
                latLut.add(lat, x, y + 1);
                lonLut.add(lon, x, y + 1);
            }
        }
    }

    private void printMap() {
        for (int sj = 0; sj < 180; sj++) {
            for (int si = 0; si < 360; si++) {
                PixelBoxLut latLut = latSuperLut[sj][si];
                PixelBoxLut lonLut = lonSuperLut[sj][si];
                if (latLut != null && lonLut != null) {
                    System.out.print("3");
                } else if (latLut != null) {
                    System.out.print("2");
                } else if (lonLut != null) {
                    System.out.print("1");
                } else {
                    System.out.print("-");
                }
            }
            System.out.println();
        }
    }

    private int getSuperLutJ(float lat) {
        int sj = (int) (90 - lat);
        if (sj == 180) {
            sj = 0;
        }
        return sj;
    }

    private int getSuperLutI(float lon) {
        int si = (int) (lon + 180);
        if (si == 360) {
            si = 0;
        }
        return si;
    }

    private boolean quadTreeSearch(final int depth,
                                   final float lat,
                                   final float lon,
                                   final int x, final int y,
                                   final int w, final int h,
                                   final Result result) {
        if (w < 2 || h < 2) {
            return false;
        }

        final int x1 = x;
        final int x2 = x1 + w - 1;

        final int y1 = y;
        final int y2 = y1 + h - 1;

        // todo - solve 180° longitude problem here
        GeoPos geoPos = new GeoPos();
        getGeoPosInternal(x1, y1, geoPos);
        final float lat0 = geoPos.lat;
        final float lon0 = geoPos.lon;
        getGeoPosInternal(x1, y2, geoPos);
        final float lat1 = geoPos.lat;
        final float lon1 = geoPos.lon;
        getGeoPosInternal(x2, y1, geoPos);
        final float lat2 = geoPos.lat;
        final float lon2 = geoPos.lon;
        getGeoPosInternal(x2, y2, geoPos);
        final float lat3 = geoPos.lat;
        final float lon3 = geoPos.lon;

//        final float epsL = 0.04f;
        final float epsL = (float) (0.04 * (1.0 + 1.0*lat));
        final float latMin = min(lat0, min(lat1, min(lat2, lat3))) - epsL;
        final float latMax = max(lat0, max(lat1, max(lat2, lat3))) + epsL;
        final float lonMin = min(lon0, min(lon1, min(lon2, lon3))) - epsL;
        final float lonMax = max(lon0, max(lon1, max(lon2, lon3))) + epsL;

        boolean pixelFound = false;
        final boolean definitelyOutside = lat < latMin || lat > latMax || lon < lonMin || lon > lonMax;
        if (!definitelyOutside) {
            if (w == 2 && h == 2) {
                final float f = (float) Math.cos(lat * MathUtils.DTOR);
                if (result.update(x1, y1, sqr(lat - lat0, f * (lon - lon0)))) {
                    pixelFound = true;
                }
                if (result.update(x1, y2, sqr(lat - lat1, f * (lon - lon1)))) {
                    pixelFound = true;
                }
                if (result.update(x2, y1, sqr(lat - lat2, f * (lon - lon2)))) {
                    pixelFound = true;
                }
                if (result.update(x2, y2, sqr(lat - lat3, f * (lon - lon3)))) {
                    pixelFound = true;
                }
            } else if (w >= 2 && h >= 2) {
                pixelFound = quadTreeRecursion(depth, lat, lon, x1, y1, w, h, result);
            }
        }

//        for (int i = 0; i < depth; i++) {
//            System.out.print("  ");
//        }
//        System.out.println(
//                depth + ": (" + x + "," + y + ") (" + w + "," + h + ") " + definitelyOutside + "  " + pixelFound);

        return pixelFound;
    }

    private void getGeoPosInternal(int pixelX, int pixelY, GeoPos geoPos) {
        int i = width * pixelY + pixelX;
        geoPos.setLocation(latData[i], lonData[i]);
    }


    private boolean quadTreeRecursion(final int depth,
                                      final float lat, final float lon,
                                      final int i, final int j,
                                      final int w, final int h,
                                      final Result result) {
        int w2 = w >> 1;
        int h2 = h >> 1;
        final int i2 = i + w2;
        final int j2 = j + h2;
        final int w2r = w - w2;
        final int h2r = h - h2;

        if (w2 < 2) {
            w2 = 2;
        }

        if (h2 < 2) {
            h2 = 2;
        }

        final boolean b1 = quadTreeSearch(depth + 1, lat, lon, i, j, w2, h2, result);
        final boolean b2 = quadTreeSearch(depth + 1, lat, lon, i, j2, w2, h2r, result);
        final boolean b3 = quadTreeSearch(depth + 1, lat, lon, i2, j, w2r, h2, result);
        final boolean b4 = quadTreeSearch(depth + 1, lat, lon, i2, j2, w2r, h2r, result);

        return b1 || b2 || b3 || b4;
    }


    private static float min(final float a, final float b) {
        return (a <= b) ? a : b;
    }

    private static float max(final float a, final float b) {
        return (a >= b) ? a : b;
    }

    private static float sqr(final float dx, final float dy) {
        return dx * dx + dy * dy;
    }


    private static class Result {

        public static final float INVALID = Float.MAX_VALUE;

        private int x;
        private int y;
        private float delta;

        private Result() {
            delta = INVALID;
        }

        public final boolean update(final int x, final int y, final float delta) {
            final boolean b = delta < this.delta;
            if (b) {
                this.x = x;
                this.y = y;
                this.delta = delta;
            }
            return b;
        }

        @Override
        public String toString() {
            return "Result[" + x + ", " + y + ", " + delta + "]";
        }
    }


    private static PixelBoxLut createLut(Band band, float[] data, boolean useWidth) {
        final Stx stx = band.getStx(true, ProgressMonitor.NULL);
        final int w = band.getSceneRasterWidth();
        final int h = band.getSceneRasterHeight();
        PixelBoxLut lut = new PixelBoxLut(band.scale(stx.getMin()), band.scale(stx.getMax()), useWidth ? w : h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float coord = data[(w * y + x)];
                if (!Float.isNaN(coord)) {
                    lut.add(coord, x, y);
                }
            }
        }
//        lut.dump();
        lut.complete();

        lut.dump();
        return lut;
    }

    static class PixelBoxLut {
        double min;
        double max;
        PixelBox[] pixelBoxes;

        PixelBoxLut(double min, double max, int size) {
            this.min = min;
            this.max = max;
            this.pixelBoxes = new PixelBox[size];
        }

        PixelBox getPixelBox(double coord) {
            final int index = getIndex(coord);
            if (index != -1) {
                final PixelBox pixelBox = pixelBoxes[index];
                if (pixelBox != null) {
                    return pixelBox;
                }
            }
            return null;
        }

        void add(double coord, int x, int y) {
            final int index = getIndex(coord);
            if (index != -1) {
                PixelBox pixelBox = pixelBoxes[index];
                if (pixelBox != null) {
                    pixelBox.add(x, y);
                } else {
                    pixelBoxes[index] = new PixelBox(x, y);
                }
            }
        }

        int getIndex(double coord) {
            if (coord >= min && coord < max) {
                return (int) ((coord - min) / (max - min) * pixelBoxes.length);
            } else if (coord == max) {
                return pixelBoxes.length - 1;
            }
            return -1;
        }

        void complete() {
            PixelBox prevValue = pixelBoxes[0];
            boolean pixelBoxSeen = prevValue != null;
            int i0 = 0;
            if (!pixelBoxSeen) {
                for (; i0 < pixelBoxes.length; i0++) {
                    prevValue = pixelBoxes[i0];
                    if (prevValue != null) {
                        pixelBoxSeen = true;
                        for (int i = 0; i < i0; i++) {
                            pixelBoxes[i] = prevValue;
                        }
                        break;
                    }
                }
            }
            if (!pixelBoxSeen) {
                throw new IllegalStateException();
            }
            for (int i = i0; i < pixelBoxes.length; i++) {
                PixelBox currentValue = pixelBoxes[i];
                if (pixelBoxSeen) {
                    if (currentValue != null) {
                        prevValue = currentValue;
                    } else {
                        prevValue = new PixelBox(prevValue.minX, prevValue.minY, prevValue.maxX, prevValue.maxY);
                        pixelBoxes[i] = prevValue;
                        pixelBoxSeen = false;
                    }
                } else {
                    if (currentValue != null) {
                        prevValue.add(currentValue);
                        prevValue = currentValue;
                        pixelBoxSeen = true;
                    } else {
                        pixelBoxes[i] = prevValue;
                    }
                }
            }
        }

        public void dump() {
            System.out.printf("min\t%s\n", min);
            System.out.printf("max\t%s\n", max);
            System.out.printf("%s\t%s\t%s\t%s\t%s\n", "i", "minX", "maxX", "minY", "maxY");
            for (int i = 0; i < pixelBoxes.length; i++) {
                PixelBox pixelBox = pixelBoxes[i];
                if (pixelBox != null) {
                    System.out.printf("%d\t%d\t%d\t%d\t%d\n", i, pixelBox.minX, pixelBox.maxX, pixelBox.minY, pixelBox.maxY);
                } else {
                    System.out.printf("%d\t%d\t%d\t%d\t%d\n", i, -1, -1, -1, -1);
                }
            }
        }
    }

    static final class PixelBox {
        int minX;
        int minY;
        int maxX;
        int maxY;

        PixelBox(int minX, int minY) {
            this(minX, minY, minX, minY);
        }

        PixelBox(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        void add(int x, int y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        public void add(PixelBox other) {
            add(other.minX, other.minY);
            add(other.maxX, other.maxY);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PixelBox pixelBox = (PixelBox) o;

            if (maxX != pixelBox.maxX) return false;
            if (maxY != pixelBox.maxY) return false;
            if (minX != pixelBox.minX) return false;
            if (minY != pixelBox.minY) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = minX;
            result = 31 * result + minY;
            result = 31 * result + maxX;
            result = 31 * result + maxY;
            return result;
        }

        @Override
        public String toString() {
            return "PixelBox{" +
                    "minX=" + minX +
                    ", minY=" + minY +
                    ", maxX=" + maxX +
                    ", maxY=" + maxY +
                    '}';
        }
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        // todo: implement
        return false;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public Datum getDatum() {
        return Datum.WGS_84;
    }

    @Override
    public void dispose() {
    }
}
