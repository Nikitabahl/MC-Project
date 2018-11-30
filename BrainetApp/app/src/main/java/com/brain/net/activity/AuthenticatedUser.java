package com.brain.net.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.brain.net.R;
import com.brain.net.helper.BrainNetHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AuthenticatedUser extends Activity {

    @BindView(R.id.tv_server)
    TextView serverType;

    @BindView(R.id.tv_initial_battery)
    TextView initialBattery;

    @BindView(R.id.tv_final_battery)
    TextView finalBattery;

    @BindView(R.id.tv_time_taken)
    TextView timeTaken;

    @BindView(R.id.bt_logout)
    Button logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //bind xml to activity
        ButterKnife.bind(this);

        String server = getIntent().getStringExtra(MainActivity.SERVER);
        int initialBatteryLevel = getIntent().getIntExtra(MainActivity.BATTERY_LEVEL_1, 0);
        int finalBatteryLevel = getIntent().getIntExtra(MainActivity.BATTERY_LEVEL_2, 0);
        Long latency = getIntent().getLongExtra(MainActivity.LATENCY, 0L);

        serverType.setText(BrainNetHelper.getServerText(server));
        initialBattery.setText(BrainNetHelper.getInitialBattery(initialBatteryLevel));
        finalBattery.setText(BrainNetHelper.getFinalBattery(finalBatteryLevel));
        timeTaken.setText(BrainNetHelper.getLatency(latency));

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AuthenticatedUser.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
