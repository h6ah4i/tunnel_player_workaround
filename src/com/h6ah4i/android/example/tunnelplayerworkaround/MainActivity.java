
package com.h6ah4i.android.example.tunnelplayerworkaround;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.h6ah4i.android.music_player.MediaPlayerLocalService;

public class MainActivity
        extends Activity
        implements MediaPlayerLocalService.MediaPlayerLocalServiceEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQ_CHOOSE_AUDIO = 1;

    private static final boolean mIsDebuggable = BuildConfig.DEBUG;

    private MediaPlayerLocalService mPlayerService;
    private boolean mIsBound = false;

    private Uri mPlayPendingData;

    private SimpleVisualizerView mVisualizerView;
    private Button mStartButton;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mPlayerService =
                    ((MediaPlayerLocalService.LocalBinder) service).getService();

            mPlayerService.setEventListener(MainActivity.this);

            onPlayerServiceAvailable(mPlayerService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mPlayerService = null;
            mIsBound = false;
        }
    };

    private void doBindService() {
        if (!mIsBound) {
            bindService(new Intent(MainActivity.this,
                    MediaPlayerLocalService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
            mIsBound = true;
        }
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mPlayerService = null;
            mIsBound = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mIsDebuggable) {
            Log.d(TAG, "onActivityResult");
        }
        switch (requestCode) {
            case REQ_CHOOSE_AUDIO:
                if (resultCode == RESULT_OK) {
                    final Uri uri = data.getData();
                    if (uri != null) {

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                playMedia(uri);
                            }
                        }, 100);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void playMedia(Uri uri) {
        if (mPlayerService != null) {
            mPlayerService.play(uri);
        } else {
            mPlayPendingData = uri;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (mIsDebuggable) {
            Log.d(TAG, "onCreate");
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // prepare surface view
        mVisualizerView = (SimpleVisualizerView) findViewById(R.id.surfaceView);

        // bind button action
        mStartButton = (Button) findViewById(R.id.playButton);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSongPicker();
            }
        });

        doBindService();
    }

    @Override
    protected void onDestroy() {
        if (mIsDebuggable) {
            Log.d(TAG, "onDestroy");
        }

        if (mPlayerService != null) {
            mPlayerService.stop();
        }

        doUnbindService();

        super.onDestroy();
    }

    @Override
    public void onMediaCompletion(MediaPlayerLocalService playerService) {
    }

    @Override
    public void onMediaPause(MediaPlayerLocalService playerService) {
        mVisualizerView.onPause();
    }

    @Override
    public void onMediaPrepared(MediaPlayerLocalService playerService) {
        final int audioSessionId = playerService.getAudioSessionId();

        if (mIsDebuggable) {
            Log.d(TAG, "onMediaPrepared(" + audioSessionId + ")");
        }

        if (mVisualizerView != null) {

            // NOTE: "session 0" is global mixer output
            mVisualizerView.bindAudioSession(0);
            // mVisualizerView.bindAudioSession(audioSessionId);
        }
    }

    @Override
    public void onMediaResume(MediaPlayerLocalService playerService) {
        mVisualizerView.onResume();
    }

    @Override
    public void onMediaStart(MediaPlayerLocalService playerService) {
        mVisualizerView.onResume();
    }

    @Override
    public void onMediaStop(MediaPlayerLocalService playerService) {
        mVisualizerView.onPause();
    }

    @Override
    protected void onPause() {
        if (mIsDebuggable) {
            Log.d(TAG, "onPause");
        }

        super.onPause();
    }

    private void onPlayerServiceAvailable(MediaPlayerLocalService playerService) {
        if (playerService.isPrepared()) {
            if (playerService.isPlaying()) {
                onMediaResume(playerService);
            } else {
                onMediaPause(playerService);
            }
        } else {
            final Uri uri = mPlayPendingData;
            mPlayPendingData = null;

            if (uri != null) {
                playerService.play(uri);
            }
        }
    }

    private void launchSongPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        Intent c = Intent.createChooser(intent, "Pick a music file");
        startActivityForResult(c, REQ_CHOOSE_AUDIO);
    }
}
