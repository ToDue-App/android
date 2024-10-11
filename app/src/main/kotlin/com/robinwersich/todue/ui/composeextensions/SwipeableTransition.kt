package com.robinwersich.todue.ui.composeextensions

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.robinwersich.todue.utility.isSame
import com.robinwersich.todue.utility.map

/** Has distinct anchor states but can be between two of those states. */
interface FluentState<T> {
  fun isBetween(firstAnchor: T, secondAnchor: T): Boolean

  /** If the current state is in the given [radius] around the given [anchor]. */
  fun isAround(anchor: T, @FloatRange(from = 0.0, to = 1.0) radius: Float): Boolean
}

/**
 * This fulfills a similar role as [Transition][androidx.compose.animation.core.Transition], but
 * instead of allowing only state changes, triggering fire-once animations, it allows to provide
 * animation progress as an input value, so that it can be controlled by swipeable composables, such
 * as [anchoredDraggable][androidx.compose.foundation.gestures.anchoredDraggable] or
 * [Pager][androidx.compose.foundation.pager.Pager].
 *
 * Even though a [SwipeableTransition] can be between two distinct states, it doesn't have a current
 * value for these in-between states. For this, use a [SwipeableValue], which can retrieved using
 * [interpolatedValue].
 *
 * Note that this kind of transition has a "direction", meaning that states have an order and
 * certain states come "before" others. Depending on the underlying swipeable this order is based on
 * layout or swipe direction.
 *
 * @param transitionStates A function that returns the two states between which is transitioned. The
 *   [first][Pair.first] state is considered to come "before" the [second][Pair.second] one. If the
 *   transition is currently settled, the two states should be equal.
 * @param progress A function that returns the progress of the transition from the
 *   [first][Pair.first] to the [second][Pair.second] state.
 */
class SwipeableTransition<T>(
  val transitionStates: () -> Pair<T, T>,
  @FloatRange(from = 0.0, to = 1.0) val progress: () -> Float,
) : FluentState<T> {
  /** Convenience constructor for previews. */
  constructor(state: T) : this({ state to state }, { 0f })

  override fun isBetween(firstAnchor: T, secondAnchor: T): Boolean {
    val (prevState, nextState) = transitionStates()
    return prevState == firstAnchor && nextState == secondAnchor ||
      prevState == secondAnchor && nextState == firstAnchor
  }

  override fun isAround(anchor: T, radius: Float): Boolean {
    val (prevState, nextState) = transitionStates()
    return prevState == anchor && nextState == anchor ||
      prevState == anchor && progress() <= radius ||
      nextState == anchor && progress() >= 1f - radius
  }

  /** Specifies if the transition is currently animating or not. */
  val isSettled: Boolean
    get() = transitionStates().let { (prev, next) -> prev == next }

  /** Returns if the transition is currently settled on the given [anchor]. */
  fun isSettled(anchor: T) =
    transitionStates().let { (prev, next) -> prev == anchor && next == anchor }

  /** Returns if the current transition state is in any of the given [stateRelations]. */
  fun isState(vararg stateRelations: SwipeableStateRelation<T>): Boolean {
    val transition = transitionStates()
    return stateRelations.any { relation -> transition in relation }
  }

  /**
   * Returns a smoothly interpolated value derived from the current
   * [SwipeableTransition.transitionStates] and [SwipeableTransition.progress].
   *
   * @param interpolateValue A function that interpolates between two values of the target type.
   * @param targetValueByState A function that returns the target value for a given state.
   */
  fun <V> interpolateValue(
    interpolateValue: (start: V, end: V, progress: Float) -> V,
    targetValueByState: (state: T) -> V,
  ): V {
    val (prevState, nextState) = transitionStates()
    val prevValue = targetValueByState(prevState)
    val nextValue = targetValueByState(nextState)
    return if (prevValue == nextValue) prevValue
    else interpolateValue(prevValue, nextValue, progress())
  }

  /**
   * Returns a [SwipeableValue] with a smoothly interpolated value derived from the current
   * [SwipeableTransition.transitionStates] and [SwipeableTransition.progress].
   *
   * @param interpolateValue A function that interpolates between two values of the target type.
   * @param targetValueByState A function that returns the target value for a given state.
   * @param aggregate If [targetValueByState] is a many-to-one mapping.
   */
  @Composable
  fun <V> interpolatedValue(
    interpolateValue: (start: V, end: V, progress: Float) -> V,
    targetValueByState: (state: T) -> V,
    aggregate: Boolean = false,
  ): SwipeableValue<V> =
    remember(this, interpolateValue, targetValueByState) {
      SwipeableValue(this, interpolateValue, targetValueByState, aggregate)
    }

  /**
   * Creates a derived [SwipeableTransition] using the provided [transform].
   *
   * @param aggregate If [transform] is a many-to-one mapping
   * @param transform A function mapping states of this transition to new ones
   */
  fun <S> derive(aggregate: Boolean = false, transform: (T) -> S): SwipeableTransition<S> {
    val getStates =
      if (aggregate) {
        val states =
          derivedStateOf(pairReferentialEqualityPolicy()) { transitionStates().map(transform) }
        ({ states.value })
      } else ({ transitionStates().map(transform) })
    val getProgress =
      if (aggregate) {
        val progressState = derivedStateOf { if (getStates().isSame) 0f else progress() }
        ({ progressState.value })
      } else ({ progress() })
    return SwipeableTransition(transitionStates = getStates, progress = getProgress)
  }

  /**
   * Returns a derived [SwipeableTransition] using the provided [stateTransform]
   *
   * @see derive()
   */
  @Composable
  fun <S> derived(aggregate: Boolean = false, stateTransform: (T) -> S) =
    remember(this) { derive(aggregate, stateTransform) }

  /**
   * Returns a derived [State] with discrete values derived from the continuous state of this
   * transition.
   */
  @Composable
  fun <S> derivedState(transform: FluentState<T>.() -> S) =
    remember(this, transform) { derivedStateOf { transform() } }
}

