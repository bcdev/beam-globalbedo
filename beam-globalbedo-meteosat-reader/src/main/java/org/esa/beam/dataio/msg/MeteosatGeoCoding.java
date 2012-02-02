package org.esa.beam.dataio.msg;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;

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

    private Band latBand;
    private Band lonBand;
    private PixelBoxLut latLut;
    private PixelBoxLut lonLut;

    public MeteosatGeoCoding(Band latitude, Band longitude) {
        this.latBand = latitude;
        this.lonBand = longitude;

    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        final int x = (int) Math.floor(pixelPos.x);
        final int y = (int) Math.floor(pixelPos.y);
        if (latBand.isPixelValid(x, y) && lonBand.isPixelValid(x, y)) {
            try {
                final float[] lat = new float[1];
                final float[] lon = new float[1];
                latBand.readPixels(x, y, 1, 1, lat);
                lonBand.readPixels(x, y, 1, 1, lon);
                geoPos.setLocation(lat[0], lon[0]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

        try {
            fillLatLonLuts();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final PixelBox latPixelBox = latLut.getPixelBox(geoPos.lat);
        final PixelBox lonPixelBox = lonLut.getPixelBox(geoPos.lon);
        if (latPixelBox != null && lonPixelBox != null) {
            // System.out.println("geoPos = " + geoPos);
            // System.out.println("latPixelBox = " + latPixelBox + ", index = " + latLut.getIndex(geoPos.lat));
            // System.out.println("lonPixelBox = " + lonPixelBox + ", index = " + lonLut.getIndex(geoPos.lon));

            int x1 = Math.max(latPixelBox.minX, lonPixelBox.minX);
            int y1 = Math.max(latPixelBox.minY, lonPixelBox.minY);
            int x2 = Math.min(latPixelBox.maxX, lonPixelBox.maxX);
            int y2 = Math.min(latPixelBox.maxY, lonPixelBox.maxY);

            if (x1 == x2 && y1 == y2) {
                pixelPos.setLocation(x1 + 0.5F, y1 + 0.5F);
            } else {
                int w = x2 - x1 + 1;
                int h = y2 - y1 + 1;
                double minDist = Double.MAX_VALUE;
                int bestX = -1;
                int bestY = -1;
                if (w > 0 && h > 0) {
                    final MultiLevelImage latImage = latBand.getGeophysicalImage();
                    final MultiLevelImage lonImage = lonBand.getGeophysicalImage();
                    final Rectangle rect = new Rectangle(x1, y1, w, h);
                    System.out.println("rect = " + rect);
                    Raster latData = latImage.getData(rect);
                    Raster lonData = lonImage.getData(rect);

                    for (int y = y1; y <= y2; y++) {
                        for (int x = x1; x <= x2; x++) {
                            if (latBand.isPixelValid(x, y) && lonBand.isPixelValid(x, y)) {
                                float lat = latData.getSampleFloat(x, y, 0);
                                float lon = lonData.getSampleFloat(x, y, 0);
                                final float dx = geoPos.lat - lat;
                                final float dy = lonDiff(geoPos.lon, lon);
                                // todo - this is not really correct (use spherical dist.)
                                double dist = dx * dx + dy * dy;
                                if (dist < minDist) {
                                    minDist = dist;
                                    bestX = x;
                                    bestY = y;
                                }
                            }
                        }
                    }
                }
                if (bestX != -1 && bestY != -1) {
                    pixelPos.setLocation(bestX + 0.5F, bestY + 0.5F);
                } else {
                    pixelPos.setInvalid();
                }
            }
        } else {
            pixelPos.setInvalid();
        }

        return pixelPos;
    }

    private static float lonDiff(float a1, float a2) {
        float d = a1 - a2;
        if (d < 0.0f) {
            d = -d;
        }
        if (d > 180.0f) {
            d = 360.0f - d;
        }
        return d;
    }


    private void fillLatLonLuts() throws IOException {
        if (latLut == null) {
            final long t0 = System.currentTimeMillis();
            latLut = createLut(latBand, false);
            lonLut = createLut(lonBand, true);
            final long t1 = System.currentTimeMillis();
            System.out.println("fillLatLonLuts:  " + (t1 - t0) + "ms");
        }
    }

    private PixelBoxLut createLut(Band band, boolean useWidth) throws IOException {
        final Stx stx = band.getStx(true, ProgressMonitor.NULL);
        final int w = band.getSceneRasterWidth();
        final int h = band.getSceneRasterHeight();
        PixelBoxLut lut = new PixelBoxLut(band.scale(stx.getMin()), band.scale(stx.getMax()), useWidth ? w : h);
        final float[] line = new float[w];
        for (int y = 0; y < h; y++) {
            // todo - this may be too slow
            band.readPixels(0, y, line.length, 1, line);
            for (int x = 0; x < w; x++) {
                if (band.isPixelValid(x, y)) {
                    lut.add(line[x], x, y);
                }
            }
        }
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
            if (pixelBoxes[0] == null) {
                throw new IllegalStateException();
            }
            if (pixelBoxes[pixelBoxes.length - 1] == null) {
                throw new IllegalStateException();
            }
            boolean pixelBoxSeen = true;
            PixelBox prevValue = pixelBoxes[0];
            for (int i = 1; i < pixelBoxes.length; i++) {
                PixelBox currentValue = pixelBoxes[i];
                if (pixelBoxSeen) {
                    if (currentValue == null) {
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
                System.out.printf("%d\t%d\t%d\t%d\t%d\n", i, pixelBox.minX, pixelBox.maxX, pixelBox.minY, pixelBox.maxY);
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
