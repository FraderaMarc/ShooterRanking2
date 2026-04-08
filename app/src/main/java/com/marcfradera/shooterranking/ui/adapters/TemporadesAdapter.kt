package com.marcfradera.shooterranking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.marcfradera.shooterranking.databinding.ItemTemporadaBinding
import com.marcfradera.shooterranking.ui.vm.TemporadaUiItem

class TemporadesAdapter(
    private val onClick: (TemporadaUiItem) -> Unit,
    private val onEdit: (TemporadaUiItem) -> Unit,
    private val onDelete: (TemporadaUiItem) -> Unit
) : RecyclerView.Adapter<TemporadesAdapter.VH>() {

    private val items = mutableListOf<TemporadaUiItem>()

    fun submitList(data: List<TemporadaUiItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            ItemTemporadaBinding.inflate(
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

    inner class VH(private val binding: ItemTemporadaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TemporadaUiItem) {
            binding.root.setOnClickListener { onClick(item) }

            binding.root.setOnLongClickListener { view ->
                PopupMenu(view.context, view).apply {
                    menu.add(0, 1, 0, "Editar")
                    menu.add(0, 2, 1, "Eliminar")

                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            1 -> {
                                onEdit(item)
                                true
                            }
                            2 -> {
                                onDelete(item)
                                true
                            }
                            else -> false
                        }
                    }

                    show()
                }
                true
            }

            binding.titleText.text = "${item.temporada.any_inici}-${item.temporada.any_fi}"
            binding.detailText.text = "Equips: ${item.equipsCount}"
        }
    }
}