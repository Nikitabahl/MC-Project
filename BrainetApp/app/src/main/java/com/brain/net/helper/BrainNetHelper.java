package com.brain.net.helper;

import android.os.Environment;

import java.util.Arrays;
import java.util.List;

public class BrainNetHelper {

    public static List<String> getBrainSignalFileList() {

        return Arrays.asList("saheb.edf", "natalya.edf", "nikita.edf", "harshdeep.edf");
    }

    public static String getFogUrl() {
        return "http://fog-url:8080";
    }

    public static String getCloudUrl() {
        return "http://cloud-url:8080";
    }

    public static String getClassifier() {
        return "SVM";
    }

    public static String getDBFilePath() {
        return Environment.getExternalStorageDirectory().getPath();
    }
}
