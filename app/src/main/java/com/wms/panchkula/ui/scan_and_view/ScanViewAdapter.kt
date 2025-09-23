package com.wms.panchkula.ui.scan_and_view


import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.databinding.RvItemScanViewBinding
import com.wms.panchkula.ui.goodsreceipt.model.IssueFromModel

class ScanViewAdapter(
    private val context: Context,

    private var binLocationList: List<IssueFromModel.Value>
) : RecyclerView.Adapter<ScanViewAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: RvItemScanViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(installation: IssueFromModel.Value, position: Int) {

            binding.tvBatch.text = installation.Batch.ifEmpty { "NA" }
            binding.tvQuantity.text =  installation.Quantity.toDoubleOrNull()?.let { String.format("%.2f", it) } ?: "0.00"
            binding.tvWarehouseBin.text = installation.BinCode.ifEmpty { "NA" }


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RvItemScanViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(binLocationList[position], position)
    }

    fun setFilteredItems(filteredItems: ArrayList<IssueFromModel.Value>) {
        binLocationList  = filteredItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = binLocationList.size


}
