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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.mku.func.BiConsumer;
import com.mku.salmon.tunnel.crypt.SalmonTunnelTransformer;
import com.mku.salmon.tunnel.net.TunnelManager;
import com.mku.salmon.tunnel.net.TunnelOptions;
import com.mku.salmon.tunnel.transform.TunnelTransformer;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class TunnelService extends Service {

    final static String ACTION_START = "start";
    final static String ACTION_STOP = "stop";
    private static final int NOTIFICATION_ID = 11;
    private static final String TAG = "TunnelService";
    private static TunnelService instance;
    private static BiConsumer<StatusEvent,String> statusListener;

    private PowerManager.WakeLock mWakeLock;
    private TunnelManager manager;

    public TunnelService() {
    }

    public static void setStatusEventListener(BiConsumer<StatusEvent,String> statusListener) {
        TunnelService.statusListener = statusListener;
    }

    public void onManagerConnected(String s) {
        try {
            instance.mWakeLock.acquire(60*1000*1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onManagerDisconnected(String s) {
        try {
            instance.mWakeLock.release(60*1000*1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public void onManagerError(String msg) {
        if(statusListener!=null)
            statusListener.accept(StatusEvent.ERROR, msg);
    }
	
    public static ArrayList<String> getIPv4sAndPorts() throws SocketException {
        int port = Settings.getSourcePort();

        ArrayList<String> hostsAndPorts = new ArrayList<>();

        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        NetworkInterface ni;
        while (nis.hasMoreElements()) {
            ni = nis.nextElement();
            if (!ni.isLoopback() && ni.isUp()) {
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    if (ia.getAddress().getAddress().length == 4) {
                        hostsAndPorts.add(ia.getAddress().toString().replaceAll("/", "") + ":" + port);
                    }
                }
            }
        }
        return hostsAndPorts;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        instance = this;
        if(statusListener!=null)
            statusListener.accept(StatusEvent.STARTED, null);
        createNotification();
        createWakeLock();
        if (!start()) {
            stopSelf();
        }
    }

    private void createWakeLock() {
        PowerManager manager = (PowerManager) instance.getSystemService(Context.POWER_SERVICE);
        mWakeLock = manager.newWakeLock((
                PowerManager.SCREEN_DIM_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE),
                TAG+":tunnelservice");
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    getPackageName(),
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, getPackageName())
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Running Tunnel Service")
                    .setContentIntent(pendingIntent).build();

            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private boolean start() {
		try {
			TunnelOptions options = new TunnelOptions();
			options.setSourcePort(Settings.getSourcePort());
			options.setTargetPort(Settings.getTargetPort());
			options.setHost(Settings.getHost());
			if(Settings.getServiceDevice())
				options.setRemote(true);
			TunnelTransformer transformer = new SalmonTunnelTransformer(Settings.getPassword(), options.isRemote());
			options.setTransformer(transformer);
			options.setName((options.isRemote() ? "Remote" : "Local") + "Manager");
			options.setBufferSize(options.getBufferSize());
			options.setVerbose(true);
			if(manager != null && !manager.isClosed())
				return false;
			manager = new TunnelManager(options, this::onManagerReady,
					this::onManagerConnected, this::onManagerDisconnected,
					this::onManagerError);
			manager.start();
			return true;
		} catch(Exception ex) {
			if(statusListener!=null)
				statusListener.accept(StatusEvent.ERROR, ex.getMessage());
			ex.printStackTrace();
		}
		return false;
    }

    private void onManagerReady(Integer integer) {
        Log.d(TAG, "Manager is ready");
    }

    @Override
    public void onDestroy() {
		if (manager!=null)
			manager.close();
		if(statusListener!=null)
			statusListener.accept(StatusEvent.STOPPED, null);
		instance = null;
		manager = null;
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    public enum StatusEvent {
        STARTED,
        STOPPED,
		ERROR
    }
}
