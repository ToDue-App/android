package de.robinwersich.todue.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.robinwersich.todue.ui.components.Task
import de.robinwersich.todue.ui.components.TaskUiState
import de.robinwersich.todue.ui.theme.ToDueTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeScreenViewModel = viewModel(factory = HomeScreenViewModel.Factory)) {
  Scaffold(
    floatingActionButton = {
      FloatingActionButton(onClick = viewModel::addTask) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
      }
    }
  ) { paddingValues ->
    TaskList(
      todos = viewModel.taskList.collectAsState().value,
      onDoneChanged = viewModel::setDone,
      modifier = Modifier.padding(paddingValues)
    )
  }
}

@Composable
fun TaskList(
  todos: List<TaskUiState>,
  onDoneChanged: (id: Int, done: Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(modifier = modifier) {
    items(items = todos, key = { it.id }) {
      val taskId = it.id
      Column {
        Task(
          state = it,
          onDoneChanged = { done -> onDoneChanged(taskId, done) },
        )
        Divider(thickness = Dp.Hairline)
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun TaskListPreview() {
  ToDueTheme {
    TaskList(
      todos = List(50) { TaskUiState(id = it, text = "Task $it", dueDate = LocalDate.now()) },
      onDoneChanged = { _, _ -> },
    )
  }
}