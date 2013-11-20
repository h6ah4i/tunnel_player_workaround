
package com.h6ah4i.android.music_player;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.h6ah4i.android.example.tunnelplayerworkaround.BuildConfig;
import com.h6ah4i.android.example.tunnelplayerworkaround.MainActivity;
import com.h6ah4i.android.example.tunnelplayerworkaround.R;
import com.h6ah4i.android.utils.LocalServiceBinder;

public class MediaPlayerLocalService extends Service {
    // inner-classes
    public static class LocalBinder extends
            LocalServiceBinder<MediaPlayerLocalService> {
        public LocalBinder(MediaPlayerLocalService service) {
            super(service);
        }
    }

    // interfaces
    public static interface MediaPlayerLocalServiceEventListener {
        void onMediaCompletion(MediaPlayerLocalService mps);

        void onMediaPause(MediaPlayerLocalService mps);

        void onMediaPrepared(MediaPlayerLocalService mps);

        void onMediaResume(MediaPlayerLocalService mps);

        void onMediaStart(MediaPlayerLocalService mps);

        void onMediaStop(MediaPlayerLocalService mps);
    }

    private static final class MediaPlayerLocalServiceNullEventListener
            implements MediaPlayerLocalServiceEventListener {

        @Override
        public void onMediaCompletion(MediaPlayerLocalService mps) {
        }

        @Override
        public void onMediaPause(MediaPlayerLocalService mps) {
        }

        @Override
        public void onMediaPrepared(MediaPlayerLocalService mps) {
        }

        @Override
        public void onMediaResume(MediaPlayerLocalService mps) {
        }

        @Override
        public void onMediaStart(MediaPlayerLocalService mps) {
        }

        @Override
        public void onMediaStop(MediaPlayerLocalService mps) {
        }
    }

    // constants
    private static final String TAG = MediaPlayerLocalService.class.getSimpleName();

    private static final int ONGOING_NOTIFICATION = 1;
    private static final boolean mIsDebuggable = BuildConfig.DEBUG;

    // binder object
    private final IBinder mBinder = new LocalBinder(this);

    // fields
    private MediaPlayer mSilentPlayer;
    private MediaPlayer mPlayer;
    private boolean mIsPlayerPrepared;

    private MediaPlayerLocalServiceEventListener mListener;

