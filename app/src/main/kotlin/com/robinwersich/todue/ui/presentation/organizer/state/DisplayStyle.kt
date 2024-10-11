package com.robinwersich.todue.ui.presentation.organizer.state

import com.robinwersich.todue.domain.model.TimeBlock
import com.robinwersich.todue.domain.model.Timeline

/** Describes the semantic position of a timeline in the organizer. */
enum class TimelineStyle {
  /** The timeline is not visible, but was / will become a parent next. */
  HIDDEN_PARENT,
  /** The timeline is displayed in a split view, as the parent timeline. */
  PARENT,
  /** The timeline is occupies the full width of the screen. */
  FULLSCREEN,
  /** The timeline is displayed in a split view, as the child timeline. */
  CHILD,
  /** The timeline is not visible, but it was / will become a child next. */
  HIDDEN_CHILD,
}

fun timelineStyle(timeline: Timeline, navPos: TimelineNavigationPosition) =
  when {
    timeline > navPos.timeline -> TimelineStyle.HIDDEN_PARENT
    timeline == navPos.timeline && navPos.showChild -> TimelineStyle.PARENT
    timeline == navPos.timeline && !navPos.showChild -> TimelineStyle.FULLSCREEN
    timeline == navPos.visibleChild -> TimelineStyle.CHILD
    timeline < (navPos.visibleChild ?: navPos.timeline) -> TimelineStyle.HIDDEN_CHILD
    else -> error("Unhandled TimelinePresentation case.")
  }

/** Describes how a time block should be displayed in the organizer. */
enum class TimeBlockStyle {
  /** Reduced content is shown, block is displayed as a tray for sorting tasks to child blocks. */
  TRAY,
  /** The content of the block is shown in full detail. */
  FULLSCREEN,
  /** Only the name of the block is shown. */
  PREVIEW,
}

fun timeBlockStyle(timeBlock: TimeBlock, timeline: Timeline, navPos: NavigationPosition) =
  when {
    timeline == navPos.timelineNavPos.timeline && !navPos.timelineNavPos.showChild ->
      TimeBlockStyle.FULLSCREEN
    timeline == navPos.timelineNavPos.timeline && timeBlock == navPos.timeBlock ->
      TimeBlockStyle.TRAY
    else -> TimeBlockStyle.PREVIEW
  }
