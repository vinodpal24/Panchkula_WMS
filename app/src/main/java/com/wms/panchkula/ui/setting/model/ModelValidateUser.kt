package com.wms.panchkula.ui.setting.model


import com.google.gson.annotations.SerializedName

data class ModelValidateUser(
    @SerializedName("value")
    var value: List<Value>? = listOf()
) {
    data class Value(
        @SerializedName("isAdmin")
        var isAdmin: String? = "",
        @SerializedName("validUser")
        var validUser: String? = "",
        @SerializedName("Password")
        var password: String? = ""
    )
}