package com.wms.panchkula.ui.goodsOrder.adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.databinding.GoodsLineItemAdapterBinding
import com.wms.panchkula.ui.goodsOrder.autocomplete.WareHouseGoodAutoCompleteAdapter
import com.wms.panchkula.ui.goodsOrder.model.LocalListForGoods
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems
import com.google.android.material.textfield.TextInputEditText
import com.wms.panchkula.Global_Classes.GlobalMethods.setDecimalLimit

class GoodsChanchalAdapter(private var context: Context, private var scannedItemForGood: MutableList<LocalListForGoods>, private val tvHeaderQuantity: TextView, private val listener: OnItemActionListener) : RecyclerView.Adapter<GoodsChanchalAdapter.ViewHolder>() {

    interface OnItemActionListener {
        fun onQuantityChanged(position: Int, newQuantity: String, tvBatchQuantity : TextInputEditText)
        fun onItemRemoved(position: Int)
        fun onWareHouseChanged(position: Int, newQuantity: String, warehouse:String, currentItem: LocalListForGoods)
    }

    var headerQuantString=""

    private var onDeleteItemClick: OnDeleteItemClickListener? = null

    interface OnDeleteItemClickListener {
        fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap: ArrayList<String>, pos: Int, batchTypeList: ArrayList<String>, serialTypeList: ArrayList<String>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = GoodsLineItemAdapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(scannedItemForGood[position], position)

    }


    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return scannedItemForGood.size
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class ViewHolder(val binding: GoodsLineItemAdapterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(currentItem: LocalListForGoods, position: Int) {
            binding.tableRowWareHouse.visibility = View.GONE
            binding.tvDocEntry.text = ":   "+currentItem.DocEntry
            binding.tvItemCode.text = ":   "+ currentItem.ItemCode
            binding.tvItemDesc.text = ":   "+currentItem.ItemDescription

            setDecimalLimit(binding.tvBatchQuantity,15,4)
            binding.sizeLayout.visibility = View.GONE
            currentItem.Size = binding.edSize.text.toString().trim()

            // Create the custom adapter with the list of listings
            val adapter = WareHouseGoodAutoCompleteAdapter(itemView.context, currentItem.wareHouseListing)

            // Set the adapter to the AutoCompleteTextView
            binding.acWareHouseCode.setAdapter(adapter)

            // Set item click listener
            binding.acWareHouseCode.onItemClickListener = AdapterView.OnItemClickListener { _, _, positionAc, _ ->
                val selectedItem = adapter.getItem(positionAc)
                selectedItem?.let {
                    // Toast.makeText(itemView.context, "Selected: ${it.Quantity}, ID: ${it.Warehouse}", Toast.LENGTH_SHORT).show()
                    binding.acWareHouseCode.setText(it.Warehouse)

                    if (currentItem.ScanType.equals("Serial")){

                        listener.onWareHouseChanged(position,"1",currentItem.wareHouseListing[positionAc].Warehouse, currentItem)

                        binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal("1").toString())
                    }else{

                        listener.onWareHouseChanged(position,currentItem.wareHouseListing[positionAc].Quantity,currentItem.wareHouseListing[positionAc].Warehouse, currentItem)

                        Log.e("it.Quantity)=>",(it.Quantity).toString())
                        binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal(it.Quantity).toString())

                    }

                    // Create the custom adapter with the list of listings
                    val adapter = WareHouseGoodAutoCompleteAdapter(itemView.context, currentItem.wareHouseListing)

                    // Set the adapter to the AutoCompleteTextView
                    binding.acWareHouseCode.setAdapter(adapter)
                    //  maxQuantity = GlobalMethods.changeDecimal(it.Quantity)!!.toDoubleOrNull() ?: 0.0  // Update maxQuantity


                }

            }
            binding.scanedItemLayout.setOnClickListener {
             }


            if (currentItem.SerialNumber == null){
                binding.tvBatchType.text = "Batch No."
                binding.tvBatch.text = ":   "+ currentItem.Batch
                binding.batchQtyLayout.visibility = View.VISIBLE
                binding.tvSerialQty.visibility = View.GONE

            }

            else if (currentItem.SerialNumber == "" && currentItem.Batch == ""){
                binding.tvBatchType.text = "None"
                binding.tvBatch.text = ":  "+currentItem.NoneVal
                binding.batchQtyLayout.visibility = View.VISIBLE
                binding.tvSerialQty.visibility = View.GONE

                binding.tvBatchType.visibility = View.GONE
                binding.tvBatch.visibility = View.GONE
                binding.tvUOM.visibility = View.GONE
                binding.tvDocEntry.visibility = View.GONE

                binding.tvItemCode.text = ":   "+ currentItem.ItemCode
                binding.tvItemDesc.text = ":   "+currentItem.ItemDescription
                binding.tvWidth.text    = ":   NA"
                binding.tvBatchLength.text = ":   NA"
                binding.tvBatchGsm.text    = ":   NA"
                binding.tvBatchGrossWeigth.text = ":   NA"

            }
            else{
                binding.tvBatchType.text = "Serial No."
                binding.tvBatch.text = ":   "+ currentItem.SerialNumber
                binding.batchQtyLayout.visibility = View.GONE
                binding.tvSerialQty.visibility = View.VISIBLE
//                binding.tableRowWareHouse.visibility = View.GONE
            }


            binding.tvSerialQty.text = ":   " + GlobalMethods.changeDecimal(currentItem.Quantity)

        //    binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal(currentItem.Quantity).toString())

            binding.tvBatchQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val newQuantity = s.toString() ?: "0.0"
                    var QUANTITYVAL = newQuantity
                    Log.e("onQuantityChanged=>B",newQuantity.toString())

                    if (QUANTITYVAL.isEmpty()) {
                        QUANTITYVAL = "0.0"
                    }




                  listener.onQuantityChanged(adapterPosition, newQuantity, binding.tvBatchQuantity)
                }
                override fun afterTextChanged(s: Editable?) {



                }
            })

            binding.ivDelete.setOnClickListener {
                listener.onItemRemoved(position)
            }


        }


    }

    fun setOnDeleteItemClickListener(listener: OnDeleteItemClickListener) {
        onDeleteItemClick = listener
    }


    fun removeItem(position: Int) {
        scannedItemForGood.removeAt(position)
//        notifyItemRemoved(position)
//        notifyItemRangeChanged(position, scannedItemForGood.size)
        notifyDataSetChanged()
    }


}