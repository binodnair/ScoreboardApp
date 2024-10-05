package com.binod.scoreboard;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.sql.Time;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class StartScreen extends AppCompatActivity {
    private final int REQUEST_PERMISSION_PHONE_STATE=1;
    public static String leagueName, wiFiSSID, wiFiPasswd, ftpServer, ftpUser, ftpPasswd;
    public static int clubId, matchId, updateMode = 1;
    public static boolean ignoreConfig = false;
    AutoCompleteTextView atvLeague;
    EditText etClubId, etMatchId, etWiFiSSID, etWiFiPasswd, etFTPServer, etFTPUser, etFTPPasswd;
    TextView tvVersionLabel;
    private static String blankLeague = "--BLANK--";
    private static final String[] leagueNames = new String[] {
            blankLeague,"FWCWL", "FT20", "TampaCricket", "TCL"
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeFields();
        setUpdateMode(updateMode);
        if (ignoreConfig) showScoreScreen();

        MaterialButtonToggleGroup updateModeButtonGroup =  findViewById(R.id.updateModeButtonGroup);
        updateModeButtonGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener(){
            @Override
            public void onButtonChecked (MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    if (checkedId == R.id.btnWiFi) {
                        updateMode = 0;
                    }
                    else if (checkedId == R.id.btnAppAuto) {
                        updateMode = 1;
                    }
                    else if (checkedId == R.id.btnAppManual) {
                        updateMode = 2;
                    }
                }
                setUpdateMode(updateMode);
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scoreboard_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Utils.showMenu(this, item);
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.writeLog("ON DESTROY in Start Screen");
    }
    private void setUpdateMode (int mode) {
        MaterialButtonToggleGroup updateModeButtonGroup = findViewById(R.id.updateModeButtonGroup);
        TextInputLayout textInputLayoutWiFiSSID = findViewById(R.id.textInputLayoutWiFiSSID);
        TextInputLayout textInputLayoutWiFiPasswd = findViewById(R.id.textInputLayoutWiFiPasswd);

        if (mode == 0) {
            // WiFi
            updateModeButtonGroup.check(R.id.btnWiFi);
            textInputLayoutWiFiSSID.setVisibility(View.VISIBLE);
            textInputLayoutWiFiPasswd.setVisibility(View.VISIBLE);
        }
        else if (mode == 1) {
            updateModeButtonGroup.check(R.id.btnAppAuto);
            textInputLayoutWiFiSSID.setVisibility(View.INVISIBLE);
            textInputLayoutWiFiPasswd.setVisibility(View.INVISIBLE);
        }
        else if (mode == 2) {
            updateModeButtonGroup.check(R.id.btnAppManual);
            textInputLayoutWiFiSSID.setVisibility(View.INVISIBLE);
            textInputLayoutWiFiPasswd.setVisibility(View.INVISIBLE);
        }
    }
    private void initializeFields() {
        tvVersionLabel = findViewById(R.id.tvVersionLabel);
        atvLeague = findViewById(R.id.atvLeague);
        etClubId = findViewById(R.id.etClubId);
        etMatchId = findViewById(R.id.etMatchId);
        etWiFiSSID = findViewById(R.id.etWiFiSSID);
        etWiFiPasswd = findViewById(R.id.etWiFiPasswd);
        //etFTPServer = findViewById(R.id.etFTPServer);
        //etFTPUser = findViewById(R.id.etFTPUser);
        //etFTPPasswd = findViewById(R.id.etFTPPasswd);
        // set application version
        String appVer = "ICAT Scoreboard ver: " + Utils.APP_VER + "  ";
        tvVersionLabel.setText(appVer);
        // fill the dropdown
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, leagueNames);
        atvLeague.setAdapter(ad);
        if (!ignoreConfig) loadConfig();
        atvLeague.setText(leagueName);
        if (clubId > 0) etClubId.setText(""+clubId);
        if (matchId > 0) etMatchId.setText(""+matchId);
        if (wiFiSSID != "") etWiFiSSID.setText(wiFiSSID);
        if (wiFiPasswd != "") etWiFiPasswd.setText(wiFiPasswd);
        //if (ftpServer != "") etFTPServer.setText(ftpServer);
        //if (ftpUser != "") etFTPUser.setText(ftpUser);
        //if (ftpPasswd != "") etFTPPasswd.setText(ftpPasswd);
    }
    //load config details from file
    private void loadConfig() {
        String configInfo = Utils.loadConfig();
        try {
            Hashtable configHash = Utils.parseDataString(configInfo);
            if (configHash == null) {
                //clear config file
                //Utils.writeLog("Clearing config due to Hash Empty");
                //Utils.clearConfig();
                return;
            }

            if (!configHash.get("LEGE").equals(null)) leagueName = configHash.get("LEGE").toString();
            if (!configHash.get("SSID").equals(null)) wiFiSSID   = configHash.get("SSID").toString();
            if (!configHash.get("PWD").equals(null)) wiFiPasswd = configHash.get("PWD").toString();
            if (!configHash.get("FTPSRV").equals(null)) ftpServer  = configHash.get("FTPSRV").toString();
            if (!configHash.get("FTPUSR").equals(null)) ftpUser    = configHash.get("FTPUSR").toString();
            if (!configHash.get("FTPPWD").equals(null)) ftpPasswd  = configHash.get("FTPPWD").toString();

            if (leagueName.equals("null")) leagueName = "";
            if (wiFiSSID.equals("null")) wiFiSSID = "";
            if (wiFiPasswd.equals("null")) wiFiPasswd = "";
            if (ftpServer.equals("null")) ftpServer = "";
            if (ftpUser.equals("null")) ftpUser = "";
            if (ftpPasswd.equals("null")) ftpPasswd = "";

            if (!configHash.get("CLID").equals(null)) clubId  = Integer.parseInt(configHash.get("CLID").toString());
            if (!configHash.get("MTID").equals(null)) matchId = Integer.parseInt(configHash.get("MTID").toString());
            Utils.setFtpInfo(ftpServer, ftpUser, ftpPasswd);
        }
        catch (Exception ex) {
            //clear config file
            //Utils.writeLog("Clearing config due to Hash Exception");
            //Utils.clearConfig();
        }
    }
    //save config details to file
    private void saveConfig() {
        String configInfo = prepareWiFiMessage();
        Utils.saveConfig(configInfo);
        Utils.setFtpInfo(ftpServer, ftpUser, ftpPasswd);
    }
    public void onBtnListMatchesClick(View myView) {
        showListMatches();
    }
    public void onBtnLoadMatchClick(View myView) { showScoreScreen(); }
    public void onBtnExitClick(View myView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(myView.getContext());
        builder.setCancelable(true);
        builder.setTitle("Exit");
        builder.setMessage("This will stop all updates and Exit the app. Are you sure?");
        builder.setPositiveButton("Yes",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(getApplicationContext(), "Shutting down the application. Please wait...");
                        stopScoreService();
                        finish();
                        System.exit(0);
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
    private boolean checkFields() {
        if (updateMode == 2) {
            clubId = 0;
            matchId = 0;
            leagueName = "FWCWL";
            return true;
        }
        if (atvLeague.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Enter League Name from Cricclubs.com", Toast.LENGTH_LONG).show();
            return false;
        }
        if (etClubId.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Enter Club ID from Cricclubs.com", Toast.LENGTH_LONG).show();
            return false;
        }
        if (etMatchId.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(), "Enter Match ID from Cricclubs.com", Toast.LENGTH_LONG).show();
            return false;
        }
        if (updateMode == 0) {
            if (etWiFiSSID.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), "Enter SSID of the WiFi for auto update", Toast.LENGTH_LONG).show();
                return false;
            }
            if (etWiFiPasswd.getText().toString().isEmpty()) {
                Toast.makeText(getApplicationContext(), "Enter Password of the WiFi for auto update", Toast.LENGTH_LONG).show();
                return false;
            }
            wiFiSSID = etWiFiSSID.getText().toString();
            wiFiPasswd = etWiFiPasswd.getText().toString();
            //ftpServer = etFTPServer.getText().toString();
            //ftpUser = etFTPUser.getText().toString();
            //ftpPasswd = etFTPPasswd.getText().toString();
        }

        leagueName = atvLeague.getText().toString();
        clubId = Integer.parseInt(etClubId.getText().toString());
        matchId = Integer.parseInt(etMatchId.getText().toString());
        if (leagueName.equals(blankLeague)) leagueName = "";

        return true;
    }
    private void showListMatches() {
        ListMatches.updateMode = StartScreen.updateMode;
        Intent intent = new Intent(this, ListMatches.class);
        startActivity(intent);
    }
    private void showScoreScreen() {
        if (!checkFields()) return;
        saveConfig();
        if (updateMode == 0) {
            // wifi mode - stop the score service to avoid future updates from the app
            Utils.showToast(getApplicationContext(), "Waiting for any previous Score services to stop");
            this.stopScoreService();
            // prepare wifi command and send to scoreboard over bluetooth
            String btMessage = prepareWiFiMessage();
            ProcessBT btThread = new ProcessBT(btMessage);
            btThread.setContext(getApplicationContext());
            btThread.start();
            Utils.showToast(getApplicationContext(), "Scoreboard update initiated");
        }
        else {
            // auto or manual app - start service and show score screen
            startScoreService();
            Intent intent = new Intent(this, ScoreScreen.class);
            startActivity(intent);
        }
    }
    private String prepareWiFiMessage() {
        String btMessage = "";
        btMessage += "TYPE=WIFI~";
        btMessage += "LEGE=" + leagueName + "~";
        btMessage += "CLID=" + clubId + "~";
        btMessage += "MTID=" + matchId + "~";
        btMessage += "SSID=" + wiFiSSID + "~";
        btMessage += "PWD=" + wiFiPasswd + "~";
        btMessage += "FTPSRV=" + ftpServer + "~";
        btMessage += "FTPUSR=" + ftpUser + "~";
        btMessage += "FTPPWD=" + ftpPasswd + "~";
        btMessage += "TIME=" + Utils.getCurrentTime();

        return btMessage;
    }
    private void startScoreService() {
        ScoreService.leagueName = leagueName;
        ScoreService.clubId = clubId;
        ScoreService.matchId = matchId;
        ScoreService.updateMode = updateMode;
        ScoreService.FORCE_EXIT = false;
        Intent intent = new Intent(this, ScoreService.class);
        startService(intent);
    }
    private void stopScoreService() {
        Utils.writeLog("Stopping Score Service");
        ScoreService.FORCE_EXIT = true;
        stopService(new Intent(this, ScoreService.class));
        Utils.writeLog("Stopped Score Service");

        try {
            Thread.sleep(5 * 1000);
        }
        catch (Exception ex) {
            Utils.writeError("Exception while waiting for score service to stop: " + ex.getMessage());
        }
    }
}