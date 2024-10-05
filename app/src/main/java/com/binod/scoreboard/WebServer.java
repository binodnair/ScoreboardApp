package com.binod.scoreboard;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

public class WebServer {
    private int port;
    private SSLServerSocket serverSocket;
    private boolean isRunning;

    public WebServer(int port) {
        this.port = port;
        this.isRunning = false;
    }

    public void start() {
        new Thread(() -> {
            try {
                // Load the keystore containing the server's SSL certificate
                char[] keystorePassword = "password".toCharArray();
                KeyStore keystore = KeyStore.getInstance("JKS");
                keystore.load(new FileInputStream("path/to/keystore"), keystorePassword);

                // Create a KeyManagerFactory to create a KeyManager for the SSLContext
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
                keyManagerFactory.init(keystore, keystorePassword);

                // Create a TrustManagerFactory to create a TrustManager for the SSLContext
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
                trustManagerFactory.init(keystore);

                // Create an SSLContext with the KeyManager and TrustManager
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

                // Create an SSLServerSocketFactory from the SSLContext
                SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();

                // Create an SSLServerSocket using the SSLServerSocketFactory
                serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);

                // Configure the SSLServerSocket to require client authentication
                serverSocket.setNeedClientAuth(true);

                // Start accepting connections
                isRunning = true;
                while (isRunning) {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();
                    HttpsRequestHandler requestHandler = new HttpsRequestHandler(socket);
                    new Thread(requestHandler).start();
                }
            } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        isRunning = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class HttpsRequestHandler implements Runnable {
        private SSLSocket socket;

        public HttpsRequestHandler(SSLSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

                // Read the HTTPS request
                StringBuilder requestBuilder = new StringBuilder();
                String line;
                while ((line = input.readLine()) != null && line.length() > 0) {
                    requestBuilder.append(line).append("\r\n");
                }
                String request = requestBuilder.toString();

                // Write the HTTPS response
                String response = "HTTP/1.1 200 OK\r\n\r\nHello, World!";
                output.print(response);
                output.flush();

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
