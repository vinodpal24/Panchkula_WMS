package com.wms.panchkula.ui.goodsreceipt


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.MaterialProgressDialog
import com.wms.panchkula.Model.OtpErrorModel
import com.wms.panchkula.Retrofit_Api.NetworkClients
import com.wms.panchkula.Retrofit_Api.QuantityNetworkClient
import com.wms.panchkula.SessionManagement.SessionManagement
import com.wms.panchkula.Retrofit_Api.ApiConstantForURL
import com.wms.panchkula.databinding.ActivityGoodsReceiptBinding
import com.google.gson.GsonBuilder
import com.webapp.internetconnection.CheckNetwoorkConnection
import com.wms.panchkula.ui.goodsreceipt.adapter.GoodReceiptAdapter
import com.wms.panchkula.ui.goodsreceipt.adapter.SearchAdapter
import com.wms.panchkula.ui.goodsreceipt.model.GetItemstModel
import com.wms.panchkula.ui.login.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import androidx.appcompat.widget.SearchView
import com.wms.panchkula.R
import com.pixplicity.easyprefs.library.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.Serializable

class GoodsReceiptActivity : AppCompatActivity() {

    lateinit var binding: ActivityGoodsReceiptBinding
    private var requestListModel_gl: ArrayList<GetItemstModel.Value> = ArrayList()
    lateinit var materialProgressDialog: MaterialProgressDialog
    lateinit var checkNetwoorkConnection: CheckNetwoorkConnection
    private var requestAdapter: GoodReceiptAdapter? = null
    private lateinit var sessionManagement: SessionManagement

    var page = 0
    var apicall: Boolean = true
    var isScrollingpage: Boolean = false
    var limit = 100
    var flag: String = ""
    lateinit var layoutManager: RecyclerView.LayoutManager


