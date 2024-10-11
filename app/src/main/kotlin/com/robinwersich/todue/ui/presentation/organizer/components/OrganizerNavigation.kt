package com.robinwersich.todue.ui.presentation.organizer.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.robinwersich.todue.domain.model.TimeBlock
import com.robinwersich.todue.domain.model.Timeline
import com.robinwersich.todue.domain.model.size
import com.robinwersich.todue.domain.model.toDoubleRange
import com.robinwersich.todue.ui.composeextensions.Leq
import com.robinwersich.todue.ui.composeextensions.Lt
import com.robinwersich.todue.ui.composeextensions.Near
import com.robinwersich.todue.ui.composeextensions.PaddedRoundedCornerShape
import com.robinwersich.todue.ui.composeextensions.instantStop
import com.robinwersich.todue.ui.composeextensions.interpolatedInt
import com.robinwersich.todue.ui.composeextensions.modifiers.padding
import com.robinwersich.todue.ui.composeextensions.modifiers.scaleFromSize
import com.robinwersich.todue.ui.composeextensions.modifiers.size
import com.robinwersich.todue.ui.presentation.organizer.state.NavigationState
import com.robinwersich.todue.ui.presentation.organizer.state.TimeBlockStyle
import com.robinwersich.todue.ui.presentation.organizer.state.TimelineStyle
import com.robinwersich.todue.ui.presentation.organizer.state.timeBlockStyle
import com.robinwersich.todue.utility.size
import kotlinx.collections.immutable.ImmutableList

