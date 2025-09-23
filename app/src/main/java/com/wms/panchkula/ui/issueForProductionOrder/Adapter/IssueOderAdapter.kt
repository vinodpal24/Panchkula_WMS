package com.wms.panchkula.ui.issueForProductionOrder.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel
import com.wms.panchkula.databinding.RvItemPrimaryBinding
import kotlin.collections.ArrayList

class IssueOderAdapter(var list: ArrayList<ProductionListModel.Value>,var callBack: (List<ProductionListModel.Value>, pos : Int) -> Unit): RecyclerView.Adapter<IssueOderAdapter.ViewHolder>() {

    //TODO comment interface declare...
    private var onItemClickListener: ((List<ProductionListModel.Value>, pos : Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemPrimaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(list[position]){
                binding.tvFgItem.text=this.FGName
                binding.layoutFgName.visibility= View.VISIBLE
                binding.tvDocNum.text = this.DocumentNumber

                //binding.tvProd.text = this.ItemNo
                binding.tvDocDate.text = GlobalMethods.convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY(this.PostingDate)
//                binding.tvP.text = this.ProductionOrderStatus
//                binding.tvAbsolute.text = this.AbsoluteEntry
//                binding.tvSeries.text = this.Series

                //TODO comment interface...
                binding.cvListItem.setOnClickListener {
                    //Toast.makeText(binding.root.context, "Clicked: ${this.DocumentNumber}", Toast.LENGTH_SHORT).show()
                    callBack(list, position)
                    /*onItemClickListener?.let { click->
                        Toast.makeText(binding.root.context, "Clicked clicklistener: ${this.DocumentNumber}", Toast.LENGTH_SHORT).show()
                        click(list, position)
                    }*/
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