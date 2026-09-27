package com.opticast.player.player

// Android's DolbyVisionProfileDvheSt (profile 8), NOT an HEVC profile identifier.
internal const val DOLBY_VISION_PROFILE_8 = 0x100
private val profile8Codec = Regex("^(dvhe|dvh1)\\.08\\.[0-9]{2}$", RegexOption.IGNORE_CASE)

/** Never strip native DV, DRM, another profile, or an already-correct HEVC configuration. */
internal fun shouldClearDolbyProfileForHevc(
    api: Int, sourceMime: String?, codecs: String?, configuredMime: String?,
    configuredProfile: Int?, protectedContent: Boolean,
): Boolean = api >= 29 && !protectedContent && sourceMime == "video/dolby-vision" &&
    configuredMime == "video/hevc" && configuredProfile == DOLBY_VISION_PROFILE_8 &&
    codecs != null && profile8Codec.matches(codecs)

internal fun safeDecoderName(name: String?): String = name?.replace(Regex("[^a-zA-Z0-9._-]"), "_")?.take(160) ?: "not recorded"
