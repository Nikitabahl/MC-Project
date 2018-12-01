package com.brain.net.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.VideoView;

import com.brain.net.R;
import com.brain.net.helper.BrainNetHelper;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends Activity {

    @BindView(R.id.video_stimulus)
    VideoView videoView;

    @BindView(R.id.et_user_name)
    EditText userName;

    @BindView(R.id.btn_login)
    Button btnLogin;

    @BindView(R.id.file_spinner)
    Spinner fileSpinner;

    @BindView(R.id.server_spinner)
    Spinner serverSpinner;

    File selectedFile;
    private static final String FOG = "Fog";
    private static final String CLOUD = "Cloud";
    private static String serverType = FOG;
    private List<String> fileList = new ArrayList<>();
    private SQLiteDatabase myDatabase;
    private static final int REQUEST_WRITE_STORAGE = 112;

    public static final String SERVER = "server";
    public static final String LATENCY = "latency";
    public static final String BATTERY_LEVEL_1 = "battery_level_1";
    public static final String BATTERY_LEVEL_2 = "battery_level_2";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Boolean hasPermission = (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

        if (!hasPermission) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }

        //bind xml to activity
        ButterKnife.bind(this);

        File dbpath = new File(BrainNetHelper.getFilePathDirectory());

        if (!dbpath.exists()) {
            dbpath.mkdirs();
        }

        File database = new File(dbpath, BrainNetHelper.getDbFile());

        if (!database.exists()) {
            try {
                database.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        myDatabase = SQLiteDatabase.openOrCreateDatabase(database, null);

        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS adaptive_metrics(server VARCHAR, " +
                "latency long, count int)");

        serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                serverType = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        File rootDirectory = new File(BrainNetHelper.getFilePathDirectory());
        File[] files = rootDirectory.listFiles();
        fileList.clear();

        for (File file : files) {
            fileList.add(file.getName());
        }

        selectedFile = files[0];

        ArrayAdapter<String> file_list = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, fileList);

        file_list.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fileSpinner.setAdapter(file_list);

        fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                String selectedFileName = adapterView.getItemAtPosition(i).toString();

                File rootDirectory = new File(BrainNetHelper.getFilePathDirectory());
                File[] files = rootDirectory.listFiles();

                for(File file : files){

                    if(file.getName().equals(selectedFileName)) {
                        selectedFile =  file;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        btnLogin.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideo();
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                String url;

                switch (serverType) {

                    case FOG:
                        url = BrainNetHelper.getFogUrl();
                        break;
                    case CLOUD:
                        url = BrainNetHelper.getCloudUrl();
                        break;
                    default:
                        url = getAdaptiveUrl();
                        break;
                }

                new LoginAsyncTask().execute(userName.getText().toString(), url, serverType);
            }
        });
    }

    private void playVideo() {

        Toast.makeText(getApplicationContext(),
                "Wait for Authentication. Collecting brain Signals", Toast.LENGTH_SHORT).show();

        String uriPath = "android.resource://" + getPackageName() + "/" + R.raw.soothing;
        Uri uri = Uri.parse(uriPath);

        videoView.setVideoURI(uri);
        videoView.requestFocus();

        videoView.start();
    }

    private String getAdaptiveUrl() {

        //Some logic to decide whether to use fog or cloud server
        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        Cursor resultSet = myDatabase.rawQuery("Select * from adaptive_metrics " +
                "WHERE server = '" + FOG + "'",null);

        long networkDelayFog = Long.MAX_VALUE;
        long networkDelayCloud = Long.MAX_VALUE;

        if (resultSet.getCount() == 1) {
            resultSet.moveToFirst();
             networkDelayFog = resultSet.getLong(1);
        }
        resultSet.close();

        resultSet = myDatabase.rawQuery("Select * from adaptive_metrics " +
                "WHERE server = '" + CLOUD + "'",null);

        if (resultSet.getCount() == 1) {
            resultSet.moveToFirst();
            networkDelayCloud = resultSet.getLong(1);
        }

        resultSet.close();

        if (batLevel > 70) {
            serverType = CLOUD;
            return BrainNetHelper.getCloudUrl();
        } else {

            if (networkDelayCloud == networkDelayFog && networkDelayCloud == Long.MAX_VALUE) {

                double num = Math.random();

                if (num < 0.5) {
                    serverType = CLOUD;
                    return BrainNetHelper.getCloudUrl();
                } else {
                    serverType = FOG;
                    return BrainNetHelper.getFogUrl();
                }
            }

            if (networkDelayCloud <= networkDelayFog) {
                serverType = CLOUD;
                return BrainNetHelper.getCloudUrl();
            }  else {
                serverType = FOG;
                return BrainNetHelper.getFogUrl();
            }
        }
    }

    private long getNetworkDelay(String host) {

        int timeout = 3000;
        boolean reachable = false;
        long beforeTime = System.currentTimeMillis();

        try {
            reachable = InetAddress.getByName(host).isReachable(timeout);
        } catch (Exception e) {
            Log.e("Network Delay", e.getMessage());
        }
        long afterTime = System.currentTimeMillis();
        long timeDifference = afterTime - beforeTime;

        if (!reachable) {
            timeDifference = Long.MAX_VALUE;
        }
        return timeDifference;
    }

    public class LoginAsyncTask extends AsyncTask<String, String, String> {

        int responseCode = 0;
        public ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        long startTimer, endTimer;
        String server, responseBody;

        BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
        int initialBatteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        @Override
        protected void onPreExecute() {
            startTimer = System.currentTimeMillis();
            dialog.setTitle("Login Loader");
            dialog.setMessage("Authenticating......");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... args) {

            String url = args[1];
            String userName = args[0];
            server = args[2];
            String classifier = BrainNetHelper.getClassifier();

            OkHttpClient client = new OkHttpClient();
            RequestBody fileData = RequestBody.create(MediaType.parse("text/csv"), selectedFile);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("classifier", classifier)
                    .addFormDataPart("type","text/csv")
                    .addFormDataPart("file", selectedFile.getName(), fileData)
                    .build();


            url += "?userName=" + userName;

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            try {
                Response response = client.newCall(request).execute();

                System.out.println(request);
                System.out.println(response);

                responseBody = response.body().string();
                responseCode = response.code();


            } catch (IOException e) {
                Log.e("Login call", e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(String args) {

            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            endTimer = System.currentTimeMillis();
            long timer = endTimer - startTimer;

            switch (responseCode) {

                case 200:

                    insertOrUpdateLatencyData(server, timer);

                    if (responseBody.equals("true")) {
                        Toast.makeText(getApplicationContext(), "User Authenticated in "
                                + Long.toString(timer) + " ms", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this, AuthenticatedUser.class);
                        int finalBatteryLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

                        intent.putExtra(LATENCY, timer);
                        intent.putExtra(BATTERY_LEVEL_1, initialBatteryLevel);
                        intent.putExtra(BATTERY_LEVEL_2, finalBatteryLevel);
                        intent.putExtra(SERVER, server);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "User UnAuthorized", Toast.LENGTH_SHORT).show();
                    }

                    break;

                case 404:
                    Toast.makeText(getApplicationContext(),
                            "User UnAuthorized", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Toast.makeText(getApplicationContext(),
                            "Server took too long to respond", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void insertOrUpdateLatencyData(String server, long timer) {

        Cursor resultSet = myDatabase.rawQuery("Select * from adaptive_metrics " +
                "WHERE server = '" + server + "'",null);

        if (resultSet.getCount() == 0) {
            myDatabase.execSQL("INSERT into " +
                    "adaptive_metrics(server, latency, count) " +
                    "VALUES ('" + server + "', "+ timer +","+ 1 +")");
        } else {
            resultSet.moveToFirst();
            int count = resultSet.getInt(1);
            long timerDB = resultSet.getLong(2);

            timerDB += timerDB * count;
            count += 1;
            timerDB /= (long) count;

            myDatabase.execSQL("Update adaptive_metrics set latency = " + timerDB +
                    " , count = " + count + " WHERE server = '" + server + "'");
        }
        resultSet.close();
    }
}
