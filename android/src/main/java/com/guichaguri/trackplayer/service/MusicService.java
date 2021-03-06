package com.guichaguri.trackplayer.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaButtonReceiver;

import android.support.v4.media.session.MediaSessionCompat;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import javax.annotation.Nullable;

/**
 * @author Guichaguri
 */
public class MusicService extends HeadlessJsTaskService {

    MusicManager manager;
    Handler handler;

    @Nullable
    @Override
    protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        return new HeadlessJsTaskConfig("TrackPlayer", Arguments.createMap(), 0, true);
    }

    @Override
    public void onHeadlessJsTaskFinish(int taskId) {
        // Overridden to prevent the service from being terminated
    }

    public void emit(String event, Bundle data) {
        Intent intent = new Intent(Utils.EVENT_INTENT);

        intent.putExtra("event", event);
        if(data != null) intent.putExtra("data", data);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void destroy() {
        if(handler != null) {
            handler.removeMessages(0);
            handler = null;
        }

        if(manager != null) {
            manager.destroy();
            manager = null;
        }
    }

    private void onStartForeground() {
        boolean serviceForeground = false;

        if(manager != null) {
            // The session is only active when the service is on foreground
            serviceForeground = manager.getMetadata().getSession().isActive();
        }

        System.out.print("Service is foreground: " + serviceForeground);
        if(!serviceForeground) {
            ReactInstanceManager reactInstanceManager = getReactNativeHost().getReactInstanceManager();
            ReactContext reactContext = reactInstanceManager.getCurrentReactContext();

            // Checks whether there is a React activity
            if(reactContext == null || !reactContext.hasCurrentActivity()) {
                Notification notification;
                System.out.println("Creating notification in service for sdk : " + Build.VERSION.SDK_INT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    String channel = "amharic-radio-temp-notification-id";
                    System.out.println("Creating a new channel in service id: " + channel);
                    NotificationChannel notificationChannel = new NotificationChannel(channel, "Playback", NotificationManager.IMPORTANCE_DEFAULT);
                    NotificationManager not = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    not.createNotificationChannel(notificationChannel);
                    notification = new NotificationCompat.Builder(this, channel).build();
                    System.out.println("Created notification channel in service...");
                } else{
                    System.out.println("Creating notification in service for sdk : " + Build.VERSION.SDK_INT);
                    notification = new NotificationCompat.Builder(this).build();
                    System.out.println("Created notification for older sdk");
                }

                System.out.println("Sets the service to foreground with an empty notification: " + notification);
                startForeground(1, notification);
                System.out.print("Stops the service right after");
                stopSelf();
                System.out.println("Notification stopped.");
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(Utils.CONNECT_INTENT.equals(intent.getAction())) {
            return new MusicBinder(this, manager);
        }

        return super.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("onStartCommand called.");
        if(intent != null && Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            // Check if the app is on background, then starts a foreground service and then ends it right after
            System.out.println("Calling onStartForeground");
            onStartForeground();
            
            if(manager != null) {
                MediaButtonReceiver.handleIntent(manager.getMetadata().getSession(), intent);
            }
            
            return START_NOT_STICKY;
        }

        manager = new MusicManager(this);
        handler = new Handler();

        System.out.println("calling super.onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        System.out.println("onDestroy called.");
        super.onDestroy();

        destroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        System.out.println("onTaskRemoved called.");
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }
}
