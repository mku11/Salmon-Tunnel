package com.mku.salmon.tunnel.main;
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

import com.mku.salmon.tunnel.net.TunnelManager;
import com.mku.salmon.tunnel.net.TunnelOptions;
import com.mku.salmon.tunnel.crypt.SalmonTunnelTransformer;

public class Main {
    private static TunnelManager manager;

    public static void main(String[] args) {
        try {
            TunnelOptions options = new TunnelOptions();
            getOptions(args, options);
            if (!checkOptions(options)) {
                printUsage();
                return;
            }
            System.out.println("Control-C to exit");
            System.out.println("Enter password");
            String password = new String(System.console().readPassword());
            options.setTransformer(new SalmonTunnelTransformer(password, options.isRemote()));
            start(options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            shutdown();
        }
    }

    private static boolean checkOptions(TunnelOptions options) {
        if (options.getSourcePort() == 0 || options.getTargetPort() == 0)
            return false;
        return true;
    }

    private static void shutdown() {

    }

    public static void start(TunnelOptions options) {
        manager = new TunnelManager(options);
        manager.start();
    }

    private static void getOptions(String[] args, TunnelOptions options) {
        for (String arg : args) {
            String[] parts;
            if (arg.startsWith("-"))
                parts = arg.split("=", 2);
            else
                parts = new String[]{arg};
            switch (parts[0]) {
                case "-v":
                    options.setVerbose(true);
                    break;
                case "-d":
                    options.setDebug(true);
                    break;
                case "-sp":
                case "-source-port":
                    options.setSourcePort(Integer.parseInt(parts[1]));
                    break;
                case "-tp":
                case "-target-port":
                    options.setTargetPort(Integer.parseInt(parts[1]));
                    break;
                case "-host":
                    options.setHost(parts[1]);
                    break;
                case "-bs":
                case "-buffer-size":
                    options.setBufferSize(Integer.parseInt(parts[1]));
                    break;
                case "-n":
                case "-name":
                    options.setName(parts[1]);
                    break;
                case "-help":
                    printUsage();
                    System.exit(0);
                    break;
            }
        }
    }

    private static void printUsage() {
        System.out.println("Salmon Tunnel v" + SalmonTunnelConfig.getVersion());
        System.out.println("License: " + SalmonTunnelConfig.getLicense());
        System.out.println("Options:");
        System.out.println("\t-help: this help");
        System.out.println("\t-sp|-source-port=<local port> The source port");
        System.out.println("\t-tp|-target-port=<remote port> The target port");
        System.out.println("\t-host=<host> The remote host that runs the service (set this only if this is the client machine)");
        System.out.println("\t-bs|-buffer-size=<buffer size> The buffer size to use");
        System.out.println("\t-n|-name=<process name>");
        System.out.println("\t-v: verbose");
        System.out.println("\t-d: debug");
    }

}
