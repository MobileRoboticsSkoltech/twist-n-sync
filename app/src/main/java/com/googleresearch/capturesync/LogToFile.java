package com.googleresearch.capturesync;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Simple file logging implementation,
 * creates or appends to provided file
 */
public class LogToFile {
    public LogToFile(File logFile) {
        mLogFile = logFile;
    }

    private final File mLogFile;

    public void appendLog(String text, Context context)
    {
        try(
            BufferedWriter buf = new BufferedWriter(new FileWriter(mLogFile, true))
        ) {
            buf.append(text);
            buf.newLine();
        } catch (IOException e)
        {
            // TODO Auto-generated catch
            e.printStackTrace();
        }
    }
}
