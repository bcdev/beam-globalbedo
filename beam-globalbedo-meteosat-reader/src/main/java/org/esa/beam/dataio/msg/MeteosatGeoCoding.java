package org.esa.beam.dataio.msg;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;

import java.awt.*;
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
                lonBand.readPixels(x, y, 1, 1, lon) ;
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

        int x1 = Math.max(latPixelBox.minX, lonPixelBox.minX);
        int y1 = Math.max(latPixelBox.minY, lonPixelBox.minY);
        int x2 = Math.min(latPixelBox.maxX, lonPixelBox.maxX);
        int y2 = Math.min(latPixelBox.maxY, lonPixelBox.maxY);

        if (x1 == x2 && y1 == y2) {
            pixelPos.setLocation(x1 + 0.5F, y1 + 0.5F);
        } else {
            int w = x2 - x1 + 1;
            int h = y2 - y1 + 1;
            final float[] lats = new float[w * h];
            final float[] lons = new float[w * h];
            try {
                latBand.readPixels(x1, y1, w, h, lats);
                lonBand.readPixels(x1, y1, w, h, lons);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            double minDist = Double.MAX_VALUE;
            int bestX = -1;
            int bestY = -1;
            for (int y = y1; y <= y2; y++) {
                for (int x = x1; x <= x2; x++) {
                    if (latBand.isPixelValid(x, y) && lonBand.isPixelValid(x, y)) {
                        final int i = (y - y1) * w + (x - x1);
                        final float dx = geoPos.lat - lats[i];
                        final float dy = geoPos.lon - lons[i];
                        double dist = dx * dx + dy * dy;
                        if (dist < minDist) {
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
                pixelPos.setInvalid();
            }
        }

        return pixelPos;
    }

    private void fillLatLonLuts() throws IOException {
        if (latLut == null) {
            latLut = createLut(latBand, false);
            lonLut = createLut(lonBand, true);
        }
    }

    private PixelBoxLut createLut(Band band, boolean useWidth) throws IOException {
        final Stx stx = band.getStx(true, ProgressMonitor.NULL);
        final int w = band.getSceneRasterWidth();
        final int h = band.getSceneRasterHeight();
        PixelBoxLut lut = new PixelBoxLut(band.scale(stx.getMin()), band.scale(stx.getMax()), useWidth ? w : h);
        final float[] line = new float[w];
        for (int y = 0; y < h; y++) {
            band.readPixels(0, y, line.length, 1, line);
            for (int x= 0; x < w; x++) {
                if (band.isPixelValid(x, y)) {
                    lut.add(line[x], x, y);
                }
            }
        }
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
                for (int i = index; i < pixelBoxes.length; i++) {
                    if (pixelBoxes[i] != null) {
                        return pixelBoxes[i];
                    }
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
    }

    static class PixelBox {
        int minX;
        int minY;
        int maxX;
        int maxY;

        PixelBox(int minX, int minY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = minX;
            this.maxY = minY;
        }

        void add(int x, int y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
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
