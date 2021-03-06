package com.gigya.android.gigyademo;

import android.app.Application;
import android.util.Log;
import android.webkit.WebView;

import com.gigya.android.gigyademo.model.CustomAccount;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;

public class ApplicationExt extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /*
        Allow WebViews debugging.
         */
        WebView.setWebContentsDebuggingEnabled(true);

        /*
        Enabling Gigya logging for additional monitoring purposes.
         */
        GigyaLogger.setDebugMode(true);
        GigyaLogger.enableSmartLog(this);
        Log.d("GigyaSampleApplication", Gigya.VERSION);

        /*
        Attaching your application context to the Gigya SDK is a mandatory step.
        {https://developers.gigya.com/display/GD/Android+SDK+v4#AndroidSDKv4-BasicIntegration}
         */
        Gigya.setApplication(this);

        /*
        The demo application contains various demo api keys that are liked to sites with different configurations.
        To allow the app to run smoothly every time you will chose to change it we will save it in the preferences so
        the next time the app will restart it will remain liked to the same site.
         */
        final String apiKeyFromPrefs = getSharedPreferences("demo", MODE_PRIVATE).getString("savedKey", null);
        if (apiKeyFromPrefs != null) {
            Gigya.getInstance(CustomAccount.class).init(apiKeyFromPrefs);
        } else {
            Gigya.getInstance(CustomAccount.class);
        }

        /*
        Determine your application signature. Used for Google's SMS Retriever flow.
        {https://developers.google.com/identity/sms-retriever/overview}
        Will log out the application hash as: pkg: com.gigya.android.gigyademo -- hash: *********.
         */
        new AppSignatureHelper(this).getAppSignatures();
    }
}
