package com.binod.scoreboard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.binod.scoreboard.databinding.ActivityLogViewBinding;
import com.binod.scoreboard.databinding.ActivityStatusScreenBinding;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Timer;
import java.util.TimerTask;

public class StatusScreen extends AppCompatActivity {
    private ActivityStatusScreenBinding binding;
    TextView tvESPLogWindow;
    private String strLogMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStatusScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle("View Status");

        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    tvESPLogWindow.setText(strLogMessage);
                }
                catch (Exception ex) {
                    Utils.writeLog("Error: " + ex.getMessage());
                }
            }
        });

        tvESPLogWindow = findViewById(R.id.tvESPLogWindow);
        tvESPLogWindow.setText("Retrieving ESP log file. Click Refresh button to load details...");
        startFTP();
    }

    private void startFTP() {
        // initialize FTP client
        ProcessFTP ftp = new ProcessFTP(Utils.getFtpServer(), Utils.getFtpUser(), Utils.getFtpPasswd(), 21);
        ftp.setRemoteFolder("/htdocs/scoreboard/status/");
        ftp.setStatusScreen(this);
        ftp.start();
    }

    public void updateLogMessage(String logMessage) {
        strLogMessage = logMessage;
    }
}