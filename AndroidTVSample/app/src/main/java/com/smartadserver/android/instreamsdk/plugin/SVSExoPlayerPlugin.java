package com.smartadserver.android.instreamsdk.plugin;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.exoplayer2.ExoPlayer;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;

import java.util.concurrent.Callable;

/**
 * Implementation of the {@link SVSContentPlayerPlugin} interface for Exo player.
 * Source : https://github.com/smartadserver/smart-instream-android-plugins/
 */
public class SVSExoPlayerPlugin implements SVSContentPlayerPlugin {

    private Handler mainHandler;
    private Handler exoPlayerHandler;
    private ViewGroup contentPlayerContainer;
    private ExoPlayer exoPlayer;
    private View exoPlayerView;
    private boolean isLiveContent;

    /**
     * Constructor
     * @param exoPlayer the ExoPlayer object
     * @param exoPlayerView the View where the ExoPlayer renders the video
     * @param contentPlayerContainer the ViewGroup containing the VideoViewExoPlayer view
     */
    public SVSExoPlayerPlugin(ExoPlayer exoPlayer, View exoPlayerView, ViewGroup contentPlayerContainer, boolean isLiveContent) {
        this.mainHandler = new Handler(Looper.getMainLooper());
        // create another handler for exoplayer related code only if exoplayer was not created in the main thread
        this.exoPlayerHandler = exoPlayer.getApplicationLooper() == Looper.getMainLooper() ?
                mainHandler : new Handler(exoPlayer.getApplicationLooper());
        this.exoPlayer = exoPlayer;
        this.exoPlayerView = exoPlayerView;
        this.contentPlayerContainer = contentPlayerContainer;
        this.isLiveContent = isLiveContent;
    }

    /**
     * Performs any action necessary when the ad playback has finished, including
     * resuming the content playback
     */
    @Override
    public void adBreakEnded() {
        //make sure that exoPlayer is visible
        mainHandler.post(() -> exoPlayerView.setVisibility(View.VISIBLE));
        exoPlayerHandler.post(() -> exoPlayer.setPlayWhenReady(true));
    }

    /**
     * Performs any action necessary when the ad playback is about to start, including
     * pausing the content playback
     */
    @Override
    public void adBreakStarted() {
        exoPlayerHandler.post(() -> exoPlayer.setPlayWhenReady(false));
        //make sure that exoPlayer is hidden
        mainHandler.post(() -> exoPlayerView.setVisibility(View.INVISIBLE));
    }

    /**
     * Returns whether the content media is currently being played
     */
    @Override
    public boolean isPlaying() {
        return callOnExoplayerThreadAndWaitForResult(() -> exoPlayer.getPlayWhenReady());
    }

    /**
     * Returns the content media duration
     */
    @Override
    public long getContentDuration() {
        return isLiveContent ? -1 : callOnExoplayerThreadAndWaitForResult(() -> exoPlayer.getDuration());
    }

    /**
     * Returns the current position in the content media
     */
    @Override
    public long getCurrentPosition() {
        return callOnExoplayerThreadAndWaitForResult(() -> exoPlayer.getCurrentPosition());
    }

    /**
     * Returns the {@link ViewGroup} component that contains the content player
     */
    @Override
    public ViewGroup getContentPlayerContainer() {
        return contentPlayerContainer;
    }

    /**
     * Utility method to synchronously execute a {@link Callable task} on this plugin's ExoPlayer thread
     * and get the result
     * @param callable a {@link Callable} that returns a result
     * @param <V> the type of the callable result
     * @return the result
     */
    private <V> V callOnExoplayerThreadAndWaitForResult(Callable<V> callable) {

        Object[] holder = new Object[1];
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    holder[0] = callable.call();
                } catch (Exception e) {
                }
                synchronized (this) {
                    this.notify();
                }
            }
        };

        // check if current looper is exoplayer looper, to run directly
        if (Looper.myLooper() == exoPlayerHandler.getLooper()) {
            r.run();
        } else {
            synchronized (r) {
                exoPlayerHandler.post(r);
                try {
                    r.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return (V)holder[0];

    }
}
