package  com.wms.panchkula.ui.purchase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
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
import com.wms.panchkula.ui.purchase.model.ModelCustomers
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.Serializable


class PurchaseOrderActivity : AppCompatActivity() {

    lateinit var binding: ActivityPurchaseOrderBinding
    private var requestListModel_gl: ArrayList<PurchaseRequestModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var requestAdapter: PurchaseRequestAdapter? = null
    private var selectedPurchaseOrderList: ArrayList<PurchaseRequestModel.Value> = ArrayList()
    private var customerList: ArrayList<ModelCustomers.Value> = ArrayList()
    private lateinit var sessionManagement: SessionManagement
    private var cartBadge: TextView? = null
    var page = 0
    var apicall: Boolean = true
    var isScrollingpage: Boolean = false
    var selectedCardCode = ""
    var limit = 100
    var flag: String = ""
    lateinit var layoutManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.searchLayout.visibility = View.VISIBLE
        supportActionBar?.setDisplayShowHomeEnabled(true)
        materialProgressDialog = MaterialProgressDialog(this@PurchaseOrderActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@PurchaseOrderActivity)

        title = "Purchase Orders"

        //todo loading initial list items and calling adapter-----
        Log.e("loadMoreListItems==>", "Items_loading...")
        // loadIssueOrderListItems(0)

