package com.googleresearch.capturesync;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ReadFromFileTests {
    Context unitTestContext;

    @Before
    public void initContext() {
        unitTestContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        unitTestContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void readFromFile_CoordinatesArray_isCorrect() {
        double[][] correctArray = { { (double)1.8405248, (double)-1.7660676, (double)0.49335796 }, { (double)1.8783984, (double)-1.8546431, (double)0.5410055 }, { (double)1.8924483, (double)-1.896182, (double)0.5593314 } };
        String namefile = "ForUnitTestFile1.csv";

        Assert.assertArrayEquals(correctArray, ReadFromFile.ReadFromCSV(namefile, unitTestContext).first);
    }

    @Test
    public void readFromFile_TimesArray_isCorrect() {
        double[] correctArray = { (double)691166767933511L / 1e9, (double)691166769907511L / 1e9, (double)691166771907511L / 1e9 };
        String namefile = "ForUnitTestFile1.csv";

        Assert.assertArrayEquals(correctArray, ReadFromFile.ReadFromCSV(namefile, unitTestContext).second, 0.1);
    }

    @Test
    public void readFromFile_IncorrectCSV() {
        double[] correctArray = new double[0];
        String namefile = "ForUnitTestFile2.csv";

        Assert.assertArrayEquals(correctArray, ReadFromFile.ReadFromCSV(namefile, unitTestContext).second, 0.1);
    }

    @Test
    public void readFromFile_EmptyFile() {
        double[] correctArray = new double[0];
        String namefile = "ForUnitTestFile3.csv";

        Assert.assertArrayEquals(correctArray, ReadFromFile.ReadFromCSV(namefile, unitTestContext).second, 0.1);
    }
}
