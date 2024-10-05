package com.binod.scoreboard;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class Utils {
    public static double APP_VER = 1.7;
    private static String appDir = Environment.DIRECTORY_DOWNLOADS;
    private static String firmwareFile = "/firmware.bin";
    private static String logFile = "/SB_Log.txt";
    private static String configFile = "/SB_Config.txt";
    private static String ftpServer, ftpUser, ftpPasswd;
    private static int httpServerPort = 8088;
    private static String verStringESP = "";
    public static String readLog() {
        return fileRead(logFile);
    }
    public static String readLog(String logFile) {
        return fileRead(logFile);
    }
    public static void writeLog(String message) {
        fileWrite(logFile, getTimeStamp() + " " + message, false, true);
    }
    public static String getTimeStamp() {
        SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        return logDateFormat.format(Calendar.getInstance().getTime());
    }
    public static String getCurrentTime() {
        Date date = new Date();
        return String.valueOf(date.getTime()/1000-4*60*60);
    }
    public static String getLocalFirmwareFileName() {
        String fullPath = Environment.getExternalStoragePublicDirectory(appDir).getPath() + firmwareFile;
        return fullPath;
    }
    public static long getLocalFirmwareFileSize() {
        return fileSize(firmwareFile);
    }
    public static String getVerStringESP() {
        return verStringESP;
    }
    public static void setVerStringESP(String verStringESP) {
        Utils.verStringESP = verStringESP;
    }
    public static void setFtpInfo(String ftpServer, String ftpUser, String ftpPasswd) {
        Utils.ftpServer = ftpServer;
        Utils.ftpUser = ftpUser;
        Utils.ftpPasswd = ftpPasswd;
    }
    public static String getAppDir() {
        return appDir;
    }
    public static String getFtpServer() {
        return ftpServer;
    }
    public static String getFtpUser() {
        return ftpUser;
    }
    public static String getFtpPasswd() {
        return ftpPasswd;
    }
    public static int getHttpServerPort() {
        return httpServerPort;
    }
    public static void writeError (String message) {
        writeLog("ERROR: " + message);
    }
    public static void clearLog() {
        fileWrite(logFile,"", true, false);
    }
    public static void saveConfig(String configInfo) {
        //Utils.writeLog("Saving config info: " + configInfo);
        Utils.writeLog("Saving config info");
        fileWrite(configFile, configInfo, false, false);
    }
    public static String loadConfig() {
        Utils.writeLog("Loading config info");
        String configInfo = fileRead(configFile);
        //Utils.writeLog("Loaded config: " + configInfo);
        getESPVersion();
        return configInfo;
    }
    public static void initFirmwareUpdate(Context context) {
        // get the firmware update command
        String btMessage = getFirmwareCommand(context);
        ProcessBT btThread = new ProcessBT(btMessage);
        btThread.firmwareCheck = false;
        btThread.start();
    }
    private static void getESPVersion() {
        if (verStringESP == "") {
            // get the version details from ESP
            String btMessage = getVersionQuery();
            ProcessBT btThread = new ProcessBT(btMessage);
            btThread.firmwareCheck = true;
            btThread.start();
        }
    }
    public static void clearConfig() {
        fileWrite(configFile,"", true, false);
    }
    public static void showToast(Context context, String m) {
        Toast.makeText(context, m, Toast.LENGTH_LONG).show();
    }
    public static Hashtable parseDataString(String inStr) {
        Hashtable outHash = new Hashtable();
        try {
            StringTokenizer tokens = new StringTokenizer(inStr, "~");

            while (tokens.hasMoreTokens()) {
                String[] keyVal = tokens.nextToken().split("=");
                if (keyVal.length == 2) {
                    outHash.put(keyVal[0], keyVal[1]);
                }
                else {
                    outHash.put(keyVal[0], "null");
                }
            }
        }
        catch (Exception ex) {
            Utils.writeError("String parse error: " + ex.getMessage());
            return null;
        }
        return outHash;
    }
    public static void showMenu(Context context, MenuItem item) {
        switch (item.getItemId()){
            case R.id.idLogs:
                //Show app log from phone
                Intent logIntent = new Intent(context, LogView.class);
                context.startActivity(logIntent);
                break;
            case R.id.idStatus:
                Intent statIntent = new Intent(context, StatusScreen.class);
                context.startActivity(statIntent);
                break;
            case R.id.idFirmware:
                Intent firmIntent = new Intent(context, FirmwareUpdate.class);
                context.startActivity(firmIntent);
                break;
        }
    }
    private static long fileSize(String fileName) {
        Utils.writeLog("Getting file size for: " + fileName);
        long fSize = -1;
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(appDir).getPath() + fileName);
            fSize = file.length();
        }
        catch (Exception ex) {
            Utils.writeError("File size error: " + ex.getMessage());
        }
        return fSize;
    }
    private static String fileRead(String fileName) {
        Utils.writeLog("Reading file contents from: " + fileName);

        String fileDir = Environment.getExternalStoragePublicDirectory(appDir).getPath();
        StringBuilder sb = new StringBuilder();

        try {
            FileReader reader = new FileReader(fileDir + fileName);
            BufferedReader br = new BufferedReader(reader);
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            br.close();
            reader.close();
        }
        catch (Exception ex) {
            Utils.writeError("File read error: " + ex.getMessage());
        }

        Utils.writeLog("Retrieved file contents");
        return sb.toString();
    }
    private static void fileWrite(String fileName, String message, boolean isClear, boolean isAppend) {
        String fileDir = Environment.getExternalStoragePublicDirectory(appDir).getPath();
        FileWriter writer;
        try {
            if (isClear) {
                Log.d("MyApp", "LOG: Clearing file: " + fileName);
                writer = new FileWriter(fileDir + fileName);
                Log.d("MyApp", "LOG: File cleared");
            }
            else {
                String outS = message + "\n";
                writer = new FileWriter(fileDir + fileName, isAppend);
                writer.write(outS);
                Log.d("MyApp", "LOG: " + outS);
            }
            writer.close();
        }
        catch (Exception e) {
            String m = "File ERROR: " + e.getMessage();
            Log.d("MyApp", "LOG: " + m);
        }
    }
    private static String getFirmwareCommand(Context context) {
        String ipAddress = "192.0.0.4"; // default IP
        Utils.writeLog("Server IP Address: " + ipAddress);
        String firmURL = "http://" + ipAddress + ":" + httpServerPort + "/firmwareDownload";
        long firmSize = Utils.getLocalFirmwareFileSize();
        String btMessage = "";
        btMessage += "TYPE=FIRM~";
        btMessage += "FIRMURL=" + firmURL + "~";
        btMessage += "FIRMSIZE=" + firmSize + "~";
        btMessage += "TIME=" + Utils.getCurrentTime();

        return btMessage;
    }
    private static String getVersionQuery() {
        String btMessage = "";
        btMessage += "TYPE=VER~";
        btMessage += "TIME=" + Utils.getCurrentTime();

        return btMessage;
    }
}
