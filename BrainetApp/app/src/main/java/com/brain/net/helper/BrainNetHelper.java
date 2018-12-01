package com.brain.net.helper;

import android.os.Environment;

import java.util.Arrays;
import java.util.List;

public class BrainNetHelper {

    public static List<String> getBrainSignalFileList() {

        return Arrays.asList("saheb.edf", "natalya.edf", "nikita.edf", "harshdeep.edf");
    }

    public static String getFogServer() {
        return "http://192.168.0.8:8080";
    }

    public static String getCloudServer() {
        return "http://52.36.250.14:8080";
    }

    public static String getFogUrl() {
        return getFogServer() + "/files/authenticate";
    }

    public static String getCloudUrl() {
        return getCloudServer() + "/files/authenticate";
    }

    public static String getClassifier() {
        return "SVM";
    }

    public static String getFilePathDirectory() {
        return Environment.getExternalStorageDirectory().getPath() + "/brainnet";
    }

    public static String getDbFile() {
        return "brainnet.db";
    }

    public static String getDbPath() {
        return getFilePathDirectory() + "/" + getDbFile();
    }

    public static String getServerText(String serverText) {
        return "Server type : " + serverText;
    }

    public static String getInitialBattery(int initialBattery) {
        return "Initial Battery : " + initialBattery + " %";
    }

    public static String getFinalBattery(int finalBattery) {
        return "Final Battery : " + finalBattery + " %";
    }

    public static String getLatency(Long latency) {
        return "Time taken : " + latency + " ms";
    }
}
