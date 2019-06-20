package com.gigya.android.sample

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.webkit.WebView
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.tfa.GigyaTFA

@Suppress("unused") // Referenced in manifest.
class GigyaSampleApplication : Application() {

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate() {
        super.onCreate()

        // Allow WebViews debugging.
        WebView.setWebContentsDebuggingEnabled(true)
        // Gigya logs.
        GigyaLogger.setDebugMode(true)
        GigyaLogger.enableSmartLog(this)
        Log.d("GigyaSampleApplication", Gigya.VERSION)

        Gigya.setApplication(this)
        // Initialization with implicit configuration & myAccountLiveData scheme.
        //Gigya.getInstance(MyAccount::class.java)

        /*
        Initialization with implicit configuration & without a custom myAccountLiveData scheme.
        Will use the default GigyaAccount scheme.
        */
        // Gigya.getInstance(applicationContext)

        /*
        Explicit initialization.
         */
        //Gigya.getInstance(MyAccount::class.java).init(getString(R.string.api_with_phone_totp_tfa))

        Gigya.getInstance(MyAccount::class.java).init("3_OKJbactZHkyOZuX-YBoE9-w_Mcogk80h_ihWZcC4je2yH1VQW7ye5I2na7wzp2Ho", "us1-st1.gigya.com")
    }
}