    // listener objects
    private final OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            onMediaStop();
            onMediaCompletion();
        }
    };

    private final OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.setLooping(false);

            mIsPlayerPrepared = true;

            onMediaPrepared();

            try {
                mp.start();
                onMediaStart();
            } catch (IllegalStateException ex) {
                Log.e(TAG, "onMediaPrepared", ex);
            }
        }
    };

    public MediaPlayerLocalService() {
        mListener = new MediaPlayerLocalServiceNullEventListener();
    }

    @Override
    public void onCreate() {
        if (mIsDebuggable) {
            Log.d(TAG, "onCreate");
        }

        super.onCreate();

        // create a silent media player to avoid
        // tunnel player's side effect
        mSilentPlayer = createSilentMediaPlayer();

        // create an another media player for normal
        // audio playback
        mPlayer = createMediaPlayer();
    }

    @Override
    public void onDestroy() {
        if (mIsDebuggable) {
            Log.d(TAG, "onDestroy");
        }

        stopInternal();

        safeReleaseMediaPlayer(mPlayer);
        mPlayer = null;
        safeReleaseMediaPlayer(mSilentPlayer);
        mSilentPlayer = null;

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mIsDebuggable) {
            Log.d(TAG, "onStartCommand");
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (mIsDebuggable) {
            Log.d(TAG, "onBind");
        }
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mIsDebuggable) {
            Log.d(TAG, "onUnbind");
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        if (mIsDebuggable) {
            Log.d(TAG, "onRebind");
        }
        super.onRebind(intent);
    }

    public void setEventListener(MediaPlayerLocalServiceEventListener listener) {
        if (listener != null) {
            mListener = listener;
        } else {
            if (!(mListener instanceof MediaPlayerLocalServiceNullEventListener)) {
                mListener = new MediaPlayerLocalServiceNullEventListener();
            }
        }
    }

    public int getAudioSessionId() {
        return mPlayer.getAudioSessionId();
    }

    public boolean isPlaying() {
        final MediaPlayer player = mPlayer;

        if (player != null) {
            return player.isPlaying();
        }

        return false;
    }

    public boolean isPrepared() {
        return mIsPlayerPrepared;
    }

    public void pause() {
        final MediaPlayer player = mPlayer;

        if (player == null)
            return;

        if (mIsPlayerPrepared) {
            if (player.isPlaying()) {
                player.pause();
                onMediaPause();
            }
        }
    }

    public void play(Uri uri) {
        final MediaPlayer player = mPlayer;

        try {
            stopInternal();

            player.reset();
            player.setVolume(1.0f, 1.0f);

            player.setOnPreparedListener(mOnPreparedListener);
            player.setOnCompletionListener(mOnCompletionListener);

            player.setDataSource(getApplicationContext(), uri);
            player.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "playOne", e);
        } catch (SecurityException e) {
            Log.e(TAG, "playOne", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "playOne", e);
        } catch (IOException e) {
            Log.e(TAG, "playOne", e);
        }
    }

    public void resume() {

        final MediaPlayer player = mPlayer;

        if (player == null)
            return;

        try {
            player.setVolume(1.0f, 1.0f);
        } catch (Exception e) {
        }

        if (mIsPlayerPrepared) {
            if (!player.isPlaying()) {
                player.start();
                onMediaResume();
            }
        }
    }

    public void stop() {
        stopInternal();
    }

    private void onMediaCompletion() {
        mListener.onMediaCompletion(MediaPlayerLocalService.this);
    }

    private void onMediaPause() {
        stopForeground(true);
        mListener.onMediaPause(this);
    }

    private void onMediaPrepared() {
        stopForeground(true);
        mListener.onMediaPrepared(this);
    }

    private void onMediaResume() {
        startForeground(ONGOING_NOTIFICATION, createServiceNotification(false));
        mListener.onMediaResume(this);
    }

    private void onMediaStart() {
        startForeground(ONGOING_NOTIFICATION, createServiceNotification(true));
        mListener.onMediaStart(this);
    }

    private void onMediaStop() {
        stopForeground(true);
        mListener.onMediaStop(this);
    }

    private Notification createServiceNotification(boolean useTicker) {
        final String tickerStr = "ticker";
        final String titleStr = "title";
        final String textStr = "text";

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        final PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        final Notification notification = (new Notification.Builder(this))
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker(((useTicker) ? tickerStr : null)).setWhen(0)
                .setContentIntent(contentIntent).setContentTitle(titleStr)
                .setContentText(textStr).setOngoing(true).build();

        return notification;
    }

    private MediaPlayer createMediaPlayer() {
        final MediaPlayer player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        return player;
    }

    private MediaPlayer createSilentMediaPlayer() {
        final Context context = this;
        boolean result = false;

        MediaPlayer mp = null;
        AssetFileDescriptor afd = null;

        try {
            mp = new MediaPlayer();

            afd = context.getAssets().openFd("workaround_1min.mp3");

            mp.setDataSource(
                    afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength());
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.prepare();

            // NOTE: start() is no needed
            // mp.start();

            result = true;
        } catch (IOException e) {
            Log.e(TAG, "createSilentMediaPlayer()", e);
        } catch (RuntimeException e) {
            Log.e(TAG, "createSilentMediaPlayer()", e);
        } finally {
            if (!result && mp != null) {
                try {
                    mp.release();
                } catch (IllegalStateException e) {
                }
            }
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {
                }
            }
        }

        return mp;
    }

    private void stopInternal() {
        final MediaPlayer player = mPlayer;

        if (player == null)
            return;

        if (mIsPlayerPrepared) {
            if (player.isPlaying()) {
                player.stop();
                mIsPlayerPrepared = false;
                onMediaStop();
            }
        }
    }

    private static void safeReleaseMediaPlayer(MediaPlayer player) {
        try {
            if (player != null) {
                player.release();
            }
        } catch (IllegalStateException ex) {
        }
    }

}
