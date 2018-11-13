package com.brain.net.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.brain.net.R;
import com.brain.net.helper.BrainNetHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

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

        /*File[] files = root.listFiles();
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

                /*File[] files = root.listFiles();

                for(File file : files){

                    if(file.getName().equals(adapterView.getItemAtPosition(i).toString())) {
                        selectedFile =  file;
                    }

                }*/
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
}
