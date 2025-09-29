package com.wms.panchkula.Retrofit_Api

import com.wms.panchkula.Model.*
import com.wms.panchkula.ui.deliveryOrderModule.Model.DeliveryModel
import com.wms.panchkula.ui.goodsOrder.model.GoodsIssueSeriesModel
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryPostResponse
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryRequestModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.InventoryGenExitsModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.wms.panchkula.ui.issueForProductionOrder.Model.WarehouseBPL_IDModel
import com.wms.panchkula.ui.login.Model.LoginResponseModel
import com.wms.panchkula.ui.scan_and_view.ScanViewModel
import com.google.gson.JsonObject
import com.wms.panchkula.ui.goodsreceipt.model.GetItemstModel
import com.wms.panchkula.ui.goodsreceipt.model.IssueFromModel
import com.wms.panchkula.ui.inventoryOrder.Model.BinLocationModel
import com.wms.panchkula.ui.inventoryOrder.Model.DefaultToBinLocationModel
import com.wms.panchkula.ui.inventoryTransfer.model.Warehouse_BPLID
import com.wms.panchkula.ui.pickList.model.PickListsResponse
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderStageModel
import com.wms.panchkula.ui.production.model.batchCode.StageStatusUpdateRequest
import com.wms.panchkula.ui.production.model.batchCode.StageUpdateRequest
import com.wms.panchkula.ui.production.model.rfp.RFPResponse
import com.wms.panchkula.ui.purchase.model.FreightTypeModel
import com.wms.panchkula.ui.purchase.model.ModelCustomers
import com.wms.panchkula.ui.purchase.model.PurchasePostResponse
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import com.wms.panchkula.ui.purchase.model.SystemBinModel
import com.wms.panchkula.ui.purchase.model.TaxListModel
import com.wms.panchkula.ui.setting.model.ModelGetBranch
import com.wms.panchkula.ui.setting.model.ModelValidateUser
import retrofit2.Call
import retrofit2.http.*

interface NetworkInterface {

    //todo login API---
    @POST(ApiConstant.LOGIN)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun doGetLoginCall(@Body jsonObject: JsonObject): Call<LoginResponseModel>

    @GET(ApiConstant.USER)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun validateUser(
        @Query("UserName") userName: String,
        @Query("Password") password: String
    ): Call<ModelValidateUser>

    @POST(ApiConstant.LOGIN)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun doGetLoginWithSap(@Body jsonObject: JsonObject): Call<LoginResponseModel>

    @POST(ApiConstant.LOGIN)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun doGetLoginWithWms(@Body jsonObject: JsonObject): Call<LoginResponseModel>

    // todo dblist api
    @GET("DatabaseListAPI/Database")
    fun getDatabaseList(): Call<DBNameListModel>
//    @GET("DatabaseListAPI/Database")
//    fun getDatabaseList(@Query("DBName") dbName: String): Call<DBNameListModel>


    @POST(ApiConstant.LOGOUT)
    fun doGetLogoutCall(
        @Header("Cookie") cookieToken: String
    ): Call<LoginResponseModel>

    //todo production list api...
    @GET(ApiConstant.PRODUCTION_ORDER)
    fun doGetProductionList(
        @Query("DBName") DBName: String,
        @Query("BPLId") BPLId: String,
        @Query("DocNum") docNum: String
    ): Call<ProductionListModel>

    @GET(ApiConstant.PRODUCTION_ORDER_LIST)
    fun doGetProductionListByQr(
        @Query("DBName") DBName: String,
        @Query("BPLId") BPLId: String,
        @Query("DocNum") docNum: String
    ): Call<ProductionOrderStageModel>


    //todo production list api...
    @GET(ApiConstant.RETURN_COMPONENT)
    fun doGetReturnComponentList(
        @Query("DBName") DBName: String,
        @Query("BPLId") BPLId: String,
        @Query("DocNum") docNum: String
    ): Call<ProductionListModel>

    @GET(ApiConstant.PRODUCTION_ORDER)
    fun doGetProductionListPagination(
        @Query("DBName") DBName: String,
        @Query("FromNo") FromNo: String,
        @Query("ToNo") ToNo: String,
        @Query("BPLId") BPLId: String
    ): Call<ProductionListModel>

    //todo production list api test...
    @GET(ApiConstant.PRODUCTION_ORDERS)
    fun doGetProductionListCount(
        //  @Header("Prefer") Prefer :String,
        @Query("DBName") DBName: String,
        @Query("FromNo") FromNo: String,
        @Query("ToNo") ToNo: String,
//        @Query("$"+"orderby") orderby: String
    ): Call<ProductionListModel>

    //todo login API---
    @POST(ApiConstant.INVENTORY_GEN_EXITS)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun doGetInventoryGenExits(@Body jsonObject: JsonObject): Call<InventoryGenExitsModel>

