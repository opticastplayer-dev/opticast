package com.opticast.player.ui.screens

/** Dialog controls must never share LazyColumn keys with saved section IDs.
 * Section keys remain unchanged because list-owned dragging uses their identities.
 */
internal enum class LibraryCustomizeControl {
    STYLE, RESET, MINIMAL_DESCRIPTION, MANAGE_COLLECTIONS;
    val lazyKey: String get() = "customize-control:$name"
}
