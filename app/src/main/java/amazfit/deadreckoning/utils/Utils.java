package amazfit.deadreckoning.utils;

import android.content.SharedPreferences;

import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.Collections;

public  class Utils {

    public Utils() {}

    public static final String PREFS_NAME = "Inertial Navigation Preferences";

    public static final float[][] IDENTITY_MATRIX = {{1,0,0},
                                                     {0,1,0},
                                                     {0,0,1}};

    public static float getXFromPolar(double radius, double angle) {
        return (float)(radius * Math.cos(angle));
    }

    public static float getYFromPolar(double radius, double angle) {
        return (float)(radius * Math.sin(angle));
    }

    public static float nsToSec(float time) {
        return time / 1000000000.0f;
    }

    public static int factorial(int num) {
        int factorial = 1;
        for (int i = 1; i <= num; i++) {
            factorial *= i;
        }
        return factorial;
    }

    public static float[][] multiplyMatrices(float[][] a, float[][] b) {

        int numRows = a.length;

        int numCols = b[0].length;

        int numElements = b.length;

        float[][] c = new float[numRows][numCols];

        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                for (int element = 0; element < numElements; element++)
                    c[row][col] += a[row][element] * b[element][col];
            }
        }

        return c;

    }


    public static float[][] addMatrices(float[][] a, float[][] b) {

        int numRows = a.length;
        int numColumns = a[0].length;

        float[][] c = new float[numRows][numColumns];

        for (int row = 0; row < numRows; row++)
            for (int column = 0; column < numColumns; column++)
                c[row][column] = a[row][column] + b[row][column];

        return c;
    }

    public static float[][] scaleMatrix(float a[][], float scalar) {

        int numRows = a.length;
        int numColumns = a[0].length;

        float[][] b = new float[numRows][numColumns];

        for (int row = 0; row < numRows; row++)
            for (int column = 0; column < numColumns; column++)
                b[row][column] = a[row][column] * scalar;

        return b;

    }

    public static void addArrayToSharedPreferences(String arrayName, ArrayList<String> array, SharedPreferences.Editor editor) {
        editor.putInt(arrayName + "_size", array.size());
        for (int i = 0; i < array.size(); i++) {
            editor.putString(arrayName + "_" + i, array.get(i));
        }
        editor.apply();
    }

    public static ArrayList<String> getArrayFromSharedPreferences(String arrayName, SharedPreferences prefs) {

        int arraySize = prefs.getInt(arrayName + "_size", 0);

        ArrayList<String> newArray = new ArrayList<>();

        for (int i = 0; i < arraySize; i++) {
            newArray.add(prefs.getString(arrayName + "_" + i, null));
        }

        return newArray;

    }

    public static ArrayList<Float> arrayToList(float[] staticArray) {
        ArrayList<Float> dynamicList = new ArrayList<>();
        for (float staticArrayValue : staticArray)
            dynamicList.add(staticArrayValue);
        return dynamicList;
    }

    public static ArrayList<String> arrayToList(String[] staticArray) {
        ArrayList<String> dynamicList = new ArrayList<>();
        Collections.addAll(dynamicList, staticArray);
        return dynamicList;
    }



    public static float[][] denseMatrixToArray(DenseMatrix64F matrix) {
        float array[][] = new float[matrix.getNumRows()][matrix.getNumCols()];
        for (int row = 0; row < matrix.getNumRows(); row++)
            for (int col = 0; col < matrix.getNumCols(); col++)
                array[row][col] = (float) matrix.get(row,col);
        return array;
    }

    public static double[][] vectorToMatrix(double[] array) {
        return new double[][]{{array[0]},{array[1]},{array[2]}};
    }

    public static ArrayList<Float> createList(float... args) {
        ArrayList<Float> list = new ArrayList<>();
        for (float arg : args)
            list.add(arg);
        return list;
    }


    public static float radsToDegrees(double rads) {
        double degrees = (rads < 0) ? (2.0 * Math.PI + rads) : rads;
        degrees *= (180.0 / Math.PI);
        return (float)degrees;
    }

    public static float polarAdd(double initHeading, double deltaHeading) {

        double currHeading = initHeading + deltaHeading;

        if(currHeading < -Math.PI)
            return (float)((currHeading % Math.PI) + Math.PI);
        else if (currHeading > Math.PI)
            return (float)((currHeading % Math.PI) + -Math.PI);
        else
            return (float)currHeading;

    }

    public static float calcCompHeading(double magHeading, double gyroHeading) {

        if (magHeading < 0)
            magHeading = magHeading % (2.0 * Math.PI);
        if (gyroHeading < 0)
            gyroHeading = gyroHeading % (2.0 * Math.PI);

        double compHeading = 0.02 * magHeading + 0.98 * gyroHeading;

        if (compHeading > Math.PI)
            compHeading = (compHeading % Math.PI) + -Math.PI;

        return (float)compHeading;

    }

    public static float calcNorm(double... args) {
        double sumSq = 0;
        for (double arg : args)
            sumSq += Math.pow(arg, 2);
        return (float)Math.sqrt(sumSq);
    }

    public static double[] floatVectorToDoubleVector(float[] floatValues) {
        double[] doubleValues = new double[floatValues.length];
        for (int i = 0; i < floatValues.length; i++)
            doubleValues[i] = floatValues[i];
        return doubleValues;
    }
}
