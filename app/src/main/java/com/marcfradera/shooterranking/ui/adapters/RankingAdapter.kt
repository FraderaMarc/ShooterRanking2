package com.marcfradera.shooterranking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.marcfradera.shooterranking.data.model.JugadorRankingItem
import com.marcfradera.shooterranking.databinding.ItemRankingBinding

class RankingAdapter(
    private val onStats: (JugadorRankingItem) -> Unit,
    private val onShotMap: (JugadorRankingItem) -> Unit,
    private val onLongClick: (JugadorRankingItem) -> Unit
) : RecyclerView.Adapter<RankingAdapter.VH>() {

    private val items = mutableListOf<JugadorRankingItem>()

    fun submitList(data: List<JugadorRankingItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            ItemRankingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position + 1)
    }

    inner class VH(private val binding: ItemRankingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: JugadorRankingItem, rank: Int) {
            val pctText = "${(item.pct * 100).toInt()}%"
            val tirosText = "${item.made}/${item.attempted} $pctText"

            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }

            binding.nameText.text = "$rank. ${item.jugador.nom_jugador}"
            binding.dorsalPositionText.text =
                "Dorsal ${item.jugador.numero_jugador} · ${item.jugador.posicio_jugador}"
            binding.sessionsText.text = "Sessions: ${item.sessions}"
            binding.shotsText.text = tirosText

            binding.statsButton.setOnClickListener { onStats(item) }
            binding.shotButton.setOnClickListener { onShotMap(item) }
        }
    }
}