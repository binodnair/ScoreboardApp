package com.binod.scoreboard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.binod.scoreboard.databinding.ActivityLogViewBinding;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class LogView extends AppCompatActivity {
    private ActivityLogViewBinding binding;
    TextView tvAppLogWindow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLogViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle("View Log");

        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setCancelable(true);
                builder.setTitle("Clear Log");
                builder.setMessage("This will clear the contents of the log file. Are you sure?");
                builder.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                clearLog();
                                displayLog();
                            }
                        });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        tvAppLogWindow = findViewById(R.id.tvAppLogWindow);
        tvAppLogWindow.setText("Log is empty");
        displayLog();
    }
    public void clearLog() {
        Utils.clearLog();
        Toast.makeText(getApplicationContext(), "Log cleared!", Toast.LENGTH_LONG).show();
    }
    private void displayLog() {
        tvAppLogWindow.setText(Utils.readLog());
    }
}