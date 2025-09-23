package  com.wms.panchkula.ui.production.ui.rfp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.databinding.ActivityPurchaseOrderBinding
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.ui.login.LoginActivity
import com.wms.panchkula.ui.production.adapter.rfp.RFPAdapter
import com.wms.panchkula.ui.production.model.rfp.RFPResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException


class RFPActivity : AppCompatActivity() {

    lateinit var binding: ActivityPurchaseOrderBinding
    private var requestListModel_gl: ArrayList<RFPResponse.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var requestAdapter: RFPAdapter? = null
    private lateinit var sessionManagement: SessionManagement
    var page = 0
    var apicall: Boolean = true
    var isScrollingpage: Boolean = false
    var limit = 100
    var flag: String = ""
    lateinit var layoutManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)

        materialProgressDialog = MaterialProgressDialog(this@RFPActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@RFPActivity)

        title = "Production Orders"

        //todo loading initial list items and calling adapter-----
        Log.e("loadMoreListItems==>", "Items_loading...")
        // loadIssueOrderListItems(0)


        loadInvoiceRequestItems("")
        setInvoiceOrderAdapter()


        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }

        binding.swipeRefreshLayout.setOnRefreshListener(OnRefreshListener { // Call your data refreshing method here
            refreshData()
            loadInvoiceRequestItems("")
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
        val searchView = SearchView((this@RFPActivity).supportActionBar!!.themedContext)

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.actionView = searchView
        searchView.queryHint = "Search Here"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                loadInvoiceRequestItems(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                handleSearch(newText)

                return true
            }
        })

        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true // allow expansion
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                // This is called when search is closed
                loadInvoiceRequestItems("") // show all items again
                requestAdapter?.notifyDataSetChanged()
                return true // allow collapse
            }
        })

        /*searchView.setOnCloseListener {
            //Toast.makeText(this@RFPActivity,"close clicked on search view.",Toast.LENGTH_SHORT).show()
            loadInvoiceRequestItems("")
            requestAdapter?.notifyDataSetChanged()
            false  //
        }*/
        return true
    }

    //todo search filter..
    private fun handleSearch(query: String) {
        val filteredList = grpoSearchList(query)
        requestAdapter?.setFilteredItems(filteredList)
    }

    //todo this function filter issue for production list based on text...
    fun grpoSearchList(query: String): ArrayList<RFPResponse.Value> {
        val filteredList = ArrayList<RFPResponse.Value>()
        for (item in requestListModel_gl) {
            if (item.ItemNo.contains(
                    query,
                    ignoreCase = true
                ) || item.DocumentNumber.contains(query, ignoreCase = true)
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
                networkClient.getRFPList(bplId, docNum).apply {
                    enqueue(object : Callback<RFPResponse> {
                        override fun onResponse(
                            call: Call<RFPResponse>,
                            response: Response<RFPResponse>
                        ) {
                            try {
                                if (response.isSuccessful) {

                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value

                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                        binding.tvNoDataFound.visibility = View.INVISIBLE
                                        binding.rvProductionList.visibility = View.VISIBLE
                                        Log.e("page---->", page.toString())
                                        requestListModel_gl.clear()
                                        requestListModel_gl.addAll(productionList_gl)
                                        setInvoiceOrderAdapter()
                                        Log.e("List---->", "" + requestListModel_gl.size)

                                        if (requestListModel_gl.size == 0) {
                                            Toast.makeText(this@RFPActivity, "No New Transfer Requests Found", Toast.LENGTH_SHORT).show()
                                        }


                                        requestAdapter?.notifyDataSetChanged()

                                        if (productionListModel1.value.size < 100)
                                            apicall = false

                                    } else {
                                        binding.tvNoDataFound.visibility = View.VISIBLE
                                        binding.rvProductionList.visibility = View.INVISIBLE
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
                                            GlobalMethods.showError(this@RFPActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@RFPActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@RFPActivity, LoginActivity::class.java)
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

                        override fun onFailure(call: Call<RFPResponse>, t: Throwable) {
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
        requestAdapter = RFPAdapter(requestListModel_gl)
        binding.rvProductionList.adapter = requestAdapter


        //todo adapter on item click listener....
        requestAdapter?.OnItemClickListener { list, pos ->

            var productionValueList = list.get(pos)
            // var productionLinesList = list.get(pos).DocumentLines
            var itemObject = list.get(pos);

            // Create an ArrayList and add the selected object
            val itemList = ArrayList<RFPResponse.Value>().apply {
                add(itemObject)
            }

            // Print itemList before sending
            Log.d("RFPActivity", "Sending itemList: $itemList")

            CoroutineScope(Dispatchers.IO).launch {

                val bundle = Bundle().apply { putSerializable("inventReqModel", itemObject) }

                var intent: Intent = Intent(this@RFPActivity, RFPLinesActivity::class.java)
                intent.putExtra("productionLinesList", "")
                // Pass the ArrayList through the intent
                intent.putExtra("itemList", itemList)
                // Pass additional data if needed
                intent.putExtras(bundle)
                startActivity(intent)


                /*   val bundle = Bundle().apply { putSerializable("inventReqModel", itemObject) }
                   var intent: Intent = Intent(this@RFPActivity, PurchaseTransferLinesActivity::class.java)
                   intent.putExtra("productionLinesList", productionLinesList as Serializable)
                   intent.putExtra("productionValueList", productionValueList as Serializable)
                   intent.putExtra("InventoryReqObject", productionValueList as Serializable)
                 //  intent.putExtra("numAtCard",itemObject.NumAtCard )
                   intent.putExtras(bundle)
                   startActivity(intent)*/

                /*if (productionValueList.size > 0) {
                    sessionManagement.setWarehouseCode(this@RFPActivity, productionValueList[0].WarehouseCode)
                    Log.e("warehouse", "onCreate: "+sessionManagement.getWarehouseCode(this@RFPActivity) )
                }*/

            }
        }
    }


    override fun onResume() {
        super.onResume()
        if (!requestListModel_gl.isEmpty())
            requestListModel_gl.clear()
    }


}