    @POST(ApiConstant.INVENTORY_GEN_ENTRIES)
    @Headers("Content-Type:application/json;charset=UTF-8")
    fun doGetInventoryGenEntries(@Body jsonObject: JsonObject): Call<InventoryGenExitsModel>

    //todo scan item lines api...
    @GET(ApiConstant.BATCH_NUMBER_DETAILS)
    fun doGetBatchNumScanDetails(
        @Query("$" + "filter") filter: String,
    ): Call<ScanedOrderBatchedItems>

    @GET(ApiConstant.SCAN_AND_VIEW)
    fun doScanAndView(
        @Query("DBName") DBName: String,
        @Query("BatchSerial") BatchSerial: String,
        @Query("ItemCode") ItemCode: String,
        @Query("WhsCode") WhsCode: String,
        @Query("ItemType") ItemType: String,
        @Query("BPLId") BPLId: String?,
        @Query("Location") location: String?
    ): Call<ScanViewModel>

    @GET(ApiConstant.GET_LOCATION)
    fun getLocationForScanView(
        @Query("BPLId") BPLId: String?
    ): Call<ModelWarehouseLocation>


    //todo serial scan item lines api...
    @GET(ApiConstant.SERIAL_NUMBER_DETAILS)
    fun doGetSerialNumScanDetails(
        @Query("$" + "filter") filter: String,
    ): Call<ScanedOrderBatchedItems>

    //todo get BPL id api...
    @GET(ApiConstant.BPLID_WAREHOUSE)
    fun doGetBplID(
        @Query("$" + "select") select: String,
        @Query("$" + "filter") filter: String,
    ): Call<WarehouseBPL_IDModel>


    @GET(ApiConstant.GetBin)
    fun getBin(
        @Query("ItemCode") ItemCode: String,
        @Query("WhsCode") WhsCode: String,
        @Query("DistNumber") DistNumber: String
    ): Call<GetAbsModel>


    @GET(ApiConstant.GetBranch)
    fun doGetBPLID(
        @Query("Warehouse") DBName: String,
        @Query("DocType") DocType: String,
        @Query("BPLId") BPLId: String
    ): Call<Warehouse_BPLID>

    @GET(ApiConstant.GetBatchFromIssueFromProd)
    fun GetBatchFromIssueFromProd(
        @Query("ItemCode") DBName: String,
        @Query("ProdDocEntry") ItemCode: String,
        @Query("ItemType") ItemType: String
    ): Call<IssueFromModel>

    //todo get delivery order list..
    @GET(ApiConstant.DELIVERY_ORDER)
    fun deliveryOrder(
        @Query("$" + "filter") filter: String,
        @Query("$" + "orderby") orderby: String,
    ): Call<DeliveryModel>

    //todo post delivery order items...
    @POST(ApiConstant.DELIVERY_NOTES)
    fun doGetDeliveryNotes(@Body jsonObject: JsonObject): Call<InventoryGenExitsModel>


    //todo get quantity values--
    @GET(ApiConstant.GET_QUANTITY)
    fun getQuantityValue(
        @Query("DBName") DBName: String,
        @Query("Batch") Batch: String,
        @Query("ItemCode") ItemCode: String,
        @Query("WhsCode") Warehouse: String

    ): Call<GetQuantityModel>

    @GET(ApiConstant.GET_SUGGESTION_QUANTITY)
    fun getQuantityForSuggestion(
        @Query("DBName") DBName: String,
        @Query("BatchInDate") Quantity: String,
        @Query("ItemCode") ItemCode: String,
        @Query("WhsCode") Warehouse: String

    ): Call<GetSuggestionQuantity>


    //todo get quantity values--
    @GET(ApiConstant.Inventory_List)
    fun getInventoryListGRPO(
        @Query("BPLId") BPLId: String,
        @Query("DocNum") docNum: String
    ): Call<InventoryRequestModel>

    @GET(ApiConstant.GET_ALL_BRANCHES)
    fun getBranchList(): Call<ModelGetBranch>

    @GET(ApiConstant.Inventory_List_Rq)
    fun getInventoryList(
        @Query("DBName") DBName: String,
        @Query("BPLId") BPLId: String,
        @Query("DocNum") docNum: String
    ): Call<InventoryRequestModel>

    @GET(ApiConstant.GoodReceiptsItem)
    fun GoodReceiptsItems(
        @Query("ItemName") DBName: String,
        @Query("BPLId") BPLId: String

    ): Call<GetItemstModel>


    @POST(ApiConstant.STOCK_TRANSFER)
    fun dostockTransfer(@Body jsonObject: JsonObject): Call<InventoryPostResponse>

    @POST(ApiConstant.GoodsReceipt_TRANSFER)
    fun doGoodsReceiptTransfer(@Body jsonObject: JsonObject): Call<InventoryPostResponse>

