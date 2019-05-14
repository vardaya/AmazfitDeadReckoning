package amazfit.deadreckoning.bias;

import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

import amazfit.deadreckoning.utils.Utils;

public class MagneticFieldBias {


    private double[][] XTX; //X-Transposed * X is a 4x4 matrix
    private double[][] XTY; //X_Transposed * Y is a 4x1 vector (stored in a matrix for easier manipulation)

    boolean firstRun;

    float reserveX, reserveY, reserveZ;

    public MagneticFieldBias() {
        firstRun = true;
        XTX = new double[4][4];
        XTY = new double[4][1];
    }

    public void calcBias(float[] rawMagneticValues) {

        float x, y, z;

        if (firstRun) {
            reserveX = rawMagneticValues[0];
            reserveY = rawMagneticValues[1];
            reserveZ = rawMagneticValues[2];

            firstRun = false;
            return;
        } else {
            x = reserveX;
            y = reserveY;
            z = reserveZ;

            reserveX = rawMagneticValues[0];
            reserveY = rawMagneticValues[1];
            reserveZ = rawMagneticValues[2];
        }

        //calculating magnetic field bias
        XTX = new double[][]{{XTX[0][0] + x * x, XTX[0][1] + x * y, XTX[0][2] + x * z, XTX[0][3] + x},
                             {XTX[1][0] + x * y, XTX[1][1] + y * y, XTX[1][2] + y * z, XTX[1][3] + y},
                             {XTX[2][0] + x * z, XTX[2][1] + y * z, XTX[2][2] + z * z, XTX[2][3] + z},
                             {XTX[3][0] + x,     XTX[3][1] + y,     XTX[3][2] + z,     XTX[3][3] + 1}};

        XTY = new double[][] {{XTY[0][0] + x * (x * x + y * y + z * z)},
                              {XTY[1][0] + y * (x * x + y * y + z * z)},
                              {XTY[2][0] + z * (x * x + y * y + z * z)},
                              {XTY[3][0] + (x * x + y * y + z * z)}};

    }

    public float[] getBias() {
        SimpleMatrix M_XTX = new SimpleMatrix(XTX);
        SimpleMatrix M_XTY = new SimpleMatrix(XTY);

        SimpleMatrix M_XTX_Inverse = new SimpleMatrix(new double[4][4]);
        CommonOps.invert(M_XTX.getMatrix(), M_XTX_Inverse.getMatrix());

        SimpleMatrix M_B = M_XTX_Inverse.mult(M_XTY);

        float[][] B = Utils.denseMatrixToArray(M_B.getMatrix());

        float xBias = B[0][0] / 2.0f;
        float yBias = B[1][0] / 2.0f;
        float zBias = B[2][0] / 2.0f;
        float magneticFieldStrength = (float)Math.sqrt(B[3][0] + Math.pow(xBias, 2) + Math.pow(yBias, 2) + Math.pow(zBias, 2));

        return new float[] {xBias, yBias, zBias, magneticFieldStrength};
    }


}
