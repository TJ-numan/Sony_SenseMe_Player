package com.tjnuman.sensemeplayer

import kotlin.math.pow
import kotlin.math.sqrt

data class Cluster(val centroid: FloatArray, val points: MutableList<FloatArray> = mutableListOf())

fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
    return sqrt(a.zip(b) { x, y -> (x - y).pow(2) }.sum())
}

fun kMeans(
    data: List<FloatArray>,
    k: Int,
    maxIterations: Int = 100
): List<Cluster> {
    require(data.isNotEmpty()) { "Data cannot be empty" }
    require(k > 0 && k <= data.size) { "Invalid number of clusters" }

    val centroids = data.shuffled().take(k).map { it.copyOf() }
    val clusters = centroids.map { Cluster(it) }

    repeat(maxIterations) {
        clusters.forEach { it.points.clear() }

        for (point in data) {
            val closestCluster = clusters.minByOrNull { euclideanDistance(it.centroid, point) }!!
            closestCluster.points.add(point)
        }

        clusters.forEach { cluster ->
            if (cluster.points.isNotEmpty()) {
                for (i in cluster.centroid.indices) {
                    cluster.centroid[i] = cluster.points.map { it[i] }.average().toFloat()
                }
            }
        }
    }

    return clusters
}
