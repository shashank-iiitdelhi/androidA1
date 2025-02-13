package com.example.myapplication

import java.util.PriorityQueue

class GraphFlight {
    private val adjacencyList = mutableMapOf<String, MutableList<Pair<String, Double>>>()

    fun buildGraph(routes: List<Route>) {
        adjacencyList.clear() // Ensure a fresh graph each time

        for (route in routes) {
            adjacencyList.computeIfAbsent(route.startCity) { mutableListOf() }
                .add(Pair(route.destinationCity, route.distance))

            // No need to add reverse routes if they are already included in MainActivity
        }
    }

    fun dijkstra(start: String, end: String): Pair<Double, List<String>> {
        if (start !in adjacencyList || end !in adjacencyList) {
            return Pair(Double.MAX_VALUE, emptyList()) // No route exists
        }

        val pq = PriorityQueue<Pair<String, Double>>(Comparator.comparingDouble { it.second })
        val distances = mutableMapOf<String, Double>().withDefault { Double.MAX_VALUE }
        val previous = mutableMapOf<String, String?>()

        distances[start] = 0.0
        pq.add(Pair(start, 0.0))

        while (pq.isNotEmpty()) {
            val (current, currentDist) = pq.poll()
            if (current == end) break

            adjacencyList[current]?.forEach { (neighbor, distance) ->
                val newDist = currentDist + distance
                if (newDist < distances.getValue(neighbor)) {
                    distances[neighbor] = newDist
                    previous[neighbor] = current
                    pq.add(Pair(neighbor, newDist))
                }
            }
        }

        // No valid path found
        if (distances[end] == Double.MAX_VALUE) {
            return Pair(Double.MAX_VALUE, emptyList())
        }

        // Construct the shortest path
        val path = mutableListOf<String>()
        var step: String? = end
        while (step != null) {
            path.add(step)
            step = previous[step]
        }

        return Pair(distances[end] ?: Double.MAX_VALUE, path.reversed())
    }

    fun getDistance(city1: String, city2: String): Double {
        return adjacencyList[city1]?.find { it.first == city2 }?.second ?: Double.MAX_VALUE
    }

}
