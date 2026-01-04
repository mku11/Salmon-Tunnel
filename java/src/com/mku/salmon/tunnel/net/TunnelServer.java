package com.mku.salmon.tunnel.net;
/*
MIT License

Copyright (c) 2025 Max Kas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import com.mku.convert.BitConverter;
import com.mku.func.BiConsumer;
import com.mku.func.Consumer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TunnelServer {
    private final int port;
    private final Consumer<String> onConnected;
    private final Consumer<String> onDisconnected;
    private final BiConsumer<byte[], Integer> onRead;
    private final Consumer<Integer> onReady;
    private final String name;
    private final boolean isRemote;
    private final int headerLength;
    private OutputStream outputStream;
    private InputStream inputStream;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private SocketAddress remoteSocketAddress;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private boolean isClosed;
    private final Object lockSend = new Object();

    byte[] buff;

    public TunnelServer(String name, int port, boolean isRemote,
                        int headerLength,
                        int bufferSize,
                        Consumer<Integer> onReady,
                        Consumer<String> onConnected,
                        Consumer<String> onDisconnected,
                        BiConsumer<byte[], Integer> onRead) {
        this.name = name;
        this.port = port;
        this.isRemote = isRemote;
        this.headerLength = headerLength;
        this.onReady = onReady;
        this.onConnected = onConnected;
        this.onDisconnected = onDisconnected;
        this.onRead = onRead;

        buff = new byte[bufferSize];
    }

    public void start() {
        executor.execute(() -> {
            try {
                if (!isRemote)
                    serverSocket = new ServerSocket(this.port, 0, InetAddress.getByName("localhost"));
                else
                    serverSocket = new ServerSocket(this.port);
                startListen();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                close();
            }
        });
    }

    public void startListen() {
        while (!isClosed) {
            try {
                if (clientSocket == null) {
                    onReady.accept(this.port);
                    clientSocket = serverSocket.accept();
                    clientSocket.setKeepAlive(true);
                    clientSocket.setTcpNoDelay(true);
                    remoteSocketAddress = clientSocket.getRemoteSocketAddress();
                    outputStream = clientSocket.getOutputStream();
                    inputStream = clientSocket.getInputStream();
                    onConnected.accept(remoteSocketAddress.toString());
                }
                startRead();
            } catch (Exception e) {
                if (!(e instanceof SocketException)) {
                    e.printStackTrace();
                }
                closeClientSocket();
                onDisconnected.accept(remoteSocketAddress.toString());
            }
        }
    }

    public void closeClientSocket() {
        if (clientSocket == null)
            return;

        try {
            if (inputStream != null)
                inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        inputStream = null;

        try {
            if (outputStream != null)
                outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream = null;

        try {
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientSocket = null;
    }

    private void closeServer() {
        if (isClosed)
            return;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        serverSocket = null;
        isClosed = true;
    }

    public void close() {
        closeClientSocket();
        closeServer();
    }

    private synchronized void startRead() throws IOException {
        if (isClosed)
            return;
        int count;
        int bytesRead;
        boolean readAllBytes;
        if (isRemote) {
            bytesRead = inputStream.read(buff, 0, 2);
            if (bytesRead < 0)
                throw new SocketException("connection closed: " + remoteSocketAddress.toString());
            count = (int) BitConverter.toLong(buff, 0, 2);
            readAllBytes = true;
        } else {
            count = buff.length - headerLength;
            readAllBytes = false;
        }

        int totalBytesRead = 0;
        while (totalBytesRead < count) {
            bytesRead = inputStream.read(buff, totalBytesRead, count - totalBytesRead);
            if (bytesRead < 0)
                throw new SocketException("connection closed: " + remoteSocketAddress.toString());
            totalBytesRead += bytesRead;
            if (!readAllBytes)
                break;
        }
        onRead.accept(buff, totalBytesRead);
    }

    public void send(byte[] buffer, int length) throws IOException {
        synchronized (lockSend) {
            if (isRemote) {
                byte[] len = BitConverter.toBytes(buffer.length, 2);
                outputStream.write(len, 0, len.length);
            }
            outputStream.write(buffer, 0, length);
        }
    }

    public int getPort() {
        return port;
    }
}
