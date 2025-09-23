package com.wms.panchkula.ui.returnComponents.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityReturnComponentListBinding
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.login.LoginActivity
import com.wms.panchkula.ui.returnComponents.adapter.ReturnComponentsIssueOderAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.Serializable


class ReturnComponentListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReturnComponentListBinding
    lateinit var materialProgressDialog: MaterialProgressDialog
    private lateinit var sessionManagement: SessionManagement
    private var productionListModel_gl: ArrayList<ProductionListModel.Value> = ArrayList()
    private var returnComponentsIssueOderAdapter: ReturnComponentsIssueOderAdapter? = null
    private lateinit var layoutManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReturnComponentListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        clickListeners()
    }

    private fun initViews() {
        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = "Return Components"
        materialProgressDialog = MaterialProgressDialog(this@ReturnComponentListActivity)
        sessionManagement = SessionManagement(this@ReturnComponentListActivity)
        callReturnComponentListApi("")
        setReturnComponentsAdapter()
    }

    private fun setReturnComponentsAdapter() {
        /*layoutManager = LinearLayoutManager(this)
        binding.rvReturnComponents.layoutManager = layoutManager
        returnComponentsIssueOderAdapter = ReturnComponentsIssueOderAdapter(productionListModel_gl)
        binding.rvReturnComponents.adapter = returnComponentsIssueOderAdapter
        returnComponentsIssueOderAdapter?.notifyDataSetChanged()*/


        if (returnComponentsIssueOderAdapter == null) {
            layoutManager = LinearLayoutManager(this)
            binding.rvReturnComponents.layoutManager = layoutManager
            returnComponentsIssueOderAdapter = ReturnComponentsIssueOderAdapter(productionListModel_gl)
            binding.rvReturnComponents.adapter = returnComponentsIssueOderAdapter

            // set click listener once here
            returnComponentsIssueOderAdapter?.OnItemClickListener { list, pos ->
                Log.e("warehouse==>", "" + pos)
                var productionValueItem = list[pos]
                var productionLinesList = list[pos].ProductionOrderLines
                Log.e("warehouse==>", "" + productionLinesList.size)

                CoroutineScope(Dispatchers.IO).launch {
                    var intent: Intent = Intent(this@ReturnComponentListActivity, ReturnComponentLinesActivity::class.java)
                    intent.putExtra("productionLinesList", productionLinesList as Serializable)
                    intent.putExtra("productionValueItem", productionValueItem as Serializable)
                    intent.putExtra("pos", pos)
                    startActivity(intent)

                    withContext(Dispatchers.Main) {

                        if (productionLinesList.size > 0) {
                            sessionManagement.setWarehouseCode(this@ReturnComponentListActivity, productionLinesList[0].Warehouse)
                        }

                        /*else {
                        sessionManagement.setWarehouseCode(this@ProductionListActivity, productionValueList.Warehouse)
                            }*/


                    }
                }
            }
        } else {
            returnComponentsIssueOderAdapter?.notifyDataSetChanged()
        }
    }

    private fun callReturnComponentListApi(docNum: String) {
        materialProgressDialog.show()
        var apiConfig = ApiConstantForURL()

        NetworkClients.updateBaseUrlFromConfig(apiConfig)
        QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
        val networkClient = QuantityNetworkClient.create(this)
        val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""

        networkClient.doGetReturnComponentList(sessionManagement.getCompanyDB(this)!!, bplId, docNum).apply { // "odata.maxpagesize=" + 100, "ProductionOrderStatus eq '" + "boposReleased" + "'","" + skip,
            // "AbsoluteEntry desc"
            enqueue(object : Callback<ProductionListModel> {
                override fun onResponse(call: Call<ProductionListModel>, response: Response<ProductionListModel>) {
                    try {
                        if (response.isSuccessful) {
                            Log.e("api_hit_response===>", response.toString())
                            materialProgressDialog.dismiss()
                            var productionListModel1 = response.body()!!
                            var productionList_gl = productionListModel1.value

                            if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                Log.e("List Size--->", productionListModel1.value.size.toString())
                                productionListModel_gl.clear()
                                productionListModel_gl.addAll(productionList_gl)
                                returnComponentsIssueOderAdapter?.notifyDataSetChanged()

                            }

                        } else {
                            materialProgressDialog.dismiss()
                            val gson1 = GsonBuilder().create()
                            var mError: OtpErrorModel
                            try {
                                val s = response.errorBody()!!.string()
                                mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                if (mError.error.code == 400) {
                                    GlobalMethods.showError(this@ReturnComponentListActivity, mError.error.message.value)
                                }
                                if (mError.error.code == 306 && mError.error.message.value != null) {
                                    GlobalMethods.showError(this@ReturnComponentListActivity, mError.error.message.value)
                                    val mainIntent = Intent(this@ReturnComponentListActivity, LoginActivity::class.java)
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

                override fun onFailure(call: Call<ProductionListModel>, t: Throwable) {
                    Log.e("issueCard_failure-----", t.toString())
                    materialProgressDialog.dismiss()
                }
            })
        }
    }

    private fun clickListeners() {

        binding.swipeRefreshLayout.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { // Call your data refreshing method here
            refreshData()
            callReturnComponentListApi("")
        })
    }

    private fun refreshData() {
        // Simulate data refresh (replace with actual data fetching logic)
        returnComponentsIssueOderAdapter!!.notifyDataSetChanged() // Notify your adapter of changes
        binding.swipeRefreshLayout.setRefreshing(false) // Stop the refresh animation

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        val searchView = SearchView((this@ReturnComponentListActivity).supportActionBar!!.themedContext)

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.actionView = searchView
        searchView.queryHint = "Search Here"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                callReturnComponentListApi(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                handleSearch(newText)
                return true
            }
        })
        return true
    }

    //todo search filter..
    private fun handleSearch(query: String) {

        val filteredList = issueSearchList(query)
        returnComponentsIssueOderAdapter?.setFilteredItems(filteredList)

    }

    //todo this function filter issue for production list based on text...
    fun issueSearchList(query: String): ArrayList<ProductionListModel.Value> {
        val filteredList = ArrayList<ProductionListModel.Value>()
        for (item in productionListModel_gl) {
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
        callReturnComponentListApi("")
    }


}



    