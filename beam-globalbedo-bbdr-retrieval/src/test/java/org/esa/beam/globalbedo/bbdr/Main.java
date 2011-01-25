package org.esa.beam.globalbedo.bbdr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.gpf.operators.standard.WriteOp;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class Main {

    public static void main(String[] args) throws IOException {
        JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
        final String sourceProductFileName = args[0];
        final String targetProductFileName = args[1];
        final Product sourceProduct = ProductIO.readProduct(sourceProductFileName);
        final BbdrOp bbdrOp = new BbdrOp();
        bbdrOp.setSourceProduct("source", sourceProduct);
        final Product targetProduct = bbdrOp.getTargetProduct();
        final WriteOp writeOp = new WriteOp(targetProduct, new File(targetProductFileName),
                                            ProductIO.DEFAULT_FORMAT_NAME);

        writeOp.writeProduct(ProgressMonitor.NULL);
    }

}
