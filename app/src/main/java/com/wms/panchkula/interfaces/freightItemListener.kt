package com.wms.panchkula.interfaces

import com.wms.panchkula.ui.purchase.model.FreightDataModel

interface freightItemListener {
    fun onDataUpdated(freightDataList: ArrayList<FreightDataModel.DocumentAdditionalExpenses>)
}