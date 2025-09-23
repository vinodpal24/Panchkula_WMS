package  com.wms.panchkula.ui.inventoryOrder.oldUI

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.wms.panchkula.R
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityInventoryOrderBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.ui.inventoryOrder.Model.InventoryRequestModel
import com.wms.panchkula.ui.login.LoginActivity
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Model.ErrorModel
import com.wms.panchkula.ui.inventoryOrder.oldAdapter.InventoryRequestAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.Serializable

class InventoryOrderActivity : AppCompatActivity() {

    lateinit var binding: ActivityInventoryOrderBinding
    private var requestListModel_gl: ArrayList<InventoryRequestModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var requestAdapter: InventoryRequestAdapter? = null
    private lateinit var sessionManagement: SessionManagement
    var page = 0
    var apicall: Boolean = true
    var isScrollingpage: Boolean = false
    var limit = 100
    var flag: String = ""
    lateinit var layoutManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)

        materialProgressDialog = MaterialProgressDialog(this@InventoryOrderActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@InventoryOrderActivity)

        title = "Inventory Request"

        //todo loading initial list items and calling adapter-----
        Log.e("loadMoreListItems==>", "Items_loading...")
        // loadIssueOrderListItems(0)

        setInvoiceOrderAdapter()
        loadInvoiceRequestItems("")


        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        binding.swipeRefreshLayout.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { // Call your data refreshing method here
            refreshData()
            loadInvoiceRequestItems("");
        })

    }


    private fun refreshData() {
        // Simulate data refresh (replace with actual data fetching logic)
        Handler().postDelayed({
            // loadInvoiceRequestItems();
            // Update the adapter with new data
            requestAdapter!!.notifyDataSetChanged() // Notify your adapter of changes
            binding.swipeRefreshLayout.setRefreshing(false) // Stop the refresh animation
        }, GlobalMethods.pullRefreshTime.toLong()) // Simulate a 2-second refresh time
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        val searchView = SearchView((this@InventoryOrderActivity).supportActionBar!!.themedContext)

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.actionView = searchView
        searchView.queryHint = "Search Here"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                loadInvoiceRequestItems(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                //handleSearch(newText)

                return true
            }
        })

        searchView.setOnCloseListener {
            requestAdapter?.notifyDataSetChanged()
            false  // Return false to allow the default behavior of collapsing the SearchView
        }
        return true
    }

    //todo search filter..
    private fun handleSearch(query: String) {
        val filteredList = grpoSearchList(query)
        requestAdapter?.setFilteredItems(filteredList)
    }

    //todo this function filter issue for production list based on text...
    fun grpoSearchList(query: String): ArrayList<InventoryRequestModel.Value> {
        val filteredList = ArrayList<InventoryRequestModel.Value>()
        for (item in requestListModel_gl) {
            if (item.DocNum.contains(
                    query,
                    ignoreCase = true
                ) || item.DocEntry.contains(query, ignoreCase = true)
            ) {
                filteredList.add(item)
            }
        }

        return filteredList
    }


    // todo Open invoice list--

    fun loadInvoiceRequestItems(docNum: String) {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = QuantityNetworkClient.create(this)
                val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""

                networkClient.getInventoryList(sessionManagement.getCompanyDB(applicationContext)!!, bplId, docNum).apply {
                    enqueue(object : Callback<InventoryRequestModel> {
                        override fun onResponse(
                            call: Call<InventoryRequestModel>,
                            response: Response<InventoryRequestModel>
                        ) {
                            try {
                                if (response.isSuccessful) {

                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(this@InventoryOrderActivity, "Successfully!", Toast.LENGTH_SHORT)
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                        Log.e("page---->", page.toString())
                                        requestListModel_gl.clear()
                                        requestListModel_gl.addAll(productionList_gl)
                                        if (requestListModel_gl.size == 0) {
                                            Toast.makeText(this@InventoryOrderActivity, "No New Transfer Requests Found", Toast.LENGTH_SHORT).show()
                                        }


                                        requestAdapter?.notifyDataSetChanged()

                                        if (productionListModel1.value.size < 100)
                                            apicall = false

                                    }

                                }else if(response.code()==500){
                                    materialProgressDialog.dismiss()
                                    val gson = GsonBuilder().create()
                                    var mError: ErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson.fromJson(s, ErrorModel::class.java)

                                        GlobalMethods.showError(this@InventoryOrderActivity, mError.Message)
                                        Log.e("json_error------", mError.Message)

                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        Log.e("MSZ==>", mError.error.message.value)
                                        if (mError.error.code == 400) {
                                            GlobalMethods.showError(this@InventoryOrderActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@InventoryOrderActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@InventoryOrderActivity, LoginActivity::class.java)
                                            startActivity(mainIntent)
                                            finish()
                                        }
                                        /*if (mError.error.message.value != null) {
                                            AppConstants.showError(this@ProductionListActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }*/
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<InventoryRequestModel>, t: Throwable) {
                            Log.e("issueCard_failure-----", t.toString())
                            materialProgressDialog.dismiss()
                        }
                    })
                }

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }
    }

    //todo calling api adapter here---
    private fun setInvoiceOrderAdapter() {
        layoutManager = LinearLayoutManager(this)
        binding.rvProductionList.layoutManager = layoutManager
        requestAdapter = InventoryRequestAdapter(requestListModel_gl)
        binding.rvProductionList.adapter = requestAdapter


        //todo adapter on item click listener....
        requestAdapter?.OnItemClickListener { list, pos ->
            var productionValueList = list.get(pos).StockTransferLines
            var productionLinesList = list.get(pos).StockTransferLines
            var itemObject = list.get(pos)
            Log.e("warehouse", "onCreate:InOrder ")

            CoroutineScope(Dispatchers.IO).launch {
                val bundle = Bundle().apply { putSerializable("inventReqModel", itemObject) }
                var intent: Intent = Intent(this@InventoryOrderActivity, InventoryTransferRequestLinesActivity::class.java)
                intent.putExtra("productionLinesList", productionLinesList as Serializable)
                intent.putExtra("productionValueList", productionValueList as Serializable)
                intent.putExtra("InventoryReqObject", productionValueList as Serializable)
                intent.putExtras(bundle)
                startActivity(intent)

                if (productionValueList.size > 0) {
                    sessionManagement.setWarehouseCode(this@InventoryOrderActivity, productionValueList[0].FromWarehouseCode)

                    sessionManagement.setWarehouseCode(this@InventoryOrderActivity, productionValueList[0].FromWarehouseCode, AppConstants.FROM_WAREHOUSE)
                    sessionManagement.setWarehouseCode(this@InventoryOrderActivity, productionValueList[0].WarehouseCode, AppConstants.TO_WAREHOUSE)
                    val fromWarehouseCode = sessionManagement.getWarehouseCode(this@InventoryOrderActivity, AppConstants.FROM_WAREHOUSE)
                    val toWarehouseCode = sessionManagement.getWarehouseCode(this@InventoryOrderActivity, AppConstants.TO_WAREHOUSE)
                    Log.e("BIN_MASTER", "setInvoiceOrderAdapter: From Warehouse=> $fromWarehouseCode To Warehouse=> $toWarehouseCode")
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (!requestListModel_gl.isEmpty())
            requestListModel_gl.clear()
    }


}