package org.esa.beam.globalbedo.inversion;

import Jama.LUDecomposition;
import Jama.Matrix;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.globalbedo.inversion.Accumulator;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.InversionOp;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS;
import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS;

public class InversionOpTest extends TestCase {

    public void testInversion() {

        // test to see what is going on in inversion code fed by an accumulator matrix
        float[][][] sumMatrices = new float[92][1][1];
        int index = 0;

        // M 9x9
        sumMatrices[index++][0][0] = (float) 54634.32421875               ;
        sumMatrices[index++][0][0] = (float) 587.370361328125             ;
        sumMatrices[index++][0][0] = (float) -73221.53125                 ;
        sumMatrices[index++][0][0] = (float) -439.38616943359375          ;
        sumMatrices[index++][0][0] = (float) -13.926740646362305          ;
        sumMatrices[index++][0][0] = (float) 565.5816650390625            ;
        sumMatrices[index++][0][0] = (float) -516.2998657226562           ;
        sumMatrices[index++][0][0] = (float) -14.745708465576172          ;
        sumMatrices[index++][0][0] = (float) 669.9032592773438            ;
        sumMatrices[index++][0][0] = (float) 587.370361328125             ;
        sumMatrices[index++][0][0] = (float)1822.618408203125             ;
        sumMatrices[index++][0][0] = (float)3958.828369140625             ;
        sumMatrices[index++][0][0] = (float)-13.926740646362305           ;
        sumMatrices[index++][0][0] = (float)-18.42544937133789            ;
        sumMatrices[index++][0][0] = (float)-30.271902084350586           ;
        sumMatrices[index++][0][0] = (float)-14.745708465576172           ;
        sumMatrices[index++][0][0] = (float)-20.590757369995117           ;
        sumMatrices[index++][0][0] = (float)-33.914615631103516           ;
        sumMatrices[index++][0][0] = (float)-73221.53125                  ;
        sumMatrices[index++][0][0] = (float)3958.828369140625             ;
        sumMatrices[index++][0][0] = (float)112712.84375                  ;
        sumMatrices[index++][0][0] = (float)565.5816650390625             ;
        sumMatrices[index++][0][0] = (float)-30.271902084350586           ;
        sumMatrices[index++][0][0] = (float)-875.9813232421875            ;
        sumMatrices[index++][0][0] = (float)669.9032592773438             ;
        sumMatrices[index++][0][0] = (float)-33.914615631103516           ;
        sumMatrices[index++][0][0] = (float)-1030.3544921875              ;
        sumMatrices[index++][0][0] = (float)-439.38616943359375           ;
        sumMatrices[index++][0][0] = (float)-13.926740646362305           ;
        sumMatrices[index++][0][0] = (float)565.5816650390625             ;
        sumMatrices[index++][0][0] = (float)11649.30859375                ;
        sumMatrices[index++][0][0] = (float)853.8493041992188             ;
        sumMatrices[index++][0][0] = (float)-13272.84375                  ;
        sumMatrices[index++][0][0] = (float)-350.0549621582031            ;
        sumMatrices[index++][0][0] = (float)-12.57763957977295            ;
        sumMatrices[index++][0][0] = (float)445.75030517578125            ;
        sumMatrices[index++][0][0] = (float)-13.926740646362305           ;
        sumMatrices[index++][0][0] = (float)-18.42544937133789            ;
        sumMatrices[index++][0][0] = (float)-30.271902084350586           ;
        sumMatrices[index++][0][0] = (float)853.8493041992188             ;
        sumMatrices[index++][0][0] = (float)825.0358276367188             ;
        sumMatrices[index++][0][0] = (float)1379.49560546875              ;
        sumMatrices[index++][0][0] = (float)-12.57763957977295            ;
        sumMatrices[index++][0][0] = (float)-15.78542423248291            ;
        sumMatrices[index++][0][0] = (float)-26.337247848510742           ;
        sumMatrices[index++][0][0] = (float)565.5816650390625             ;
        sumMatrices[index++][0][0] = (float)-30.271902084350586           ;
        sumMatrices[index++][0][0] = (float)-875.9813232421875            ;
        sumMatrices[index++][0][0] = (float)-13272.84375                  ;
        sumMatrices[index++][0][0] = (float)1379.49560546875              ;
        sumMatrices[index++][0][0] = (float)22817.005859375               ;
        sumMatrices[index++][0][0] = (float)445.75030517578125            ;
        sumMatrices[index++][0][0] = (float)-26.337247848510742           ;
        sumMatrices[index++][0][0] = (float)-699.7872314453125            ;
        sumMatrices[index++][0][0] = (float)-516.2998657226562            ;
        sumMatrices[index++][0][0] = (float)-14.745708465576172           ;
        sumMatrices[index++][0][0] = (float)669.9032592773438             ;
        sumMatrices[index++][0][0] = (float)-350.0549621582031            ;
        sumMatrices[index++][0][0] = (float)-12.57763957977295            ;
        sumMatrices[index++][0][0] = (float)445.75030517578125            ;
        sumMatrices[index++][0][0] = (float)21543.873046875               ;
        sumMatrices[index++][0][0] = (float)688.6531372070312             ;
        sumMatrices[index++][0][0] = (float)-27446.318359375              ;
        sumMatrices[index++][0][0] = (float)-14.745708465576172           ;
        sumMatrices[index++][0][0] = (float)-20.590757369995117           ;
        sumMatrices[index++][0][0] = (float)-33.914615631103516           ;
        sumMatrices[index++][0][0] = (float)-12.57763957977295            ;
        sumMatrices[index++][0][0] = (float)-15.78542423248291            ;
        sumMatrices[index++][0][0] = (float)-26.337247848510742           ;
        sumMatrices[index++][0][0] = (float)688.6531372070312             ;
        sumMatrices[index++][0][0] = (float)1024.93701171875              ;
        sumMatrices[index++][0][0] = (float)2057.6259765625               ;
        sumMatrices[index++][0][0] = (float)669.9032592773438             ;
        sumMatrices[index++][0][0] = (float)-33.914615631103516           ;
        sumMatrices[index++][0][0] = (float)-1030.3544921875              ;
        sumMatrices[index++][0][0] = (float)445.75030517578125            ;
        sumMatrices[index++][0][0] = (float)-26.337247848510742           ;
        sumMatrices[index++][0][0] = (float)-699.7872314453125            ;
        sumMatrices[index++][0][0] = (float)-27446.318359375              ;
        sumMatrices[index++][0][0] = (float)2057.6259765625               ;
        sumMatrices[index++][0][0] = (float) 44402.18359375               ;

        // V 9x1
        sumMatrices[index++][0][0] = (float)3640.5869140625                  ;
        sumMatrices[index++][0][0] = (float)288.4021911621094                ;
        sumMatrices[index++][0][0] = (float)-4153.6962890625                 ;
        sumMatrices[index++][0][0] = (float)2169.84716796875                 ;
        sumMatrices[index++][0][0] = (float)249.37490844726562               ;
        sumMatrices[index++][0][0] = (float)-2214.97900390625                ;
        sumMatrices[index++][0][0] = (float)2780.689697265625                ;
        sumMatrices[index++][0][0] = (float)265.36859130859375               ;
        sumMatrices[index++][0][0] = (float)-3021.56005859375                ;

        // E 1x1
        sumMatrices[index++][0][0] = (float) 1096.514404296875               ;

        // mask
        sumMatrices[index][0][0] = (float) 29.961593627929688               ;

        Accumulator accumulator = Accumulator.createForInversion(sumMatrices, 0, 0);
        double maskAcc = accumulator.getMask();

        final Matrix mAcc = AlbedoInversionUtils.getMatrix2DTruncated(accumulator.getM());
        Matrix vAcc = AlbedoInversionUtils.getMatrix2DTruncated(accumulator.getV());
        final Matrix eAcc = AlbedoInversionUtils.getMatrix2DTruncated(accumulator.getE());

        Matrix parameters = new Matrix(NUM_BBDR_WAVE_BANDS * NUM_ALBEDO_PARAMETERS, 1, AlbedoInversionConstants.NO_DATA_VALUE);

        final LUDecomposition lud = new LUDecomposition(mAcc);
        if (!lud.isNonsingular()) {
            maskAcc = 0.0;
        }

        double entropy = -1.0;
        if (maskAcc != 0.0) {
            parameters = mAcc.solve(vAcc);
            entropy = InversionOp.getEntropy(mAcc);
        }
        // 'Goodness of Fit'...
        double goodnessOfFit = InversionOp.getGoodnessOfFit(mAcc, vAcc, eAcc, parameters, maskAcc);
        System.out.println();
    }


}