    //todo get POs values--
    @GET(ApiConstant.purchase_order_list)
    fun getPurchaseOrderList(
        @Query("BPLId") BPLId: String,
        @Query("CardCode") cardCode: String,
        @Query("DocNum") docNum: String
    ): Call<PurchaseRequestModel>

    @GET(ApiConstant.GET_CUSTOMER)
    fun getCustomerSearch(
        @Query("CardName") cardName: String
    ): Call<ModelCustomers>

    @GET(ApiConstant.RFP_list)
    fun getRFPList(
        @Query("BPLId") BPLId: String,
        @Query("DocNum") docNum: String
    ): Call<RFPResponse>

    // todo get Sale to invoice
    @GET(ApiConstant.sale_to_invoice_list)
    fun getSaleToInvoiceList(@Query("BPLId") BPLId: String): Call<PurchaseRequestModel>

    @Headers("Content-Type: application/json")
    @POST(ApiConstant.GRPO_Posting)
    fun GRPO_Posting(@Body jsonObject: JsonObject): Call<PurchasePostResponse>  //InventoryGenEntries

    @POST(ApiConstant.RFP_Posting)
    fun RFP_Posting(@Body jsonObject: JsonObject): Call<PurchasePostResponse>

    @POST(ApiConstant.ARDraft_Posting)
    fun ARDraft_Posting(@Body jsonObject: JsonObject): Call<PurchasePostResponse>

    @POST(ApiConstant.Draft_Posting)
    fun Draft_Posting(@Body jsonObject: JsonObject): Call<PurchasePostResponse>

    @POST(ApiConstant.PickList_Posting)
    fun PickList_Posting(@Body jsonObject: JsonObject): Call<PickListsResponse>


    @GET(ApiConstant.WareHouseBin)
    fun getAbsWithWareHouseCode(
        @Query("whscode") DBName: String

    ): Call<GetAbsModel>

    //todo  get quantity values with warehouse code--
    @GET(ApiConstant.GET_WAREHOUSE_QUANTITY)
    fun getQuantityGoodsWithWareHouseCode(
        @Query("DBName") DBName: String,
        @Query("ItemCode") ItemCode: String,
        @Query("ItemType") itemType: String,
        @Query("BatchOrSerial") BatchOrSerial: String,

        ): Call<GetQuantityModel>


    //todo get quantity values with warehouse code--
    @GET(ApiConstant.GOODS_ISSUE_SERIES)
    fun getGoodsIssueSeries(
        @Query("DBName") DBName: String,
    ): Call<GoodsIssueSeriesModel>

    @POST(ApiConstant.DELIVERY_NOTES)
    fun postDeliveryDocument(@Body jsonObject: JsonObject): Call<PurchasePostResponse>

    @POST(ApiConstant.SALE_TO_INVOICES_POSTING)
    fun postSalesInvoiceDocument(@Body jsonObject: JsonObject): Call<PurchasePostResponse>

    @GET(ApiConstant.UserManagement)
    fun userAccessMgmt(
        @Query("User") user: String

    ): Call<ModelDashboardItem>

    @GET(ApiConstant.GET_SERIES)
    fun getDocSeries(
        @Query("BPLId") bplid: String,
        @Query("ObjCode") docObjectCode: String,
    ): Call<ModelSeries>

    @GET(ApiConstant.TAX_LIST)
    fun getTaxList(): Call<TaxListModel>

    @GET(ApiConstant.FREIGHT_MASTER)
    fun getFreightType(): Call<FreightTypeModel>

    @GET(ApiConstant.WAREHOUSE_BIN_LOCATION)
    fun getBinLocationByWarehouse(
        @Query("WhsCode") whsCode: String,
        @Query("ItemCode") itemCode: String,
        @Query("DistNumber") batch: String
    ): Call<BinLocationModel>

    @GET(ApiConstant.DEFAULT_TO_BIN)
    fun getToBinLocationByWarehouse(
        @Query("whscode") whsCode: String
    ): Call<DefaultToBinLocationModel>

    @GET(ApiConstant.GET_WAREHOUSE)
    fun getWarehouse(): Call<GetWarehouseModel>

    @GET(ApiConstant.GET_SYSTEM_BIN)
    fun getSystemBinByWarehouse(
        @Query("WhsCode") whsCode: String
    ): Call<SystemBinModel>

    @PATCH("${ApiConstant.PRODUCTION_ORDER_UPDATE}({DocEntry})")
    fun updateProductionOrderStage(
        @Path("DocEntry") docEntry: String,
        @Body stageQtyUpdate: StageUpdateRequest
    ): Call<Void>

    @PATCH("${ApiConstant.PRODUCTION_ORDER_UPDATE}({DocEntry})")
    fun updateStageStatus(
        @Path("DocEntry") docEntry: String,
        @Body stageStatusUpdate: StageStatusUpdateRequest
    ): Call<Void>

}