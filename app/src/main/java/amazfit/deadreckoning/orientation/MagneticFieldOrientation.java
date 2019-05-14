package amazfit.deadreckoning.orientation;

import org.ejml.simple.SimpleMatrix;

import amazfit.deadreckoning.utils.Utils;

public final class MagneticFieldOrientation {

    private MagneticFieldOrientation() {}

    private static SimpleMatrix m_rotationNED = new SimpleMatrix(new double[][]{{0,1,0},
                                                                                {1,0,0},
                                                                                {0,0,-1}});
    public static float[][] getOrientationMatrix(float[] G_values, float[] M_values, float[] M_bias) {

        //gravitáció kivonás
        double[][] M_init_unbiased = Utils.vectorToMatrix(removeBias(M_values, M_bias));

        SimpleMatrix m_M_init_unbiased = new SimpleMatrix(M_init_unbiased);
        SimpleMatrix m_G_values = new SimpleMatrix(Utils.vectorToMatrix(Utils.floatVectorToDoubleVector(G_values)));

        m_M_init_unbiased =  m_rotationNED.mult(m_M_init_unbiased);
        m_G_values =  m_rotationNED.mult(m_G_values);

        float[][] G_m_values = Utils.denseMatrixToArray(m_G_values.getMatrix());

        //roll/pitch számítás
        double G_r = Math.atan2(G_m_values[1][0], G_m_values[2][0]);
        double G_p = Math.atan2(-G_m_values[0][0], G_m_values[1][0] * Math.sin(G_r) + G_m_values[2][0] * Math.cos(G_r));

        //rotációs mátrix roll/pitch
        double[][] R_rp = {{Math.cos(G_p), Math.sin(G_p) * Math.sin(G_r), Math.sin(G_p) * Math.cos(G_r)},
                            {0, Math.cos(G_r), -Math.sin(G_r)},
                            {-Math.sin(G_p), Math.cos(G_p) * Math.sin(G_r), Math.cos(G_p) * Math.cos(G_r)}};


        //mátrix konverzió szorzáshoz
        SimpleMatrix m_R_rp = new SimpleMatrix(R_rp);

        //mágneses mező rotációja a gravitáció szerint
        SimpleMatrix m_M_rp = m_R_rp.mult(m_M_init_unbiased);

        //irány számítás mágneses mezőből
        double h = -1*(Math.atan2(-m_M_rp.get(1), m_M_rp.get(0)) + 11.0 * Math.PI/180.0);

        //rotációs mátrix mutatja az irányt, negatív kelet és észak felé
        double[][] R_h = {{Math.cos(h), -Math.sin(h), 0},
                          {Math.sin(h),  Math.cos(h), 0},
                          {0,            0,           1}};

        //rotációs mátrix számítása,  roll/pitch mátrix szorzása az irány mátrixal
        SimpleMatrix m_R_h = new SimpleMatrix(R_h);
        SimpleMatrix m_R = m_R_rp.mult(m_R_h);
        return Utils.denseMatrixToArray(m_R.getMatrix());

    }

    public static float getHeading(float[] G_values, float[] M_values, float[] M_bias) {
        float[][] orientationMatrix = getOrientationMatrix(G_values, M_values, M_bias);
        return (float) Math.atan2(orientationMatrix[1][0], orientationMatrix[0][0]);
    }


    private static double[] removeBias(float[] M_init, float[] M_bias) {
        //utolsó 3 érték figyelmen kihagyása
        double[] M_biasRemoved = new double[3];
        for (int i = 0; i < 3; i++)
            M_biasRemoved[i] = M_init[i] - M_bias[i];
        return M_biasRemoved;
    }

}
