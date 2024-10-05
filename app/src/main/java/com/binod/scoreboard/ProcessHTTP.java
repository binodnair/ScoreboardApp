package com.binod.scoreboard;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import fi.iki.elonen.NanoHTTPD;

public class ProcessHTTP extends NanoHTTPD {
    public ProcessHTTP() {
        this(8080); // Set the server port
    }
    public ProcessHTTP(int port) {
        super(port); // Set the server port
    }
    @Override
    public Response serve(IHTTPSession session) {
        Utils.writeLog("Request received from client");

        // Serve a firmware file
        String firmFile = Utils.getLocalFirmwareFileName();
        File file = new File(firmFile);

        try {
            Utils.writeLog("Sending the firmware file from: " + firmFile);
            InputStream inputStream = new FileInputStream(file);
            return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", inputStream, file.length());
        }
        catch (Exception ex) {
            Utils.writeError("File Send Error: " + ex.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error retrieving file");
        }
    }
}
