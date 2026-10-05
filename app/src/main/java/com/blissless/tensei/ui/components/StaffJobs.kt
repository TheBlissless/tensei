package com.blissless.tensei.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Lines a staff card spends on jobs.
 *
 * Two lines is what fits the fixed-height cells of the detail rails, and it is
 * also the default: anywhere the height is not fixed, pass
 * [StaffJobLinesUnlimited] instead. Roles are frequently wider than they are
 * long ("Director, Producer, Storyboard, Episode Director"), and hiding half of
 * them is worse than letting one card be taller than its neighbours.
 */
const val StaffJobMaxLines = 2

/** Renders every job, one per line, for grids whose rows size to their tallest cell. */
const val StaffJobLinesUnlimited = Int.MAX_VALUE

/**
 * Splits one AniList staff edge role into individual jobs.
 *
 * AniList packs multiple jobs into a single string ("Director, Producer") and
 * uses underscores in some of them, so one edge can carry several jobs. Only the
 * separators and a leading lowercase letter are touched here on purpose: the
 * previous formatting lowercased the whole role before re-capitalizing it, which
 * turned "CG" into "Cg", "OP" into "Op" and "Story & Art" into "Story & art".
 */
fun staffJobs(role: String?): List<String> =
    role.orEmpty()
        .replace('_', ' ')
        .split(',')
        .map { job -> job.trim().replaceFirstChar { c -> if (c.isLowerCase()) c.uppercase() else c.toString() } }
        .filter { job -> job.isNotEmpty() }
        .distinct()

/**
 * Renders [jobs] as at most [limit] lines, one job per line.
 *
 * When there are more jobs than lines the last visible job carries a
 * "+N more" tail, so a person credited four times on one anime shows that they
 * have four jobs instead of appearing to have two, and no line is wasted on the
 * marker itself.
 */
fun staffJobsText(jobs: List<String>, limit: Int = StaffJobMaxLines): String {
    if (jobs.isEmpty()) return ""
    if (jobs.size <= limit) return jobs.joinToString("\n")

    val shown = jobs.take(limit)
    val hidden = jobs.size - limit + 1
    return shown.dropLast(1).joinToString("\n") + "\n${shown.last()} +$hidden more"
}

/** Job text for a single staff edge. Renders nothing when the edge carries no role. */
@Composable
fun StaffEdgeJobText(
    role: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null,
    maxLines: Int = StaffJobMaxLines,
) {
    StaffJobText(
        jobs = staffJobs(role),
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines
    )
}

/** Job text for an already parsed job list; one job per line. */
@Composable
fun StaffJobText(
    jobs: List<String>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null,
    maxLines: Int = StaffJobMaxLines,
) {
    // A job can still wrap onto a second line even when every job gets a line of
    // its own, so the hard cap stays in place as a ceiling; only the job list is
    // built without a limit.
    val text = staffJobsText(jobs, maxLines)
    if (text.isEmpty()) return

    Text(
        text = text,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
