package amazfit.deadreckoning.orientation;

import amazfit.deadreckoning.utils.Utils;

public class GyroscopeDeltaOrientation {

    private boolean isFirstRun;
    private float sensitivity;
    private float lastTimestamp;
    private float[] gyroBias;


    public GyroscopeDeltaOrientation() {
        this.gyroBias = new float[3];
        this.sensitivity = 0.0025f;
        this.isFirstRun = true;
    }

    public GyroscopeDeltaOrientation(float sensitivity, float[] gyroBias) {
        this();
        this.sensitivity = sensitivity;
        this.gyroBias = gyroBias;
    }

    public float[] calcDeltaOrientation(long timestamp, float[] rawGyroValues) {
        //első timestamp
        if (isFirstRun) {
            isFirstRun = false;
            lastTimestamp = Utils.nsToSec(timestamp);
            return new float[3];
        }

        float[] unbiasedGyroValues = removeBias(rawGyroValues);

        return integrateValues(timestamp, unbiasedGyroValues);
    }

    public void setBias(float[] gyroBias) {
        this.gyroBias = gyroBias;
    }

    private float[] removeBias(float[] rawGyroValues) {
        //utolsó 3 érték figyelmen kívül hagyása
        float[] unbiasedGyroValues = new float[3];

        for (int i = 0; i < 3; i++)
            unbiasedGyroValues[i] = rawGyroValues[i] - gyroBias[i];

        for (int i = 0; i < 3; i++)
            if (Math.abs(unbiasedGyroValues[i]) > sensitivity)
                unbiasedGyroValues[i] = unbiasedGyroValues[i];
            else
                unbiasedGyroValues[i] = 0;

        return unbiasedGyroValues;
    }

    private float[] integrateValues(long timestamp, float[] gyroValues) {
        double currentTime = Utils.nsToSec(timestamp);
        double deltaTime = currentTime - lastTimestamp;

        float[] deltaOrientation = new float[3];

        //szögsebessség integrálása
        for (int i = 0; i < 3; i++)
            deltaOrientation[i] = gyroValues[i] * (float)deltaTime;

        lastTimestamp = (float) currentTime;

        return deltaOrientation;
    }

}
