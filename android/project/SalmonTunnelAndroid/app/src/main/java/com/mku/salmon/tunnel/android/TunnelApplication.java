package com.mku.salmon.tunnel.android;

import android.app.Application;

public class TunnelApplication extends Application {

    public static TunnelApplication getInstance() {
        return instance;
    }

    private static TunnelApplication instance;

    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
