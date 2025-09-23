package com.wms.panchkula.ui.issueForProductionOrder.UI.productionOrderLines


import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.R
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.databinding.ActivityListProductionBinding
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.test.TestActivity
import com.wms.panchkula.ui.deliveryOrderModule.Adapter.DeliveryListAdapter
import com.wms.panchkula.ui.deliveryOrderModule.Model.DeliveryModel
import com.wms.panchkula.ui.deliveryOrderModule.UI.DeliveryDocumentLineActivity
import com.wms.panchkula.ui.issueForProductionOrder.Adapter.IssueOderAdapter
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.ui.login.LoginActivity
import com.google.gson.GsonBuilder
import com.pixplicity.easyprefs.library.Prefs
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.Global_Classes.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.io.Serializable


class ProductionListActivity : AppCompatActivity(), DeliveryListAdapter.ItemClickListener {
    private lateinit var activityListBinding: ActivityListProductionBinding
    private var issueOderAdapter: IssueOderAdapter? = null
    private var productionListModel_gl: ArrayList<ProductionListModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var deliveryModelList_gl: ArrayList<DeliveryModel.Value> = ArrayList()
    private var deliveryAdapter: DeliveryListAdapter? = null
    private lateinit var sessionManagement: SessionManagement
    private lateinit var searchView: SearchView

    var page = 0
    var apicall: Boolean = true
    var apicallBP: Boolean = true
    var isScrollingpage: Boolean = false
    var limit = 100
    var flag: String = ""

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityListBinding = ActivityListProductionBinding.inflate(layoutInflater)
        setContentView(activityListBinding.root)

//      clearCache(this)
        deleteCache(this)

        supportActionBar?.setDisplayShowHomeEnabled(true)

        materialProgressDialog = MaterialProgressDialog(this@ProductionListActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@ProductionListActivity)

