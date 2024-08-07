package com.smartadserver.android.bumpersample;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;
import com.smartadserver.android.instreamsdk.admanager.SVSAdManager;
import com.smartadserver.android.instreamsdk.admanager.SVSCuePoint;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRule;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRuleData;
import com.smartadserver.android.instreamsdk.model.adbreak.event.SVSAdBreakEvent;
import com.smartadserver.android.instreamsdk.model.adobjects.SVSAdInfo;
import com.smartadserver.android.instreamsdk.model.adplacement.SVSAdPlacement;
import com.smartadserver.android.instreamsdk.model.adplayerconfig.SVSAdPlayerConfiguration;
import com.smartadserver.android.instreamsdk.model.contentdata.SVSContentData;
import com.smartadserver.android.instreamsdk.plugin.SVSExoPlayerPlugin;
import com.smartadserver.android.instreamsdk.util.SVSLibraryInfo;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Simple activity that contains one an instance of {@link androidx.media3.exoplayer.ExoPlayer} as content player
 */
@UnstableApi public class MainActivity extends AppCompatActivity implements SVSAdManager.UIInteractionListener {

    // Constants

    // content video url
    static final private String CONTENT_VIDEO_URL = "https://ns.sascdn.com/mobilesdk/samples/videos/BigBuckBunnyTrailer_360p.mp4";

    // Smart Instream SDK placement parameters
    static final public int SITE_ID = 205812;
    static final public int PAGE_ID = 890742;
    static final public int FORMAT_ID = 27153;
    static final public String TARGET = "";

    // Bumper duration
    static final public int BUMPER_DURATION = 3000;

    // Smart Instream SDK main ad manager class
    private SVSAdManager adManager;

    // ViewGroup that contains the content player
    private ViewGroup contentPlayerContainer;

    // flag to mark that SVSAdManager was started
    private boolean adManagerStarted;

    // ExoPlayer related properties
    private PlayerView exoPlayerView;
    private ExoPlayer simpleExoPlayer;
    private ImageButton fullscreenButton;
    private ImageButton fullscreenExitButton;

    // Ad Pod Signalement related ImageView
    private ImageView adBreakBeginBumper;
    private ImageView adBreakEndBumper;

    // flags to properly handle ad breaks start/end according to Activity state
    private boolean isPaused;
    private boolean shouldStartAdBreak;
    private boolean shouldResumeContent;

    // flags to monitor ad break status
    boolean isAdBreakReady = false;
    boolean isAdBreakPlaying = false;

    Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Performs Activity initialization after creation.
     */
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set label of SDK version label
        TextView sdkVersionTextView = findViewById(R.id.sdk_version_textview);
        sdkVersionTextView.setText("Smart Instream SDK v" + SVSLibraryInfo.getSharedInstance().getVersion());

        /**
         * TCF Consent String v2 manual setting.
         *
         * By uncommenting the following code, you will set the TCF consent string v2 manually.
         * Note: the Smart Instream SDK will retrieve the TCF consent string from the SharedPreferences using the official IAB key "IABTCF_TCString".
         *
         * If you are using a CMP that does not store the consent string in the SharedPreferences using the official
         * IAB key, please store it yourself with the official key.
         */
        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // SharedPreferences.Editor editor = prefs.edit();
        // editor.putString("IABTCF_TCString", "YourTCFConsentString");
        // editor.apply();

        /**
         * CCPA Consent String manual setting.
         *
         * By uncommenting the following code, you will set the CCPA consent string manually.
         * Note: The Smart Instream SDK will retrieve the CCPA consent string from the SharedPreferences using the official IAB key "IABUSPrivacy_String".
         *
         * If you are using a CMP that does not store the consent string in the SharedPreferences using the official
         * IAB key, please store it yourself with the official key.
         */
        // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // SharedPreferences.Editor editor = prefs.edit();
        // editor.putString("IABUSPrivacy_String", "YourCCPAConsentString");
        // editor.apply();

