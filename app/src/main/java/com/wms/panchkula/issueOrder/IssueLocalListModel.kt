package com.wms.panchkula.issueOrder

import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems

data class IssueLocalListModel(
    var DocEntry: Int = 0,
    var ItemCode: String = "",
    var ItemDescription: String = "",
    var Status: String? = null,
    var Batch: String? = null,
    var SystemNumber: Int = 0,
    var SerialNumber: String? = null,

    var batchList : MutableList<ScanedOrderBatchedItems.Value> = mutableListOf(),
    var serialList : MutableList<ScanedOrderBatchedItems.Value> =  mutableListOf(),
    var noneList : MutableList<ScanedOrderBatchedItems.Value> =  mutableListOf(),

    //todo new keys
    var ScanType: String = "",
    var Quantity: String = "",
    var FixedQuantity: String = "",
    var WareHouseCode: String = "",
    var UnitPrice: String = "",
    var BatchNumber: String? = "",
    var SystemSerialNumber: Long = 0L,
    var InternalSerialNumber: String? = "",
    var NoneVal: String? = "",

    var U_Length: Double = 0.0,
    var U_Width: Double = 0.0,
    var U_GSM: Double = 0.0,
    var U_GW: Double = 0.0,
    var U_NW: Any? = null,
    var U_Type: String = "",
    var U_AGSM: Double = 0.0,
    var U_GType: Any? = null,
    var U_RQ: Double = 0.0,
    var U_PC: Any? = null,
    var U_RG: String = "",


    var ItemName : String = "",

    var WhsCode : String = "",

    var Qty : String = "",

)
