package org.esa.beam.globalbedo.bbdr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class Main {

    public static void main(String[] args) throws IOException {
        JAI.enableDefaultTileCache();
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(756 * 1024 * 1024);
        JAI.getDefaultInstance().getTileScheduler().setParallelism(4);
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose
        final String sensorName = args[0];
        final String sourceProductFileName = args[1];
        final String targetProductFileName = args[2];
        final Product sourceProduct = ProductIO.readProduct(sourceProductFileName);
        final Operator bbdrOp = new BbdrOp();
        bbdrOp.setSourceProduct(sourceProduct);
        bbdrOp.setParameter("sensor", sensorName);
        Product targetProduct = bbdrOp.getTargetProduct();

        final ReprojectionOp repro = new ReprojectionOp();
        repro.setParameter("easting", 7783653.6);    // tile h25v06
        repro.setParameter("northing", 3335851.6);

//        repro.setParameter("easting", -1111950.520);   // tile h17v03
//        repro.setParameter("northing", 6671703.118);

        repro.setParameter("crs", "PROJCS[\"MODIS Sinusoidal\"," +
                                  "GEOGCS[\"WGS 84\"," +
                                  "  DATUM[\"WGS_1984\"," +
                                  "    SPHEROID[\"WGS 84\",6378137,298.257223563," +
                                  "      AUTHORITY[\"EPSG\",\"7030\"]]," +
                                  "    AUTHORITY[\"EPSG\",\"6326\"]]," +
                                  "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," +
                                  "  UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]]," +
                                  "   AUTHORITY[\"EPSG\",\"4326\"]]," +
                                  "PROJECTION[\"Sinusoidal\"]," +
                                  "PARAMETER[\"false_easting\",0.0]," +
                                  "PARAMETER[\"false_northing\",0.0]," +
                                  "PARAMETER[\"central_meridian\",0.0]," +
                                  "PARAMETER[\"semi_major\",6371007.181]," +
                                  "PARAMETER[\"semi_minor\",6371007.181]," +
                                  "UNIT[\"m\",1.0]," +
                                  "AUTHORITY[\"SR-ORG\",\"6974\"]]");

        repro.setParameter("resampling", "Nearest");
        repro.setParameter("includeTiePointGrids", true);
        repro.setParameter("referencePixelX", 0.0);
        repro.setParameter("referencePixelY", 0.0);
        repro.setParameter("orientation", 0.0);
        repro.setParameter("pixelSizeX", 926.6254330558);
        repro.setParameter("pixelSizeY", 926.6254330558);
        repro.setParameter("width", 1200);
        repro.setParameter("height", 1200);
        repro.setParameter("orthorectify", true);
        repro.setParameter("noDataValue", 0.0);
        repro.setSourceProduct(targetProduct);

        targetProduct = repro.getTargetProduct();

        final WriteOp writeOp = new WriteOp(targetProduct, new File(targetProductFileName),
                                            ProductIO.DEFAULT_FORMAT_NAME);

        writeOp.writeProduct(ProgressMonitor.NULL);
    }

}