/**
 * A 2-dimensional navigation component that allows the user to navigate through [TimeBlock]s on a
 * time axis (vertical) and a granularity axis (horizontal). To be able to drag tasks between
 * [TimeBlock]s, there is a navigation state which shows a parent [TimeBlock] together with all its
 * children from the next smaller granularity level.
 *
 * @param timelines The timelines (i.e. granularity levels) to display.
 * @param timeBlockColor The color to use as background for the time blocks.
 * @param modifier The modifier to apply to this layout.
 * @param childTimelineSizeFraction The fraction of the screen width that the child timeline should
 *   take up in a split view with two timelines.
 * @param previewTimeBlockContent The content to display for a [TimeBlock] in preview mode.
 * @param expandedTimeBlockContent The content to display for a [TimeBlock] in expanded mode.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrganizerNavigation(
  timelines: ImmutableList<Timeline>,
  timeBlockColor: Color,
  modifier: Modifier = Modifier,
  childTimelineSizeFraction: Float = 0.3f,
  previewTimeBlockContent: @Composable (Timeline, TimeBlock) -> Unit,
  expandedTimeBlockContent: @Composable (Timeline, TimeBlock) -> Unit,
) {
  val positionalThreshold = 0.3f
  val velocityThreshold = with(LocalDensity.current) { 500.dp.toPx() }

  val navigationState =
    remember(timelines, childTimelineSizeFraction) {
      NavigationState(
        timelines = timelines,
        childTimelineSizeRatio = childTimelineSizeFraction,
        positionalThreshold = { it * positionalThreshold },
        velocityThreshold = { velocityThreshold },
        snapAnimationSpec =
          spring(
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = Int.VisibilityThreshold.toFloat(),
          ),
        decayAnimationSpec = instantStop(),
      )
    }
  val timelineDraggableState = navigationState.timelineDraggableState
  val dateDraggableState = navigationState.dateDraggableState
  LaunchedEffect(navigationState) { navigationState.updateDateAnchorsOnSwipe() }

  OrganizerNavigationLayout(
    navigationState = navigationState,
    timeBlockColor = timeBlockColor,
    modifier =
      remember(navigationState) {
        modifier
          .clipToBounds()
          .anchoredDraggable(
            timelineDraggableState,
            orientation = Orientation.Horizontal,
            reverseDirection = true,
          )
          .anchoredDraggable(
            dateDraggableState,
            orientation = Orientation.Vertical,
            reverseDirection = true,
          )
          .onSizeChanged { navigationState.updateViewportSize(it) }
      },
    previewTimeBlockContent = previewTimeBlockContent,
    expandedTimeBlockContent = expandedTimeBlockContent,
  )
}

@Composable
private fun OrganizerNavigationLayout(
  navigationState: NavigationState,
  timeBlockColor: Color,
  modifier: Modifier = Modifier,
  previewTimeBlockContent: @Composable (Timeline, TimeBlock) -> Unit,
  expandedTimeBlockContent: @Composable (Timeline, TimeBlock) -> Unit,
) {
  // Workaround for missing compositions based on activeTimelineBlocks
  // TODO: investigate why this is necessary
  navigationState.navPosTransition.transitionStates()
  // TODO: maybe use viewport size from navigationState instead of constraints
  BoxWithConstraints(modifier.fillMaxSize()) {
    for ((timeline, timeBlocks) in navigationState.activeTimelineBlocks) {
      key(timeline) {
        val timelineStyle = navigationState.timelineStyleTransitions.getValue(timeline)
        val timelineWidth by
          timelineStyle.interpolatedInt(aggregate = true) {
            val relativeWidth =
              when (it) {
                TimelineStyle.HIDDEN_CHILD,
                TimelineStyle.CHILD -> navigationState.childTimelineSizeRatio
                TimelineStyle.FULLSCREEN -> 1f
                TimelineStyle.PARENT,
                TimelineStyle.HIDDEN_PARENT -> 1f - navigationState.childTimelineSizeRatio
              }
            (relativeWidth * constraints.maxWidth).toInt()
          }
        val timelineOffset by
          timelineStyle.interpolatedInt(aggregate = true) {
            val relativeOffset =
              when (it) {
                TimelineStyle.HIDDEN_CHILD -> -navigationState.childTimelineSizeRatio
                TimelineStyle.CHILD,
                TimelineStyle.FULLSCREEN -> 0f
                TimelineStyle.PARENT -> navigationState.childTimelineSizeRatio
                TimelineStyle.HIDDEN_PARENT -> 1f
              }
            (relativeOffset * constraints.maxWidth).toInt()
          }

        for (timeBlock in timeBlocks) {
          key(timeBlock) {
            val blockHeight by
              navigationState.visibleDateRange.derived {
                (timeBlock.size / it.size * constraints.maxHeight).toInt()
              }
            val blockOffset by
              navigationState.visibleDateRange.derived {
                ((timeBlock.toDoubleRange().start - it.start) / it.size * constraints.maxHeight)
                  .toInt()
              }

            val timeBlockStyle =
              navigationState.navPosTransition.derived(aggregate = true) {
                timeBlockStyle(timeBlock, timeline, it)
              }
            val showExpanded by
              timeBlockStyle.derivedState { !isAround(TimeBlockStyle.PREVIEW, 0.8f) }

            var composeExpanded by remember { mutableStateOf(showExpanded) }
            val expandedAlpha by
              animateFloatAsState(
                if (showExpanded) 1f else 0f,
                label = "expandedAlpha",
                finishedListener = { composeExpanded = it > 0f }
              )
            val showBlockBounds = !timeBlockStyle.isSettled(TimeBlockStyle.FULLSCREEN)
            // TODO: is a dynamic padding actually necessary?
            val padding by animateDpAsState(if (showBlockBounds) 4.dp else 0.dp, label = "padding")
            val cornerRadius by
              animateDpAsState(if (showBlockBounds) 24.dp else 0.dp, label = "cornerRadius")

            // Preview
            Box(
              Modifier.offset { IntOffset(timelineOffset, blockOffset) }
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size { IntSize(timelineWidth, blockHeight) }
                .padding { PaddingValues(padding) }
                .drawBehind {
                  drawRoundRect(timeBlockColor, cornerRadius = CornerRadius(cornerRadius.toPx()))
                }
                .graphicsLayer { alpha = 1f - expandedAlpha }
            ) {
              previewTimeBlockContent(timeline, timeBlock)
            }

            // Expanded View
            if (composeExpanded) {
              Box(
                Modifier.offset { IntOffset(timelineOffset, blockOffset) }
                  .wrapContentSize(Alignment.TopStart, unbounded = true)
                  .size { IntSize(timelineWidth, blockHeight) }
                  .graphicsLayer {
                    shape = PaddedRoundedCornerShape(cornerRadius, padding)
                    clip = true
                    alpha = expandedAlpha
                  }
                  .scaleFromSize(padding = PaddingValues(4.dp)) {
                    IntSize(
                      width =
                        if (timelineStyle.isState(Near(TimelineStyle.PARENT))) timelineWidth
                        else constraints.maxWidth,
                      height =
                        when {
                          timelineStyle.isState(Lt(TimelineStyle.FULLSCREEN)) -> blockHeight
                          timelineStyle.isState(Leq(TimelineStyle.PARENT)) ->
                            navigationState
                              .focussedTimeBlockSize(timeBlock)
                              ?.times(constraints.maxHeight)
                              ?.toInt() ?: constraints.maxHeight
                          else -> constraints.maxHeight
                        },
                    )
                  }
              ) {
                expandedTimeBlockContent(timeline, timeBlock)
              }
            }
          }
        }
      }
    }
  }
}
