package com.wms.panchkula.ui.issueForProductionOrder.Adapter

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.databinding.BatchItemsScannedLayoutBinding
import com.wms.panchkula.Global_Classes.GlobalMethods.setDecimalLimit
import com.wms.panchkula.ui.issueForProductionOrder.Model.ScanedOrderBatchedItems

class batchDemoAdapter(
    private var context: Context,
    private var scanedBatchedItemsList_gl: MutableList<ScanedOrderBatchedItems.Value>,
    private var quantityHashMap: MutableList<String>,
    private var flag: String,
    private var listener: OnItemActionListener,
    private var onTextChanged: (String, Int) -> Unit,
) : RecyclerView.Adapter<batchDemoAdapter.ViewHolder>() {

    constructor(context: Context, scannedBatchedItemsList: MutableList<ScanedOrderBatchedItems.Value>, quantityHashMap: MutableList<String>, flag: String, listener: OnItemActionListener) : this(context, scannedBatchedItemsList, quantityHashMap, flag,listener,{ _, _ -> }) {
        // Additional initialization code if needed

        this.scanedBatchedItemsList_gl = scannedBatchedItemsList
        this.context = context
        this.quantityHashMap = quantityHashMap
        this.flag = flag
        this.listener = listener
        this.onTextChanged = { _, _ ->}
    }


    private var onDeleteItemClick: OnDeleteItemClickListener? = null

    interface OnDeleteItemClickListener {
        fun onDeleteItemClick(list: MutableList<ScanedOrderBatchedItems.Value>, quantityHashMap: MutableList<String>, pos: Int)
    }

    interface OnItemActionListener {
        fun onItemRemoved(list: MutableList<ScanedOrderBatchedItems.Value>, quantityHashMap: MutableList<String>, pos: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BatchItemsScannedLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(scanedBatchedItemsList_gl[position],position)

        with(holder) {
            with(scanedBatchedItemsList_gl[position]) {
                setDecimalLimit(binding.tvBatchQuantity,15,4)
                quantityHashMap[position].let {
                    binding.tvSerialQty.text = ":   " + GlobalMethods.changeDecimal(it)

//                binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal(it).toString())//todo
                    binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal(it).toString())
                }

                binding.ivDelete.setOnClickListener {
                    /*if (flag.equals("SerialQR")){
                        onDeleteItemClick?.onDeleteItemClick(scanedBatchedItemsList_gl, ArrayList(), position)
                    }else{

                    }*/
                    onDeleteItemClick?.onDeleteItemClick(scanedBatchedItemsList_gl, quantityHashMap, position)
                    listener.onItemRemoved(scanedBatchedItemsList_gl, quantityHashMap, position)
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return scanedBatchedItemsList_gl.size
    }

    inner class ViewHolder(val binding: BatchItemsScannedLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(value: ScanedOrderBatchedItems.Value, pos: Int) {

            binding.tvDocEntry.text = ":   " + value.DocEntry
            binding.tvItemCode.text = ":   " + value.ItemCode
            binding.tvItemDesc.text = ":   " + value.ItemDescription
            binding.tvWidth.text = ":   " + value.U_Width.toString()
            binding.tvBatchLength.text = ":   " + value.U_Length.toString()
            binding.tvBatchGsm.text = ":   " + value.U_GSM.toString()
            binding.tvBatchGrossWeigth.text = ":   " + value.U_GW.toString()


            if (flag.equals("SerialQR")) {
                binding.tvBatchType.text = "Serial No."
                binding.tvBatch.text = ":   " + value.SerialNumber
            }
            else if (flag.equals("NoneQR")){
                binding.tvBatchType.text = "None No."
                binding.tvBatch.text = ":   " + value.NoneVal
//                binding.ivDelete.visibility = View.GONE
            }
            else {
                binding.tvBatchType.text = "Batch No."
                binding.tvBatch.text = ":   " + value.Batch
            }


            if (flag.equals("SerialQR")) {
                binding.batchQtyLayout.visibility = View.GONE
                binding.tvSerialQty.visibility = View.VISIBLE
            } /*else if (flag.equals("NoneQR")){
                binding.batchQtyLayout.visibility = View.GONE
                binding.tvSerialQty.visibility = View.VISIBLE
            }*/else {
                binding.batchQtyLayout.visibility = View.VISIBLE
                binding.tvSerialQty.visibility = View.GONE
            }


            binding.tvBatchQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    quantityHashMap[position] = charSequence.toString()
                    onTextChanged(charSequence.toString(), position)
                }

                override fun afterTextChanged(editable: Editable) {}
            })


        }
    }

    fun setOnDeleteItemClickListener(listener: OnDeleteItemClickListener) {
        onDeleteItemClick = listener
    }


    fun removeItem(position: Int) {
//        scanedBatchedItemsList_gl.removeAt(position)
        quantityHashMap.removeAt(position)
        notifyItemRemoved(position)
//        notifyItemRangeChanged(position, scanedBatchedItemsList_gl.size)
        notifyItemRangeChanged(position, quantityHashMap.size)
    }


}