package com.smartadserver.android.androidtvsample;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.RandomTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;
import com.smartadserver.android.instreamsdk.admanager.SVSAdManager;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRule;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRuleData;
import com.smartadserver.android.instreamsdk.model.adplacement.SVSAdPlacement;
import com.smartadserver.android.instreamsdk.model.adplayerconfig.SVSAdPlayerConfiguration;
import com.smartadserver.android.instreamsdk.model.contentdata.SVSContentData;
import com.smartadserver.android.instreamsdk.plugin.SVSExoPlayerPlugin;

/**
 * Simple activity that contains one an instance of {@link com.google.android.exoplayer2.ExoPlayer} as content player
 * <p>
 * This sample can be use both on AndroidTV and Amazon FireTV.
 */
@SuppressWarnings({"DanglingJavadoc", "SpellCheckingInspection"})
public class MainActivity extends Activity implements SVSAdManager.UIInteractionListener {

    // Constants

    // content video url
    static final private String CONTENT_VIDEO_URL = "https://ns.sascdn.com/mobilesdk/samples/videos/BigBuckBunnyTrailer_360p.mp4";

    // Smart Instream SDK placement parameters
    static final public int SITE_ID = 205812;
    static final public int PAGE_ID = 890742;
    static final public int FORMAT_ID = 27153;
    static final public String TARGET = "";

    // Smart Instream SDK main ad manager class
    private SVSAdManager adManager;

    // ViewGroup that contains the content player
    private ViewGroup contentPlayerContainer;

    // flag to mark that SVSAdManager was started
    private boolean adManagerStarted;

    private boolean adBreakStarted = false;

    // ExoPlayer related properties
    private DefaultBandwidthMeter defaultBandwidthMeter;
    private PlayerView simpleExoPlayerView;
    private SimpleExoPlayer simpleExoPlayer;

