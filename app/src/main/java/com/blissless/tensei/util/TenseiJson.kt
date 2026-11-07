package com.blissless.tensei.util

import kotlinx.serialization.json.Json

/**
 * The app-wide shared [Json] instance for the common configuration
 * (`ignoreUnknownKeys` — forward/backwards-compatible DTO decoding).
 *
 * Previously each file constructed its own `Json { ignoreUnknownKeys = true }`;
 * Json instances are stateless but not free, so share one. Sites that need
 * different semantics (e.g. `encodeDefaults` when writing, `isLenient` for
 * hand-edited input, `coerceInputValues` for widget payloads) keep their own
 * configured instance.
 */
object TenseiJson {
    val default: Json = Json {
        ignoreUnknownKeys = true
    }
}
