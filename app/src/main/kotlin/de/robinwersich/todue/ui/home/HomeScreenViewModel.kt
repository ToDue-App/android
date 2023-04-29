package de.robinwersich.todue.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.robinwersich.todue.data.repositories.DatabaseTaskRepository
import de.robinwersich.todue.toDueApplication
import de.robinwersich.todue.ui.TaskUiData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeScreenViewModel(
  taskRepository: DatabaseTaskRepository,
) : ViewModel() {
  val taskList: StateFlow<List<TaskUiData>> =
    taskRepository
      .getAllTasks()
      .map { tasks -> tasks.map { TaskUiData(it.id, it.text, it.dueDate, it.doneDate != null) } }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
      )

  companion object {
    val Factory = viewModelFactory {
      initializer { HomeScreenViewModel(toDueApplication().container.tasksRepository) }
    }
  }
}
