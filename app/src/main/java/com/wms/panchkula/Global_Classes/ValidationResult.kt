package com.wms.panchkula.Global_Classes

data class ValidationResult(
    val success : Boolean,
    val errors: String,
//    val error: Error
) {
    data class Error(
        val code: Long,
        val message: Message
    )

    data class Message(
        val lang: String,
        val value: String
    )

}