package com.blissless.tensei.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

private val spoilerRegex = Regex("~!(.+?)!~", setOf(RegexOption.DOT_MATCHES_ALL))
private val imageRegex = Regex("!\\[[^]]*]\\([^)]*\\)")
private val wordBangRegex = Regex("!(\\w+)!")
private val entityQuote = Regex("&quot;")
private val entityLt = Regex("&lt;")
private val entityGt = Regex("&gt;")
private val entityAmp = Regex("&amp;")

private val starBoldRegex = Regex("\\*\\*([^\\n]+?)\\*\\*")
private val starItalicRegex = Regex("\\*(?!\\s)([^\\n*]+?)\\*")

private val boldRegex = Regex("__(.+?)__")
private val italicRegex = Regex("_(.+?)_")
private val linkRegex = Regex("\\[(.+?)]\\((.+?)\\)")

/**
 * Strips the markup AniList ships inside its `description` / `about` fields: HTML line
 * breaks and emphasis tags, spoiler markers (`~!text!~`), markdown images, HTML
 * entities, stray tildes and the `!word!` spoiler form. The markup's *content* is
 * kept, so nothing but the markers disappears - in particular a plain `!` in prose
 * survives.
 *
 * This is plain text: markdown emphasis markers (`**bold**`, `__bold__`, `_italic_`,
 * links) are left in place. Use [anilistAnnotated] when the text is going to be
 * rendered, so those markers become styling instead of punctuation.
 */
fun cleanAniListText(text: String): String = text
    .replace("<br>", "\n").replace("<br/>", "\n")
    .replace("<b>", "").replace("</b>", "")
    .replace("<i>", "").replace("</i>", "")
    // Spoilers first: dropping the tildes before this ran left `!spoiler text!` with its
    // bangs intact, and the old `!(\w+)!` pass only ever matched single words - never a
    // multi-word, CJK or multi-line spoiler.
    .replace(spoilerRegex, "$1")
    .replace(imageRegex, "")
    .replace(entityLt, "<").replace(entityGt, ">")
    .replace(entityQuote, "\"").replace(entityAmp, "&")
    .replace("~", "")
    .replace(wordBangRegex, "$1")

/**
 * AniList description as an [AnnotatedString]: [cleanAniListText] plus bold, italic and
 * link parsing for every spelling AniList accepts (`__x__`/`**x**`, `_x_`/`*x*`).
 *
 * [animeTitles] turns mentions of those exact titles into tappable `ANIME` annotations
 * carrying the media id; links get a `URL` annotation. Both are plain string
 * annotations, so callers can read them from a `ClickableText` click offset.
 */
fun anilistAnnotated(
    text: String,
    color: Color,
    primary: Color,
    animeTitles: Map<String, Int> = emptyMap(),
): AnnotatedString {
    val cleaned = cleanAniListText(text)
        // AniList accepts the star spellings alongside the underscore ones; rewrite them
        // so a single parser covers both. A bullet (`* item`) and a rule (`***`) are left
        // alone because an italic star may not be followed by a space.
        .replace(starBoldRegex, "__$1__")
        .replace(starItalicRegex, "_$1_")

    return buildAnnotatedString {
        var remaining = cleaned
        while (remaining.isNotEmpty()) {
            val boldMatch = boldRegex.find(remaining)
            val italicMatch = italicRegex.find(remaining)
            val linkMatch = linkRegex.find(remaining)

            val candidates = mutableListOf<Pair<Int, Regex>>()
            boldMatch?.let { candidates.add(it.range.first to boldRegex) }
            italicMatch?.let { candidates.add(it.range.first to italicRegex) }
            linkMatch?.let { candidates.add(it.range.first to linkRegex) }

            if (candidates.isEmpty()) {
                appendAnimeTitles(remaining, animeTitles, primary)
                break
            }

            candidates.sortBy { it.first }
            val (_, chosenRegex) = if (candidates.size > 1 && candidates[0].first == candidates[1].first) {
                val bold = candidates.find { it.second == boldRegex }
                bold ?: candidates.first()
            } else {
                candidates.first()
            }
            val match = chosenRegex.find(remaining)!!

            if (match.range.first > 0) {
                appendAnimeTitles(remaining.substring(0, match.range.first), animeTitles, primary)
            }

            when (chosenRegex) {
                boldRegex -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    appendWithAnimeLinks(match.groupValues[1], animeTitles, primary)
                    pop()
                }
                italicRegex -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    appendWithAnimeLinks(match.groupValues[1], animeTitles, primary)
                    pop()
                }
                linkRegex -> {
                    val url = match.groupValues[2]
                    pushStringAnnotation("URL", url)
                    pushStyle(SpanStyle(
                        color = primary,
                        textDecoration = TextDecoration.Underline
                    ))
                    append(match.groupValues[1])
                    pop()
                    pop()
                }
            }

            remaining = remaining.substring(match.range.last + 1)
        }
        addStyle(SpanStyle(color = color), 0, length)
    }
}

private fun AnnotatedString.Builder.appendWithAnimeLinks(text: String, animeTitles: Map<String, Int>, primary: Color) {
    if (animeTitles.isEmpty()) {
        append(text)
        return
    }
    val titleMatch = animeTitles.entries.firstOrNull { (title, _) ->
        text.equals(title, ignoreCase = true)
    }
    if (titleMatch != null) {
        pushStringAnnotation("ANIME", titleMatch.value.toString())
        pushStyle(SpanStyle(color = primary, textDecoration = TextDecoration.Underline))
        append(text)
        pop()
        pop()
    } else {
        append(text)
    }
}

private fun AnnotatedString.Builder.appendAnimeTitles(text: String, animeTitles: Map<String, Int>, primary: Color) {
    if (animeTitles.isEmpty()) {
        append(text)
        return
    }
    var remaining = text
    while (remaining.isNotEmpty()) {
        val match = animeTitles.entries
            .mapNotNull { (title, id) ->
                val idx = remaining.indexOf(title, ignoreCase = true)
                if (idx >= 0) Triple(idx, title, id) else null
            }
            .minByOrNull { it.first }

        if (match == null) {
            append(remaining)
            break
        }
        if (match.first > 0) {
            append(remaining.substring(0, match.first))
        }
        pushStringAnnotation("ANIME", match.third.toString())
        pushStyle(SpanStyle(color = primary, textDecoration = TextDecoration.Underline))
        append(match.second)
        pop()
        pop()
        remaining = remaining.substring(match.first + match.second.length)
    }
}
