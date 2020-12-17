package com.smartadserver.android.jwplayersample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.longtailvideo.jwplayer.JWPlayerView;
import com.longtailvideo.jwplayer.events.BufferChangeEvent;
import com.longtailvideo.jwplayer.events.listeners.VideoPlayerEvents;
import com.longtailvideo.jwplayer.fullscreen.FullscreenHandler;
import com.longtailvideo.jwplayer.media.playlists.PlaylistItem;
import com.smartadserver.android.instreamsdk.SVSContentPlayerPlugin;
import com.smartadserver.android.instreamsdk.admanager.SVSAdManager;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRule;
import com.smartadserver.android.instreamsdk.adrules.SVSAdRuleData;
import com.smartadserver.android.instreamsdk.model.adbreak.SVSAdBreakType;
import com.smartadserver.android.instreamsdk.model.adbreak.event.SVSAdBreakEvent;
import com.smartadserver.android.instreamsdk.model.adplacement.SVSAdPlacement;
import com.smartadserver.android.instreamsdk.model.adplayerconfig.SVSAdPlayerConfiguration;
import com.smartadserver.android.instreamsdk.model.contentdata.SVSContentData;
import com.smartadserver.android.instreamsdk.plugin.SVSJWPlayerPlugin;
import com.smartadserver.android.instreamsdk.util.SVSLibraryInfo;

import java.lang.reflect.Field;

/**
 * Simple activity that contains one an instance of {@link JWPlayerView} as content player
 */
@SuppressWarnings("DanglingJavadoc")
public class MainActivity extends AppCompatActivity implements SVSAdManager.UIInteractionListener {

    // Constants

    // content video url
    static final private String CONTENT_VIDEO_URL = "https://ns.sascdn.com/mobilesdk/samples/videos/BigBuckBunnyTrailer_360p.mp4";
//    static final private String CONTENT_VIDEO_URL = "https://d3hjh6d7n71rqm.cloudfront.net/mediasfiles/2020/1/10/1578656618/1578656618/2020-01-10-124338.588987rptpq.mp4";


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

    // JWPlayer player view
    private JWPlayerView jwPlayerView;

    /**
     * Performs Activity initialization after creation.
     */
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set label of SDK version label
        TextView sdkVersionTextview = findViewById(R.id.sdk_version_textview);
        sdkVersionTextview.setText("Smart Instream SDK v" + SVSLibraryInfo.getSharedInstance().getVersion());

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
        configurePlayer();
        createAdManager();

