package com.vishruthdev.destiny.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Label for the Today / Tomorrow / Pick Date chips in the add dialogs. Three chips share a
 * narrow dialog, and on a typical phone width the default size wrapped "Tomorrow" onto two
 * lines, so these stay on one line at a slightly smaller size.
 */
@Composable
internal fun StartOptionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        softWrap = false
    )
}
