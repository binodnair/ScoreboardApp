package com.binod.scoreboard;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Looper;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class ProcessBT extends Thread {
    private String btMessage;
    private Context context;
    private int updateThreadCount = 0; // Bluetooth thread tracker
    public static boolean firmwareCheck = false;
    ProcessBT (String btMessage) {
        this.btMessage = btMessage;
    }
    public void setContext(Context context) {
        this.context = context;
    }
    private void showToast (String m) {
        if (context == null) return;
        Utils.showToast(this.context, m);
    }
    public void run() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice mDevice = null;
        Looper.prepare();
        Utils.writeLog("BT message ready for send: " + btMessage);

        if (mBluetoothAdapter  == null) {
            String m = "Bluetooth is not supported by this device.";
            Utils.writeError(m);
            showToast(m);
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            String m = "Bluetooth is OFF. Turn ON Bluetooth and try again!";
            Utils.writeError(m);
            showToast(m);
            return;
        }
        // Get a list of all paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // Loop through paired devices to find the desired device
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("ESP32")) {
                    mDevice = device;
                    break;
                }
            }
        }
        // If the target device was not found, message and EXIT
        if (mDevice == null) {
            String m = "Scoreboard update FAILED. Bluetooth pair with ESP32 and try again.";
            Utils.writeError(m);
            showToast(m);
            return;
        }

        // try maximum of 5 times
        int maxTry = 5;
        this.updateThreadCount = ++Utils.updateThreadCount; // starting a new BT update process; increment the master tracker and save a local copy for reference

        for (int i=0; i<maxTry; i++) {
            if (Utils.updateThreadCount > this.updateThreadCount) break; // a newer thread started sending BT message. Quit this thread
            Utils.writeLog("Trying BT Send - Thread #: " + this.updateThreadCount + ", Retry Count #: " + (i+1));
            if (doBTTask(mDevice)) break;
            if (i == maxTry-1) Utils.writeLog("Failed to connect to ESP32. Thread #: " + this.updateThreadCount + " giving up...");
        }
    }

    private boolean doBTTask(BluetoothDevice mDevice) {
        BluetoothSocket mSocket = null;
        boolean result = true;

        try {
            Utils.writeLog("Starting BT send");
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            if (mSocket != null) mSocket.close();
            mSocket = null;
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mSocket.isConnected()) {
                mSocket.close();
                mSocket = null;
                mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
                if (!mSocket.isConnected()) mSocket.connect();
            }
            if (mSocket.isConnected()) {
                OutputStream outputStream = mSocket.getOutputStream();
                InputStream inputStream = mSocket.getInputStream();
                String m = "";

                //send a line of text to ESP32
                Utils.writeLog("Sending BT Message: " + btMessage);
                byte[] bytes = btMessage.getBytes();
                outputStream.write(bytes);
                m = "Sent data to Scoreboard successfully";
                Utils.writeLog(m);
                showToast(m);
                // if the request is for firmware check, retrieve the response
                if (firmwareCheck) {
                    byte[] buffer = new byte[512];
                    int numBytes;
                    Utils.writeLog("Waiting for response from Scoreboard");
                    try {
                        String respMessage = "";

                        while (true) {
                            // Read from the InputStream.
                            numBytes = inputStream.read(buffer);
                            String str = new String(buffer, 0, numBytes);

                            if (str.endsWith("$$$$$")) {
                                //this is the last chunk; remove the line terminator and append
                                if (str.length() > 5) {
                                    String strLastChunk = str.substring(0, str.length()-5);
                                    respMessage += strLastChunk;
                                }
                                break;
                            }
                            respMessage += str;
                        }
                        Utils.writeLog("Received response from ESP32");
                        Utils.setVerStringESP(respMessage);
                    }
                    catch (Exception ex) {
                        Utils.writeError("BT Receive: " + ex.getMessage());
                        result = false;
                    }
                }
                Thread.sleep(3000);
                mSocket.close();
                mSocket = null;
            }
            else {
                String m = "Scoreboard socket connection failed. Will try again later";
                Utils.writeError(m);
                result = false;
            }
        }
        catch (Exception e) {
            String m = "BT Send: " + e.getMessage();
            Utils.writeError(m);
            result = false;
        }

        return result;
    }
}
