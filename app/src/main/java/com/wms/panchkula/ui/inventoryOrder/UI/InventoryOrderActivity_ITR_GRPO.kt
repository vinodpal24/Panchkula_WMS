package  com.wms.panchkula.ui.inventoryOrder.UI

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
import com.wms.panchkula.ui.inventoryOrder.adapter.InventoryRequestAdapter_ITR_GRPO
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.ui.invoiceOrder.UI.InventoryTransferLinesActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.Serializable

class InventoryOrderActivity_ITR_GRPO : AppCompatActivity(){

    lateinit var binding: ActivityInventoryOrderBinding
    private var requestListModel_gl: ArrayList<InventoryRequestModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var requestAdapter: InventoryRequestAdapter_ITR_GRPO? = null
    private lateinit var sessionManagement: SessionManagement
    private lateinit var searchView: SearchView

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

        materialProgressDialog = MaterialProgressDialog(this@InventoryOrderActivity_ITR_GRPO)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@InventoryOrderActivity_ITR_GRPO)

        title = "Goods Receipt PO"

        //todo loading initial list items and calling adapter-----
        Log.e("loadMoreListItems==>", "Items_loading...")
        // loadIssueOrderListItems(0)

        setInvoiceOrderAdapter()
        loadInvoiceRequestItems("")

        binding.swipeRefreshLayout.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { // Call your data refreshing method here
            searchView.setQuery("", false)         // Clear the search text
            searchView.clearFocus()
            refreshData()
            loadInvoiceRequestItems("");
        })


        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }

    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        searchView = SearchView((this@InventoryOrderActivity_ITR_GRPO).supportActionBar!!.themedContext)

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.actionView = searchView
        searchView.queryHint = "Search Here"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                loadInvoiceRequestItems(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                /*if (newText.trim().length>1){
                    handleSearch(newText)
                }*/
                return true
            }
        })

        searchView.setOnCloseListener {
            requestAdapter?.notifyDataSetChanged()
            false  // Return false to allow the default behavior of collapsing the SearchView
        }
        return true
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

    fun loadInvoiceRequestItems(docNum:String)
        {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
                val bplId = if (Prefs.getString(AppConstants.BPLID,"").isNotEmpty()) Prefs.getString(AppConstants.BPLID,"") else ""

                val networkClient = QuantityNetworkClient.create(this)   //sessionManagement.getCompanyDB(applicationContext)!!
                networkClient.getInventoryListGRPO(bplId,docNum).apply {
                    enqueue(object : Callback<InventoryRequestModel> {
                        override fun onResponse(
                            call: Call<InventoryRequestModel>,
                            response: Response<InventoryRequestModel>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    requestListModel_gl.clear()
                                    //Toast.makeText(this@InventoryOrderActivity_ITR_GRPO, "Successfully!", Toast.LENGTH_SHORT).show()
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                        Log.e("page---->", page.toString())
                                        for (item in productionList_gl) {
                                            for (item1 in item.StockTransferLines) {
                                                if (item1.RemainingOpenQuantity.toDouble() > 0.0) {
                                                    requestListModel_gl.add(item)
                                                    break // Prevents adding the same item multiple times
                                                }
                                            }
                                        }

                                        //requestListModel_gl.addAll(productionList_gl)
                                        if (requestListModel_gl.size == 0) {
                                            Toast.makeText(this@InventoryOrderActivity_ITR_GRPO, "No New Transfer Requests Found", Toast.LENGTH_SHORT).show()
                                        }


                                        requestAdapter?.notifyDataSetChanged()

                                        if (productionListModel1.value.size < 100)
                                            apicall = false

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
                                            GlobalMethods.showError(this@InventoryOrderActivity_ITR_GRPO, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@InventoryOrderActivity_ITR_GRPO, mError.error.message.value)
                                            val mainIntent = Intent(this@InventoryOrderActivity_ITR_GRPO, LoginActivity::class.java)
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
    private fun setInvoiceOrderAdapter()
        {
        layoutManager = LinearLayoutManager(this)
        binding.rvProductionList.layoutManager = layoutManager
        requestAdapter = InventoryRequestAdapter_ITR_GRPO(requestListModel_gl)
        binding.rvProductionList.adapter = requestAdapter


        //todo adapter on item click listener....
        requestAdapter?.OnItemClickListener { list, pos ->
            var productionValueList = list.get(pos).StockTransferLines
            var productionLinesList = list.get(pos).StockTransferLines
            var itemObject = list.get(pos);

            CoroutineScope(Dispatchers.IO).launch {
                val bundle = Bundle().apply { putSerializable("inventReqModel", itemObject) }
                var intent: Intent = Intent(this@InventoryOrderActivity_ITR_GRPO, InventoryTransferLinesActivity::class.java)
                intent.putExtra("productionLinesList", productionLinesList as Serializable)
                intent.putExtra("productionValueList", productionValueList as Serializable)
                intent.putExtra("InventoryReqObject", productionValueList as Serializable)
                intent.putExtras(bundle)
                startActivity(intent)

                if (productionValueList.size > 0) {
                    sessionManagement.setWarehouseCode(this@InventoryOrderActivity_ITR_GRPO, productionValueList[0].FromWarehouseCode)
                    Log.e("warehouse", "onCreate: "+sessionManagement.getWarehouseCode(this@InventoryOrderActivity_ITR_GRPO) )
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
       /* if(!requestListModel_gl.isEmpty())
            requestListModel_gl.clear()*/
    }


}