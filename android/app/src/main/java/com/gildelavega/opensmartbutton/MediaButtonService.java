/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gildelavega.opensmartbutton;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MediaButtonService extends Service {

    private static final String TAG = "MediaButtonService";
    private boolean ptt_down = false;
    private MediaSessionCompat ms;
    private long lastEvent;
    private Timer buttonUpTimer;
    private static final long MAX_PTT_DOWN_TIME_MS = 20000;

    public MediaButtonService() {
    }


    public synchronized void scheduleButtonUp(){
        if(buttonUpTimer != null){
            buttonUpTimer.cancel();
        }
        buttonUpTimer = new Timer();
        buttonUpTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent("com.zello.ptt.up");
                ptt_down = false;
                intent.putExtra("com.zello.stayHidden", true);
                MediaButtonService.this.sendBroadcast(intent);
            }
        }, MAX_PTT_DOWN_TIME_MS);

    }

    public synchronized void toggle(){
        Intent intent = new Intent(ptt_down ? "com.zello.ptt.up" : "com.zello.ptt.down");
        ptt_down = !ptt_down;
        if(ptt_down){
            scheduleButtonUp();
        }
        else{
            buttonUpTimer.cancel();
        }
        intent.putExtra("com.zello.stayHidden", true);
        MediaButtonService.this.sendBroadcast(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service onCreate");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String NOTIFICATION_CHANNEL_ID = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NOTIFICATION_CHANNEL_ID = "com.gildelavega.opensmartbutton";
            String channelName = "OpenSmartButton";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);
        }
        Notification notification =
                new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.antenna)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.notification_ticker))
                        .build();

        startForeground(1, notification);

        ms = new MediaSessionCompat(getApplicationContext(), TAG, new ComponentName(this, MediaButtonReceiver.class), null);
        ms.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        ms.setActive(true);

        ms.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                Log.i(TAG, "Event: " + mediaButtonIntent);
                long currentTime = System.currentTimeMillis();
                if(currentTime - lastEvent > 500) {
//                    toggle();
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        //deprecated in API 26
                        v.vibrate(500);
                    }
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });

//        // you can button by receiver after terminating your app
//        // ms.setMediaButtonReceiver(mbr);
//
//        // play dummy audio
//        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
//                AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
//        at.play();
//        ms.setPlaybackState(new PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PAUSED,0, 0).build());
//
//        // a little sleep
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        at.stop();
//        at.release();
        Log.i(TAG, "Finished playing");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting service. Intent: " + intent);

        MediaButtonReceiver.handleIntent(ms, intent);

//        // you can button by receiver after terminating your app
//        // ms.setMediaButtonReceiver(mbr);
//
//        // play dummy audio
//        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
//                AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
//        at.play();
//        ms.setPlaybackState(new PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PAUSED,0, 0).build());
//
//        // a little sleep
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        at.stop();
//        at.release();
        Log.i(TAG, "Finished playing");


        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
