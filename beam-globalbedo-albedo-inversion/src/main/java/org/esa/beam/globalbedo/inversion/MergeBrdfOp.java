package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;

/**
 * Operator for merging BRDF Snow/NoSnow contributions.
 * The breadboard file is 'MergseBRDF.py' provided by Gerardo López Saldaña.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class MergeBrdfOp extends PixelOperator {

    // todo: implement

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void configureTargetProduct(Product targetProduct) throws OperatorException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
