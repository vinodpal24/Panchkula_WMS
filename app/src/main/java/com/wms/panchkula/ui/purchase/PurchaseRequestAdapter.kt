package com.wms.panchkula.ui.purchase

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.R
import com.wms.panchkula.databinding.RvItemPrimaryBinding
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import kotlin.collections.ArrayList

class PurchaseRequestAdapter(var list: ArrayList<PurchaseRequestModel.Value>) :
    RecyclerView.Adapter<PurchaseRequestAdapter.ViewHolder>() {

    //TODO comment interface declare...
    private var onItemClickListener: ((List<PurchaseRequestModel.Value>, pos: Int,layout:LinearLayout) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemPrimaryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(list[position]) {
                binding.tvDocNum.text = this.DocNum
                binding.layoutPoItem.setBackgroundResource(if(this.isItemSelected) R.drawable.rounded_border_grey_selected else R.drawable.rounded_border_ligh_grey)
                //binding.docNum.text = "Doc Entry  : "
                //binding.tvProd.text = this.DocEntry

                binding.tvDocDate.text =
                    GlobalMethods.convert_yyyy_MM_dd_T_hh_mm_ss_into_ddMMYYYY(this.DocDate)

                //TODO comment interface...
                binding.cvListItem.setOnClickListener {
                    onItemClickListener?.let { click ->


                        click(list, position,binding.layoutPoItem)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    class ViewHolder(val binding: RvItemPrimaryBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun OnItemClickListener(listener: (List<PurchaseRequestModel.Value>, pos: Int,layout:LinearLayout) -> Unit) {
        onItemClickListener = listener
    }

    //todo filter search list call this function whenever the search query changes and list update..

    fun setFilteredItems(filteredItems: ArrayList<PurchaseRequestModel.Value>) {
        list = filteredItems
        notifyDataSetChanged()
    }

    fun clearItems() {
        list.clear()
        Log.e("Clear==>", "" + list.size)
        notifyDataSetChanged()
    }

}