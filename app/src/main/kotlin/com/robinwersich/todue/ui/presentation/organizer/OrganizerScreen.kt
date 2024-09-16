package com.robinwersich.todue.ui.presentation.organizer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.robinwersich.todue.domain.model.TimeUnit
import com.robinwersich.todue.domain.model.Timeline
import com.robinwersich.todue.ui.presentation.organizer.components.ExpandedTimeBlockContent
import com.robinwersich.todue.ui.presentation.organizer.components.OrganizerNavigation
import com.robinwersich.todue.ui.presentation.organizer.components.PreviewTimeBlockContent
import com.robinwersich.todue.ui.presentation.organizer.formatting.rememberTimeBlockFormatter
import com.robinwersich.todue.ui.theme.ToDueTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
fun OrganizerScreen(state: OrganizerState, onEvent: (OrganizerEvent) -> Unit = {}) {
  val timelines = remember {
    persistentListOf(
      Timeline(0, TimeUnit.DAY),
      Timeline(1, TimeUnit.WEEK),
      Timeline(2, TimeUnit.MONTH),
    )
  }
  val formatter = rememberTimeBlockFormatter()
  OrganizerNavigation(
    timelines = timelines,
    timeBlockColor = MaterialTheme.colorScheme.surfaceContainer,
    previewTimeBlockContent = { _, timeBlock ->
      PreviewTimeBlockContent(timeBlock = timeBlock, formatter = formatter)
    },
    expandedTimeBlockContent = { _, timeBlock ->
      ExpandedTimeBlockContent(timeBlock = timeBlock, formatter = formatter)
    },
  )
}

@Preview(showSystemUi = true)
@Composable
private fun OrganizerScreenPreview() {
  ToDueTheme { OrganizerScreen(OrganizerState()) }
}
