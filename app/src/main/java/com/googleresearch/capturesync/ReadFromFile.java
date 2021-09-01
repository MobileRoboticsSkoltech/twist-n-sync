package com.googleresearch.capturesync;

import android.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class ReadFromFile {
    private static int countOfRows(File currentFile)
            throws IOException {
        int counter = 0;
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(currentFile));
        while (lineNumberReader.readLine() != null);
        counter = lineNumberReader.getLineNumber();
        return counter;
    }

    public static Pair<double[][], double[]> readFromCSV(File currentFile)
            throws IOException {
        InputStream inputStream = new FileInputStream(currentFile);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        int countOfRows = countOfRows(currentFile);
        double[][] coordinatesArray = new double[countOfRows][3];
        double[] timeArray = new double[countOfRows];
        int index = 0;

        while(bufferedReader.ready()) {
            String[] line = bufferedReader.readLine().split(",");
            double[] tempCoordinatesArray = { Double.parseDouble(line[0]), Double.parseDouble(line[1]), Double.parseDouble(line[2])};
            coordinatesArray[index] = tempCoordinatesArray;
            timeArray[index] = Double.parseDouble(line[3]) / 1e9;

            index++;
        }

        return new Pair<>(coordinatesArray, timeArray);
    }
}
