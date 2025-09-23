package com.wms.panchkula.ui.scan_and_view

import com.wms.panchkula.ui.goodsreceipt.model.IssueFromModel

class ScanViewModel (
    val AvailableStock: String,
    val Batch_Lot_Serial: String,
    val ExpiryDate: String,
    val InDate: String,
    val InStock: String,
    val Commited: String,
    val ItemCode: String,
    val ItemName: String,
    val LeadTime: String,
    val Max: String,
    val MfgDate: String,
    val Min: String,
    val Ordered: String,
    val Qty: String,
    val UOM: String,
    val WhsCode: String,

    val batchdet: ArrayList<IssueFromModel.Value>
)