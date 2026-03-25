package com.marcfradera.shooterranking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.marcfradera.shooterranking.databinding.ItemEquipBinding
import com.marcfradera.shooterranking.ui.vm.EquipUiItem

class EquipsAdapter(
    private val onClick: (EquipUiItem) -> Unit,
    private val onLongClick: (EquipUiItem) -> Unit
) : RecyclerView.Adapter<EquipsAdapter.VH>() {

    private val items = mutableListOf<EquipUiItem>()

    fun submitList(data: List<EquipUiItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            ItemEquipBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val binding: ItemEquipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EquipUiItem) {
            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }

            binding.titleText.text = item.equip.nom_equip
            binding.detailText.text = "Jugadores: ${item.jugadorsCount}"
        }
    }
}