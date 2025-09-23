package com.wms.panchkula.Retrofit_Api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.ui.login.LoginActivity
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import okhttp3.Authenticator
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

object QuantityNetworkClient {

    private const val TIMEOUT_MINUTES = 5L

    @Volatile
    var BASE_URL: String = "" // Ensure visibility across threads

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private val gsonConverterFactory: GsonConverterFactory by lazy {
        GsonConverterFactory.create(
            GsonBuilder()
                .setLenient()
                .disableHtmlEscaping()
                .create()
        )
    }

    // Utility to build a fresh OkHttpClient.Builder each time
    private fun newOkHttpBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .readTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .writeTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES)
    }

    // Extension function to avoid duplicate interceptors
    private fun OkHttpClient.Builder.addIfAbsent(interceptor: Interceptor): OkHttpClient.Builder {
        if (!this.interceptors().any { it.javaClass == interceptor.javaClass }) {
            this.addInterceptor(interceptor)
        }
        return this
    }

    /**
     * Update base URL dynamically from configuration
     */
    fun updateBaseUrlFromConfig(config: ApiConstantForURL) {
        BASE_URL = config.QUANTITY_BASE_URL
        //BASE_URL = "http://103.197.77.85:9090/api/"
        Log.d("Updated BASE_URL", "Standard Base Url => ${BASE_URL}")
    }

    /**
     * Creates an OkHttpClient without session headers (used for login or unauthenticated calls)
     */
    private fun getBaseClient(): OkHttpClient {
        return newOkHttpBuilder()
            .addIfAbsent(loggingInterceptor)
            .build()
    }

    /**
     * Creates an OkHttpClient with session headers
     */
    private fun getAuthenticatedClient(context: Context, sessionManagement: SessionManagement): OkHttpClient {
        return newOkHttpBuilder()
            .addIfAbsent(loggingInterceptor)
            .addIfAbsent(createSessionCookieInterceptor(context, sessionManagement))
            .authenticator(SessionAuthenticator(context, sessionManagement))
            .build()
    }

    /**
     * Session interceptor to manage session cookies and timeout checks
     */
    private fun createSessionCookieInterceptor(context: Context, sessionManagement: SessionManagement): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val sessionId = sessionManagement.getSessionId(context) // Get current session ID

            // Only add the cookie if a session ID exists
            val requestBuilder = original.newBuilder().apply {
                if (!sessionId.isNullOrEmpty()) {
                    addHeader("Cookie", "B1SESSION=$sessionId")
                }
                header("content-type", "application/json") // Ensure JSON content type
                cacheControl(CacheControl.FORCE_NETWORK) // Prevent stale cached responses
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }

    fun create(context: Context): NetworkInterface {
        val sessionManagement = SessionManagement(context) // Initialize session management
        // Determine if the client should be authenticated or not.
        // "Login" implies the user is not yet authenticated, so use the base client.
        // Any other status implies authenticated, so use the client with authenticator.
        val isCurrentlyLoggedIn = sessionManagement.getFromWhere(context) != "Login"

        val okHttpClient = if (isCurrentlyLoggedIn) {
            getAuthenticatedClient(context, sessionManagement)
        } else {
            getBaseClient()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL) // Use the dynamically updated base URL
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // For RxJava support
            .addConverterFactory(gsonConverterFactory) // For JSON serialization/deserialization
            .client(okHttpClient) // Set the configured OkHttpClient
            .build()

        return retrofit.create(NetworkInterface::class.java)
    }

    /**
     * OkHttp Authenticator to handle 401 (Unauthorized) or 301 (Moved Permanently, as per your backend)
     * responses by attempting to refresh the session token and re-attempting the original request.
     */
    class SessionAuthenticator(
        private val context: Context,
        private val sessionManagement: SessionManagement
    ) : Authenticator {

        // Using a ReentrantLock to ensure that only one thread attempts to refresh the token at a time.
        // This prevents multiple simultaneous refresh calls if many requests fail concurrently.
        private val lock = ReentrantLock()

        // A separate OkHttpClient instance for the refresh token API call.
        // This client MUST NOT include this Authenticator itself to prevent a recursive loop
        // (Authenticator calling itself when trying to refresh).
        private val refreshClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .readTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .writeTimeout(TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }) // Still log refresh calls
                .build()
        }

        /**
         * This method is called by OkHttp when a request receives a 401 or 301 response.
         * It attempts to re-authenticate and provide a new request.
         */
        override fun authenticate(route: Route?, response: Response): Request? {
            // Only proceed if the response code indicates an authentication issue.
            if (response.code != 401 && response.code != 301) {
                return null // Not an authentication problem, so no re-authentication attempt
            }

            // Acquire the lock to ensure only one thread performs the refresh operation.
            lock.lock()
            try {
                // Get the current session ID *after* acquiring the lock.
                // This is crucial: if another thread refreshed the token while we were waiting for the lock,
                // we should use the new token directly instead of attempting to refresh again.
                val currentSessionId = sessionManagement.getSessionId(context)

                // Check if the request that failed already used a stale token, and if a new one is available.
                // If the current request's cookie is different from the session management's current ID,
                // it means another thread already refreshed and updated the session ID.
                val originalSessionIdInRequest = response.request.header("Cookie")
                    ?.split(";")
                    ?.find { it.trim().startsWith("B1SESSION=") }
                    ?.split("=")
                    ?.getOrNull(1)

                if (currentSessionId != null && originalSessionIdInRequest != currentSessionId) {
                    // Token already refreshed by another thread. Retry the original request with the new token.
                    Log.d("SessionAuthenticator", "Session ID already refreshed by another thread. Retrying original request with new ID.")
                    return response.request.newBuilder()
                        .header("Cookie", "B1SESSION=$currentSessionId")
                        .build()
                }

                // If we reach here, it means this thread is responsible for refreshing the session.
                Log.d("SessionAuthenticator", "Session expired (401/301 received). Attempting to refresh...")

                // Attempt to refresh the session by making a synchronous API call.
                val newSessionId = refreshSession()

                if (newSessionId != null) {
                    // If refresh is successful, save the new session ID.
                    sessionManagement.setSessionId(context,newSessionId)
                    sessionManagement.setFromWhere(context,"Authenticated") // Mark as authenticated again
                    Log.d("SessionAuthenticator", "Session refreshed successfully. New Session ID: $newSessionId")

                    // Return a new request with the updated session ID header.
                    // OkHttp will then automatically retry the original failed request with this new token.
                    return response.request.newBuilder()
                        .header("Cookie", "B1SESSION=$newSessionId")
                        .build()
                } else {
                    // If refresh fails for any reason, log out the user.
                    Log.e("SessionAuthenticator", "Failed to refresh session. Redirecting to login.")
                    logoutScreen(context)
                    return null // Do not retry the request; it means re-authentication is not possible
                }
            } finally {
                lock.unlock() // Always release the lock
            }
        }

        /**
         * **IMPORTANT: Implement your actual session refresh API call here.**
         * This method makes a synchronous API call (e.g., a login call) to obtain a new session ID.
         *
         * @return The new session ID if the refresh is successful, otherwise null.
         */
        private fun refreshSession(): String? {
            // IMPORTANT: Replace this with your actual session refresh logic.
            // This typically involves making a login call with stored credentials
            // (username/password) or a refresh token.

            val username = sessionManagement.getUsername(context)
            val password = sessionManagement.getSapPassword(context)
            val dbName = sessionManagement.getCompanyDB(context)

            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                Log.e("SessionAuthenticator", "No stored credentials available for session refresh.")
                return null
            }

            try {
                // Create a temporary Retrofit instance for the refresh call.
                // This instance uses 'refreshClient' which does NOT have this Authenticator,
                // preventing a recursive loop.
                val refreshRetrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL) // Use the same base URL
                    .addConverterFactory(gsonConverterFactory)
                    .client(refreshClient) // Use the client without the authenticator
                    .build()

                // Get an instance of your NetworkInterface (or a dedicated login service)
                val refreshService = refreshRetrofit.create(NetworkInterface::class.java)

                // Prepare your login request body. Adjust keys and values based on your API.
                var jsonObject = JsonObject()
                jsonObject.addProperty("CompanyDB", dbName)
                jsonObject.addProperty("Password", password)
                jsonObject.addProperty("UserName", "manager")

                // Execute the login call SYNCHRONOUSLY.
                // DO NOT use suspend functions directly here if not in a coroutine scope,
                // or use .await() on a Call<T> if you're using Kotlin's suspend.
                // For Authenticator, .execute() is the correct synchronous way.
                val response = refreshService.doGetLoginCall(jsonObject).execute()

                if (response.isSuccessful) {
                    // Parse the response to extract the new session ID.
                    // Your API might return the session ID in a 'Set-Cookie' header,
                    // or within the JSON response body. Adjust parsing accordingly.
                    val newSessionId = response.headers()["Set-Cookie"]
                        ?.split(";")
                        ?.find { it.trim().startsWith("B1SESSION=") }
                        ?.split("=")
                        ?.getOrNull(1) // Get the value after '='

                    if (newSessionId.isNullOrEmpty()) {
                        Log.e("SessionAuthenticator", "Refresh successful, but no B1SESSION cookie found in response.")
                        return null
                    }
                    return newSessionId
                } else {
                    Log.e("SessionAuthenticator", "Refresh login failed: ${response.code()} - ${response.message()}")
                    val errorBody = response.errorBody()?.string()
                    if (!errorBody.isNullOrEmpty()) {
                        Log.e("SessionAuthenticator", "Refresh error body: $errorBody")
                    }
                    return null
                }
            } catch (e: Exception) {
                Log.e("SessionAuthenticator", "Error during session refresh: ${e.message}", e)
                return null
            }
        }

    }

    /**
     * Redirect to LoginActivity and clear activity stack
     */
    private fun logoutScreen(context: Context) {
        val mainIntent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.applicationContext.startActivity(mainIntent)
        (context as? Activity)?.finish()
    }

}