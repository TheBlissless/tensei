package mihon.core.common.extensions

import kotlinx.serialization.json.JsonObject

/**
 * Empty [JsonObject] companion constant, matching `mihon.core.common.extensions.EMPTY`
 * that extension ABI expects via `JsonObject.EMPTY`.
 */
val JsonObject.Companion.EMPTY: JsonObject
    get() = JsonObject(emptyMap())
