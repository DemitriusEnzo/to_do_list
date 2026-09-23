package com.github.DemitriusEnzo.to_do_list.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.DemitriusEnzo.to_do_list.data.Tarefa
import com.github.DemitriusEnzo.to_do_list.data.TarefaDatabase
import com.github.DemitriusEnzo.to_do_list.repository.TarefaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TarefaViewModel(private val repository: TarefaRepository) : ViewModel() {

    val tarefas: StateFlow<List<Tarefa>> = repository.tarefas
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun inserir(tarefa: Tarefa) = viewModelScope.launch { repository.inserir(tarefa) }

    fun atualizar(tarefa: Tarefa) = viewModelScope.launch { repository.atualizar(tarefa) }

    fun deletar(tarefa: Tarefa) = viewModelScope.launch { repository.deletar(tarefa) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = TarefaDatabase.getDatabase(context).tarefaDao()
                    return TarefaViewModel(TarefaRepository(dao)) as T
                }
            }
    }
}
