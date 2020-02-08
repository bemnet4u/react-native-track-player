package com.guichaguri.trackplayer.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.session.MediaButtonReceiver;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.guichaguri.trackplayer.service.Utils;
import javax.annotation.Nullable;

/**
 * @author Guichaguri
 */
public class MusicService extends HeadlessJsTaskService {


    MusicManager manager;
    Handler handler;

    @Override
    public void onCreate(){
        super.onCreate();
        Utils.createNotificationChannel(this);
        // https://stackoverflow.com/questions/44425584/context-startforegroundservice-did-not-then-call-service-startforeground
        // Need to call startForeground to make sure we don't crash.
        startForeground(1, new NotificationCompat.Builder(this, Utils.NOTIFICATION_CHANNEL).build());
    }

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
                String channel = Utils.getNotificationChannel((Context) this);

                // Sets the service to foreground with an empty notification
                startForeground(1, new NotificationCompat.Builder(this, Utils.NOTIFICATION_CHANNEL).build());
                // Stops the service right after
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
        return START_NOT_STICKY;
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