/**
 * Stores the calculation of an interpolated value of a [SwipeableTransition] so it can be retrieved
 * later, deferring state reads.
 */
class SwipeableValue<V>(private val getValue: () -> V) : State<V> {
  companion object {
    /**
     * Generic factory method for creating a [SwipeableValue].
     *
     * @param transition The [SwipeableTransition] to derive the value from.
     * @param interpolateValue A function that interpolates between two values of the target type.
     * @param targetValueByState A function that returns the target value for a given state.
     * @param aggregate If [targetValueByState] is a many-to-one mapping.
     */
    operator fun <T, V> invoke(
      transition: SwipeableTransition<T>,
      interpolateValue: (start: V, end: V, progress: Float) -> V,
      targetValueByState: (state: T) -> V,
      aggregate: Boolean = false,
    ): SwipeableValue<V> {
      var getValue = {
        val (prev, next) = transition.transitionStates()
        val prevValue = targetValueByState(prev)
        val nextValue = targetValueByState(next)
        if (prevValue == nextValue) prevValue
        else interpolateValue(prevValue, nextValue, transition.progress())
      }
      if (aggregate) {
        val state = derivedStateOf(getValue)
        getValue = { state.value }
      }
      return SwipeableValue(getValue)
    }
  }

  override val value
    get() = getValue()

  /** Creates a value derived from this one. */
  fun <U> derive(transform: (V) -> U) = SwipeableValue { transform(getValue()) }

  /** Returns a remembered value derived from this one. */
  @Composable fun <U> derived(transform: (V) -> U) = remember(this, transform) { derive(transform) }

  override fun hashCode() = getValue.hashCode()

  override fun equals(other: Any?) = other is SwipeableValue<*> && getValue == other.getValue
}

fun <T> SwipeableTransition<T>.interpolateFloat(targetValueByState: (state: T) -> Float) =
  interpolateValue(interpolateValue = ::lerp, targetValueByState = targetValueByState)

@Composable
fun <T> SwipeableTransition<T>.interpolatedFloat(
  aggregate: Boolean = false,
  targetValueByState: (state: T) -> Float,
) =
  interpolatedValue(
    interpolateValue = ::lerp,
    targetValueByState = targetValueByState,
    aggregate = aggregate,
  )

fun <T> SwipeableTransition<T>.interpolateInt(targetValueByState: (state: T) -> Int) =
  interpolateValue(interpolateValue = ::lerp, targetValueByState = targetValueByState)

@Composable
fun <T> SwipeableTransition<T>.interpolatedInt(
  aggregate: Boolean = false,
  targetValueByState: (state: T) -> Int,
) =
  interpolatedValue(
    interpolateValue = ::lerp,
    targetValueByState = targetValueByState,
    aggregate = aggregate,
  )

fun <T> SwipeableTransition<T>.interpolateColor(targetValueByState: (state: T) -> Color) =
  interpolateValue(interpolateValue = ::lerp, targetValueByState = targetValueByState)

@Composable
fun <T> SwipeableTransition<T>.interpolatedColor(
  aggregate: Boolean = false,
  targetValueByState: (state: T) -> Color,
) = interpolatedValue(interpolateValue = ::lerp, targetValueByState = targetValueByState, aggregate)

fun <T> SwipeableTransition<T>.interpolateDp(targetValueByState: (state: T) -> Dp) =
  interpolateValue(interpolateValue = ::lerp, targetValueByState = targetValueByState)

@Composable
fun <T> SwipeableTransition<T>.interpolatedDp(
  aggregate: Boolean = false,
  targetValueByState: (state: T) -> Dp,
) = interpolatedValue(interpolateValue = ::lerp, targetValueByState = targetValueByState, aggregate)

/** Describes a desired relation between the current transition state and another state. */
sealed interface SwipeableStateRelation<T> {
  /** Returns if the given [transition] has the desired relation. */
  operator fun contains(transition: Pair<T, T>): Boolean
}

/** The transition state should be directly before, after or at the given [state]. */
class Near<T>(val state: T) : SwipeableStateRelation<T> {
  override fun contains(transition: Pair<T, T>) =
    state == transition.first || state == transition.second
}

/** The transition state should be at the given [state]. */
class Eq<T>(val state: T) : SwipeableStateRelation<T> {
  override fun contains(transition: Pair<T, T>) =
    state == transition.first && state == transition.second
}

/** The transition state should be at or before the given [state]. */
class Leq<T>(val state: T) : SwipeableStateRelation<T> {
  override fun contains(transition: Pair<T, T>) = transition.second == state
}

/** The transition state should be at or after the given [state]. */
class Geq<T>(val state: T) : SwipeableStateRelation<T> {
  override fun contains(transition: Pair<T, T>) = transition.first == state
}

/** The transition state should be directly before but not at the given [state]. */
class Lt<T>(val state: T) : SwipeableStateRelation<T> {
  override fun contains(transition: Pair<T, T>) =
    state == transition.second && state != transition.first
}

/** The transition state should be directly after but not at the given [state]. */
class Gt<T>(val state: T) : SwipeableStateRelation<T> {
  override fun contains(transition: Pair<T, T>) =
    state == transition.first && state != transition.second
}
