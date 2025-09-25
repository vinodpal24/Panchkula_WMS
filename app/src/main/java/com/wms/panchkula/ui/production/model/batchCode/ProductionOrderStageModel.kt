package com.wms.panchkula.ui.production.model.batchCode

import com.wms.panchkula.ui.goodsreceipt.model.IssueFromModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import java.io.Serializable

data class ProductionOrderStageModel(
    val value: ArrayList<Value>
): Serializable {
    data class Value(
        val AbsoluteEntry: String,
        val DocType: Any,
        val DocumentNumber: String,
        val FGName: String,
        val ItemNo: String,
        val PostingDate: String,
        val PlannedQuantity: String,
        val ProductionOrdersStages: ArrayList<ProductionOrdersStage>,
        val Remarks: String,
        val Warehouse: String
    ):Serializable {
        data class ProductionOrdersStage(
            val Father: String,
            val Name: String,
            val ProductionOrderLines: ArrayList<ProductionOrderLine>,
            val SequenceNumber: Any,
            val StageEntry: Any,
            val StageId: String,
            val U_AQty:Double=0.0,
            val U_RQty:Double=0.0,
            var OpenQty:Double?=0.0, // locally handle
            var AcceptQty:Double?=0.0, // locally handle
            var RejectQty:Double?=0.0, // locally handle
        ):Serializable {
            data class ProductionOrderLine(
                var isScanned : Int = 0,
                var DocumentAbsoluteEntry: Long= 0,
                var LineNumber: String ="",
                var ItemNo: String = "",
                var BaseQuantity: Double = 0.0,
                var PlannedQuantity: Double = 0.0,
                var IssuedQuantity: Double= 0.0,
                var IssueQuantity: Double= 0.0,
                var ProductionOrderIssueType: String?,
                var Batch: String? = "",
                var Serial: String? = "",
                var None: String? = "",
                var Warehouse: String= "",
                var VisualOrder: Long = 0,
                var DistributionRule: Any? = null,
                var LocationCode: Long = 0,
                var Project: Any? = null,
                var DistributionRule2: Any? = null,
                var DistributionRule3: Any? = null,
                var DistributionRule4: Any? = null,
                var DistributionRule5: Any? = null,
                var UoMEntry: Long = 0,
                var UoMCode: Long= 0,
                var WipAccount: Any? = null,
                var ItemType: String?=null,
                var LineText: Any? = null,
                var AdditionalQuantity: Double= 0.0,
                var ResourceAllocation: Any? = null,
                var StartDate: String? = null,
                var EndDate: String? = null,
                var StageID: Any? = null,
                var RequiredDays: Double= 0.0,
                var ItemName: String? = null,
                var SerialNumbers: List<Any?> = listOf(),
                var BatchNumbers: List<Any?> = listOf(),
                var BinManaged: String?=null,


                //new keys
                var DocEntry: Int = 0,
                var ItemCode: String = "",
                var ItemDescription: String = "",
                var SystemNumber: Int = 0,
                var SerialNumber: String? = null,
                var ScanType: String?=null,
                var Quantity: String = "",
                var FixedQuantity: String = "",
                var WareHouseCode: String = "",
                var UnitPrice: String = "",
                var BatchNumber: String? = "",
                var SystemSerialNumber: Long = 0,
                var InternalSerialNumber: String? = "",
                var NoneVal: String? = "",
                var U_Length: Double = 0.0,
                var U_Width: Double = 0.0,
                var U_GSM: Double = 0.0,
                var U_GW: Double = 0.0,
                var U_NW: Any? = null,
                var batchList : MutableList<ScanedOrderBatchedItems.Value> = mutableListOf(),
                var serialList : MutableList<ScanedOrderBatchedItems.Value> =  mutableListOf(),
                var noneList : MutableList<ScanedOrderBatchedItems.Value> =  mutableListOf(),
                var IssueFromModelList : MutableList<IssueFromModel.Value> = mutableListOf(),
                var binAllocationJSONs: ArrayList<PurchaseRequestModel.binAllocationJSONs>
            ): Serializable
        }
    }
}