package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StopAdapter(private var stops: List<Stop>) :
    RecyclerView.Adapter<StopAdapter.StopViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.stop_item, parent, false)
        return StopViewHolder(view)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        val stop = stops[position]
        holder.cityText.text = stop.city
        holder.countryText.text = stop.country
    }

    override fun getItemCount(): Int = stops.size

    class StopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cityText: TextView = view.findViewById(R.id.city_text)
        val countryText: TextView = view.findViewById(R.id.country_text)
    }
}
