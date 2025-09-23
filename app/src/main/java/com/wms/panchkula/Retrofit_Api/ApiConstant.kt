package com.wms.panchkula.Retrofit_Api



object ApiConstant {
//    internal const val BASE_URL = "http://103.107.67.172:50001/b1s/v1/" //OLD Standard BASE IP
//    internal const val BASE_URL = "http://192.168.0.218:50001/b1s/v1/"//Again Change
    internal const val BASE_URL1 = "http://115.247.228.186:50001/b1s/v1/"
    internal const val QUANTITY_BASE_URL1 = "http://115.247.228.186:8080/api/"



    internal const val LOGIN = "Login"
    internal const val LOGOUT = "Logout"
    internal const val USER = "User"
    internal const val UserManagement = "UserManagement"
    internal const val GET_SERIES = "GetSeries"

    /****** Branch  *****/ // added by Vinod Pal @21Apr,2025
    internal const val GET_ALL_BRANCHES = "GetBranchList"

    //todo ISSUE ORDER API..
    internal const val PRODUCTION_ORDERS = "Values/ProductionOrderPagination"
    internal const val PRODUCTION_ORDER = "Values/Production"
    internal const val PRODUCTION_ORDER_UPDATE = "ProductionOrders" //added by Vinod @19 Sept, 2025
    internal const val PRODUCTION_ORDER_LIST = "Values/ProductionList" //added by Vinod @18 Sept, 2025
    internal const val INVENTORY_GEN_EXITS = "InventoryGenExits"
    internal const val INVENTORY_GEN_ENTRIES = "InventoryGenEntries"
    internal const val BATCH_NUMBER_DETAILS = "BatchNumberDetails"
    internal const val SCAN_AND_VIEW = "WareHouseQuantity/ScanAndView"
    internal const val SERIAL_NUMBER_DETAILS = "SerialNumberDetails"
    internal const val BPLID_WAREHOUSE = "Warehouses"
    internal const val GetBranch = "GetBranch"
    internal const val GetBin = "GetBin"
    internal const val RETURN_COMPONENT = "Values/ReturnFromProduction"


    internal const val GetBatchFromIssueFromProd = "GetBatchFromIssueFromProd"

    //todo DELIVERY ORDER API..
    internal const val DELIVERY_ORDER = "Orders"
    internal const val DELIVERY_NOTES = "DeliveryNotes"

    //todo Purchase ORDER API..
    internal const val Inventory_List    = "GRPOInventroyTransferRequestList"  //  InventroyTransferRequestList
    internal const val Inventory_List_Rq = "InventroyTransferRequestList"  //  InventroyTransferRequestList
    internal const val GoodReceiptsItem  = "GetItems"
    //todo INVENTORY ORDER API..
    internal const val purchase_order_list = "PurchaseOrder/PurchaseOrder"
    internal const val GET_CUSTOMER = "GetCust"
    internal const val RFP_list = "Values/ProductionForRFPList"
    internal const val GRPO_Posting = "PurchaseDelivery/PurchaseOrderDelivery"
    internal const val RFP_Posting = "InventoryGenEntries"
    internal const val ARDraft_Posting = "ARDraft"
    internal const val Draft_Posting = "Drafts"

    internal const val PickList_Posting = "PickLists"

    internal const val sale_to_invoice_list = "GetSalesList"
    internal const val SALE_TO_INVOICES_POSTING = "Invoices"

    internal const val STOCK_TRANSFER = "StockTransfers"
    internal const val GoodsReceipt_TRANSFER = "GoodsReceiptPost"


    const val MEDIA_TYPE_JSON = "application/json; charset=utf-8"

    //todo GET QUANTITY ----

    const val GET_QUANTITY = "SaleInvoice/GetQuantity?"
    const val GET_SUGGESTION_QUANTITY = "DatabaseListAPI/GetBatchQuantity?"

    const val GET_WAREHOUSE_QUANTITY = "WareHouseQuantity/WareHouseQuantity?"

    const val GOODS_ISSUE_SERIES = "General/GoodsIssueSeries?"
    const val WareHouseBin = "binMaster?"
    const val DEFAULT_TO_BIN = "defaultToBin"
    const val WAREHOUSE_BIN_LOCATION="binMasterRequest"

    const val TAX_LIST = "TaxList"
    const val FREIGHT_MASTER = "FreightMaster"

    /********** location api  ***********/
    const val GET_LOCATION = "GetLocation" // added by Vinod Pal @29May,2025
    const val GET_WAREHOUSE = "GetWareHouse" // added by Vinod Pal @07Jul,2025
    const val GET_SYSTEM_BIN = "GetSystemBin" // added by Vinod Pal @07Jul,2025

}