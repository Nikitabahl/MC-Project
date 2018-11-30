package com.brain.net.helper;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;

import java.util.Arrays;
import java.util.List;

public class BrainNetHelper {

    public static List<String> getBrainSignalFileList() {

        return Arrays.asList("saheb.edf", "natalya.edf", "nikita.edf", "harshdeep.edf");
    }

    public static String getFogServer() {
        return "http://10.157.20.98:8080";
    }

    public static String getCloudServer() {
        return "http://54.184.104.34:8080";
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
}
