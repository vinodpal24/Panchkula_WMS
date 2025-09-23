package com.wms.panchkula.ui.purchase.model

import java.io.Serializable

data class PurchasePostResponse(



    val DocEntry: String,
    val DocNum: String,
    val DocType: String,
    val DocDate: String,
    val DocDueDate: String,
    val CardCode: String,
    val NumAtCard: String,
    val DocCurrency: String,
    val Comments: String,
    val BPL_IDAssignedToInvoice: Int,
    val PayToCode: String,
    val SalesPersonCode: Int,
    val Series: Int,
    val TaxDate: String,
    val ShipToCode: String,
    val DocumentLines: List<DocumentLine>
) : Serializable

data class DocumentLine(
    val ItemCode: String,
    val ItemDescription: String,
    val Quantity: Double,
    val Price: Double,
    val TaxCode: String,
    val NetTaxAmount: String,
    val TaxTotal: String,
    val HSNEntry: String,
    val HSNCode: String? = null,
    val MeasureUnit: String,
    val VatSum: String? = null,
    val DocTotal: String? = null,
    val Currency: String,
    val DiscountPercent: Double,
    val WarehouseCode: String,
    val BaseType: String,
    val BaseEntry: String,
    val BaseLine: String,
    val ProjectCode: String? = null
) : Serializable


