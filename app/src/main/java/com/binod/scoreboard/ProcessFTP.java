package com.binod.scoreboard;

import android.os.Environment;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

public class ProcessFTP extends Thread {
    private String ftpServer;
    private String ftpUser;
    private String ftpPasswd;
    private int ftpPort;
    private String logFile = "/EspLog.txt";
    private String remoteFile = "";
    private String remoteFolder = "";
    private StatusScreen statusScreen;

    ProcessFTP(String ftpServer, String ftpUser, String ftpPasswd, int ftpPort) {
        this.ftpServer = ftpServer;
        this.ftpPasswd = ftpPasswd;
        this.ftpPort = ftpPort;
        this.ftpUser = ftpUser;
        Utils.writeLog("FTP client initialized");
    }
    public void run() {
        // download log file from FTP server to local storage
        String localFile = Environment.getExternalStoragePublicDirectory(Utils.getAppDir()).getPath() + logFile;
        if (this.statusScreen != null) {
            if (downloadFile(remoteFile, localFile)) {
                Utils.writeLog("Displaying log contents");
                statusScreen.updateLogMessage(Utils.readLog(logFile));
            }
        }
    }

    public void setStatusScreen(StatusScreen statusScreen) {
        this.statusScreen = statusScreen;
    }

    public void setRemoteFile(String remoteFile) {
        this.remoteFile = remoteFile;
    }

    public void setRemoteFolder(String remoteFolder) {
        this.remoteFolder = remoteFolder;
    }
    private boolean downloadFile(String remoteFile, String localFile) {
        Utils.writeLog("Downloading file over FTP");
        if (remoteFolder == "" && remoteFile == "") {
            Utils.writeLog("Both remote file and remote folder cannot be blank at the same time");
            return false;
        }

        try {
            FTPClient mFTPClient = new FTPClient();
            mFTPClient.connect(ftpServer, ftpPort);
            Utils.writeLog("Connected. Reply: " + mFTPClient.getReplyString());
            // now check the reply code, if positive mean connection success
            if (!mFTPClient.login(ftpUser, ftpPasswd)) {
                Utils.writeLog("Failed to connect to FTP Server: " + mFTPClient.getReplyString());
                return false;
            }
            Utils.writeLog("Connected to remote server");
            //if remote file is not set, get the latest file
            if (remoteFile == "") {
                FTPFile[] files = mFTPClient.listFiles(remoteFolder);
                if (files.length == 0) {
                    Utils.writeLog("No files found in remote folder: " + remoteFolder);
                    return false;
                }
                // Sort files by timestamp in descending order
                Arrays.sort(files, new Comparator<FTPFile>() {
                    @Override
                    public int compare(FTPFile file1, FTPFile file2) {
                        return Long.compare(file2.getTimestamp().getTimeInMillis(),
                                file1.getTimestamp().getTimeInMillis());
                    }
                });

                remoteFile = remoteFolder + files[2].getName();
            }
            Utils.writeLog("Downloading Remote file: " + remoteFile + " to: " + localFile);
            mFTPClient.setFileType(FTP.ASCII_FILE_TYPE);
            mFTPClient.enterLocalPassiveMode();
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFile));
            mFTPClient.retrieveFile(remoteFile, outputStream);
            if (outputStream != null) outputStream.close();
            Utils.writeLog("Download Completed");
            if (mFTPClient != null) {
                mFTPClient.logout();
                mFTPClient.disconnect();
            }
        } catch (Exception ex) {
            Utils.writeLog("FTP Error: " + ex.getMessage());
            return false;
        }
        return true;
    }
}
