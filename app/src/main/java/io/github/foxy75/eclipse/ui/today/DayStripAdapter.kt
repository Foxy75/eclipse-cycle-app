package io.github.foxy75.eclipse.ui.today

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        val dayNumber = Dates.dayOfMonth(day).toString()
        holder.binding.dayNum.text = dayNumber

        val selected = day == selectedDay
        if (selected) {
            holder.binding.root.setBackgroundResource(R.drawable.bg_day_item_selected)
            holder.binding.dayLetter.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            holder.binding.dayNum.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            holder.binding.dayNum.background = null
        } else {
            holder.binding.root.setBackgroundResource(R.drawable.bg_day_item)
            holder.binding.dayLetter.setTextColor(ContextCompat.getColor(ctx, R.color.ink_secondary))
            holder.binding.dayNum.setTextColor(ContextCompat.getColor(ctx, R.color.ink))
            holder.binding.dayNum.background = null
        }

        val dot = holder.binding.dot
        when {
            day in factDays -> {
                dot.visibility = View.VISIBLE
                dot.setBackgroundResource(if (selected) R.drawable.bg_dot_white else R.drawable.bg_dot_terracotta)
            }
            day in predictedDays -> {
                dot.visibility = View.VISIBLE
                dot.setBackgroundResource(if (selected) R.drawable.bg_dot_white else R.drawable.bg_dot_teal)
            }
            else -> dot.visibility = View.INVISIBLE
        }

        holder.binding.root.setOnClickListener { onSelect(day) }
    }
}