    companion object {
        private const val TAG = "DemoActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoodsReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.hide()
//        todo new code for test good items
//        getDataToPostOnJson()
//        getPostJson()
        AppConstants.selectedList.clear()
        materialProgressDialog = MaterialProgressDialog(this@GoodsReceiptActivity)
        checkNetwoorkConnection = CheckNetwoorkConnection(application)
        sessionManagement = SessionManagement(this@GoodsReceiptActivity)

        title = "Goods Receipt"

        searchItem()
        // setInvoiceOrderAdapter()

        // todo add by tarun
        binding.actionCart.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val bundle = Bundle().apply {

                    var intent: Intent =
                        Intent(this@GoodsReceiptActivity, GoodsReceiptLineActivity::class.java)
                    intent.putExtra("selectedList", selectedList as Serializable)

                    startActivity(intent)


                }

            }
        }


    }

    lateinit var textView: TextView

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_goods_receipt, menu)

        // Find the cart item in the menu
        val cartItem = menu?.findItem(R.id.action_cart)

        // Get the actionView (which might be a layout containing the cart icon and the badge)
        val actionView = cartItem?.actionView

        // Find the cart count badge inside the action view (TextView)
        val cartCountTextView = actionView?.findViewById<TextView>(R.id.cart_badge)
        // Log.e("cartCountTextView",""+cartCountTextView!!.text.toString())
        if (cartCountTextView != null) {
            textView = cartCountTextView
            Log.e("textView", "textView")
        }
        // Update the cart badge with the current count (you may be pulling this from your data source)
        updateCartBadge(cartCountTextView)

        // Set a click listener on the action view
        actionView?.setOnClickListener {
            // When the cart icon is clicked, handle the item click event
            onOptionsItemSelected(cartItem)
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_cart -> {


                CoroutineScope(Dispatchers.IO).launch {
                    val bundle = Bundle().apply {

                        var intent: Intent =
                            Intent(this@GoodsReceiptActivity, GoodsReceiptLineActivity::class.java)
                        intent.putExtra("selectedList", selectedList as Serializable)

                        startActivity(intent)


                    }

                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun updateCartBadge(cartBadge: TextView?) {
        val count = 5
        Log.e("selectedList=>", AppConstants.selectedList.size.toString())
        //  cartBadge?.visibility = if (count > 0) View.VISIBLE else View.GONE
        cartBadge?.text = count.toString()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        AppConstants.scannedItemForGood.clear()
    }

    private lateinit var adapter: SearchAdapter
    private val filteredList = mutableListOf<GetItemstModel.Value>()
    private fun searchItem() {

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SearchAdapter(requestListModel_gl) // Initially empty list
        binding.recyclerView.adapter = adapter

        adapter?.OnItemClickListener { list, pos ->
           /* filteredList.add(list.get(pos))
            AppConstants.selectedList.add(list.get(pos))
            selectedList.clear()
            selectedList.addAll(AppConstants.selectedList)
            //updateCartBadge(textView)

            // requestAdapter?.notifyDataSetChanged()
            if (filteredList.size > 0) {
                binding.cartBadge.setText("" + filteredList.size.toString())
            }*/

            val selectedItem = list[pos]

            // Toggle selection: if already selected, remove; else, add
            if (AppConstants.selectedList.contains(selectedItem)) {
                AppConstants.selectedList.remove(selectedItem)
                filteredList.remove(selectedItem)
            } else {
                AppConstants.selectedList.add(selectedItem)
                filteredList.add(selectedItem)
            }

            // Update selectedList and badge
            selectedList.clear()
            selectedList.addAll(AppConstants.selectedList)



            // Update cart badge
            if (filteredList.isNotEmpty()) {
                binding.cartBadge.text = filteredList.size.toString()
            } else {
                binding.cartBadge.text = "0"
            }
            Log.i("CART_ITEM","Selected List => adapter?.OnItemClickListener : ${selectedList.size}")
            Log.i("CART_ITEM","Filter List => adapter?.OnItemClickListener : ${selectedList.size}")
            Log.i("CART_ITEM","Saved Selected List => adapter?.OnItemClickListener : ${AppConstants.selectedList.size}")

        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.e("Test==>", query.toString())
                loadInvoiceRequestItems(query.toString())
                return false
            }


            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {

                    filteredList.clear()
                } else {
                    Log.e("Test==1>", newText.toString())

                    filteredList.clear()
                    /* filteredList.addAll(requestListModel_gl.filter {
                         it.ItemName.contains(newText, ignoreCase = true) // Change field as needed
                     })*/
                }
                adapter.notifyDataSetChanged()
                return true
            }
        })


    }

    private var selectedList: ArrayList<GetItemstModel.Value> = ArrayList()


    fun loadInvoiceRequestItems(searchData: String) {
        checkNetwoorkConnection.observe(this) { isConnected ->
            if (isConnected) {
                materialProgressDialog.show()
                var apiConfig = ApiConstantForURL()

                NetworkClients.updateBaseUrlFromConfig(apiConfig)

                QuantityNetworkClient.updateBaseUrlFromConfig(apiConfig)

                val networkClient = QuantityNetworkClient.create(this)
                val bplId = if (Prefs.getString(AppConstants.BPLID, "").isNotEmpty()) Prefs.getString(AppConstants.BPLID, "") else ""
                networkClient.GoodReceiptsItems(searchData, bplId).apply {
                    enqueue(object : Callback<GetItemstModel> {
                        override fun onResponse(
                            call: Call<GetItemstModel>,
                            response: Response<GetItemstModel>
                        ) {
                            try {
                                if (response.isSuccessful) {
                                    Log.e("api_hit_response===>", response.toString())
                                    materialProgressDialog.dismiss()
                                    var productionListModel1 = response.body()!!
                                    var productionList_gl = productionListModel1.value
                                    Toast.makeText(this@GoodsReceiptActivity, "Successfully!", Toast.LENGTH_SHORT)
                                    if (!productionList_gl.isNullOrEmpty() && productionList_gl.size > 0) {
                                        Log.e("page---->", page.toString())
                                        requestListModel_gl.clear()
                                        requestListModel_gl.addAll(productionList_gl)
                                        if (requestListModel_gl.size == 0) {
                                            Toast.makeText(this@GoodsReceiptActivity, "No New Transfer Requests Found", Toast.LENGTH_SHORT).show()
                                        }


                                        adapter?.notifyDataSetChanged()

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
                                            GlobalMethods.showError(this@GoodsReceiptActivity, mError.error.message.value)
                                        }
                                        if (mError.error.code == 306 && mError.error.message.value != null) {
                                            GlobalMethods.showError(this@GoodsReceiptActivity, mError.error.message.value)
                                            val mainIntent = Intent(this@GoodsReceiptActivity, LoginActivity::class.java)
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

                        override fun onFailure(call: Call<GetItemstModel>, t: Throwable) {
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

    override fun onResume() {
        super.onResume()
        Log.i("CART_ITEM","onResume() => Selected List (before clear): ${selectedList.size}")
        selectedList.clear()
        Log.i("CART_ITEM","onResume() => Selected List (after clear): ${selectedList.size}")
        Log.i("CART_ITEM","onResume() => Saved Selected List : ${AppConstants.selectedList.size}")

        selectedList.addAll(AppConstants.selectedList)
        Log.i("CART_ITEM","onResume() => Selected List (after add save list): ${selectedList.size}")
        if (selectedList.isNotEmpty()) {
            binding.cartBadge.text = selectedList.size.toString()
        } else {
            binding.cartBadge.text = "0"
        }
    }


}