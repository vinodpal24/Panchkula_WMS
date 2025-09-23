package com.wms.panchkula.ui.splashScreen

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Global_Notification.NetworkConnection
import com.wms.panchkula.R
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivitySplashBinding
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.ui.home.HomeActivity
import com.wms.panchkula.ui.login.LoginActivity
import com.wms.panchkula.ui.setting.SettingActivity

@Suppress("DEPRECATION")
class SplashActivity : AppCompatActivity() {
    var networkConnection = NetworkConnection()
    private lateinit var sessionManagement: SessionManagement
    private lateinit var binding: ActivitySplashBinding
    lateinit var materialProgressDialog: MaterialProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        sessionManagement = SessionManagement(applicationContext)
        materialProgressDialog = MaterialProgressDialog(this@SplashActivity)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val slideAnimation = AnimationUtils.loadAnimation(this, R.anim.bottom_slide)
        binding.headerIcon.startAnimation(slideAnimation)


        if (networkConnection.getConnectivityStatusBoolean(applicationContext)) {
            // todo end rotate
            Handler().postDelayed({
                gotoLogin()
            }, 3000)

        } else {
            var alertDialog = AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Internet Connection Alert")
                .setMessage("Please Check Your Internet Connection")
                .setPositiveButton("") { dialogInterface, i ->
                    var intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }.show()
            alertDialog.setCanceledOnTouchOutside(false)
        }

    }

    fun gotoLogin() {
        Log.e("Splash==>", "SessionId: ${sessionManagement.getSessionId(applicationContext)}\nSession Timeout: ${sessionManagement.getSessionTimeout(applicationContext)} ")
        val savedBPLID = Prefs.getString(AppConstants.BPLID, "")
        if (!sessionManagement.getSessionId(applicationContext).isNullOrEmpty() && !sessionManagement.getSessionTimeout(applicationContext).equals(null)) {

            if (savedBPLID.isEmpty()) {
                /*if (Prefs.getString(AppConstants.AppIP, "").isNotEmpty() && Prefs.getString(AppConstants.DBUrl, "").isNotEmpty()) {*/
                    Log.e("Splash==>", "gotoLogin() => second if condition to navigate to SettingActivity")
                    val intent = Intent(this@SplashActivity, SettingActivity::class.java)
                    startActivity(intent)
                /*} else {
                    Log.e("Splash==>", "gotoLogin() => second else condition to navigate to LoginActivity")
                    val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                    startActivity(intent)
                }*/

            } else {
                Log.e("Splash==>", "gotoLogin() => first else condition to navigate to HomeActivity")
                val intent = Intent(this@SplashActivity, HomeActivity::class.java)
                startActivity(intent)
            }

            finish()

        } else {
            Log.e("Splash==>", "gotoLogin() => main else condition to navigate to LoginActivity")
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}