        //loadInvoiceRequestItems()
        AppConstants.selectedPOList.clear()
        callGetCustomerApi("")
        setInvoiceOrderAdapter()

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm != null && currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }

        binding.swipeRefreshLayout.setOnRefreshListener(OnRefreshListener { // Call your data refreshing method here
            refreshData()
            loadInvoiceRequestItems(selectedCardCode, "")
        })

    }

    private fun callGetCustomerApi(searchText: String) {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = QuantityNetworkClient.create(this)

                networkClient.getCustomerSearch(searchText).apply {
                    enqueue(object : Callback<ModelCustomers> {
                        override fun onResponse(
                            call: Call<ModelCustomers>, response: Response<ModelCustomers>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    materialProgressDialog.dismiss()
                                    // Assuming response.body()?.value is a list of customer objects, each containing cardName and cardCode
                                    response.body()?.value?.let { customerList.addAll(it) }

                                    val listCustomer: List<String> = response.body()?.value?.mapNotNull { it.cardName } ?: emptyList()

                                    // Create the adapter
                                    val autocompleteAdapter = ArrayAdapter(
                                        this@PurchaseOrderActivity, R.layout.drop_down_item_textview, listCustomer
                                    )

                                    // Set adapter to AutoCompleteTextView
                                    binding.acCustomer.setAdapter(autocompleteAdapter)

                                    binding.acCustomer.setOnItemClickListener { parent, view, position, id ->
                                        val selectedName = parent.getItemAtPosition(position).toString()

                                        // Find the matching customer object by name
                                        val selectedCustomer = customerList.find { it.cardName == selectedName }
                                        Log.i("PO_LOG", "CustomerList from Api: $customerList\nSelectedCustomer: $selectedCustomer (${selectedCustomer?.cardCode})")
                                        selectedCardCode = selectedCustomer?.cardCode ?: ""

                                        binding.acCustomer.setText(selectedName, false)
                                        //requestListModel_gl.clear()

                                        loadInvoiceRequestItems(selectedCardCode, "")
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
                                            GlobalMethods.showError(this@PurchaseOrderActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@PurchaseOrderActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@PurchaseOrderActivity, LoginActivity::class.java)
                                            startActivity(mainIntent)
                                            finish()
                                        }/*if (mError.error.message.value != null) {
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

                        override fun onFailure(call: Call<ModelCustomers>, t: Throwable) {
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

    private fun refreshData() {
        // Simulate data refresh (replace with actual data fetching logic)
        Handler().postDelayed({
            // loadInvoiceRequestItems();
            // Update the adapter with new data
            requestAdapter!!.notifyDataSetChanged() // Notify your adapter of changes
            binding.swipeRefreshLayout.setRefreshing(false) // Stop the refresh animation
        }, GlobalMethods.pullRefreshTime.toLong()) // Simulate a 2-second refresh time
    }


    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        val searchView = SearchView((this@PurchaseOrderActivity).supportActionBar!!.themedContext)

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.actionView = searchView
        searchView.queryHint = "Search Here"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                binding.acCustomer.setText("")
                loadInvoiceRequestItems("", query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {

                handleSearch(newText)

                return true
            }
        })

        searchView.setOnCloseListener {
            requestAdapter?.notifyDataSetChanged()
            false  // Return false to allow the default behavior of collapsing the SearchView
        }
        return true
    }*/


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)

        // SearchView setup
        val searchItem = menu.findItem(R.id.search_icon)
        val searchView = SearchView(this@PurchaseOrderActivity.supportActionBar!!.themedContext)
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        searchItem.actionView = searchView

        searchView.queryHint = "Search Here"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                binding.acCustomer.setText("")
                loadInvoiceRequestItems("", query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                handleSearch(newText)
                return true
            }
        })
        searchView.setOnCloseListener {
            requestAdapter?.notifyDataSetChanged()
            false
        }

        // Cart badge setup
        val cartItem = menu.findItem(R.id.actionCart)
        cartItem.isVisible = true // <-- Add this line to ensure visibility
        val cartActionView = cartItem.actionView

        cartActionView?.let {
            it.visibility = View.VISIBLE
            cartBadge = it.findViewById(R.id.cartBadgeCount)
            updateCartCount(AppConstants.selectedPOList.size) // This must be called **after** cartBadge is initialized

            /*Handler(Looper.getMainLooper()).postDelayed({
                updateCartCount(0)
            }, 3000)*/

            it.setOnClickListener {
                //Toast.makeText(this, "Clicked on cart icon", Toast.LENGTH_SHORT).show()

                val itemObject = AppConstants.selectedPOList.firstOrNull()

                if (itemObject != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val intent = Intent(this@PurchaseOrderActivity, PurchaseTransferLinesActivity::class.java)
                        intent.putExtra("inventReqModel", itemObject as Serializable)
                        intent.putExtra("productionOrderList", AppConstants.selectedPOList as Serializable)
                        startActivity(intent)

                        sessionManagement.setWarehouseCode(
                            this@PurchaseOrderActivity,
                            itemObject.DocumentLines[0].WarehouseCode
                        )
                        Log.e("warehouse", "onCreate: ${sessionManagement.getWarehouseCode(this@PurchaseOrderActivity)}")
                    }
                } else {
                    Toast.makeText(this, "No item selected", Toast.LENGTH_SHORT).show()
                }
                /*var productionValueList = list.get(pos).DocumentLines
                //var productionLinesList = list.get(pos).DocumentLines
                //Log.e("PO_LOG", "ProductionList =>productionValueList: $productionValueList")
                //Log.e("PO_LOG", "ProductionList =>productionLinesList: $productionLinesList")
                var itemObject : PurchaseRequestModel.Value
                    if (AppConstants.selectedPOList.size > 0) {
                        itemObject = AppConstants.selectedPOList[0]
                    }



                if (AppConstants.selectedPOList.size > 0) {

                    CoroutineScope(Dispatchers.IO).launch {
                        var intent: Intent = Intent(this@PurchaseOrderActivity, PurchaseTransferLinesActivity::class.java)
                        intent.putExtra("inventReqModel", itemObject as Serializable)
                        //intent.putExtra("productionLinesList", productionLinesList as Serializable)
                        intent.putExtra("productionOrderList", AppConstants.selectedPOList as Serializable)
                        //intent.putExtra("productionValueList", productionValueList as Serializable)
                        //intent.putExtra("InventoryReqObject", productionValueList as Serializable)
                        //  intent.putExtra("numAtCard",itemObject.NumAtCard )
                        startActivity(intent)

                        if (AppConstants.selectedPOList.size > 0) {
                            sessionManagement.setWarehouseCode(this@PurchaseOrderActivity, AppConstants.selectedPOList[0].DocumentLines[0].WarehouseCode)
                            Log.e("warehouse", "onCreate: " + sessionManagement.getWarehouseCode(this@PurchaseOrderActivity))
                        }

                    }
                }*/
            }
        }

        return true
    }


    private fun updateCartCount(count: Int) {
        cartBadge?.apply {
            text = count.toString()
            visibility = if (count > 0) View.VISIBLE else View.GONE
        } ?: Log.w("CartBadge", "cartBadge view is not initialized yet")
    }


    //todo search filter..
    private fun handleSearch(query: String) {
        val filteredList = grpoSearchList(query)
        requestAdapter?.setFilteredItems(filteredList)
    }

    //todo this function filter issue for production list based on text...
    fun grpoSearchList(query: String): ArrayList<PurchaseRequestModel.Value> {
        val filteredList = ArrayList<PurchaseRequestModel.Value>()
        for (item in requestListModel_gl) {
            if (item.DocNum.contains(
                    query, ignoreCase = true
                ) || item.DocEntry.contains(query, ignoreCase = true)
            ) {
                filteredList.add(item)
            }
        }

        return filteredList
    }


    // todo Open invoice list--

    fun loadInvoiceRequestItems(selectedCardCode: String, searchText: String) {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = QuantityNetworkClient.create(this)
                val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""

                networkClient.getPurchaseOrderList(bplId, selectedCardCode, searchText).apply {
                    enqueue(object : Callback<PurchaseRequestModel> {
                        override fun onResponse(
                            call: Call<PurchaseRequestModel>, response: Response<PurchaseRequestModel>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("API_CALL", "Success")
                                    materialProgressDialog.dismiss()

                                    val productionListModel1 = response.body()!!
                                    val productionList_gl = productionListModel1.value

                                    // Clear previous list data
                                    requestListModel_gl.clear()

                                    // If the response list is not empty, add data to the list
                                    if (!productionList_gl.isNullOrEmpty()) {
                                        requestListModel_gl.addAll(productionList_gl)
                                        if (!searchText.isNullOrEmpty()) {
                                            requestAdapter?.setFilteredItems(requestListModel_gl)
                                        }

                                    }
                                    // If no data found, notify the adapter and show a no data screen
                                    requestAdapter?.notifyDataSetChanged()

                                    // If the list size is less than the limit, don't make another API call
                                    if (productionListModel1.value.size < 100) apicall = false
                                }/*if (response.isSuccessful) {

                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Log.i("PO_LOG","ProductionList from API: $productionList_gl")
                                    requestListModel_gl.clear()
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {

                                        Log.e("PO_LOG","if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0)")

                                        requestListModel_gl.addAll(productionList_gl)
                                        Log.d("PO_LOG","ProductionList after added API: $requestListModel_gl")
                                        if (productionList_gl.isNotEmpty()) {
                                            setInvoiceOrderAdapter()
                                        } else {
                                            Toast.makeText(this@PurchaseOrderActivity, "No New Transfer Requests Found", Toast.LENGTH_SHORT).show()
                                        }

                                        requestAdapter?.notifyDataSetChanged()

                                        if (productionListModel1.value.size < 100)
                                            apicall = false

                                    }

                                }*/ else {
                                    materialProgressDialog.dismiss()
                                    val gson1 = GsonBuilder().create()
                                    var mError: OtpErrorModel
                                    try {
                                        val s = response.errorBody()!!.string()
                                        Log.e("API_CALL", "Error: $s")
                                        mError = gson1.fromJson(s, OtpErrorModel::class.java)
                                        Log.e("MSZ==>", mError.error.message.value)
                                        if (mError.error.code == 400) {
                                            GlobalMethods.showError(this@PurchaseOrderActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@PurchaseOrderActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@PurchaseOrderActivity, LoginActivity::class.java)
                                            startActivity(mainIntent)
                                            finish()
                                        }/*if (mError.error.message.value != null) {
                                            AppConstants.showError(this@ProductionListActivity, mError.error.message.value)
                                            Log.e("json_error------", mError.error.message.value)
                                        }*/
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                materialProgressDialog.dismiss()
                            }
                        }

                        override fun onFailure(call: Call<PurchaseRequestModel>, t: Throwable) {
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
        Log.e("PO_LOG", "ProductionList in setInvoiceOrderAdapter(): $requestListModel_gl")
        layoutManager = LinearLayoutManager(this)
        binding.rvProductionList.layoutManager = layoutManager
        requestAdapter = PurchaseRequestAdapter(requestListModel_gl)
        binding.rvProductionList.adapter = requestAdapter


        //todo adapter on item click listener....
        requestAdapter?.OnItemClickListener { list, pos, layout ->
            Log.e("PO_LOG", "ProductionList requestAdapter?.OnItemClickListener: $list")
            /*var productionValueList = list.get(pos).DocumentLines
            var productionLinesList = list.get(pos).DocumentLines
            Log.e("PO_LOG", "ProductionList =>productionValueList: $productionValueList")
            Log.e("PO_LOG", "ProductionList =>productionLinesList: $productionLinesList")
            var itemObject = list.get(pos);

            CoroutineScope(Dispatchers.IO).launch {
                val bundle = Bundle().apply { putSerializable("inventReqModel", itemObject) }
                var intent: Intent = Intent(this@PurchaseOrderActivity, PurchaseTransferLinesActivity::class.java)
                intent.putExtra("productionLinesList", productionLinesList as Serializable)
                intent.putExtra("productionValueList", productionValueList as Serializable)
                intent.putExtra("InventoryReqObject", productionValueList as Serializable)
                //  intent.putExtra("numAtCard",itemObject.NumAtCard )
                intent.putExtras(bundle)
                startActivity(intent)

                if (productionValueList.size > 0) {
                    sessionManagement.setWarehouseCode(this@PurchaseOrderActivity, productionValueList[0].WarehouseCode)
                    Log.e("warehouse", "onCreate: " + sessionManagement.getWarehouseCode(this@PurchaseOrderActivity))
                }
            }*/

            val selectedItem = list[pos]

            // Toggle selection: if already selected, remove; else, add
            if (AppConstants.selectedPOList.contains(selectedItem)) {
                selectedItem.isItemSelected = false
                AppConstants.selectedPOList.remove(selectedItem)
            } else {
                selectedItem.isItemSelected = true
                AppConstants.selectedPOList.add(selectedItem)
            }

            layout.setBackgroundResource(if (selectedItem.isItemSelected) R.drawable.rounded_border_grey_selected else R.drawable.rounded_border_ligh_grey)

            // Update selectedList and badge
            selectedPurchaseOrderList.clear()
            selectedPurchaseOrderList.addAll(AppConstants.selectedPOList)


            // Update cart badge
            if (selectedPurchaseOrderList.isNotEmpty()) {
                updateCartCount(selectedPurchaseOrderList.size)
            } else {
                updateCartCount(0)
            }

            Log.i("CART_ITEM", "Selected List => adapter?.OnItemClickListener : ${selectedPurchaseOrderList.size}")
            Log.i("CART_ITEM", "Filter List => adapter?.OnItemClickListener : ${selectedPurchaseOrderList.size}")
            Log.i("CART_ITEM", "Saved Selected List => adapter?.OnItemClickListener : ${AppConstants.selectedPOList.size}")
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("PO_LOG", "call onResume()")
        if (!requestListModel_gl.isEmpty()) {
            //requestListModel_gl.clear()
            Log.d("PO_LOG", "requestListModel_gl clear: $requestListModel_gl")
            loadInvoiceRequestItems(selectedCardCode, "")
        }
    }


}