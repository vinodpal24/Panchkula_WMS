package com.wms.panchkula.Model

class OtpErrorModel(
    val error: Error
){
    data class Error (
        val code: Int,
        val message: Message
    )

    data class Message (
        val lang: String,
        val value: String
    )
}