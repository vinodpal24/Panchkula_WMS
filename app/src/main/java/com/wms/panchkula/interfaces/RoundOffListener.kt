package com.wms.panchkula.interfaces

interface RoundOffListener {
    fun onRoundOffUpdated(rounding: String, roundOffValue: Double)
}