        activityListBinding.swipeRefreshLayout.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener { // Call your data refreshing method here
            searchView.setQuery("", false)         // Clear the search text
            searchView.clearFocus()
            refreshData()
            loadIssueOrderListItems("")
        })
        //todo get arguments from previous activity...
        val myIntent = intent
        flag = myIntent.getStringExtra("flag")!!
        Log.d("onCreate====>", "onCreate ($flag)")
        if (flag.equals("Issue_Order")) {

            title = "Issue For Production"
            Log.d("onCreate====>", "onCreate (if)")
            //todo loading initial list items and calling adapter-----
            Log.e("loadMoreListItems==>", "Items_loading...")
            loadIssueOrderListItems("")
            setIssueOrderAdapter()

            //todo adapter on item click listener....
            /*issueOderAdapter?.OnItemClickListener { list, pos ->
                Log.e("onCreate====>", "List: $list")
                var productionValueList = list[pos]
                var productionLinesList = list[pos].ProductionOrderLines


                CoroutineScope(Dispatchers.IO).launch {
                    var intent: Intent = Intent(this@ProductionListActivity, ProductionOrderLinesActivity::class.java)
                    intent.putExtra("productionLinesList", productionLinesList as Serializable)
                    intent.putExtra("productionValueList", productionValueList as Serializable)
                    intent.putExtra("pos", pos)
                    startActivity(intent)

                    withContext(Dispatchers.Main) {

                        if (productionLinesList.size > 0) {
                            sessionManagement.setWarehouseCode(this@ProductionListActivity, productionLinesList[0].Warehouse)
                        }
                    }
                }
            }*/


            //todo recycler view scrollListener for add more items in list...
            activityListBinding.rvProductionList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    var lastCompletelyVisibleItemPosition = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    if (lastCompletelyVisibleItemPosition == productionListModel_gl.size - 1 && apicallBP) {
                        page++
                        Log.e("page--->", page.toString())
                        loadMoreProductionData(0)//todo comment due to api change there us no need for pagination--
                        isScrollingpage = false
                    } else {
                        recyclerView.setPadding(0, 0, 0, 0);
                    }

                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) { //it means we are scrolling
                        isScrollingpage = true

                    }
                }
            })
        } else if (flag.equals("Delivery_Order")) {
            title = "Delivery Order"
            Log.d("onCreate====>", "onCreate (else if)")
            loadDeliveryOrderListItems()

        }


    }


    fun clearCache(context: Activity) {
        try {
            // Get the cache directory for your application
            val cacheDir = context.cacheDir

            // Check if the cache directory exists
            if (cacheDir != null && cacheDir.isDirectory) {
                // Delete all files and subdirectories in the cache directory
                val children = cacheDir.list()
                for (child in children) {
                    val cacheFile = File(cacheDir, child)
                    cacheFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun deleteCache(context: Activity) {
        try {
            val dir: File = context.getCacheDir()
            deleteDir(dir)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }


    override fun onRestart() {
        super.onRestart()
        Log.d("Restart====>", "Restart")
        //productionListModel_gl.clear()
        deliveryModelList_gl.clear()

    }

    fun totalskipCount(curret: Int): Int {
        var total = limit * page
        return limit * page;
    }

    lateinit var layoutManager: RecyclerView.LayoutManager

    //todo set adapter...
    fun setIssueOrderAdapter() {
        layoutManager = LinearLayoutManager(this)
        activityListBinding.rvProductionList.layoutManager = layoutManager
        issueOderAdapter = IssueOderAdapter(productionListModel_gl, callBack = { list, pos ->
            var productionValueList = list[pos]
            var productionLinesList = list[pos].ProductionOrderLines


            CoroutineScope(Dispatchers.IO).launch {
                var intent: Intent = Intent(this@ProductionListActivity, ProductionOrderLinesActivity::class.java)
                intent.putExtra("productionLinesList", productionLinesList as Serializable)
                intent.putExtra("productionValueList", productionValueList as Serializable)
                intent.putExtra("pos", pos)
                startActivity(intent)

                withContext(Dispatchers.Main) {

                    if (productionLinesList.size > 0) {
                        sessionManagement.setWarehouseCode(this@ProductionListActivity, productionLinesList[0].Warehouse)
                    }
                }
            }
        })
        activityListBinding.rvProductionList.adapter = issueOderAdapter
    }

    var fromAbsoluteNo = 0
    var nextToAbsoluteEntry = 0
    var fromAbsoluteNoBp = 0
    var nextToAbsoluteEntryBP = 0


    //todo ISSUE ORDER API LIST load next production item list....
    fun loadIssueOrderListItems(docNum: String) {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = QuantityNetworkClient.create(this)
                val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
                networkClient.doGetProductionList(sessionManagement.getCompanyDB(this)!!, bplId, docNum).apply { // "odata.maxpagesize=" + 100, "ProductionOrderStatus eq '" + "boposReleased" + "'","" + skip,
                    // "AbsoluteEntry desc"
                    enqueue(object : Callback<ProductionListModel> {
                        override fun onResponse(call: Call<ProductionListModel>, response: Response<ProductionListModel>) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    if (productionList_gl.isEmpty()) {
                                        activityListBinding.tvNoDataFound.visibility = View.VISIBLE
                                        activityListBinding.rvProductionList.visibility = View.INVISIBLE
                                    } else {
                                        activityListBinding.tvNoDataFound.visibility = View.INVISIBLE
                                        activityListBinding.rvProductionList.visibility = View.VISIBLE
                                        //Toast.makeText(this@ProductionListActivity, "Successfully!", Toast.LENGTH_SHORT)


                                        if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                            productionListModel_gl.clear() // <- make sure old data is removed
                                            productionListModel_gl.addAll(productionList_gl)
                                            setIssueOrderAdapter()
                                            issueOderAdapter?.notifyDataSetChanged()

                                            if (productionListModel1.value.size < 100)
                                                apicallBP = false

                                            var tempSize = productionList_gl.size - 1

                                            var temovall = productionListModel1.value[tempSize].AbsoluteEntry
                                            Log.e("value ==>", "onResponse: " + temovall)

                                            fromAbsoluteNoBp = temovall.toInt()
                                            nextToAbsoluteEntryBP = fromAbsoluteNoBp.toInt() + 100

                                            Log.e("fromAbsoluteNo ==> BP1", "onResponse: " + fromAbsoluteNoBp)
                                            Log.e("nextValue ==> BP1", "onResponse: " + nextToAbsoluteEntryBP)

                                        }
                                    }


                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code == 400) {
                                            GlobalMethods.showError(this@ProductionListActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@ProductionListActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@ProductionListActivity, LoginActivity::class.java)
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

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }


    fun loadMoreProductionData(skip: Int) {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)
                // var nextToAbsoluteEntryBP= fromAbsoluteNoBp.toInt()+100;
                Log.e("DATA Next==>", nextToAbsoluteEntryBP.toString())

                Log.e("DATA Next==> BP1", "" + fromAbsoluteNoBp)
                Log.e("DATA Next ==> BP2", "" + nextToAbsoluteEntryBP)
                val networkClient = QuantityNetworkClient.create(this)
                val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""

                networkClient.doGetProductionListPagination(sessionManagement.getCompanyDB(this)!!, fromAbsoluteNoBp.toString(), nextToAbsoluteEntryBP.toString(), bplId).apply { // "odata.maxpagesize=" +
                    // 100, "ProductionOrderStatus eq '" + "boposReleased" + "'","" + skip, "AbsoluteEntry desc"
                    enqueue(object : Callback<ProductionListModel> {
                        override fun onResponse(call: Call<ProductionListModel>, response: Response<ProductionListModel>) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(this@ProductionListActivity, "Successfully!", Toast.LENGTH_SHORT)
                                    if (productionListModel1.value.size == 0)
                                        nextToAbsoluteEntryBP = nextToAbsoluteEntryBP.toInt() + 100
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size >= 0) {
                                        Log.e("List Size--->", productionListModel1.value.size.toString())


                                        productionListModel_gl.addAll(productionList_gl)

                                        issueOderAdapter?.notifyDataSetChanged()


                                        var tempSize = productionList_gl.size - 1

                                        var temovall = productionListModel1.value[tempSize].AbsoluteEntry
                                        Log.e("value ==>", "onResponse: " + temovall)

                                        fromAbsoluteNoBp = temovall.toInt()
                                        nextToAbsoluteEntryBP = fromAbsoluteNoBp.toInt() + 100

                                        Log.e("fromAbsoluteNo ==> BP", "onResponse: " + fromAbsoluteNo)
                                        Log.e("nextValue ==> BP", "onResponse: " + nextToAbsoluteEntry)

                                    }

                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code == 400) {
                                            GlobalMethods.showError(this@ProductionListActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@ProductionListActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@ProductionListActivity, LoginActivity::class.java)
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

            } else {
                materialProgressDialog.dismiss()
                GlobalMethods.showError(this, "No Network Connection")
            }
        }

    }


    //todo DELIVERY ORDER API LIST ITEMS BIND....
    fun loadDeliveryOrderListItems() {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                val networkClient = NetworkClients.create(this)
                networkClient.deliveryOrder("DocumentStatus eq 'bost_Open'", "DocNum desc").apply {
                    enqueue(object : Callback<DeliveryModel> {
                        override fun onResponse(
                            call: Call<DeliveryModel>,
                            response: Response<DeliveryModel>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("delivery_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var listResponse = response.body()!!
                                    deliveryModelList_gl = listResponse.value
                                    setDeliveryOrderAdapter()
                                } else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        if (mError.error.code == 400) {
                                            GlobalMethods.showError(
                                                this@ProductionListActivity,
                                                mError.error.message.value
                                            )
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(
                                                this@ProductionListActivity,
                                                mError.error.message.value
                                            )
                                            val mainIntent = Intent(
                                                this@ProductionListActivity,
                                                LoginActivity::class.java
                                            )
                                            startActivity(mainIntent)
                                            finish()
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onFailure(call: Call<DeliveryModel>, t: Throwable) {
                            Log.e("delivery_failure-----", t.toString())
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

    //todo bind delivery order adapter...
    fun setDeliveryOrderAdapter() {
        layoutManager = LinearLayoutManager(this)
        activityListBinding.rvProductionList.layoutManager = layoutManager
        deliveryAdapter = DeliveryListAdapter(deliveryModelList_gl, this@ProductionListActivity)
        activityListBinding.rvProductionList.adapter = deliveryAdapter
        deliveryAdapter?.notifyDataSetChanged()
    }


    //todo delivery document order line adapter on paricular item click listener...
    override fun onItemClick(valueList: List<DeliveryModel.Value>, pos: Int) {
        var deliveryValueList = valueList[pos]
        var documentLineList = valueList[pos].DocumentLines
        var intent: Intent = Intent(this, DeliveryDocumentLineActivity::class.java)
        intent.putExtra("documentLineList", documentLineList as Serializable)
        intent.putExtra("deliveryValueList", deliveryValueList as Serializable)
        intent.putExtra("pos", pos)
        startActivity(intent)
    }


    //todo set search icon on activity...
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search_icon -> {
                //todo Handle icon click
                return true
            }

            R.id.list_icon -> {
                //todo Handle icon click
                var intent = Intent(this@ProductionListActivity, TestActivity::class.java)
                startActivity(intent)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        searchView = SearchView((this@ProductionListActivity).supportActionBar!!.themedContext)

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.actionView = searchView
        searchView.queryHint = "Search Here"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                loadIssueOrderListItems(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                handleSearch(newText)
                return true
            }
        })

        searchView.setOnCloseListener {
            loadIssueOrderListItems("")
            issueOderAdapter?.notifyDataSetChanged()
            false  // Return false to allow the default behavior of collapsing the SearchView
        }

        return true
    }

    private fun refreshData() {
        // Simulate data refresh (replace with actual data fetching logic)
        Handler().postDelayed({
            // loadInvoiceRequestItems();
            // Update the adapter with new data
            issueOderAdapter!!.notifyDataSetChanged() // Notify your adapter of changes
            activityListBinding.swipeRefreshLayout.setRefreshing(false) // Stop the refresh animation
        }, GlobalMethods.pullRefreshTime.toLong()) // Simulate a 2-second refresh time
    }

    //todo search filter..
    private fun handleSearch(query: String) {
        if (flag.equals("Issue_Order")) {
            val filteredList = issueSearchList(query)
            issueOderAdapter?.setFilteredItems(filteredList)
        } else if (flag.equals("Delivery_Order")) {
            val deliveryFilterList = deliverySearchList(query)
            deliveryAdapter?.setFilteredItems(deliveryFilterList)
        }
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

    //todo this function filter delivery order list based on text...
    fun deliverySearchList(query: String): ArrayList<DeliveryModel.Value> {
        val filteredList = ArrayList<DeliveryModel.Value>()
        for (item in deliveryModelList_gl) {
            if (item.DocNum.contains(query, ignoreCase = true) || item.CardCode.contains(
                    query,
                    ignoreCase = true
                )
            ) {
                filteredList.add(item)
            }
        }
        return filteredList
    }


    override fun onBackPressed() {
        super.onBackPressed()
        /* var intent = Intent(this@ProductionListActivity, HomeActivity::class.java)
         intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
         startActivity(intent)*/
        finish()
    }


}