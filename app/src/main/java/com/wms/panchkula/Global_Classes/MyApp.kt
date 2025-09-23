package com.wms.panchkula.Global_Classes

import android.app.Activity
import android.app.Application
import android.content.ContextWrapper
import android.os.Bundle
import com.pixplicity.easyprefs.library.Prefs

class MyApp : Application() {
    companion object {
        var currentActivity: Activity? = null
            private set

        var currentApp: Application? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize the Prefs class
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()

        currentApp= this

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivity = activity
            }

            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                // No need to update currentActivity
            }

            override fun onActivityStopped(activity: Activity) {
                // No need to update currentActivity
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                // No need to update currentActivity
            }

            override fun onActivityDestroyed(activity: Activity) {
                // No need to update currentActivity
            }
        })




    }

}