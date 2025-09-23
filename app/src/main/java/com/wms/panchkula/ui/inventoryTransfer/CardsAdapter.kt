package com.wms.panchkula.ui.inventoryTransfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wms.panchkula.R
import com.wms.panchkula.ui.inventoryTransfer.model.CardItem

class CardItemAdapter(private val cardItems: List<CardItem>, private val listener: InventoryLineTransferActivity) : RecyclerView.Adapter<CardItemAdapter.CardItemViewHolder>() {

    interface OnCardItemClickListener {
        fun onCardItemClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item_layout, parent, false)
        return CardItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardItemViewHolder, position: Int) {
        val cardItem = cardItems[position]
        holder.bind(cardItem)
    }

    override fun getItemCount(): Int {
        return cardItems.size
    }

    inner class CardItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)


        fun bind(cardItem: CardItem) {
            tvDate.text = cardItem.title


        }
    }
}
