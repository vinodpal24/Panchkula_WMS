package com.wms.panchkula.ui.scan_and_view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.wms.panchkula.R
import com.wms.panchkula.databinding.ActivityScanViewDetailsBinding
import com.wms.panchkula.ui.goodsreceipt.model.IssueFromModel

class ScanViewDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanViewDetailsBinding
    lateinit var issueList : ArrayList<IssueFromModel.Value>
    private lateinit var scanViewAdapter:ScanViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanViewDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayShowHomeEnabled(true)
        title = "Total Batches"

        initViews()
        clickListener()

    }

    private fun initViews() {
        issueList = (intent.getSerializableExtra("data") as? ArrayList<IssueFromModel.Value>)!!
        if(issueList.isNotEmpty()){
            val totalQuantity = issueList.sumOf {
                it.Quantity.toDoubleOrNull() ?: 0.0
            }
            binding.tvTotalQty.text = "Total Quantity: ${totalQuantity?.let { String.format("%.2f", it) }}"
            setScanViewAdapter(issueList)
        }

    }

    private fun setScanViewAdapter(issueList: java.util.ArrayList<IssueFromModel.Value>) {
        binding.rvScanAndViewItem.apply {
            layoutManager =LinearLayoutManager(this@ScanViewDetailsActivity,LinearLayoutManager.VERTICAL,false)
            scanViewAdapter = ScanViewAdapter(this@ScanViewDetailsActivity,issueList)
            adapter = scanViewAdapter
            scanViewAdapter.notifyDataSetChanged()
        }
    }

    private fun clickListener() {
        binding.apply {

            btnCancel.setOnClickListener {
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        val item = menu.findItem(R.id.search_icon)
        val searchView = SearchView((this@ScanViewDetailsActivity).supportActionBar!!.themedContext)

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW or MenuItem.SHOW_AS_ACTION_IF_ROOM)
        item.actionView = searchView
        searchView.queryHint = "Search by Batch/Qty/Warehouse"

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {

                handleSearch(newText)

                return true
            }
        })

        searchView.setOnCloseListener {
            scanViewAdapter?.notifyDataSetChanged()
            false  // Return false to allow the default behavior of collapsing the SearchView
        }
        return true
    }

    private fun handleSearch(query: String) {
        val filteredList = grpoSearchList(query)
        scanViewAdapter?.setFilteredItems(filteredList)
    }
    //todo this function filter issue for production list based on text...
    fun grpoSearchList(query: String): ArrayList<IssueFromModel.Value> {
        val filteredList = ArrayList<IssueFromModel.Value>()
        for (item in issueList) {
            if (item.Batch.contains(
                    query,
                    ignoreCase = true
                ) || item.Quantity.contains(query, ignoreCase = true) || item.BinCode.contains(query, ignoreCase = true)
            ) {
                filteredList.add(item)
            }
        }

        return filteredList
    }
}