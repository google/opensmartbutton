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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.Timer;
import java.util.TimerTask;

public class MediaButtonService extends Service {

    private static final String TAG = "MediaButtonService";
    private MediaSessionCompat ms;
    private long lastEvent;
    private Timer buttonUpTimer;
    private static final long MAX_PTT_DOWN_TIME_MS = 8000;

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
                intent.putExtra("com.zello.stayHidden", true);
                MediaButtonService.this.sendBroadcast(intent);
                Log.i(TAG, "Auto button up sent");
            }
        }, MAX_PTT_DOWN_TIME_MS);

    }

    public synchronized void buttonDown(){
        Intent intent = new Intent("com.zello.ptt.down");
        intent.putExtra("com.zello.stayHidden", true);
        Log.i(TAG, "Sending intent: Button down");
        MediaButtonService.this.sendBroadcast(intent);
        scheduleButtonUp();
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

//        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null, this, MediaButtonReceiver.class);
//        ms.setMediaButtonReceiver(PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0));

        ms.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                Log.i(TAG, "Event: " + mediaButtonIntent);
                long currentTime = System.currentTimeMillis();
                if(currentTime - lastEvent > 500) {
                    lastEvent = currentTime;
                    buttonDown();
                }
                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                    //    recoverAudioFocus();
                    }
                }, 1000, 1000);
        recoverAudioFocus();
        Log.i(TAG, "Finished playing");
    }

    private void playAudio(){
        Log.i(TAG, "Playing audio");
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
        at.play();
        ms.setPlaybackState(new PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING,0, 0).build());

        // a little sleep
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        at.stop();
        at.release();
    }

    private void recoverAudioFocus(){
        AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                Log.i(TAG, "On focus change: " + focusChange);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                if(focusChange != AudioManager.AUDIOFOCUS_GAIN) {
                    playAudio();
//                }
                }
            }
        };
// Request audio focus for playback
        Integer result = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            Log.i(TAG, "Playing audio");
            result = audioManager.requestAudioFocus(new AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN).setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(afChangeListener).build());
            playAudio();
        }
        else {
            result = audioManager.requestAudioFocus(afChangeListener,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN);
        }

        Log.i(TAG, "Audio focus requested: " + result);

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

    private void stopListening(){
        ms.setCallback(null);
        ms.setActive(false);

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
