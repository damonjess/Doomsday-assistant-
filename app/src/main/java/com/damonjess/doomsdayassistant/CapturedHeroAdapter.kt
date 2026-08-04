package com.damonjess.doomsdayassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CapturedHeroAdapter(
    private var heroes: List<CapturedHero>,
    private val onRemove: (CapturedHero) -> Unit
) : RecyclerView.Adapter<CapturedHeroAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.hero_name)
        val info: TextView = view.findViewById(R.id.hero_info)
        val power: TextView = view.findViewById(R.id.hero_power)
        val removeBtn: ImageButton = view.findViewById(R.id.remove_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_captured_hero, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hero = heroes[position]
        holder.name.text = hero.name
        holder.info.text = "${hero.role} | ${hero.faction} | Lv${hero.level}"
        holder.power.text = "⚡ ${hero.power}"
        
        holder.removeBtn.setOnClickListener {
            onRemove(hero)
        }
    }

    override fun getItemCount(): Int = heroes.size

    fun updateData(newHeroes: List<CapturedHero>) {
        this.heroes = newHeroes
        notifyDataSetChanged()
    }
}
