package com.aviateclone.launcher.ui.compose

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Colonna verticale di lettere A-Z (+ #) per saltare rapidamente a una
 * sezione della lista app. Risponde sia al tap su una lettera sia al drag
 * continuo lungo la colonna.
 */
@Composable
fun SideAlphabet(
    letters: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (letters.isEmpty()) return

    Column(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight()
            .padding(vertical = 8.dp)
            .pointerInput(letters) {
                fun letterAt(y: Float): String {
                    val rowHeight = size.height.toFloat() / letters.size
                    val idx = (y / rowHeight).toInt().coerceIn(0, letters.lastIndex)
                    return letters[idx]
                }
                detectTapGestures { offset ->
                    onLetterSelected(letterAt(offset.y))
                }
            }
            .pointerInput(letters) {
                fun letterAt(y: Float): String {
                    val rowHeight = size.height.toFloat() / letters.size
                    val idx = (y / rowHeight).toInt().coerceIn(0, letters.lastIndex)
                    return letters[idx]
                }
                detectDragGestures(
                    onDrag = { change, _ -> onLetterSelected(letterAt(change.position.y)) }
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        letters.forEach { letter ->
            Text(
                letter,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
