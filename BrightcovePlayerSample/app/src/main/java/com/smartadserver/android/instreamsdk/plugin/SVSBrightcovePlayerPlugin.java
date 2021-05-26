package com.smartadserver.android.instreamsdk.plugin;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.brightcove.player.event.Event;
import com.brightcove.player.event.EventListener;
import com.brightcove.player.event.EventType;
import com.brightcove.player.view.BrightcoveExoPlayerVideoView;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;

/**
 * Implementation of the {@link SVSContentPlayerPlugin} interface for Brightcove player.
 * source : https://github.com/smartadserver/smart-instream-android-plugins/
 */
public class SVSBrightcovePlayerPlugin implements SVSContentPlayerPlugin {

    @NonNull
    private Handler mainHandler;
    @NonNull
    private ViewGroup contentPlayerContainer;
    @NonNull
    private BrightcoveExoPlayerVideoView brightcoveExoPlayerVideoView;
    private boolean isLiveContent;

    // flag to mark when content video has completed to return video duration as current position
    private boolean contentHasCompleted = false;

    /**
     * Constructor
     * @param brightcoveExoPlayerVideoView the BrightcoveExoPlayerVideoView object
     * @param contentPlayerContainer the ViewGroup containing the BrightcoveExoPlayerVideoView object
     */
    public SVSBrightcovePlayerPlugin(@NonNull BrightcoveExoPlayerVideoView brightcoveExoPlayerVideoView, @NonNull ViewGroup contentPlayerContainer, boolean isLiveContent) {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.brightcoveExoPlayerVideoView = brightcoveExoPlayerVideoView;
        this.contentPlayerContainer = contentPlayerContainer;
        this.isLiveContent = isLiveContent;

        // add listener for COMPLETED event to return proper duration when content playback is completed
        EventListener eventListener = new EventListener() {
            @Override
            public void processEvent(Event event) {
                switch (event.getType()) {
                    case EventType.COMPLETED:
                        contentHasCompleted = true;
                        break;
                    case EventType.REWIND:
                    case EventType.SEEK_TO:
                        contentHasCompleted = false;
                        break;
                }

            }
        };
        brightcoveExoPlayerVideoView.getEventEmitter().on(EventType.COMPLETED, eventListener);
        brightcoveExoPlayerVideoView.getEventEmitter().on(EventType.REWIND, eventListener);
        brightcoveExoPlayerVideoView.getEventEmitter().on(EventType.SEEK_TO, eventListener);
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
                brightcoveExoPlayerVideoView.setVisibility(View.VISIBLE);
                if (!contentHasCompleted && !isPlaying()) {
                    brightcoveExoPlayerVideoView.start();
                }
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
                brightcoveExoPlayerVideoView.setVisibility(View.INVISIBLE);
                if (isPlaying()) {
                    brightcoveExoPlayerVideoView.pause();
                }
            }
        });
    }

    /**
     * Returns whether the content media is currently being played
     */
    @Override
    public boolean isPlaying() {
        return brightcoveExoPlayerVideoView.isPlaying();
    }

    /**
     * Returns the content media duration
     */
    @Override
    public long getContentDuration() {
        return isLiveContent ? -1 : brightcoveExoPlayerVideoView.getDuration();
    }

    /**
     * Returns the current position in the content media
     */
    @Override
    public long getCurrentPosition() {
        long currentPosition = contentHasCompleted ? getContentDuration():brightcoveExoPlayerVideoView.getCurrentPosition();
        return currentPosition;
    }

    @Override
    public float getContentPlayerVolumeLevel() {
        // We can't retrieve player's volume level. Return 1 by default.
        return 1;
    }

    /**
     * Returns the {@link ViewGroup} component that contains the content player
     */
    @Override
    @NonNull
    public ViewGroup getContentPlayerContainer() {
        return contentPlayerContainer;
    }
}
