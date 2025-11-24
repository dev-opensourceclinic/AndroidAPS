package app.aaps.core.interfaces.graph

interface SeriesData {
    /**
     * Clear all stored GraphView references to prevent memory leaks.
     * Should be called when the GraphView's hosting Activity/Fragment is destroyed.
     */
    fun clearGraphViews()
}