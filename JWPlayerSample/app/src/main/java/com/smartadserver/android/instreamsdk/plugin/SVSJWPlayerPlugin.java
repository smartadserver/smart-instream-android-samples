package com.smartadserver.android.instreamsdk.plugin;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.core.PlayerState;
import com.longtailvideo.jwplayer.events.listeners.AdvertisingEvents;
import com.longtailvideo.jwplayer.events.listeners.VideoPlayerEvents;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;

/**
 * Implementation of the {@link SVSContentPlayerPlugin} interface for JW player.
 * source : https://github.com/smartadserver/smart-instream-android-plugins/
 */
public class SVSJWPlayerPlugin implements SVSContentPlayerPlugin {

    private Handler mainHandler;
    private JWPlayerView jwPlayerView;
    private ViewGroup contentPlayerContainer;
    private boolean isPlaying = false;
    private boolean isLiveContent;

    // flag to mark when content video has completed to return video duration as current position
    private boolean contentHasCompleted = false;

    /**
     * Constructor
     * @param jwPlayerView the JWPlayerView object
     * @param contentPlayerContainer the ViewGroup containing the JWPlayerView object
     */
    public SVSJWPlayerPlugin(JWPlayerView jwPlayerView, ViewGroup contentPlayerContainer, boolean isLiveContent) {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.jwPlayerView = jwPlayerView;
        this.contentPlayerContainer = contentPlayerContainer;
        this.isLiveContent = isLiveContent;

        jwPlayerView.addOnCompleteListener(new VideoPlayerEvents.OnCompleteListener() {
            @Override
            public void onComplete() {
                contentHasCompleted = true;
            }
        });
        jwPlayerView.addOnSeekedListener(new VideoPlayerEvents.OnSeekedListener() {
            @Override
            public void onSeeked() {
                contentHasCompleted = false;
            }
        });
        jwPlayerView.addOnPauseListener(new VideoPlayerEvents.OnPauseListener() {
            @Override
            public void onPause(PlayerState playerState) {
                isPlaying = false;
            }
        });
        jwPlayerView.addOnPlayListener(new VideoPlayerEvents.OnPlayListener() {
            @Override
            public void onPlay(PlayerState playerState) {
                isPlaying = true;
            }
        });


        jwPlayerView.addOnBeforePlayListener(new AdvertisingEvents.OnBeforePlayListener() {
            @Override
            public void onBeforePlay() {
                contentHasCompleted = false;
            }
        });
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
                // show JWplayer
                jwPlayerView.setVisibility(View.VISIBLE);
                if (!contentHasCompleted && !isPlaying()) {
                    jwPlayerView.play();
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
                jwPlayerView.setVisibility(View.INVISIBLE);
                // make this check first as calling pause() on a paused or completed player resumes or restarts it
                if (!contentHasCompleted && isPlaying) {
                    jwPlayerView.pause();
                }
            }
        });
    }

    /**
     * Returns whether the content media is currently being played
     */
    @Override
    public boolean isPlaying() {
        return isPlaying;
    }

    /**
     * Returns the content media duration
     */
    @Override
    public long getContentDuration() {
        return isLiveContent ? -1 : jwPlayerView.getDuration();
    }

    /**
     * Returns the current position in the content media
     */
    @Override
    public long getCurrentPosition() {
        long currentPosition = contentHasCompleted ? getContentDuration():jwPlayerView.getPosition();
        return currentPosition;
    }

    /**
     * Returns the {@link ViewGroup} component that contains the content player
     */
    @Override
    public ViewGroup getContentPlayerContainer() {
        return contentPlayerContainer;
    }

}
