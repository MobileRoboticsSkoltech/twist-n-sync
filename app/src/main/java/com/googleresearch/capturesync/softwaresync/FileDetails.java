package com.googleresearch.capturesync.softwaresync;

import java.io.Serializable;

public class FileDetails implements Serializable {
    String name;
    long size;

    public void setDetails(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }
}