    /**
     * Performs Activity initialization after creation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        simpleExoPlayerView.showController();
    }

    /**
     * Overriden to resume the {@link SVSAdManager} along with the Activity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (adManager != null) {
            adManager.onResume();
        }
    }

    /**
     * Overriden to pause both the {@link SVSAdManager} and the {@link com.google.android.exoplayer2.ExoPlayer} along with the Activity.
     */
    @Override
    protected void onPause() {
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
     * Detects remote button event to show controls.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        // if an adBreak is playing, do not let the remote controls the player.
        if (adBreakStarted) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // quits the application
                finishAffinity();
                System.exit(0);
            }
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!simpleExoPlayerView.getUseController()) {
                    simpleExoPlayerView.setUseController(true);
                }
                simpleExoPlayerView.showController();
                break;

            case KeyEvent.KEYCODE_BACK:
                if (simpleExoPlayerView.getUseController()) {
                    // back button either hides the controls
                    simpleExoPlayerView.setUseController(false);
                } else {
                    // or quits the application if controls are already hidden.
                    finishAffinity();
                    System.exit(0);
                }
                break;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                simpleExoPlayer.setPlayWhenReady(!simpleExoPlayer.getPlayWhenReady());
                break;

            case KeyEvent.KEYCODE_MEDIA_PLAY:
                simpleExoPlayer.setPlayWhenReady(true);
                break;

            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                simpleExoPlayer.setPlayWhenReady(false);
                break;

            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                simpleExoPlayer.seekTo(simpleExoPlayer.getCurrentPosition() + 15000);
                break;

            case KeyEvent.KEYCODE_MEDIA_REWIND:
                simpleExoPlayer.seekTo(simpleExoPlayer.getCurrentPosition() - 5000);
                break;
        }

        return false;
    }

    /**
     * Bind all views to their related properties.
     */
    @SuppressWarnings("Convert2Lambda")
    private void bindViews() {
        simpleExoPlayerView = findViewById(R.id.simple_exo_player_view);
        simpleExoPlayerView.setControllerAutoShow(false);
        contentPlayerContainer = findViewById(R.id.content_player_container);

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
        simpleExoPlayerView.setPlayer(getExoPlayer());

        // add a listener on ExoPlayer to detect when the video actually starts playing, to start the SVSAdManager
        getExoPlayer().addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            }

            @Override
            public void onLoadingChanged(boolean isLoading) {
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                // start the SVSAdManager only when the player is about to play.
                if (playbackState == Player.STATE_READY && !adManagerStarted) {
                    startAdManager();
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
            }

            @Override
            public void onPositionDiscontinuity(int reason) {
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            }

            @Override
            public void onSeekProcessed() {
            }
        });

        // initialize video source
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)),
                getDefaultBandwidthMeter());
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        Uri videoUri = Uri.parse(CONTENT_VIDEO_URL);
        ExtractorMediaSource.Factory factory = new ExtractorMediaSource.Factory(dataSourceFactory);
        factory.setExtractorsFactory(extractorsFactory);
        MediaSource mediaSource = factory.createMediaSource(videoUri);


        // set media source on ExoPLayer
        getExoPlayer().prepare(mediaSource);

        // start ExPlayer once prepared
        getExoPlayer().setPlayWhenReady(true);
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
    private SVSAdPlayerConfiguration instantiateAdPlayerConfiguration() {
        /*************************************************************************************************
         * SVSAdPlayerConfiguration is responsible for modifying the look and behavior ot the Ad Player.
         * This object is optional:
         * SVSAdManager will create its own if no SVSAdPlayerConfiguration is passed upon initialization.
         *************************************************************************************************/
        return new SVSAdPlayerConfiguration();
    }

    /**
     * Creates a {@link SVSContentData} instance
     */
    private SVSContentData instantiateContentData() {
        /****************************************************************
         * SVSContentData provides information about your video content.
         * This object is optional.
         ****************************************************************/
        return new SVSContentData("contentID",
                "contentTitle",
                "videoContentType",
                "videoContentCategory",
                60,
                1,
                2,
                "videoContentRating",
                "contentProviderID",
                "contentProviderName",
                "videoContainerDistributorID",
                "videoContainerDistributorName",
                new String[]{"tag1", "tag2"},
                "externalContentID",
                "videoCMSID");
    }

    /**
     * Creates the {@link SVSExoPlayerPlugin} that connects the {@link SVSAdManager} intance to the ExoPlayer content player.
     */
    private SVSContentPlayerPlugin instantiateContentPlayerPlugin() {
        /************************************************************************************************
         * To know when to display AdBreaks, the SVSAdManager needs to monitor your content, especially:
         * - total duration
         * - current time
         * To be able to start the SVSAdManager, you need to pass an object implementing
         * the SVSContentPlayerPlugin interface. Here, we instantiate a ready-to-use SVSExoPlayerPlugin
         * for the ExoPlayer.
         ************************************************************************************************/
        return new SVSExoPlayerPlugin(simpleExoPlayer, simpleExoPlayerView, contentPlayerContainer, false) {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // We override those specific methods to know when the adBreaks start and stop, to disable remote button events.
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

            @Override
            public void adBreakStarted() {
                super.adBreakStarted();
                adBreakStarted = true;
            }

            @Override
            public void adBreakEnded() {
                super.adBreakEnded();
                adBreakStarted = false;
            }
        };
    }

    /**
     * Lazy loaded defaultBandwidthMeter getter.
     */
    protected DefaultBandwidthMeter getDefaultBandwidthMeter() {
        if (defaultBandwidthMeter == null) {
            defaultBandwidthMeter = new DefaultBandwidthMeter();
        }
        return defaultBandwidthMeter;
    }

    /**
     * Lazy loaded exoPlayer instance getter.
     */
    private SimpleExoPlayer getExoPlayer() {
        if (simpleExoPlayer == null) {
            TrackSelection.Factory trackSelection = new RandomTrackSelection.Factory();
            TrackSelector trackSelector = new DefaultTrackSelector(trackSelection);
            simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        }
        return simpleExoPlayer;
    }

    /**
     * Implementation of SVSAdManager.UIInteractionListener.
     */
    @Override
    public void onFullscreenStateChangeRequest(boolean isFullscreen) {
        // AndroidTV Sample is always in fullscreen.
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

}
