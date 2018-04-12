package com.smartadserver.android.instreamsdk.plugin;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.exoplayer2.ExoPlayer;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;

/**
 * Implementation of the {@link SVSContentPlayerPlugin} interface for Exo player.
 * Source : https://github.com/smartadserver/smart-instream-android-plugins/
 */
public class SVSExoPlayerPlugin implements SVSContentPlayerPlugin {

    private Handler mainHandler;
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
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                exoPlayerView.setVisibility(View.VISIBLE); //make sure that exoPlayer is visible
                exoPlayer.setPlayWhenReady(true);
            }
        });
    }

    /**
     * Performs any action necessary when the ad playback is about to start, including
     * pausing the content playback
     */
    @Override
    public void adBreakStarted() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                exoPlayer.setPlayWhenReady(false);
                exoPlayerView.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * Returns whether the content media is currently being played
     */
    @Override
    public boolean isPlaying() {
        return exoPlayer.getPlayWhenReady();
    }

    /**
     * Returns the content media duration
     */
    @Override
    public long getContentDuration() {
        return isLiveContent ? -1 : exoPlayer.getDuration();
    }

    /**
     * Returns the current position in the content media
     */
    @Override
    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    /**
     * Returns the {@link ViewGroup} component that contains the content player
     */
    @Override
    public ViewGroup getContentPlayerContainer() {
        return contentPlayerContainer;
    }
}
