package com.wms.panchkula.ui.returnComponents.adapter


import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.GlobalMethods.toSimpleJson
import com.wms.panchkula.R
import com.wms.panchkula.databinding.RvItemReturnComponentBinding
import com.wms.panchkula.ui.goodsreceipt.model.IssueFromModel
import com.wms.panchkula.ui.issueForProductionOrder.Model.ProductionListModel

class ReturnComponentItemAdapter(
    private val context: Context,
    private val binLocationList: List<IssueFromModel.Value>,
    private val item: ProductionListModel.ProductionOrderLine
) : RecyclerView.Adapter<ReturnComponentItemAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: RvItemReturnComponentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @RequiresApi(Build.VERSION_CODES.O)
        fun bind(installation: IssueFromModel.Value, position: Int) {
            Log.e("RETURN_COMPONENT", "ReturnComponentItemAdapter=> item List: ${toSimpleJson(item)}")
            binding.layoutBatch.visibility =
                if (installation.Batch.isEmpty()) View.GONE else View.VISIBLE

            binding.tvBatch.text = installation.Batch
            binding.tvIssueQuantity.text = installation.IssueQuantity.toString()

            // ✅ 1. If binAllocationJSONs is not empty, set default qty
            val matchedQty = if (!item.binAllocationJSONs.isNullOrEmpty()) {
                item.binAllocationJSONs
                    .firstOrNull { it.BatchNum == installation.Batch }
                    ?.Quantity?.toDoubleOrNull() ?: 0.0
            } else {
                0.0
            }

            // ✅ 2. Set it as default in EditText
            binding.edReturnQuantity.setText(if(matchedQty > 0.0) matchedQty.toString() else "")

            // ✅ 3. Update background if qty > 0
            binding.layoutCard.background = if (matchedQty > 0.0) {
                ContextCompat.getDrawable(context, R.drawable.rounded_border_ligh_grey_selected)
            } else {
                ContextCompat.getDrawable(context, R.drawable.rounded_border_ligh_grey)
            }

            // ✅ 4. Store this value in model so it’s tracked
            installation.EnteredQTY = matchedQty.toString()

            binding.edReturnQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val value = s.toString().trim()
                    if (value.isNotEmpty()) {
                        try {
                            val quantity = value.toDoubleOrNull() ?: 0.0

                            // Background update
                            binding.layoutCard.background = if (quantity > 0.0) {
                                ContextCompat.getDrawable(context, R.drawable.rounded_border_ligh_grey_selected)
                            } else {
                                ContextCompat.getDrawable(context, R.drawable.rounded_border_ligh_grey)
                            }

                            // Validate
                            if (quantity <= installation.IssueQuantity) {
                                installation.EnteredQTY = value
                            } else {
                                GlobalMethods.showError(
                                    context,
                                    "Entered Quantity exceeded. Please enter a valid quantity."
                                )
                                binding.edReturnQuantity.setText("0.0")
                                installation.EnteredQTY = "0.0"
                            }

                        } catch (e: NumberFormatException) {
                            binding.layoutCard.background = ContextCompat.getDrawable(
                                context,
                                R.drawable.rounded_border_ligh_grey
                            )
                        }
                    } else {
                        binding.layoutCard.background = ContextCompat.getDrawable(
                            context,
                            R.drawable.rounded_border_ligh_grey
                        )
                    }
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RvItemReturnComponentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(binLocationList[position], position)
    }

    override fun getItemCount(): Int = binLocationList.size
}