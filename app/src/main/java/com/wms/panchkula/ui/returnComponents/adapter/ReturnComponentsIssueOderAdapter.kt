package com.wms.panchkula.ui.returnComponents.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.databinding.RvItemPrimaryBinding
import kotlin.collections.ArrayList

class ReturnComponentsIssueOderAdapter(var list: ArrayList<ProductionListModel.Value>): RecyclerView.Adapter<ReturnComponentsIssueOderAdapter.ViewHolder>() {

    //TODO comment interface declare...
    private var onItemClickListener: ((List<ProductionListModel.Value>, pos : Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemPrimaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(list[position]){
                binding.tvDocNum.text = this.DocumentNumber

                //  binding.tvProd.text = this.ItemNo
                binding.tvDocDate.text = GlobalMethods.convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY(this.PostingDate)
//                binding.tvP.text = this.ProductionOrderStatus
//                binding.tvAbsolute.text = this.AbsoluteEntry
//                binding.tvSeries.text = this.Series

                //TODO comment interface...
                binding.cvListItem.setOnClickListener {
                    onItemClickListener?.let { click->
                        click(list, position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    class ViewHolder(val binding: RvItemPrimaryBinding) : RecyclerView.ViewHolder(binding.root)

    fun OnItemClickListener(listener: (List<ProductionListModel.Value>, pos: Int) -> Unit ) {
        onItemClickListener = listener
    }

    //todo filter search list call this function whenever the search query changes and list update..

    fun setFilteredItems(filteredItems: ArrayList<ProductionListModel.Value>) {
        list = filteredItems
        notifyDataSetChanged()
    }

    fun clearItems() {
        list.clear()
        Log.e("Clear==>", "" + list.size)
        notifyDataSetChanged()
    }

}