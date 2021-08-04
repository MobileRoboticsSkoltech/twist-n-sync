package com.googleresearch.capturesync;

import android.content.Context;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.googleresearch.capturesync.softwaresync.TimeSync;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReceiveDelayTests {
    Context unitTestContext;

    @Before
    public void initContext() {
        unitTestContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        unitTestContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void receiveValueFirstTest_isCorrect() {
        String firstNameFile = "ForUnitTestFile4.csv";
        Pair<double[][], double[]> firstPair = ReadFromFile.ReadFromCSV(firstNameFile, unitTestContext);
        String secondNameFile = "ForUnitTestFile5.csv";
        Pair<double[][], double[]> secondPair = ReadFromFile.ReadFromCSV(secondNameFile, unitTestContext);

        TimeSync currentObject = new TimeSync(firstPair.first, secondPair.first, firstPair.second, secondPair.second, false);
        currentObject.resample(1.0);
        currentObject.obtainDelay();
        double timeDelay = currentObject.getTimeDelay();

        Assert.assertEquals(0.005384159941042243, timeDelay, 0.000001);
    }

    @Test
    public void receiveValueSecondTest_isCorrect() {
        String firstNameFile = "ForUnitTestFile6.csv";
        Pair<double[][], double[]> firstPair = ReadFromFile.ReadFromCSV(firstNameFile, unitTestContext);
        String secondNameFile = "ForUnitTestFile7.csv";
        Pair<double[][], double[]> secondPair = ReadFromFile.ReadFromCSV(secondNameFile, unitTestContext);

        TimeSync currentObject = new TimeSync(firstPair.first, secondPair.first, firstPair.second, secondPair.second, false);
        currentObject.resample(1.0);
        currentObject.obtainDelay();
        double timeDelay = currentObject.getTimeDelay();

        Assert.assertEquals(0.0493466958931981, timeDelay, 0.000001);
    }
}
