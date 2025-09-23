package com.wms.panchkula.ui.goodsOrder.model

import com.wms.panchkula.Model.GetQuantityModel

data class LocalListForGoods(

    var DocEntry: Int,
    var ItemCode: String,
    var ItemDescription: String,
    var Status: String? = null,
    var Batch: String?,
    var SystemNumber: Int,
    var SerialNumber: String? = null,


    //todo new keys
    var ScanType: String = "",
    var Size: String = "",
    var Quantity: String = "",
    var FixedQuantity: String = "",
    var WareHouseCode: String = "",
    var UnitPrice: String = "",
    var BatchNumber: String? = "",
    var SystemSerialNumber: Long = 0L,
    var InternalSerialNumber: String? = "",
    var NoneVal: String? = "",
    var wareHouseListing : MutableList<GetQuantityModel.Value> = mutableListOf()

    )
