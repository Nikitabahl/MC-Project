package com.brain.net.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
    private static String serverType = "fog";
    private List<String> fileList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //bind xml to activity
        ButterKnife.bind(this);

        serverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                serverType = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        /*
        File rootDirectory = new File(BrainNetHelper.getDBFilePath() + getPackageName());
        File[] files = rootDirectory.listFiles();
        fileList.clear();

        for (File file:files) {
            fileList.add(file.getName());
        }*/

        fileList = BrainNetHelper.getBrainSignalFileList();

        ArrayAdapter<String> file_list = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, fileList);

        file_list.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fileSpinner.setAdapter(file_list);

        fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                String selectedFileName = adapterView.getItemAtPosition(i).toString();

                /*
                File rootDirectory = new File(BrainNetHelper.getDBFilePath() + getPackageName());
                File[] files = rootDirectory.listFiles();

                for(File file : files){

                    if(file.getName().equals(selectedFileName) {
                        selectedFile =  file;
                    }

                }*/
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

                    case "Fog":
                        url = BrainNetHelper.getFogUrl();
                        break;
                    case "Cloud":
                        url = BrainNetHelper.getCloudUrl();
                        break;
                    default:
                        url = getAdaptiveUrl();
                        break;
                }

                new LoginAsyncTask().execute(userName.getText().toString(), url);
            }
        });
    }

    private void playVideo() {

        String uriPath = "android.resource://" + getPackageName() + "/" + R.raw.meditate;
        Uri uri = Uri.parse(uriPath);

        videoView.setVideoURI(uri);
        videoView.requestFocus();

        videoView.start();
    }

    private String getAdaptiveUrl() {

        //Some logic to decide whether to use fog or cloud server
        return null;
    }

    public class LoginAsyncTask extends AsyncTask<String, String, String> {

        int responseCode = 0;
        public ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        long startTimer, endTimer;

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
            String fileName = args[0];
            String classifier = BrainNetHelper.getClassifier();

            OkHttpClient client = new OkHttpClient();
            RequestBody fileData = RequestBody.create(MediaType.parse("text/csv"), selectedFile);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("classifier", classifier)
                    .addFormDataPart("type","text/csv")
                    .addFormDataPart("file", fileName, fileData)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            try {
                Response response = client.newCall(request).execute();

                System.out.println(request);
                System.out.println(response);

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
                    Toast.makeText(getApplicationContext(),"User Authenticated in "
                            + Long.toString(timer) + " ms", Toast.LENGTH_SHORT).show();
                    break;

                case 404:
                    Toast.makeText(getApplicationContext(),
                            "User UnAuthorized", Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Toast.makeText(getApplicationContext(),
                            "Server took too long to respond",Toast.LENGTH_SHORT).show();
            }
        }
    }
}
