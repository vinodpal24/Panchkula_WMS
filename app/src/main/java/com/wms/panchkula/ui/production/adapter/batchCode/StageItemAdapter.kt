package com.wms.panchkula.ui.production.adapter.batchCode

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.R
import com.wms.panchkula.databinding.RvItemStagesBinding
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderData
import com.wms.panchkula.ui.production.model.batchCode.ProductionOrderStageModel

class StageItemAdapter(
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
            if (stage.ProductionOrderLines.isNotEmpty()) {
                holder.binding.layoutStageItem.setBackgroundResource(R.drawable.bg_stage_item_open)
            } else {
                holder.binding.layoutStageItem.setBackgroundResource(R.drawable.bg_stage_item_freeze)
            }
            tvStageName.text = stage.Name
            val openQty: Double = when (position) {
                0 -> plannedQuantity.toDoubleOrNull() ?: 0.0   // first stage = Planned qty
                else -> stages[position - 1].U_AQty //- stages[position - 1].U_RQty  // from previous stage's accepted qty
            }
            tvOpenQty.text = openQty.toString() // "100.00"  // for demo


            root.setOnClickListener {
                onStageClick(position, stage)
            }


            btnStageUpdate.setOnClickListener {
                val accept = etAcceptQty.text?.toString()?.toDoubleOrNull() ?: 0.0
                val reject = etRejectQty.text?.toString()?.toDoubleOrNull() ?: 0.0

                when {
                    accept == 0.0 && reject == 0.0 -> {
                        Toast.makeText(root.context, "Both qty cannot be equal to 0", Toast.LENGTH_SHORT).show()
                    }
                    accept < 0.0 -> {
                        Toast.makeText(root.context, "Accept qty cannot be less than 0", Toast.LENGTH_SHORT).show()
                    }

                    reject < 0.0 -> {
                        Toast.makeText(root.context, "Reject qty cannot be less than 0", Toast.LENGTH_SHORT).show()
                    }

                    accept + reject > openQty -> {
                        Toast.makeText(root.context, "Cannot exceed open qty ($openQty)", Toast.LENGTH_SHORT).show()
                    }

                    else -> {
                        stage.AcceptQty = accept
                        stage.RejectQty = reject
                        onStageUpdate(position, stage)
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