        /******************************************
         * now perform Player related code here.
         ******************************************/
        bindViews();
        configurePlayer();
        createAdManager();
    }

    /**
     * Overriden to resume the {@link SVSAdManager} instance along with the Activity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (adManager != null) {
            adManager.onResume();
        }
        jwPlayerView.onResume();
    }

    /**
     * Overriden to pause the {@link SVSAdManager} instance along with the Activity.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (adManager != null) {
            adManager.onPause();
        }
        jwPlayerView.onPause();

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
        jwPlayerView.onDestroy();
    }

    /**
     * Bind all views to their related attributes.
     */
    private void bindViews() {
         jwPlayerView = (JWPlayerView)findViewById(R.id.jw_video_player);
//        jwPlayerView = jwFragment.getPlayer();
//        jwFragment.onResume();
//        jwFragment.setFullscreenOnDeviceRotate(true);
        contentPlayerContainer = findViewById(R.id.content_player_container);
    }

    /**
     * Configures the player. See https://developer.jwplayer.com/sdk/android/docs/developer-guide/
     */
    private void configurePlayer() {
        PlaylistItem playlistItem = new PlaylistItem(CONTENT_VIDEO_URL);

        jwPlayerView.addOnBufferChangeListener(new VideoPlayerEvents.OnBufferChangeListener() {
            @Override
            public void onBufferChange(BufferChangeEvent bufferChangeEvent) {
                if(!adManagerStarted && bufferChangeEvent.getDuration()>0){
                    jwPlayerView.pause();
                    startAdManager();
                }
            }
        });

        // add a full screen handler object on the JWPLayer player view
        jwPlayerView.setFullscreenHandler(new FullscreenHandler() {
            /**
             * Called when JWPlayer requests to enter fullscreen.
             */
            @Override
            public void onFullscreenRequested() {
                MainActivity.this.requestFullscreen(true, true);
            }

            /**
             * Called when JWPlayer requests to exit fullscreen.
             */
            @Override
            public void onFullscreenExitRequested() {
                MainActivity.this.requestFullscreen(false, true);
            }

            @Override
            public void onResume() {
            }

            @Override
            public void onPause() {
            }

            @Override
            public void onDestroy() {
            }

            @Override
            public void onAllowRotationChanged(boolean b) {
            }

            @Override
            public void updateLayoutParams(ViewGroup.LayoutParams layoutParams) {
            }

            @Override
            public void setUseFullscreenLayoutFlags(boolean b) {
            }
        });

        // here is the issue : jwPlayerView returns 0 duration until after "some time" play was requested.
        // so the trick is to
        // 1 - hide JW Player
//        jwPlayerView.setVisibility(View.INVISIBLE);
        jwPlayerView.setMute(true);

        // start loading and playing video until content duration is known in OnBufferChangeListener,
        // which will in turn pause it and start the SVSAdManager for a preroll
        jwPlayerView.load(playlistItem);
        jwPlayerView.play();
    }

    /**
     * Create the {@link SVSAdManager}.
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
         * You cannot create ad SVSadManager without an SVSAdPlacement.
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

        // Create a new SVSAdPlayerConfiguration.
        SVSAdPlayerConfiguration adPlayerConfiguration = new SVSAdPlayerConfiguration();

        // Force skip delay of 5 seconds for any ad.
        adPlayerConfiguration.getPublisherOptions().setForceSkipDelay(true);
        adPlayerConfiguration.getPublisherOptions().setSkipDelay(5000);

        // See API for more options...
        return adPlayerConfiguration;
    }

    /**
     * Creates a {@link SVSContentData} instance
     */
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
     * Creates the {@link SVSJWPlayerPlugin} that connects the {@link SVSAdManager} intance to the {@link JWPlayerView} content player.
     */
    private SVSContentPlayerPlugin instantiateContentPlayerPlugin() {
        /************************************************************************************************
         * To know when to display AdBreaks, the SVSAdManager needs to monitor your content, especially:
         * - total duration
         * - current time
         * To be able to start the SVSAdManager, you need to create a content player plugin,
         * conforming to the SVSContentPlayerPlugin interface.
         ************************************************************************************************/
        return new SVSJWPlayerPlugin(jwPlayerView, contentPlayerContainer, false);
    }

    /**
     * Implementation of SVSAdManager.UIInteractionListener.
     */
    @Override
    public void onFullscreenStateChangeRequest(boolean isFullscreen) {
        // Called when the enter (or exit) fullscreen button of an Ad is clicked by the user.
        // Adapt your UI to properly react to this user action: you should resize your container view.
        requestFullscreen(isFullscreen, false);

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
     * Whether JWPlayer or AdPlayer change their fullscreen status, we must let the SVSAdManager
     * know about it so it can adjust the UI of the ad player view.
     * Adapt your UI to properly react to the fullscreen status change.
     */
    private void requestFullscreen(boolean fullscreen, boolean requestFromPlayer) {
        if (requestFromPlayer) {
            //If its from JWPlayer we make it fullScreen immediately.
            disableShowHideAnimation(getSupportActionBar());

            // Update SystemUIVisibility to hide/show the StatusBar, the ActionBar and the NavigationBar.
            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
            if (fullscreen) {
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

            // Tell the adManager that we are going in ou out fullscreen.
            adManager.onFullscreenStateChange(fullscreen);

            // Update visibility of several components depending on isFullscreen value.
            makePlayerMatchParent(fullscreen);
        } else {
            // If not, we notified the JWPlayer and it will call requestFullscreen itself.
            jwPlayerView.setFullscreen(fullscreen, false);
        }
    }

    /**
     * Updates contentPlayerContainer's layoutParams to either match or not its parent.
     */
    private void makePlayerMatchParent(boolean matchParent) {
        if (matchParent) {
            contentPlayerContainer.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            // we set an hard value for the height cause if we don't, JWplayer will take all the screen.
            float playerLayoutHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, getResources().getDisplayMetrics());
            ViewGroup.LayoutParams lp = contentPlayerContainer.getLayoutParams();
            lp.height = (int) playerLayoutHeight;
            contentPlayerContainer.setLayoutParams(lp);
        }
    }

    /**
     * Workaround method to disable the show/hide animation and avoid making the ActionBar flicker.
     */
    public static void disableShowHideAnimation(ActionBar actionBar) {
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
