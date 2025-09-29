package com.wms.panchkula.ui.production.adapter.batchCode

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.Global_Classes.GlobalMethods
import com.wms.panchkula.Global_Classes.GlobalMethods.toPrettyJson
import com.wms.panchkula.R
import com.wms.panchkula.databinding.RvItemStagesBinding
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderData
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderStageModel

class StageItemAdapter(
    private val context: Context,
    private val stages: List<ProductionOrderStageModel.Value.ProductionOrdersStage>, private val plannedQuantity: String,
    private val onStageClick: (stagePos: Int, stage: ProductionOrderStageModel.Value.ProductionOrdersStage) -> Unit,
    private val onStageUpdate: (stagePos: Int, stage: ProductionOrderStageModel.Value.ProductionOrdersStage) -> Unit,
) : RecyclerView.Adapter<StageItemAdapter.StageViewHolder>() {

    private var expectedAcceptQty: Double? = 0.0
    private var expectedRejectQty: Double? = 0.0

    inner class StageViewHolder(val binding: RvItemStagesBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val binding = RvItemStagesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        val stage = stages[position]
        with(holder.binding) {
            /*if (stage.ProductionOrderLines.isNotEmpty()) {
                holder.binding.layoutStageItem.setBackgroundResource(R.drawable.bg_stage_item_open)
            } else {
                holder.binding.layoutStageItem.setBackgroundResource(R.drawable.bg_stage_item_freeze)
            }*/
            Log.d("STAGES","Stages List: ${toPrettyJson(stages)}")
            val accept = stage.U_AQty
            val reject = stage.U_RQty

            tvStageName.text = stage.Name
            val openQty: Double = when (position) {
                0 -> plannedQuantity.toDoubleOrNull() ?: 0.0   // first stage = Planned qty
                else -> stages[position - 1].U_AQty //- stages[position - 1].U_RQty  // from previous stage's accepted qty
            }
            stage.OpenQty = openQty
            tvOpenQty.text = openQty.toString() // "100.00"  // for demo

            if (((accept == 0.0 && reject == 0.0) && stage.OpenQty == 0.0) || ((accept > 0.0 && reject >= 0.0) || (accept >= 0.0 && reject > 0.0))) {
                layoutStageItem.alpha = 0.5F
                btnStageUpdate.isEnabled = false
                etAcceptQty.isEnabled = false
                etRejectQty.isEnabled = false
                layoutStageItem.setBackgroundResource(R.drawable.bg_stage_item_freeze)
            } else if ((accept == 0.0 && reject == 0.0) && (position == 0 || (stage.OpenQty ?: 0.0) > 0.0)) {
                layoutStageItem.alpha = 1.0F
                btnStageUpdate.isEnabled = true
                etAcceptQty.isEnabled = true
                etRejectQty.isEnabled = true
                layoutStageItem.setBackgroundResource(R.drawable.bg_stage_item_open)
            } else {
                layoutStageItem.alpha = 1.0F
                btnStageUpdate.isEnabled = true
                etAcceptQty.isEnabled = true
                etRejectQty.isEnabled = true
                layoutStageItem.setBackgroundResource(R.drawable.bg_stage_item_open)
            }
            /*layoutStageItem.setBackgroundResource(
                when {
                    // Case 1: Both Accept & Reject Qty are empty (null or 0) → freeze
                    (accept == 0.0 && reject == 0.0) -> {
                        R.drawable.bg_stage_item_freeze
                    }

                    // Case 2: Either Accept > 0 and Reject = 0 OR Accept = 0 and Reject > 0 → freeze
                    (accept > 0.0 && reject >= 0.0) || (accept >= 0.0 && reject > 0.0) -> {
                        R.drawable.bg_stage_item_freeze
                    }

                    // Case 3: Otherwise → open
                    else -> {
                        R.drawable.bg_stage_item_open
                    }
                }
            )*/


            layoutStageItem.setOnClickListener {
                if (((accept > 0.0 && reject >= 0.0) || (accept >= 0.0 && reject > 0.0))) {
                    GlobalMethods.showError(context, "This stage already updated.")
                } else {
                    onStageClick(position, stage)
                }

            }


            btnStageUpdate.setOnClickListener {
                val accept = etAcceptQty.text?.toString()?.toDoubleOrNull() ?: 0.0
                val reject = etRejectQty.text?.toString()?.toDoubleOrNull() ?: 0.0

                when {
                    /*accept == 0.0 && reject == 0.0 -> {
                        Toast.makeText(root.context, "Both qty cannot be equal to 0", Toast.LENGTH_SHORT).show()
                    }*/
                    accept < 0.0 -> {
                        Toast.makeText(root.context, "Accept qty cannot be less than 0", Toast.LENGTH_SHORT).show()
                    }

                    reject < 0.0 -> {
                        Toast.makeText(root.context, "Reject qty cannot be less than 0", Toast.LENGTH_SHORT).show()
                    }

                    /*accept + reject > openQty -> {
                        Toast.makeText(root.context, "Cannot exceed open qty ($openQty)", Toast.LENGTH_SHORT).show()
                    }*/

                    else -> {

                        stage.OpenQty = openQty
                        stage.AcceptQty = accept
                        stage.RejectQty = reject
                        if (stage.U_Status == "Yes") {
                            onStageUpdate(position, stage)
                        } else {
                            GlobalMethods.showError(context, "You can't update this stage until the required RM items have been issued for it.")
                        }

                    }
                }
            }

            /*etAcceptQty.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })*/

            // Accept Qty Change Listener
            etAcceptQty.doAfterTextChanged { editable ->
                val accept = editable?.toString()?.toDoubleOrNull() ?: return@doAfterTextChanged
                stage.AcceptQty = accept
                //onStageUpdate(position, stage)
                //expectedRejectQty = openQty - accept

                /*if (reject >= 0) {
                    if (etRejectQty.text.toString() != reject.toString()) {
                        etRejectQty.setText(reject.toString())
                    }
                    stage.AcceptQty = accept
                    stage.RejectQty = reject
                    onStageUpdate(position, stage,null)
                }*/
            }

            // Reject Qty Change Listener
            etRejectQty.doAfterTextChanged { editable ->
                val reject = editable?.toString()?.toDoubleOrNull() ?: return@doAfterTextChanged
                //expectedAcceptQty = openQty - reject
                stage.RejectQty = reject
                //onStageUpdate(position, stage)
                /*if (accept >= 0) {
                    if (etAcceptQty.text.toString() != accept.toString()) {
                        etAcceptQty.setText(accept.toString())
                    }
                    stage.AcceptQty = accept
                    stage.RejectQty = reject
                    onStageUpdate(position, stage, null)
                }*/
            }
        }
    }

    override fun getItemCount(): Int = stages.size
}