        /******************************************
         * now perform Player related code here.
         ******************************************/
        bindViews();
        createAdManager();
        configurePlayer();
    }

    /**
     * Overriden to resume the {@link SVSAdManager} along with the Activity.
     */
    @Override
    protected void onResume() {
        isPaused = false;
        super.onResume();
        if (adManager != null) {
            adManager.onResume();
            if (shouldStartAdBreak) {
                shouldStartAdBreak = false;
                adManager.startAdBreak();
            } else if (shouldResumeContent) {
                shouldResumeContent = false;
                adManager.resumeContent();
            }
        }
    }

    /**
     * Overriden to pause both the {@link SVSAdManager} and the {@link ExoPlayer} along with the Activity.
     */
    @Override
    protected void onPause() {
        isPaused = true;
        super.onPause();
        if (adManager != null) {
            adManager.onPause();
        }
        getExoPlayer().setPlayWhenReady(false);
    }

    /**
     * Overriden to cleanup the {@link SVSAdManager} instance.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adManager != null) {
            adManager.onDestroy();
        }
    }

    /**
     * Overriden to adapt Activity layout on orientation changes.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Automatically go to fullscreen if we are going to landscape,
        // or exit fullscreen if we are going to portrait.
        setFullscreen(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    /**
     * Bind all views to their related properties.
     */
    private void bindViews() {
        exoPlayerView = findViewById(R.id.exo_player_view);
        exoPlayerView.setControllerAutoShow(false);
        contentPlayerContainer = findViewById(R.id.content_player_container);
        fullscreenButton = findViewById(R.id.bt_fullscreen);
        fullscreenExitButton = findViewById(R.id.bt_fullscreen_exit);

        adBreakBeginBumper = findViewById(R.id.adBreakBeginImageView);
        adBreakEndBumper = findViewById(R.id.adBreakEndImageView);

        // add listeners on fullscreen buttons
        fullscreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFullscreen(true);
            }
        });

        fullscreenExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFullscreen(false);
            }
        });

        // add listener on replay button
        Button replayButton = findViewById(R.id.bt_replay);
        replayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replay();
            }
        });
    }

    /**
     * Configures the player. See https://google.github.io/ExoPlayer/guide.html for further information.
     */
    private void configurePlayer() {

        // connect the ExoPlayer view with the player instance
        exoPlayerView.setPlayer(getExoPlayer());

        // add a listener on ExoPlayer to detect when the video actually starts playing, to start the SVSAdManager
        getExoPlayer().addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // start the SVSAdManager only when the player is about to play.
                if (playbackState == Player.STATE_READY && !adManagerStarted) {
                    startAdManager();
                }
            }
        });

        Uri videoUri = Uri.parse(CONTENT_VIDEO_URL);

        // set media source on ExoPLayer
        getExoPlayer().setMediaItem(MediaItem.fromUri(videoUri));
        getExoPlayer().prepare();

        // do not start ExPlayer once prepared, the ad manager will take care of it
        getExoPlayer().setPlayWhenReady(false);
    }

    /**
     * Create the {@link SVSAdManager} instance.
     */
    private void createAdManager() {
        /******************************************************************************************************************************
         * The SVSAdManager is the class responsible for performing AdCalls and displaying ads.
         * To initialize this object you will need:
         * - a SVSAdPlacement instance, identifying this content video as ad inventory
         * - a listener implementing the SVSAdManager.UIInteractionListener interface to react to fullscreen enter/exit events
         *
         * Optional objects can also be passed during initialization:
         * - an array of SVSAdRule instances, to define advertising policy depending on the content duration. If null, the SVSAdManager
         *   will use a default one from the SVSConfiguration singleton class.
         * - SVSAdPlayerConfiguration, to modify the Ad Player look and behavior. If null, the SVSAdManager will use a default one from
         *   the SVSConfiguration singleton class.
         * - SVSContentData, describing your content. If null, the SVSAdManager will note use the data for targeting.
         *
         * Please refer to each initialization method for more information about these objects.
         ******************************************************************************************************************************/

        // Ad Placement, must be non null
        SVSAdPlacement adPlacement = instantiateAdPlacement();

        // Ad Rules, OPTIONAL
        SVSAdRule[] adRules = instantiateAdRules();

        //Ad Player Configuration, OPTIONAL
        SVSAdPlayerConfiguration adPlayerConfiguration = instantiateAdPlayerConfiguration();

        // Content Data, OPTIONAL
        SVSContentData contentData = instantiateContentData();

        // Create the SVSAdManager instance.
        adManager = new SVSAdManager(this, adPlacement, adRules, adPlayerConfiguration, contentData);
        adManager.addUIInteractionListener(this);

        adManager.addAdManagerListener(new SVSAdManager.AdManagerListener() {

            @Override
            public void onAdBreakEvent(@NonNull SVSAdBreakEvent adBreakEvent) {

                if (!adPlayerConfiguration.getPublisherOptions().isEnableAdBreakAutoplay()) {
                    switch (adBreakEvent.getEventType()) {
                        case SVSAdBreakEvent.EVENT_TYPE_AD_BREAK_READY:

                            // mark that an ad break is ready to be played
                            isAdBreakReady = true;

                            // If no ad break is currently playing only, start the received ad break.
                            if (!isAdBreakPlaying) {
                                getExoPlayer().setPlayWhenReady(false);
                                startAdBreakSequence();
                            }
                            break;
                        case SVSAdBreakEvent.EVENT_TYPE_AD_BREAK_COMPLETED:

                            adBreakEndBumper.setVisibility(View.VISIBLE);

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    adBreakEndBumper.setVisibility(View.GONE);

                                    // collision case : if an ad break got ready while the previous one was still being displayed (bumpers display included),
                                    // just play it as if it was the first one (including start and end bumpers)
                                    if (isAdBreakReady) {
                                        startAdBreakSequence();
                                    } else {
                                        // And resume the content
                                        isAdBreakPlaying = false;
                                        adBreakBeginBumper.setVisibility(View.GONE);

                                        if (isPaused) {
                                            shouldResumeContent = true;
                                        } else {
                                            adManager.resumeContent();
                                        }
                                    }
                                }
                            }, BUMPER_DURATION);
                            break;
                        default:
                            break;
                    }
                }
            }

            @Override
            public void onAdStarted(@NonNull SVSAdInfo svsAdInfo) {
                // Called when an ad has started. Will be called for every ads of an ad break.
            }

            @Override
            public void onCuePointsGenerated(@NonNull List<SVSCuePoint> list) {
                // Called when cuepoints used for midroll ad break have been computed.
                // You can use this method to display the ad break position in your content player UI…
            }
        });
    }

    /**
     * Convenience method to show start bumper for 3 seconds, then start the ad break ready to be played
     */
    private void startAdBreakSequence() {

        // mark that an ad break is being played, and consumed.
        isAdBreakPlaying = true;
        isAdBreakReady = false;

        adBreakBeginBumper.setVisibility(View.VISIBLE);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adBreakBeginBumper.setVisibility(View.GONE);
                if (isPaused) {
                    shouldStartAdBreak = true;
                } else {
                    adManager.startAdBreak();
                }
            }
        }, BUMPER_DURATION);
    }

    /**
     * Starts the {@link SVSAdManager} instance.
     */
    private void startAdManager() {
        if (adManagerStarted) {
            return;
        }

        adManagerStarted = true;
        SVSContentPlayerPlugin plugin = instantiateContentPlayerPlugin();
        adManager.start(plugin);
    }

    /**
     * Creates a {@link SVSAdPlacement} instance
     */
    @NonNull
    private SVSAdPlacement instantiateAdPlacement() {
        /***************************************************************
         * SVSAdPlacement is mandatory to perform ad calls.
         * You cannot create ad SVSAdManager without an SVSAdPlacement.
         ***************************************************************/

        // Create an SVSAdPlacement instance from your SiteID, PageID and FormatID.
        SVSAdPlacement adPlacement = new SVSAdPlacement(SITE_ID, PAGE_ID, FORMAT_ID);

        // Optional: you can setup the custom targeting for your placement.
        adPlacement.setGlobalTargetingString(TARGET); // Default targeting
        adPlacement.setPrerollTargetingString(null); // Preroll targeting
        adPlacement.setMidrollTargetingString(null); // Midroll targeting
        adPlacement.setPostrollTargetingString(null); // Postroll targeting

        return adPlacement;
    }

    /**
     * Creates an array of {@link SVSAdRule} instances
     */
    @NonNull
    private SVSAdRule[] instantiateAdRules() {
        /***********************************************************************************
         * SVSAdRule objects allow an advanced management of your advertising policy.
         * Please refer to the documentation for more information about these objects.
         * This object is optional:
         * SVSAdManager will create its own if no SVSAdRule are passed upon initialization.
         ***********************************************************************************/

        // Instantiate 3 SVSadruleData for Preroll, Midroll and Postroll.
        SVSAdRuleData prerollData = SVSAdRuleData.createPrerollAdRuleData(1, 1200); // Preroll with 1 ad.
        SVSAdRuleData midrollData = SVSAdRuleData.createMidrollAdRuleData(1, 1200, new double[]{50}); // Midroll with 1 ad when 50% of the content's duration is reached.
        SVSAdRuleData postrollData = SVSAdRuleData.createPostrollAdRuleData(1, 1200); // Postroll with 1 ad.

        // Instantiate a SVSAdRule with preroll, midroll and postroll SVSAdRuleData
        // this SVSAdRule will cover any duration.
        SVSAdRule adRule = new SVSAdRule(0, -1, new SVSAdRuleData[]{prerollData, midrollData, postrollData}, 0);

        // Return an array of SVSAdRule
        return new SVSAdRule[]{adRule};
    }

    /**
     * Creates a {@link SVSAdPlayerConfiguration} instance
     */
    @NonNull
    private SVSAdPlayerConfiguration instantiateAdPlayerConfiguration() {
        /*************************************************************************************************
         * SVSAdPlayerConfiguration is responsible for modifying the look and behavior of the Ad Player.
         * This object is optional:
         * SVSAdManager will create its own if no SVSAdPlayerConfiguration is passed upon initialization.
         *************************************************************************************************/

        // Create a new SVSAdPlayerConfiguration.
        SVSAdPlayerConfiguration adPlayerConfiguration = new SVSAdPlayerConfiguration();

        // Force all ads to be not skippable. See API for more options...
        adPlayerConfiguration.getPublisherOptions().setForceSkipDelay(true);
        adPlayerConfiguration.getPublisherOptions().setSkipDelay(-1);

        // Disable ad break autoplay
        adPlayerConfiguration.getPublisherOptions().setEnableAdBreakAutoplay(false);

        return adPlayerConfiguration;
    }

    /**
     * Creates a {@link SVSContentData} instance
     */
    @NonNull
    private SVSContentData instantiateContentData() {
        /****************************************************************
         * SVSContentData provides information about your video content.
         * This object is optional.
         ****************************************************************/

        // Instantiate the builder.
        SVSContentData.Builder builder = new SVSContentData.Builder();

        // Sets your parameters.
        builder.setContentID("contentID");
        builder.setContentTitle("contentTitle");
        builder.setVideoContentType("videoContentType");
        builder.setVideoContentCategory("videoContentCategory");
        builder.setVideoContentDuration(60);
        builder.setVideoSeasonNumber(1);
        builder.setVideoEpisodeNumber(2);
        builder.setVideoContentRating("videoContentRating");
        builder.setContentProviderID("contentProviderID");
        builder.setContentProviderName("contentProviderName");
        builder.setVideoContentDistributorID("videoContainerDistributorID");
        builder.setVideoContentDistributorName("videoContainerDistributerName");
        builder.setVideoContentTags(new String[]{"tag1", "tag2"});
        builder.setExternalContentID("externalContentID");
        builder.setVideoCMSID("videoCMSID");

        // Then build your instance of SVSContentData
        return builder.build();
    }

    /**
     * Creates the {@link SVSExoPlayerPlugin} that connects the {@link SVSAdManager} intance to the ExoPlayer content player.
     */
    @NonNull
    private SVSContentPlayerPlugin instantiateContentPlayerPlugin() {
        /************************************************************************************************
         * To know when to display AdBreaks, the SVSAdManager needs to monitor your content, especially:
         * - total duration
         * - current time
         * To be able to start the SVSAdManager, you need to pass an object implementing
         * the SVSContentPlayerPlugin interface. Here, we instantiate a ready-to-use SVSExoPlayerPlugin
         * for the ExoPlayer.
         ************************************************************************************************/
        return new SVSExoPlayerPlugin(simpleExoPlayer, exoPlayerView, contentPlayerContainer, false);
    }

    /**
     * Lazy loaded exoPlayer instance getter.
     */
    private ExoPlayer getExoPlayer() {
        if (simpleExoPlayer == null) {
            simpleExoPlayer = new ExoPlayer.Builder(this).build();
        }
        return simpleExoPlayer;
    }

    /**
     * Implementation of SVSAdManager.UIInteractionListener.
     */
    @Override
    public void onFullscreenStateChangeRequest(boolean isFullscreen) {
        // Called when the enter (or exit) fullscreen button of an Ad is clicked by the user.
        // Adapt your UI to properly react to this user action: you should resize your container view.
        setFullscreen(isFullscreen);

        /************************************************************************************************************
         * NOTE ABOUT FULLSCREEN / EXIT FULLSCREEN
         *
         * For obvious reasons, SVSAdManager, will never force your application or your content player into
         * fullscreen. It is your application that decides what to do with it.
         * If you allow the fullscreen / exit fullscreen buttons on the Ad Player Interface (in SVSAdPlayerConfiguration),
         * the SVSAdManager instance will request to enter fullscreen through the onFullscreenStateChangeRequest method of
         * SVSAdManager.UIInteractionListener.
         * You are responsible for responding to this event, and change the layout of your application accordingly.
         * In return, if you allow your content player to enter/exit fullscreen you must let the SVSAdManager know about it by
         * setting the new state of the content player through the onFullscreenStateChange(boolean isFullscreen) method of SVSAdManager
         ************************************************************************************************************/
    }

    /**
     * Whether ExoPlayer or AdPlayer change their fullscreen status, we must let the SVSAdManager
     * know about it so it can adjust the UI of the ad player view.
     * Adapt your UI to properly react to the fullscreen status change.
     */
    protected void setFullscreen(boolean isFullscreen) {
        disableShowHideAnimation(getSupportActionBar());

        // Update SystemUIVisibility to hide/show the StatusBar, the ActionBar and the NavigationBar.
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        if (isFullscreen) {
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getSupportActionBar().hide();
        } else {
            uiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getSupportActionBar().show();
        }
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        // Tell the adManager that we are entering or exiting fullscreen.
        adManager.onFullscreenStateChange(isFullscreen);

        // Update visibility of several components depending on isFullscreen value.
        fullscreenButton.setVisibility(isFullscreen ? View.GONE : View.VISIBLE);
        fullscreenExitButton.setVisibility(isFullscreen ? View.VISIBLE : View.GONE);

        // Update simpleExoPlayerView and contentPlayerContainer layoutParams to make the player take
        // all the screen when entering fullscreen.
        exoPlayerView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, isFullscreen ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT));
        contentPlayerContainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, isFullscreen ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /**
     * Updates the exoPlayer to replay its content from the beginning.
     */
    private void replay() {
        if (adManager != null) {
            getExoPlayer().seekTo(0);
            adManager.replay();
        }
    }

    /**
     * Workaround method to disable the show/hide animation and avoid making the ActionBar flicker.
     */
    public static void disableShowHideAnimation(@Nullable ActionBar actionBar) {
        if (actionBar != null) {
            try {
                actionBar.getClass().getDeclaredMethod("setShowHideAnimationEnabled", boolean.class).invoke(actionBar, false);
            } catch (Exception exception) {
                try {
                    Field mActionBarField = actionBar.getClass().getSuperclass().getDeclaredField("mActionBar");
                    mActionBarField.setAccessible(true);
                    Object icsActionBar = mActionBarField.get(actionBar);
                    Field mShowHideAnimationEnabledField = icsActionBar.getClass().getDeclaredField("mShowHideAnimationEnabled");
                    mShowHideAnimationEnabledField.setAccessible(true);
                    mShowHideAnimationEnabledField.set(icsActionBar, false);
                    Field mCurrentShowAnimField = icsActionBar.getClass().getDeclaredField("mCurrentShowAnim");
                    mCurrentShowAnimField.setAccessible(true);
                    mCurrentShowAnimField.set(icsActionBar, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
