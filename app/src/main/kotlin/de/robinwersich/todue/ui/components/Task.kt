package de.robinwersich.todue.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.robinwersich.todue.R
import de.robinwersich.todue.ui.theme.ToDueTheme
import java.time.LocalDate

@Composable
fun Task(state: TaskState, onEvent: (TaskEvent) -> Unit, modifier: Modifier = Modifier) {
  val taskId = state.id

  TaskContent(
    text = state.text,
    dueDate = state.dueDate,
    doneDate = state.doneDate,
    focusLevel = state.focusLevel,
    onDoneChanged = { onEvent(TaskEvent.SetDone(taskId, it)) },
    onTextChanged = { onEvent(TaskEvent.SetText(taskId, it)) },
    onRemove = { onEvent(TaskEvent.Remove(taskId)) },
    modifier = modifier,
  )
}

@Composable
fun TaskContent(
  text: String,
  dueDate: LocalDate,
  doneDate: LocalDate?,
  focusLevel: TaskFocusLevel,
  onDoneChanged: (Boolean) -> Unit,
  onTextChanged: (String) -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(
    LocalContentColor provides
      if (focusLevel == TaskFocusLevel.BACKGROUND) LocalContentColor.current.copy(alpha = 0.38f)
      else LocalContentColor.current
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
      TaskCheckbox(
        checked = doneDate != null,
        onCheckedChange = onDoneChanged,
        enabled = focusLevel != TaskFocusLevel.BACKGROUND
      )
      Column {
        CachedUpdate(value = text, onValueChanged = onTextChanged) {
          val (cachedText, setCachedText) = it
          BasicTextField(
            value = cachedText,
            onValueChange = setCachedText,
            enabled = focusLevel == TaskFocusLevel.FOCUSSED,
            textStyle =
              MaterialTheme.typography.titleLarge.merge(
                TextStyle(color = LocalContentColor.current)
              ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
          )
        }
        if (focusLevel == TaskFocusLevel.FOCUSSED) {
          ExpandedTaskInfo(
            dueDate = dueDate,
            onRemove = onRemove,
            modifier = Modifier.padding(top = 8.dp)
          )
        } else {
          CollapsedTaskInfo(dueDate = dueDate, modifier = Modifier.padding(top = 8.dp))
        }
      }
    }
  }
}

@Composable
fun TaskCheckbox(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  IconToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    enabled = enabled,
    modifier = modifier
  ) {
    Icon(
      painter =
        painterResource(if (checked) R.drawable.circle_checked else R.drawable.circle_unchecked),
      contentDescription = null
    )
  }
}

@Composable
fun CollapsedTaskInfo(
  modifier: Modifier = Modifier,
  dueDate: LocalDate? = null,
) {
  CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelSmall) {
    Row(modifier = modifier) {
      dueDate?.let {
        Icon(
          painter = painterResource(R.drawable.calendar),
          contentDescription = null,
          modifier = Modifier.padding(end = 4.dp).size(16.dp)
        )
        Text(text = it.toString())
      }
    }
  }
}

@Composable
fun ExpandedTaskInfo(
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
  dueDate: LocalDate? = null,
) {
  Column(modifier = modifier) {
    Spacer(modifier = Modifier.fillMaxWidth().height(40.dp))
    Row(
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.End,
      modifier = Modifier.fillMaxWidth()
    ) {
      IconButton(onClick = onRemove) {
        Icon(painter = painterResource(R.drawable.delete), contentDescription = null)
      }
    }
  }
}

@Preview
@Composable
fun TodoItemPreview() {
  ToDueTheme { Surface { Task(TaskState(text = "Create Todo App"), onEvent = {}) } }
}

@Preview
@Composable
fun TodoItemDonePreview() {
  ToDueTheme {
    Surface { Task(TaskState(text = "Create Todo App", doneDate = LocalDate.now()), onEvent = {}) }
  }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TodoItemDarkPreview() {
  ToDueTheme { Surface { Task(TaskState(text = "Create Todo App"), onEvent = {}) } }
}

@Preview
@Composable
fun TodoItemBackgroundPreview() {
  ToDueTheme {
    Surface {
      Task(
        TaskState(
          text = "Create Todo App",
          doneDate = LocalDate.now(),
          focusLevel = TaskFocusLevel.BACKGROUND
        ),
        onEvent = {}
      )
    }
  }
}

@Preview
@Composable
fun TodoItemMultiLinePreview() {
  ToDueTheme {
    Surface {
      Task(TaskState(text = "This is a very long task that spans two lines"), onEvent = {})
    }
  }
}

@Preview
@Composable
fun TodoItemExpandedPreview() {
  ToDueTheme {
    Surface {
      Task(TaskState(text = "Create Todo App", focusLevel = TaskFocusLevel.FOCUSSED), onEvent = {})
    }
  }
}
