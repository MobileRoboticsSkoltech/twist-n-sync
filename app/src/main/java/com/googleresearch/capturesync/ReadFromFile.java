package com.googleresearch.capturesync;

import android.content.Context;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ReadFromFile {
    private static int CountOfRows(String namefile, Context context) {
        int counter = 0;

        try {
            InputStream inputStream = context.getAssets().open(namefile);
            for (int symbol = 0; symbol != -1; symbol = inputStream.read()) {
                counter += symbol == '\n' ? 1 : 0;
            }
        } catch (IOException exception) {
            System.out.println("Error Reading The File.");
            exception.printStackTrace();
        }

        return counter;
    }

    public static Pair<double[][], double[]> ReadFromCSV(String namefile, Context context) {
        double[][] coordinatesArray = new double[0][0];
        double[] timeArray = new double[0];

        try {
            InputStream inputStream = context.getAssets().open(namefile);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            int countOfRows = CountOfRows(namefile, context);
            coordinatesArray = new double[countOfRows][3];
            timeArray = new double[countOfRows];
            int index = 0;

            while(bufferedReader.ready()) {
                String[] line = bufferedReader.readLine().split(",");
                double[] tempCoordinatesArray = { Double.parseDouble(line[0]), Double.parseDouble(line[1]), Double.parseDouble(line[2])};
                coordinatesArray[index] = tempCoordinatesArray;
                timeArray[index] = Double.parseDouble(line[3]) / 1e9;

                index++;
            }
        } catch (FileNotFoundException exception) {
            System.out.println("File Not Found.");
            exception.printStackTrace();
        } catch (IOException exception) {
            System.out.println("Error Reading The File.");
            exception.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException exception) {
            System.out.println("CSV incorrect");
            exception.printStackTrace();
            return new Pair<>(new double[0][0], new double[0]);
        }

        return new Pair<>(coordinatesArray, timeArray);
    }
}
