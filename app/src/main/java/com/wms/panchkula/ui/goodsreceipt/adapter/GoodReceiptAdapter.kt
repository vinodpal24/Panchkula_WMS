package com.wms.panchkula.ui.goodsreceipt.adapter

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.R
import com.wms.panchkula.databinding.ReceiptOrderListAdapterLayoutBinding
import com.google.android.material.button.MaterialButton
import com.wms.panchkula.Global_Classes.AppConstants
import com.wms.panchkula.Model.ModelBinLocation
import com.wms.panchkula.ui.goodsreceipt.model.GetItemstModel
import com.wms.panchkula.ui.pickList.DynamicFieldPickListTransferAdapter
import com.wms.panchkula.ui.purchase.model.PurchaseRequestModel
import kotlin.collections.ArrayList

class GoodReceiptAdapter(private var context: Context, var list: ArrayList<GetItemstModel.Value>) :
    RecyclerView.Adapter<GoodReceiptAdapter.ViewHolder>() {

    //TODO comment interface declare...
    private var onItemClickListener: ((List<GetItemstModel.Value>, pos: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ReceiptOrderListAdapterLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(list[position]) {

                binding.edSize.visibility = View.VISIBLE
                this.Size = binding.edSize.text.toString().trim()


                if (this.ItemType.equals("BATCH",true))
                {
                    binding.batchNo.visibility = View.VISIBLE
                }
                binding.tvItemCode.text = this.ItemCode
                //binding.docEntry.text = "Item Code :"

                //binding.docDateTxt.text = "Item Name  : "
                //binding.tvProd.text = this.ItemCode

                binding.tvItemName.text =this.ItemName

                binding.edPrice.addTextChangedListener { text ->

                    list[position].UnitPrice = text.toString().trim()
                }
                binding.edQty.addTextChangedListener { text ->
                    list[position].Quantity = text.toString().trim()
                }
                binding.batchNo.addTextChangedListener { text ->
                    list[position].BatchNo = text.toString().trim()
                }



                //TODO comment interface...
                binding.cvListItem.setOnClickListener {
                    onItemClickListener?.let { click ->
                        click(list, position)

                       // openDynamicFieldsDialog(context,position,binding.edQty)

                    }
                }

                binding.ivRemoveGoodsItem.setOnClickListener {
                    /*list.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, list.size)*/
                    val removedItem = list[position]

                    // Remove from the main list
                    list.removeAt(position)

                    // Also remove from selected-related lists
                    AppConstants.selectedList.remove(removedItem)

                    // Notify adapter changes
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, list.size)


                }


            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    class ViewHolder(val binding: ReceiptOrderListAdapterLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    fun OnItemClickListener(listener: (List<GetItemstModel.Value>, pos: Int) -> Unit) {
        onItemClickListener = listener
    }

    //todo filter search list call this function whenever the search query changes and list update..

    fun setFilteredItems(filteredItems: ArrayList<GetItemstModel.Value>) {
        list = filteredItems
        notifyDataSetChanged()
    }

    fun clearItems() {
        list.clear()
        Log.e("Clear==>", "" + list.size)
        notifyDataSetChanged()
    }


    private val binLocation= mutableListOf<ModelBinLocation>()

    private lateinit var adapterFields: DynamicFieldPickListTransferAdapter
    var temp = ArrayList<String>()
   private fun openDynamicFieldsDialog(context: Context, po: Int, tvTotalScannQty : TextView) {

        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_dynamic_fields_sale_transfer)
        dialog.setCancelable(false)
        // Ensure the background is transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog width to MATCH_PARENT
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)

        val btnAddBin: ImageView = dialog.findViewById(R.id.btnAddBin)
        val btnSave: MaterialButton = dialog.findViewById(R.id.btnSave)
        val btnCancel: MaterialButton = dialog.findViewById(R.id.btnCancel)
        val tvItemQty: TextView = dialog.findViewById(R.id.tvItemQty)
        val addBin_Txt: TextView = dialog.findViewById(R.id.addBin_Txt)
        val rvDynamicField: RecyclerView = dialog.findViewById(R.id.rvDynamicField)

       setFieldAdapters(context,rvDynamicField,temp,"YYYX","Y",temp)

        if(!binLocation.isEmpty())
            binLocation.clear()
        binLocation.add(ModelBinLocation())


        btnAddBin.setOnClickListener {
            binLocation.add(ModelBinLocation())
            adapterFields.notifyItemInserted(binLocation.size - 1)
        }

        btnSave.setOnClickListener {
            Log.e("BinData=>",binLocation.toString())


            val myArrayList = ArrayList<PurchaseRequestModel.binAllocationJSONs>()
            var totalQty =0.0;

            for (j in binLocation.indices)
            {
                if(!binLocation.get(j).itemQuantity.trim().isEmpty())
                    totalQty += binLocation.get(j).itemQuantity.trim().toDouble()



                /*var binAllocationJSONs = PurchaseRequestModel.binAllocationJSONs(
                    binLocation.get(j).batchNumber.trim(),
                    binLocation.get(j).binLocationCode,
                    binLocation.get(j).itemQuantity.trim(),
                    binLocation.get(j).WareHouseCode.trim(),
                    binLocation.get(j).toBinLocationCode.trim()
                )*/

                var binAllocationJSONs = PurchaseRequestModel.binAllocationJSONs(
                    binLocation.get(j).binLocation.trim(),
                    binLocation.get(j).binLocationCode.trim(),
                    binLocation.get(j).batchNumber.trim(),
                    binLocation.get(j).itemQuantity.trim(),
                    binLocation.get(j).WareHouseCode.trim(),
                    binLocation.get(j).toBinLocationCode.trim(),
                    binLocation.get(j).ManufacturerSerialNumber.trim(),
                    binLocation.get(j).InternalSerialNumber.trim(),
                    binLocation.get(j).ExpiryDate.trim(),
                    binLocation.get(j).ManufacturingDate.trim()
                )
                myArrayList.add(binAllocationJSONs)


                Log.e("BinData=>",myArrayList.toString())
            }
            if(totalQty>list[po].Quantity.toDouble())
            {
                Toast.makeText(context, "Quantity exceeded. ", Toast.LENGTH_SHORT).show()

            }
            else{
                tvTotalScannQty.setText(totalQty.toString())
                // Toast.makeText(context, "Data saved: ${inputData.size}", Toast.LENGTH_SHORT).show()

                list[po].binAllocationJSONs = arrayListOf()
                list[po].binAllocationJSONs.addAll(myArrayList)
                list[po].Quantity = totalQty.toString()
                dialog.dismiss()
                binLocation.clear()
            }
  }
        btnCancel.setOnClickListener {
        dialog.dismiss()



        }

        dialog.show()
    }
    private fun setFieldAdapters(context: Context,rvDynamicField: RecyclerView, binLocationList: List<String>,scanType:String,binManaged:String,binAbs: List<String>)
           {
        rvDynamicField.apply {
            // Initialize RecyclerView
            adapterFields = DynamicFieldPickListTransferAdapter(context,binLocationList,binAbs,
                binLocation,

                onRemoveItem = { parentPosition ->
                    binLocation.removeAt(parentPosition)
                    adapterFields?.notifyDataSetChanged()
                }
                ,scanType,binManaged
            )

            layoutManager = LinearLayoutManager(context)
            adapter = adapterFields

        }
    }


}