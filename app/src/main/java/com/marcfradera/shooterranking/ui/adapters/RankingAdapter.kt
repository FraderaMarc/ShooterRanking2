package com.marcfradera.shooterranking.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.marcfradera.shooterranking.R
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position + 1)

    inner class VH(private val binding: ItemRankingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: JugadorRankingItem, rank: Int) {
            val context = binding.root.context
            val pctText = "${(item.pct * 100).toInt()}%"
            binding.root.setOnLongClickListener { onLongClick(item); true }
            binding.nameText.text = "$rank. ${item.jugador.nom_jugador}"
            binding.dorsalPositionText.text = context.getString(
                R.string.jersey_position_format,
                if (item.jugador.numero_jugador == 100) "00" else item.jugador.numero_jugador.toString(),
                when (item.jugador.posicio_jugador.trim().lowercase()) {
                    "1", "base" -> context.getString(R.string.position_point_guard)
                    "2", "escolta" -> context.getString(R.string.position_shooting_guard)
                    "3", "aler" -> context.getString(R.string.position_small_forward)
                    "4", "aler-pivot", "aler pivot" -> context.getString(R.string.position_power_forward)
                    "5", "pivot", "pívot" -> context.getString(R.string.position_center)
                    else -> context.getString(R.string.position_unknown)
                }
            )
            binding.sessionsText.text = context.getString(R.string.sessions_count, item.sessions)
            binding.shotsText.text = "${item.made}/${item.attempted} $pctText"
            binding.statsButton.text = context.getString(R.string.stats_short)
            binding.shotButton.text = context.getString(R.string.map_short)
            binding.statsButton.setOnClickListener { onStats(item) }
            binding.shotButton.setOnClickListener { onShotMap(item) }
        }
    }
}
