package com.wms.panchkula.interfaces

import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems

interface PassList {
    fun passList(dataList : List<ScanedOrderBatchedItems.Value>)
}