package com.opticast.player.ui.screens

internal val designSections = listOf("featured", "continue", "collections", "recent", "titles", "watched")
internal val sectionNames = mapOf("featured" to "Featured", "continue" to "Continue Watching", "collections" to "Collections", "recent" to "Recently added", "titles" to "Movies / TV / Favorites", "watched" to "Watched")
internal fun normalizeSectionOrder(order: List<String>) = (order.filter { it in designSections } + designSections).distinct()
internal fun moveLibrarySection(order: List<String>, id: String, delta: Int): List<String> {
    val result = normalizeSectionOrder(order).toMutableList()
    val from = result.indexOf(id)
    if (from < 0) return result
    val to = (from + delta).coerceIn(0, result.lastIndex)
    result.add(to, result.removeAt(from))
    return result
}
internal fun visibleLibrarySections(style: String, order: List<String>, hidden: Set<String>, searching: Boolean): List<String> = when {
    searching || style == "minimal" -> listOf("titles")
    else -> normalizeSectionOrder(order).filter { it == "titles" || it !in hidden }
}
internal fun addRecentQuery(history: List<String>, query: String): List<String> {
    val term = query.trim().take(100)
    if (term.length < 2) return history
    return (listOf(term) + history.filterNot { it.equals(term, true) }).take(8)
}
internal fun searchTypeMatches(filter: String, isShow: Boolean): Boolean = when (filter) {
    "movies" -> !isShow
    "tv" -> isShow
    else -> true
}
internal data class LibraryDesign(val style: String = "classic", val order: List<String> = classicSectionOrder, val hidden: Set<String> = emptySet(), val stats: Boolean = true)
internal data class PersonalCollection(val id: String, val name: String, val videoIds: List<Long>)

internal val classicSectionOrder = listOf("continue", "featured", "recent", "titles", "watched", "collections")
internal fun supportedLibraryStyle(saved: String?): String = if (saved == "minimal") "minimal" else "classic"
internal fun showLibraryStatistics(style: String, enabled: Boolean, searching: Boolean) = !searching && style == "classic" && enabled
internal fun includeCompletedTitles(style: String, hidden: Set<String>, searching: Boolean) = searching || style == "minimal" || "watched" in hidden
internal enum class FeaturedTap { SELECT, PLAY, DETAILS }
internal fun featuredTap(selectionMode: Boolean, watchButton: Boolean): FeaturedTap = when {
    selectionMode -> FeaturedTap.SELECT
    watchButton -> FeaturedTap.PLAY
    else -> FeaturedTap.DETAILS
}

/** Stable identity reorder: pointer coordinates are owned by the list, not a moving row. */
internal fun moveLibrarySectionTo(order: List<String>, id: String, target: String): List<String> {
    val from = order.indexOf(id); val to = order.indexOf(target)
    if (from < 0 || to < 0 || from == to) return order
    return order.toMutableList().apply { removeAt(from); add(to, id) }
}
