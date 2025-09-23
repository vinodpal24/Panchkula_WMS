package com.wms.panchkula.ui.goodsOrder.adapter

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

class GoodsItemLineAdapter (private var context: Context, private var scanedBatchedItemsList_gl: ArrayList<ScanedOrderBatchedItems.Value>,
                            private  var quantityHashMap: ArrayList<String>, private var flag : String,private  var batchTypeList: ArrayList<String>,
                            private  var serialTypeList: ArrayList<String>, private var onTextChanged: (String, Int) -> Unit) : RecyclerView.Adapter<GoodsItemLineAdapter.ViewHolder>() {

    constructor(
        context: Context,
        scannedBatchedItemsList: ArrayList<ScanedOrderBatchedItems.Value>,
        quantityHashMap: ArrayList<String>,
        flag: String, batchTypeList: ArrayList<String>, serialTypeList: ArrayList<String>
    ) : this(context, scannedBatchedItemsList, quantityHashMap, flag, batchTypeList, serialTypeList, {_, _->}) {
        // Additional initialization code if needed

        this.scanedBatchedItemsList_gl = scannedBatchedItemsList
        this.context = context
        this.quantityHashMap = quantityHashMap
        this.flag = flag
        this.batchTypeList = batchTypeList
        this.serialTypeList = serialTypeList

    }


    private var onDeleteItemClick: OnDeleteItemClickListener? = null
    interface OnDeleteItemClickListener {
        fun onDeleteItemClick(list: ArrayList<ScanedOrderBatchedItems.Value>, quantityHashMap: ArrayList<String>, pos: Int,
                              batchTypeList: ArrayList<String>, serialTypeList: ArrayList<String>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = BatchItemsScannedLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(scanedBatchedItemsList_gl[position]) {

               /* if (flag.equals("SerialQR")){
                    binding.tvBatchType.text = "Serial No."
                    binding.tvBatch.text = ":   "+ this.SerialNumber
                }else{
                    binding.tvBatchType.text = "Batch No."
                    binding.tvBatch.text = ":   "+ this.Batch
                }*/
                setDecimalLimit(binding.tvBatchQuantity,15,4)
                binding.sizeLayout.visibility = View.VISIBLE
                this.Size = binding.edSize.text.toString().trim()

                binding.tvDocEntry.text = ":   "+this.DocEntry
                binding.tvItemCode.text = ":   "+ this.ItemCode
                binding.tvItemDesc.text = ":   "+this.ItemDescription
                binding.tvWidth.text = ":   "+ this.U_Width.toString()
                binding.tvBatchLength.text = ":   "+this.U_Length.toString()
                binding.tvBatchGsm.text = ":   "+this.U_GSM.toString()
                binding.tvBatchGrossWeigth.text = ":   "+this.U_GW.toString()


                if (this.SerialNumber == null){
                    binding.tvBatchType.text = "Batch No."
                    binding.tvBatch.text = ":   "+ this.Batch
                    binding.batchQtyLayout.visibility = View.VISIBLE
                    binding.tvSerialQty.visibility = View.GONE
                }
                else if (this.SerialNumber == "" && this.Batch == ""){
                    binding.tvBatchType.text = "None"
                    binding.tvBatch.text = ":   NA"
                    binding.batchQtyLayout.visibility = View.VISIBLE
                    binding.tvSerialQty.visibility = View.GONE

                    binding.tvDocEntry.text = ":   NA"
                    binding.tvItemCode.text = ":   NA"
                    binding.tvItemDesc.text = ":   NA"
                    binding.tvWidth.text = ":   NA"
                    binding.tvBatchLength.text = ":   NA"
                    binding.tvBatchGsm.text = ":   NA"
                    binding.tvBatchGrossWeigth.text = ":   NA"

                }
                else{
                    binding.tvBatchType.text = "Serial No."
                    binding.tvBatch.text = ":   "+ this.SerialNumber
                    binding.batchQtyLayout.visibility = View.GONE
                    binding.tvSerialQty.visibility = View.VISIBLE
                }






                binding.ivDelete.visibility = View.GONE



                binding.ivDelete.setOnClickListener {
                    /*if (flag.equals("SerialQR")){
                        onDeleteItemClick?.onDeleteItemClick(scanedBatchedItemsList_gl, ArrayList(), position)
                    }else{

                    }*/
                    onDeleteItemClick?.onDeleteItemClick(scanedBatchedItemsList_gl, quantityHashMap, position, batchTypeList, serialTypeList)

                }


                with(quantityHashMap[position]){
                    binding.tvSerialQty.text = ":   "+ GlobalMethods.changeDecimal(this)

                    binding.tvBatchQuantity.setText(GlobalMethods.changeDecimal(this).toString())
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
    }


    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return scanedBatchedItemsList_gl.size
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    class ViewHolder(val binding: BatchItemsScannedLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun setOnDeleteItemClickListener(listener: OnDeleteItemClickListener) {
        onDeleteItemClick = listener
    }




}