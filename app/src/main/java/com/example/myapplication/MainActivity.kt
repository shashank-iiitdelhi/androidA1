package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader
import android.util.Log
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var distanceText: TextView
    private lateinit var routeFullText: TextView
    private lateinit var routeText: TextView
    private lateinit var findRouteButton: Button
    private lateinit var nextStopButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var startSpinner: Spinner
    private lateinit var endSpinner: Spinner
    private lateinit var distanceLeftText: TextView
    private var stops: List<Stop> = listOf()
    private var routes: List<Route> = listOf()
    private var graph = GraphFlight()
    private var selectedStart = ""
    private var selectedEnd = ""
    private var route = listOf<String>()
    private var totalDistance = 0.0
    private var distanceLeft = 0.0
    private var currentStopIndex = 0
    private lateinit var unitToggleButton: ToggleButton
    private var useMiles = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progress_bar)
        distanceText = findViewById(R.id.distance_text)
        distanceLeftText = findViewById(R.id.distance_left_text)
        routeText = findViewById(R.id.route_text)
        routeFullText = findViewById(R.id.route_full_text)
        findRouteButton = findViewById(R.id.find_route_button)
        nextStopButton = findViewById(R.id.next_stop_button)
        recyclerView = findViewById(R.id.recycler_view)
        startSpinner = findViewById(R.id.start_spinner)
        endSpinner = findViewById(R.id.end_spinner)

        recyclerView.layoutManager = LinearLayoutManager(this)
        unitToggleButton = findViewById(R.id.unit_toggle_button)

        CoroutineScope(Dispatchers.IO).launch {
            loadStops()
            loadRoutes()
            withContext(Dispatchers.Main) {
                setupSpinners()
            }
        }

        findRouteButton.setOnClickListener {
            if (selectedStart != selectedEnd) {
                findShortestPath()
            } else {
                Toast.makeText(this, "Select different start and end cities!", Toast.LENGTH_SHORT).show()
            }
        }

        nextStopButton.setOnClickListener {
            moveToNextStop()
        }

        unitToggleButton.setOnCheckedChangeListener { _, isChecked ->
            useMiles = isChecked
            updateDisplayedDistance()
        }
    }

    private suspend fun loadStops() {
        try {
            val inputStream = assets.open("stops.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()

            stops = lines.map { line ->
                val parts = line.split(", ")
                Stop(parts[0], parts[1])
            }

            withContext(Dispatchers.Main) {
                val stopAdapter = StopAdapter(stops)
                recyclerView.adapter = stopAdapter
                stopAdapter.notifyDataSetChanged()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading stops.txt", e)
        }
    }

    private fun loadRoutes() {
        try {
            val inputStream = assets.open("routes.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val lines = reader.readLines()

            val routeList = mutableListOf<Route>()

            for (line in lines) {
                val parts = line.split(", ")
                if (parts.size < 4) {
                    Log.e("MainActivity", "Skipping invalid route entry: $line")
                    continue
                }
                val city1 = parts[0]
                val city2 = parts[1]
                val visa = parts[2]
                val distance = parts[3].toDouble()

                routeList.add(Route(city1, city2, visa, distance))
                routeList.add(Route(city2, city1, visa, distance))
            }

            routes = routeList
            graph.buildGraph(routes)

        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading routes.txt", e)
        }
    }

    private fun setupSpinners() {
        val stopNames = stops.map { it.city }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, stopNames)
        startSpinner.adapter = adapter
        endSpinner.adapter = adapter

        startSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedStart = stopNames[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        endSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedEnd = stopNames[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        adapter.notifyDataSetChanged()
    }

    private fun findShortestPath() {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        nextStopButton.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val result = graph.dijkstra(selectedStart, selectedEnd)
            route = result.second
            totalDistance = result.first
            currentStopIndex = 0

            withContext(Dispatchers.Main) {
                if (route.isEmpty()) {
                    distanceText.text = "No route found!"
                    routeText.text = "Try selecting different cities."
                    progressBar.visibility = View.GONE
                    nextStopButton.visibility = View.GONE
                    return@withContext
                }

                // Update the distance text based on unit toggle
                distanceText.text = "Distance: ${formatDistance(totalDistance)}"

                val fullRoute = route.joinToString(" → ")
                routeFullText.text = "Full Route:\n$fullRoute\n\n"

                routeText.text = "Start at ${route.first()}\n"
            }
        }
    }

    private fun updateDisplayedDistance() {
        // Convert the distance into the selected unit (km or miles)
        val convertedDistance = if (useMiles) totalDistance * 0.621371 else totalDistance
        val convertedLeftDistance = if (useMiles) distanceLeft * 0.621371 else distanceLeft
        val unit = if (useMiles) "miles" else "km"
        distanceText.text = "Distance : ${String.format("%.2f", convertedDistance)} $unit"
        distanceLeftText.text = "Distance Left: ${String.format("%.2f", convertedLeftDistance)} $unit"
    }

    private fun moveToNextStop() {
        if (currentStopIndex < route.size - 1) {
            val previousCity = route[currentStopIndex]
            currentStopIndex++
            val nextCity = route[currentStopIndex]
            val distanceBetween = graph.getDistance(previousCity, nextCity)

            // Convert the distance between stops based on the unit toggle
            val formattedDistanceBetween = formatDistance(distanceBetween)

            val visaRequirement = getVisaRequirement(previousCity, nextCity)
            routeText.append("$previousCity → $nextCity($formattedDistanceBetween, Visa: $visaRequirement)\n")

            val coveredDistance = route.subList(0, currentStopIndex + 1)
                .zipWithNext { a, b -> graph.getDistance(a, b) }
                .sum()

            // Recalculate remaining distance and display it in the selected unit
            distanceLeft = totalDistance - coveredDistance

            distanceLeftText.text = "Distance Left: ${formatDistance(distanceLeft)}"

            // Update progress percentage
            val progressPercent = if (totalDistance > 0) {
                ((coveredDistance / totalDistance) * 100).toInt()
            } else {
                100
            }

            progressBar.progress = progressPercent

            // Update percentage text inside progress bar
            val progressTextView = findViewById<TextView>(R.id.progress_percentage)
            progressTextView.text = "$progressPercent%"

            if (currentStopIndex == route.size - 1) {
                nextStopButton.visibility = View.GONE
                progressBar.progress = 100
                progressTextView.text = "100%"
            }
        }
    }

    fun getVisaRequirement(city1: String, city2: String): String {
        return routes.firstOrNull { it.startCity == city1 && it.destinationCity == city2 }?.visaRequirement ?: "N/A"
    }

    private fun formatDistance(distance: Double): String {
        val convertedDistance = if (useMiles) distance * 0.621371 else distance
        val unit = if (useMiles) "miles" else "km"
        return "${String.format("%.2f", convertedDistance)} $unit"
    }

}
