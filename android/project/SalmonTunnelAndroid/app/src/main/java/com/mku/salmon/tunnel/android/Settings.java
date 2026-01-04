package com.mku.salmon.tunnel.android;
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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {
    private static final int DEFAULT_BUFFER_SIZE = 32;
    private static SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TunnelApplication.getInstance());

    public static int getSourcePort() {
        return prefs.getInt(TunnelConfig.PREFS_KEY_SETTINGS_SOURCE_PORT, 0);
    }

    public static void setSourcePort(int port) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(TunnelConfig.PREFS_KEY_SETTINGS_SOURCE_PORT, port);
        edit.apply();
    }

    public static boolean getServiceDevice() {
        return prefs.getBoolean(TunnelConfig.PREFS_KEY_SETTINGS_SERVICE_DEVICE, true);
    }


    public static void setServiceDevice(boolean serviceDevice) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(TunnelConfig.PREFS_KEY_SETTINGS_SERVICE_DEVICE, serviceDevice);
        edit.apply();
    }

    public static int getTargetPort() {
        return prefs.getInt(TunnelConfig.PREFS_KEY_SETTINGS_TARGET_PORT, 0);
    }

    public static void setTargetPort(int port) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(TunnelConfig.PREFS_KEY_SETTINGS_TARGET_PORT, port);
        edit.apply();
    }

    public static String getHost() {
        return prefs.getString(TunnelConfig.PREFS_KEY_SETTINGS_HOST, null);
    }

    public static void setHost(String host) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(TunnelConfig.PREFS_KEY_SETTINGS_HOST,host);
        edit.apply();
    }

    public static String getPassword() {
        return prefs.getString(TunnelConfig.PREFS_KEY_SETTINGS_PASSWORD, null);
    }


    public static void setPassword(String password) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(TunnelConfig.PREFS_KEY_SETTINGS_PASSWORD,password);
        edit.apply();
    }

    public static int getBufferSize() {
        return prefs.getInt(TunnelConfig.PREFS_KEY_SETTINGS_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
    }

    public static void setBufferSize(int size) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(TunnelConfig.PREFS_KEY_SETTINGS_BUFFER_SIZE, size);
        edit.apply();
    }
}
