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

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

public class TunnelManager {
    private final TunnelOptions options;
    private final Consumer<Integer> onReady;
    private final Consumer<String> onConnected;
    private final Consumer<String> onDisconnected;
    private final Consumer<String> onError;
    private TunnelServer server;
    private TunnelClient client;
    private final Object initLock = new Object();
    private boolean isInit = false;

    private boolean closed;

    public TunnelManager(TunnelOptions options) {
        this(options, null, null, null, null);
    }

    public TunnelManager(TunnelOptions options, Consumer<Integer> onReady,
                         Consumer<String> onConnected, Consumer<String> onDisconnected,
                         Consumer<String> onError) {
        this.options = options;
        this.onReady = onReady;
        this.onConnected = onConnected;
        this.onDisconnected = onDisconnected;
        this.onError = onError;
    }

    public void start() {
        server = new TunnelServer(options.getName() + ":server",
                options.getSourcePort(), options.isRemote(),
                options.getTransformer().getHeaderLength(),
                options.getBufferSize(),
                this::onServerReady, this::onServerConnected, this::onServerDisconnected,
                this::onServerReceive);
        server.start();
    }

    private synchronized void onServerReady(Integer integer) {
        System.out.println(options.getName() + ": ready to accept connections");
        if (onReady != null)
            onReady.accept(server.getPort());
    }

    private synchronized void onServerConnected(String s) {
        if (options.isVerbose())
            System.out.println(options.getName() + ": server connection from: " + s);
        try {
            options.getTransformer().initialize();
            client = new TunnelClient(options.getName() + ":client", options.getHost(),
                    options.getTargetPort(), options.isRemote(), options.getTransformer().getHeaderLength(),
                    options.getBufferSize(),
                    this::onClientConnected, this::onClientDisconnected,
                    this::onClientReceive);
            client.start();
            if (options.isRemote()) {
                sendConfiguration();
                isInit = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            closeClient();
        }
        if(onConnected != null)
            onConnected.accept(s);
    }

    private synchronized void onServerDisconnected(String s) {
        if (options.isVerbose())
            System.out.println(options.getName() + ": server disconnected from: " + s);
        closeClient();
        client = null;
        if(onDisconnected != null)
            onDisconnected.accept(s);
    }

    private synchronized void onClientConnected(String s) {
        if (options.isVerbose())
            System.out.println(options.getName() + ": client connected to: " + s);
    }

    private synchronized void onClientDisconnected(String s) {
        if (options.isVerbose())
            System.out.println(options.getName() + ": client disconnected from: " + s);
        server.closeClientSocket();
        client = null;
    }

    public synchronized void close() {
        closeClient();
        closeServer();
        closed = true;
    }

    private synchronized void closeClient() {
        if (client != null)
            client.close();
        client = null;
        isInit = false;
    }

    private synchronized void closeServer() {
        if (server != null)
            server.close();
        server = null;
    }


    private void onServerReceive(byte[] bytes, Integer count) {
        while (!isInit) {
            synchronized (initLock) {
                try {
                    if (options.isVerbose())
                        System.out.println(options.getName() + ": server received wait for init: " + count);
                    initLock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (client == null)
                return;
        }
        if (options.isDebug()) {
            if (options.isRemote()) {
                System.out.println(options.getName() + ": server tunnel received: " + count);
            } else {
                System.out.println(options.getName() + ": server received: " + count);
            }
        }
        if (count > 0) {
            byte[] data = Arrays.copyOf(bytes, count);
            data = options.getTransformer().transform(data, true);
            try {
                if (client == null)
                    return;
                if (options.isDebug()) {
                    if (options.isRemote()) {
                        System.out.println(options.getName() + ": client send: " + count);
                    } else {
                        System.out.println(options.getName() + ": client tunnel send: " + count);
                    }
                }
                client.send(data, data.length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void onServerSent(byte[] bytes, Integer count) {

    }

    private void onClientReceive(byte[] bytes, Integer count) {
        if (options.isDebug()) {
            if (options.isRemote()) {
                System.out.println(options.getName() + ": client received: " + count);
            } else {
                System.out.println(options.getName() + ": client tunnel received: " + count);
            }
        }
        if (count > 0) {
            byte[] data = Arrays.copyOf(bytes, count);
            if (!options.isRemote() && options.getTransformer().getConfiguration() == null) {
                // first packet is the configuration sent from the remote manager
                setConfiguration(data);
            } else {
                data = options.getTransformer().transform(data, false);
                try {
                    if (options.isDebug()) {
                        if (options.isRemote()) {
                            System.out.println(options.getName() + ": server tunnel send: " + count);
                        } else {
                            System.out.println(options.getName() + ": server send: " + count);
                        }
                    }
                    server.send(data, data.length);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Send the configuration via the tunnel from the remote manager to the local manager
     * @throws IOException
     */
    private synchronized void sendConfiguration() throws IOException {
        if (options.isVerbose())
            System.out.println(options.getName() + ": server sending configuration");
        byte[] header = options.getTransformer().getConfiguration();
        server.send(header, header.length);
    }

    /**
     * Set the configuration on the local manager
     * @param data
     */
    private void setConfiguration(byte[] data) {
        synchronized (initLock) {
            try {
                if (options.isVerbose())
                    System.out.println(options.getName() + ": setting configuration");
                options.getTransformer().setConfiguration(data);
            } catch (IOException e) {
                if(onError != null)
                    onError.accept(e.getMessage());
                e.printStackTrace();
                closeClient();
            } finally {
                isInit = true;
                initLock.notify();
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

}
