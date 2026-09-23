package com.github.DemitriusEnzo.to_do_list.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.DemitriusEnzo.to_do_list.data.Tarefa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ListaTarefasScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val primeira = Tarefa(id = 1, titulo = "Primeira", descricao = "")
    private val segunda = Tarefa(id = 2, titulo = "Segunda", descricao = "")

    @Test
    fun cancelarFechaDialogoSemExcluir() {
        val excluidas = mutableListOf<Tarefa>()
        exibirLista { excluidas.add(it) }

        composeRule.onNodeWithContentDescription("Excluir Primeira").performClick()
        composeRule.onNodeWithText("A tarefa \"Primeira\" será excluída definitivamente.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()

        composeRule.runOnIdle { assertTrue(excluidas.isEmpty()) }
        composeRule.onNodeWithContentDescription("Excluir Primeira").assertIsDisplayed()
    }

    @Test
    fun confirmarExcluiApenasATarefaSelecionada() {
        val excluidas = mutableListOf<Tarefa>()
        exibirLista { excluidas.add(it) }

        composeRule.onNodeWithContentDescription("Excluir Segunda").performClick()
        composeRule.onNodeWithText("A tarefa \"Segunda\" será excluída definitivamente.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Excluir").performClick()

        composeRule.runOnIdle { assertEquals(listOf(segunda), excluidas) }
        composeRule.onNodeWithText("Excluir tarefa?").assertDoesNotExist()
    }

    private fun exibirLista(onDeletar: (Tarefa) -> Unit) {
        composeRule.setContent {
            MaterialTheme {
                ListaTarefasContent(
                    tarefas = listOf(primeira, segunda),
                    onNovaTarefa = {},
                    onEditarTarefa = {},
                    onCheckedChange = { _, _ -> },
                    onDeletar = onDeletar
                )
            }
        }
    }
}
