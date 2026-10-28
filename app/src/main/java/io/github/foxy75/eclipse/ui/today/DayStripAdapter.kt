package io.github.foxy75.eclipse.ui.today

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.core.Dates
import io.github.foxy75.eclipse.databinding.ItemDayStripBinding

class DayStripAdapter(
    private val days: List<Long>,
    private val factDays: Set<Long>,
    private val predictedDays: Set<Long>,
    private var selectedDay: Long,
    private val onSelect: (Long) -> Unit
) : RecyclerView.Adapter<DayStripAdapter.VH>() {

    class VH(val binding: ItemDayStripBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDayStripBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = days.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val day = days[position]
        val ctx = holder.binding.root.context
        val weekdays = ctx.resources.getStringArray(R.array.weekdays_short)

        holder.binding.dayLetter.text = weekdays[Dates.weekday(day) - 1]
        holder.binding.dayNum.text = Dates.dayOfMonth(day).toString()

        val selected = day == selectedDay
        holder.binding.dayNum.setTextColor(ctx.resources.getColor(
            if (selected) R.color.white else R.color.ink, null
        ))
        holder.binding.dayNum.setBackgroundResource(
            if (selected) R.drawable.bg_pill_active else R.color.transparent
        )

        val dot = holder.binding.dot
        when {
            day in factDays -> {
                dot.visibility = android.view.View.VISIBLE
                dot.setBackgroundResource(R.drawable.bg_dot_terracotta)
            }
            day in predictedDays -> {
                dot.visibility = android.view.View.VISIBLE
                dot.setBackgroundResource(R.drawable.bg_dot_teal)
            }
            else -> dot.visibility = android.view.View.INVISIBLE
        }

        holder.binding.root.setOnClickListener { onSelect(day) }
    }
}