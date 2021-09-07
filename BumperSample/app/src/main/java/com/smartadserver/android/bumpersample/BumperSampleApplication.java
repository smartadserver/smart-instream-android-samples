package com.smartadserver.android.bumpersample;

import android.app.Application;

import com.smartadserver.android.instreamsdk.util.SVSConfiguration;

/**
 * Use custom {@link Application} class to perform Smart instream SDK initialization
 */
public class BumperSampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // the configuration must be setup before making any ad call
        SVSConfiguration.getSharedInstance().configure(this, MainActivity.SITE_ID);
    }
}
