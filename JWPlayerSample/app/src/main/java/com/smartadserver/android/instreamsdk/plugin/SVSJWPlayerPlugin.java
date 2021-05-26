package com.smartadserver.android.instreamsdk.plugin;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.core.PlayerState;
import com.longtailvideo.jwplayer.events.AdCompleteEvent;
import com.longtailvideo.jwplayer.events.BeforePlayEvent;
import com.longtailvideo.jwplayer.events.CompleteEvent;
import com.longtailvideo.jwplayer.events.PauseEvent;
import com.longtailvideo.jwplayer.events.PlayEvent;
import com.longtailvideo.jwplayer.events.SeekedEvent;
import com.longtailvideo.jwplayer.events.listeners.AdvertisingEvents;
import com.longtailvideo.jwplayer.events.listeners.VideoPlayerEvents;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;

/**
 * Implementation of the {@link SVSContentPlayerPlugin} interface for JW player.
 * source : https://github.com/smartadserver/smart-instream-android-plugins/
 */
public class SVSJWPlayerPlugin implements SVSContentPlayerPlugin {

    @NonNull
    private Handler mainHandler;
    @NonNull
    private JWPlayerView jwPlayerView;
    @NonNull
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
    public SVSJWPlayerPlugin(@NonNull JWPlayerView jwPlayerView, @NonNull ViewGroup contentPlayerContainer, boolean isLiveContent) {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.jwPlayerView = jwPlayerView;
        this.contentPlayerContainer = contentPlayerContainer;
        this.isLiveContent = isLiveContent;

        jwPlayerView.addOnCompleteListener(new VideoPlayerEvents.OnCompleteListener() {

            @Override
            public void onComplete(CompleteEvent completeEvent) {
                contentHasCompleted = true;
            }
        });

        jwPlayerView.addOnAdCompleteListener(new AdvertisingEvents.OnAdCompleteListener() {
            @Override
            public void onAdComplete(AdCompleteEvent adCompleteEvent) {
                contentHasCompleted = true;
            }
        });

        jwPlayerView.addOnSeekedListener(new VideoPlayerEvents.OnSeekedListener() {
            @Override
            public void onSeeked(SeekedEvent seekedEvent) {
                contentHasCompleted = false;
            }

        });
        jwPlayerView.addOnPauseListener(new VideoPlayerEvents.OnPauseListener() {
            @Override
            public void onPause(PauseEvent pauseEvent) {
                isPlaying = false;
            }

        });
        jwPlayerView.addOnPlayListener(new VideoPlayerEvents.OnPlayListener() {
            @Override
            public void onPlay(PlayEvent playEvent) {
                isPlaying = true;
            }

        });


        jwPlayerView.addOnBeforePlayListener(new AdvertisingEvents.OnBeforePlayListener() {
            @Override
            public void onBeforePlay(BeforePlayEvent beforePlayEvent) {
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
                jwPlayerView.setMute(false);
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
        return isLiveContent ? -1 : (long)(jwPlayerView.getDuration() * 1000);
    }

    /**
     * Returns the current position in the content media
     */
    @Override
    public long getCurrentPosition() {
        long currentPosition = contentHasCompleted ? getContentDuration():(long)(jwPlayerView.getPosition() * 1000);
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
