package com.binod.scoreboard;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.binod.scoreboard.databinding.ActivityFirmwareBinding;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.io.File;
import java.io.FileInputStream;
import java.util.Hashtable;

import fi.iki.elonen.NanoHTTPD;

public class FirmwareUpdate extends AppCompatActivity {
    TextView tvFirmwareWindow;
    Button btnFirmwareUpdate;
    private ProcessHTTP httpd = null;
    private String strMessage = "";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firmware);
        tvFirmwareWindow = findViewById(R.id.tvFirmwareWindow);
        btnFirmwareUpdate = findViewById(R.id.btnFirmwareUpdate);
        tvFirmwareWindow.setText("");
        btnFirmwareUpdate.setEnabled(false);
        showCurrentVer();
    }
    private void showCurrentVer() {
        //display current version of firmware
        String firmMessage = Utils.getVerStringESP();
        if (firmMessage == "") {
            tvFirmwareWindow.setText("Not able to retrieve current ESP32 version details.\n\nTry restarting Scoreboard and/ or the app");
            // disable update button
            btnFirmwareUpdate.setEnabled(false);
            return;
        }
        Utils.writeLog(firmMessage);
        tvFirmwareWindow.setText(firmMessage);

        //parse the response from ESP32
        Hashtable respHash = Utils.parseDataString(firmMessage);

        if (respHash == null) {
            //invalid response received
            String strMsg = "Invalid response: " + firmMessage;
            Utils.showToast(getApplicationContext(), strMsg);
            return;
        }

        String stat = respHash.get("STAT").toString();
        String name = respHash.get("NAME").toString();
        String ver  = respHash.get("VER").toString();
        String firmwareFileName = Utils.getLocalFirmwareFileName();
        long firmwareFileSize = Utils.getLocalFirmwareFileSize();

        strMessage += "Scoreboard details:";
        strMessage += "\n\nName: " + name;
        strMessage += "\nCurrent Firmware version: " + ver;
        strMessage += "\n\nNew Firmware details:\n";
        if (firmwareFileSize > 0) {
            strMessage += "\nFirmware file: " + firmwareFileName;
            strMessage += "\nFirmware size: " + firmwareFileSize;
            strMessage += "\n\nClick Update button to start firmware update!";
            // enable update button
            btnFirmwareUpdate.setEnabled(true);
        }
        else {
            strMessage += "\n\nFirmware file not found. Please place firmware\nfile in following location and try again: ";
            strMessage += "\n\n" + firmwareFileName;
            // disable update button
            btnFirmwareUpdate.setEnabled(false);
        }
        tvFirmwareWindow.setText(strMessage);
    }
    public void onBtnUpdateClick(View view) {
        //get final confirmation
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setCancelable(true);
        builder.setTitle("Firmware Update");
        builder.setMessage("This will update firmware on Scoreboard. Are you sure?");
        builder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        strMessage += "\n\n\nInitializing firmware update";
                        tvFirmwareWindow.setText(strMessage);
                        //only one click allowed. disable the update button
                        btnFirmwareUpdate.setEnabled(false);
                        //start web server to serve the firmware file
                        strMessage += "\nStarting Web Server to serve firmware file";
                        tvFirmwareWindow.setText(strMessage);
                        startWebServer();
                        strMessage += "\nWeb Server started. Notifying Scoreboard to download file";
                        tvFirmwareWindow.setText(strMessage);
                        //instruct ESP to start firmware update
                        Utils.initFirmwareUpdate(getApplicationContext());
                        strMessage += "\nNotified Scoreboard. Waiting for response";
                        tvFirmwareWindow.setText(strMessage);
                        strMessage += "\n\n\nDo NOT click back button or move away from this screen. Please wait...";
                        tvFirmwareWindow.setText(strMessage);
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

    private void startWebServer() {
        try {
            int serverPort = Utils.getHttpServerPort();
            Utils.writeLog("Starting HTTP server on port: " + serverPort);
            httpd = new ProcessHTTP(serverPort);
            httpd.start();
        }
        catch (Exception ex) {
            Utils.writeLog("HTTP Server error: " + ex.getMessage());
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (httpd != null) {
            Utils.writeLog("Shutting down HTTP Server");
            try {
                httpd.stop();
                Utils.writeLog("HTTP Server stopped");
            }
            catch (Exception ex) {
                Utils.writeLog("HTTP Server error: " + ex.getMessage());
            }
        }
    }
}
