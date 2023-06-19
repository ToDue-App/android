package de.robinwersich.todue.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robinwersich.todue.data.entities.Task
import de.robinwersich.todue.data.repositories.DatabaseTaskRepository
import de.robinwersich.todue.toDueApplication
import de.robinwersich.todue.ui.components.TaskEvent
import de.robinwersich.todue.ui.components.TaskState
import de.robinwersich.todue.ui.components.toUiState
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(
  private val taskRepository: DatabaseTaskRepository,
) : ViewModel() {
  private val expandedTaskId: MutableStateFlow<Int?> = MutableStateFlow(null)
  private val taskList: Flow<List<TaskState>> =
    taskRepository.getAllTasks().combine(expandedTaskId) { tasks, expandedTaskId ->
      tasks.map { it.toUiState(expanded = it.id == expandedTaskId) }
    }

  val viewState: StateFlow<MainScreenState> =
    taskList
      .map { taskList -> MainScreenState(taskList) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainScreenState()
      )

  fun handleEvent(event: TaskEvent) {
    when (event) {
      is TaskEvent.Add ->
        viewModelScope.launch {
          taskRepository.insertTask(Task(text = "", dueDate = LocalDate.now()))
        }
      is TaskEvent.Remove -> viewModelScope.launch { taskRepository.deleteTask(event.id) }
      is TaskEvent.Expand -> expandedTaskId.value = event.id
      is TaskEvent.Collapse -> expandedTaskId.value = null
      is TaskEvent.SetText -> viewModelScope.launch { taskRepository.setText(event.id, event.text) }
      is TaskEvent.SetDone -> {
        val doneDate = if (event.done) LocalDate.now() else null
        viewModelScope.launch { taskRepository.setDoneDate(event.id, doneDate) }
      }
    }
  }

  companion object {
    val Factory = viewModelFactory {
      initializer { MainScreenViewModel(toDueApplication().container.tasksRepository) }
    }
  }
}