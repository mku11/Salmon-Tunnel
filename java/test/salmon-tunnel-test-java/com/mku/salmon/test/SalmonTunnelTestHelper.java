package com.mku.salmon.test;
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

import com.mku.salmon.tunnel.net.TunnelOptions;
import com.mku.salmon.tunnel.transform.TunnelTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SalmonTunnelTestHelper {

    public static final String TEST_PASSWORD = "password";

    static void initialize() {

    }

    static void close() {

    }

    public static TunnelOptions createRemoteManagerOptions() {
        TunnelOptions options = new TunnelOptions();
        options.setRemote(true);
        options.setName("remoteManager");
        options.setSourcePort(8002);
        options.setHost("localhost");
        options.setTargetPort(8003);
        return options;
    }

    public static TunnelOptions createLocalManagerOptions() {
        TunnelOptions options = new TunnelOptions();
        options.setName("localManager");
        options.setSourcePort(8001);
        options.setHost("localhost");
        options.setTargetPort(8002);
        return options;
    }

    public static byte[][] getBytes(String[] msgs) {
        byte[][] data = new byte[msgs.length][];
        for (int i = 0; i < msgs.length; i++) {
            data[i] = msgs[i].getBytes();
        }
        return data;
    }

    public static String[] getStrings(byte[][] data) {
        String[] msgs = new String[data.length];
        for (int i = 0; i < data.length; i++) {
            msgs[i] = new String(data[i]);
        }
        return msgs;
    }

    public static class ReverseTransformer extends TunnelTransformer {
        private final boolean isRemote;
        byte[] header = null;
        int count = 0;

        public ReverseTransformer(boolean isRemote) {
            super(isRemote);
            this.isRemote = isRemote;
            initialize();
        }

        private synchronized byte[] createHeader() {
            return "REV".getBytes();
        }

        @Override
        public synchronized void setConfiguration(byte[] header) {
            this.header = header;
        }

        @Override
        public synchronized byte[] getConfiguration() {
            return header;
        }

        public synchronized byte[] transform(byte[] data, boolean fromServer) {
            if ((!isRemote && fromServer) || (isRemote && !fromServer))
                return transformOut(data);
            else
                return transformIn(data);
        }

        private byte[] transformOut(byte[] data) {
//            if (!isRemote)
//                System.out.println("len:" + data.length + ", " + (isRemote ? "remote" : "local") + ", out: " + count);
            for (int i = 0; i < data.length / 2; i++) {
                byte temp =data[i];
                data[i] = data[data.length-1-i];
                data[data.length-1-i] = temp;
            }
            return data;
        }

        private byte[] transformIn(byte[] data) {
//            if (!isRemote)
//                System.out.println("len:" + data.length + ", " + (isRemote ? "remote" : "local") + ", in: " + data[0]);
            for (int i = 0; i < data.length / 2; i++) {
                byte temp = data[i];
                data[i] = data[data.length - 1 - i];
                data[data.length - 1 - i] = temp;
            }
            return data;
        }

        @Override
        public synchronized void initialize() {
            count = 0;
            if (isRemote)
                header = createHeader();
            else
                header = null;
        }

        @Override
        public int getHeaderLength() {
            return 0;
        }
    }

    public static byte[][] sendAndReceiveMsgs(String host, int port, byte[][] msgs,
                                              String remoteHost, int remotePort) throws IOException, InterruptedException {
        Socket localSocket = new Socket(host, port);
        localSocket.setSoTimeout(30000);
        OutputStream localSocketOutputStream = localSocket.getOutputStream();
        InputStream localSocketInputStream = localSocket.getInputStream();

        ServerSocket remoteserverSocket = new ServerSocket(remotePort);
        Socket remoteclientSocket = remoteserverSocket.accept();
        remoteclientSocket.setSoTimeout(30000);
        InputStream remoteSocketInputStream = remoteclientSocket.getInputStream();
        OutputStream remoteclientSocketOutputStream = remoteclientSocket.getOutputStream();

        byte[][] receivedMessages = new byte[msgs.length][];
        for (int i = 0; i < msgs.length; i++) {
            // write to local
            byte[] data = msgs[i];
            localSocketOutputStream.write(data, 0, data.length);
            byte[] buff = new byte[32768];
            Thread.sleep(1000);
            // read from remote
            int bytesRead = remoteSocketInputStream.read(buff, 0, buff.length);
            receivedMessages[i] = Arrays.copyOf(buff, bytesRead);

            // send it back
            remoteclientSocketOutputStream.write(buff, 0, bytesRead);
            byte[] recvBuff = new byte[32768];
            int recvBytesRead = localSocketInputStream.read(recvBuff, 0, recvBuff.length);
            byte[] recvMsg = Arrays.copyOf(recvBuff, recvBytesRead);
            assertArrayEquals(msgs[i], recvMsg);
        }

        localSocketOutputStream.close();
        localSocketInputStream.close();
        localSocket.close();

        remoteSocketInputStream.close();
        remoteclientSocketOutputStream.close();
        remoteclientSocket.close();
        remoteserverSocket.close();

        return receivedMessages;
    }
}