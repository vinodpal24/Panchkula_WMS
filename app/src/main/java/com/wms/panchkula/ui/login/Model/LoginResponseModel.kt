package com.wms.panchkula.ui.login.Model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Keep
data class LoginResponseModel(
    @SerializedName("odata.metadata") val metadata: String,
    @SerializedName("SessionId") val SessionId: String,
    @SerializedName("Version") val Version: String,
    @SerializedName("SessionTimeout") val SessionTimeout: String
): Serializable
