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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TunnelClient {
    private final int port;
    private final boolean isRemote;
    private final String server;
    private final Consumer<String> onConnected;
    private final Consumer<String> onDisconnected;
    private final BiConsumer<byte[], Integer> onRead;

    private final String name;
    private final int headerLength;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private InputStream ois;
    private Socket socket;
    private OutputStream oos;
    private SocketAddress remoteSocketAddress;
    private boolean isClosed;
    byte[] buff;

    public TunnelClient(String name, String host, int port,
                        boolean isRemote,
                        int headerLength,
                        int bufferSize,
                        Consumer<String> onConnected,
                        Consumer<String> onDisconnected,
                        BiConsumer<byte[], Integer> onRead) {
        this.headerLength = headerLength;
        this.name = name;
        this.server = host;
        this.port = port;
        this.isRemote = isRemote;
        this.onConnected = onConnected;
        this.onDisconnected = onDisconnected;
        this.onRead = onRead;
        buff = new byte[bufferSize];
    }

    public void start() throws IOException {
        socket = new Socket(server, port);
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        oos = socket.getOutputStream();
        ois = socket.getInputStream();
        remoteSocketAddress = socket.getRemoteSocketAddress();
        onConnected.accept(remoteSocketAddress.toString());

        executor.execute(() -> {
            try {
                startRead();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startRead() {
        while (!isClosed) {
            try {
                read();
            } catch (Exception ex) {
                if (!(ex instanceof SocketException)) {
                    ex.printStackTrace();
                }
                close();
                onDisconnected.accept(remoteSocketAddress.toString());
            }
        }
    }

    private synchronized void read() throws IOException {
        if (isClosed)
            return;
        int count;
        int bytesRead;
        boolean readAllBytes;
        if (!isRemote) {
            bytesRead = ois.read(buff, 0, 2);
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
            bytesRead = ois.read(buff, totalBytesRead, count - totalBytesRead);
            if (bytesRead < 0)
                throw new SocketException("connection closed: " + remoteSocketAddress.toString());
            totalBytesRead += bytesRead;
            if (!readAllBytes)
                break;
        }
        onRead.accept(buff, totalBytesRead);
    }

    private final Object lockSend = new Object();

    public void send(byte[] data, int count) throws IOException {
        synchronized (lockSend) {
            if (!isRemote) {
                byte[] len = BitConverter.toBytes(data.length, 2);
                oos.write(len, 0, len.length);
            }
            oos.write(data, 0, count);
        }
    }

    public void close() {
        if (isClosed)
            return;

        try {
            if (ois != null)
                ois.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ois = null;

        try {
            if (oos != null)
                oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        oos = null;

        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        isClosed = true;
    }
}
