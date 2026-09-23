package com.github.DemitriusEnzo.to_do_list.repository

import com.github.DemitriusEnzo.to_do_list.data.Tarefa
import com.github.DemitriusEnzo.to_do_list.data.TarefaDao
import kotlinx.coroutines.flow.Flow

class TarefaRepository(private val dao: TarefaDao) {

    val tarefas: Flow<List<Tarefa>> = dao.listarTodas()

    suspend fun inserir(tarefa: Tarefa) = dao.inserir(tarefa)

    suspend fun atualizar(tarefa: Tarefa) = dao.atualizar(tarefa)

    suspend fun deletar(tarefa: Tarefa) = dao.deletar(tarefa)
}
