
package amazfit.deadreckoning.orientation;

import amazfit.deadreckoning.utils.Utils;

//orientáció meghatározása az Euler szögeket használva majd eltárolva egy Direction Cosine Matrixban
public class GyroscopeEulerOrientation {

    private float[][] C;

    public GyroscopeEulerOrientation() {
        C = Utils.IDENTITY_MATRIX.clone();
    }

    public GyroscopeEulerOrientation(float[][] initialOrientation) {
        this();
        C = initialOrientation.clone();
    }

    public float[][] getOrientationMatrix(float[] gyroValues) {

        float wX = gyroValues[1];
        float wY = gyroValues[0];
        float wZ = -gyroValues[2];

        float[][] A = calcMatrixA(wX, wY, wZ);

        calcMatrixC(A);

        return C.clone();
    }

    public float getHeading(float[] gyroValue) {
        getOrientationMatrix(gyroValue);
        return (float) (Math.atan2(C[1][0], C[0][0]));
    }

    private float[][] calcMatrixA(float wX, float wY, float wZ) {

        float[][] A;

        //Ferdén szimmetrikus mátrix
        float[][] B = calcMatrixB(wX, wY, wZ);
        float[][] B_sq = Utils.multiplyMatrices(B, B);

        float norm = Utils.calcNorm(wX, wY, wZ);
        float B_scaleFactor = calcBScaleFactor(norm);
        float B_sq_scaleFactor = calcBSqScaleFactor(norm);

        B = Utils.scaleMatrix(B, B_scaleFactor);
        B_sq = Utils.scaleMatrix(B_sq, B_sq_scaleFactor);

        A = Utils.addMatrices(B, B_sq);
        A = Utils.addMatrices(A, Utils.IDENTITY_MATRIX);

        return A;
    }

    private float[][] calcMatrixB(float wX, float wY, float wZ) {
          return (new float[][]{{0, wZ, -wY},
                                {-wZ, 0, wX},
                                {wY, -wX, 0}});
    }

    //(sin σ) / σ ≈ 1 - (σ^2 / 3!) + (σ^4 / 5!)
    private float calcBScaleFactor(float sigma) {
        //return (float) ((1 - Math.cos(sigma)) / Math.pow(sigma, 2));
        float sigmaSqOverThreeFactorial = (float) Math.pow(sigma, 2) / Utils.factorial(3);
        float sigmaToForthOverFiveFactorial = (float) Math.pow(sigma, 4) / Utils.factorial(5);
        return (float) (1.0 - sigmaSqOverThreeFactorial + sigmaToForthOverFiveFactorial);
    }

    //(1 - cos σ) / σ^2 ≈ (1/2) - (σ^2 / 4!) + (σ^4 / 6!)
    private float calcBSqScaleFactor(float sigma) {
        float sigmaSqOverFourFactorial = (float) Math.pow(sigma, 2) / Utils.factorial(4);
        float sigmaToForthOverSixFactorial = (float) Math.pow(sigma, 4) / Utils.factorial(6);
        return (float) (0.5 - sigmaSqOverFourFactorial + sigmaToForthOverSixFactorial);
    }

    //DCM számítása az új irány alapján
    private void calcMatrixC(float[][] A) {
        C = Utils.multiplyMatrices(C, A);
    }

}
