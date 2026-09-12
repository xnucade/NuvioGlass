package com.nuvio.tv.domain.model

enum class HomeLayout(val displayName: String) {
    CLASSIC("Classic View"),
    GRID("Grid View"),
    MODERN("Modern View"),
    GLASS("Glass View");

    /**
     * Glass is a reskin of Modern, not a separate information architecture: same hero, same rows,
     * same enrichment. Everything in the view model that asks "is this the Modern pipeline?" has to
     * answer yes for Glass too, or the hero and presentation rows never get built.
     */
    val usesModernPipeline: Boolean
        get() = this == MODERN || this == GLASS
}
