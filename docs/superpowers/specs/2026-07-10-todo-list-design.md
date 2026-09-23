# To-Do List — Design Spec

**Data:** 2026-07-10  
**Contexto:** Projeto pedagógico para alunos de 3º ano de Sistemas de Informação (FIAP)  
**Objetivo:** App Android de lista de tarefas com CRUD completo, focado em ensinar Room, ViewModel, StateFlow e Navigation Compose de forma didática e sem configurações desnecessárias.

---

## Requisitos

- CRUD completo de tarefas (inserir, listar, marcar como concluída, deletar)
- Interface em **Português**
- Código simples, explícito e sem frameworks de injeção de dependência (sem Hilt/Koin)
- Alunos copiam o código e rodam no próprio Android Studio com SDK próprio
- Toda tela e todo componente reutilizável deve ter função(ões) `@Preview` para visualização isolada no Android Studio, sem precisar rodar o app num emulador

### Entity

```
Tarefa(id: Int, titulo: String, descricao: String, concluida: Boolean, dataCriacao: Long)
```

---

## Arquitetura

Padrão **MVVM com Repository**:

```
Compose UI → ViewModel → Repository → DAO → Room Database
```

Cada camada tem responsabilidade única e se comunica somente com a camada imediatamente adjacente.

---

## Estrutura de Pacotes

```
com.github.DemitriusEnzo.to_do_list/
├── data/
│   ├── Tarefa.kt
│   ├── TarefaDao.kt
│   └── TarefaDatabase.kt
├── repository/
│   └── TarefaRepository.kt
├── viewmodel/
│   └── TarefaViewModel.kt
├── ui/
│   ├── ListaTarefasScreen.kt
│   └── FormularioTarefaScreen.kt
├── navigation/
│   └── AppNavigation.kt
└── MainActivity.kt
```

---

## Camada de Dados

### Tarefa.kt
- `@Entity(tableName = "tarefas")`
- `id: Int` com `@PrimaryKey(autoGenerate = true)`, default `0`
- `titulo: String`
- `descricao: String`
- `concluida: Boolean`, default `false`
- `dataCriacao: Long`, default `System.currentTimeMillis()`

### TarefaDao.kt
- `@Dao interface`
- `listarTodas(): Flow<List<Tarefa>>` — query `SELECT * FROM tarefas ORDER BY dataCriacao DESC`
- `inserir(tarefa: Tarefa)` — `@Insert suspend fun`
- `atualizar(tarefa: Tarefa)` — `@Update suspend fun`
- `deletar(tarefa: Tarefa)` — `@Delete suspend fun`

### TarefaDatabase.kt
- `@Database(entities = [Tarefa::class], version = 1)`
- Singleton via `companion object` com double-checked locking (`@Volatile` + `synchronized`)
- Método estático `getDatabase(context: Context): TarefaDatabase`
- Nome do arquivo: `tarefas.db`

---

## Repository

### TarefaRepository.kt
- Construtor recebe `TarefaDao`
- Propriedade `tarefas: Flow<List<Tarefa>>` delegando ao DAO
- `suspend fun inserir`, `atualizar`, `deletar` — delegam ao DAO diretamente
- Camada intencionalmente fina para deixar o papel do Repository claro aos alunos

---

## ViewModel

### TarefaViewModel.kt
- Estende `ViewModel()`
- Construtor recebe `TarefaRepository`
- `tarefas: StateFlow<List<Tarefa>>` via `repository.tarefas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())`
- `fun inserir`, `atualizar`, `deletar` — lançam coroutine em `viewModelScope`
- `companion object` com `factory(context: Context): ViewModelProvider.Factory` — instancia `TarefaDatabase → TarefaDao → TarefaRepository → TarefaViewModel` manualmente, sem DI framework

---

## Navegação

### AppNavigation.kt
- `NavHost` com `rememberNavController()`
- Rota `"lista"` → `ListaTarefasScreen`
- Rota `"formulario/{tarefaId}"` → `FormularioTarefaScreen`
  - `tarefaId = 0` → nova tarefa
  - `tarefaId > 0` → editar tarefa existente

---

## Convenção de Preview

`@Preview` não consegue instanciar um `TarefaViewModel` de verdade (ele exige um `Context` real). Por isso, toda tela que depende do ViewModel é dividida em duas funções `@Composable` no mesmo arquivo:

- **Versão stateful** (`XScreen`) — recebe o `viewModel`, coleta o `StateFlow` e delega para a versão content. É a única chamada pela navegação. **Sem `@Preview`**, pois depende do ViewModel.
- **Versão content** (`XContent`) — recebe apenas dados simples e lambdas (nenhuma dependência de ViewModel/Context). É essa versão que leva a(s) função(ões) `@Preview`.

Componentes reutilizáveis sem dependência de ViewModel (ex: `TarefaItem`) já são previewable diretamente, sem precisar de divisão.

Padrão de anotação:

```kotlin
@Preview(showBackground = true, name = "Nome descritivo do estado")
@Composable
private fun NomeContentPreview() {
    NomeContent(/* dados de exemplo */)
}
```

Quando a tela tem estados visuais distintos (lista vazia vs. com itens, tarefa concluída vs. pendente, criação vs. edição), criar uma função `@Preview` por estado.

`AppNavigation` e `MainActivity` não são telas nem componentes reutilizáveis — são apenas wiring de navegação/inicialização — e por isso ficam fora dessa exigência.

---

## Telas

### ListaTarefasScreen
- `ListaTarefasScreen` (stateful): coleta `viewModel.tarefas` via `collectAsStateWithLifecycle()` e delega para `ListaTarefasContent`
- `ListaTarefasContent` (previewable): recebe `tarefas: List<Tarefa>` e os callbacks `onNovaTarefa`, `onEditarTarefa`, `onCheckedChange`, `onDeletar`
- `LazyColumn` com um item por `Tarefa`
- Cada item exibe: `titulo`, `descricao` (truncada), `Checkbox` para `concluida`
- Marcar checkbox dispara `onCheckedChange(tarefa, novoValor)` → na versão stateful, isso chama `viewModel.atualizar(tarefa.copy(concluida = novoValor))`
- Botão de deletar (ícone lixeira) por item dispara `onDeletar(tarefa)` → na versão stateful, `viewModel.deletar(tarefa)`
- `FloatingActionButton` com ícone `+` navega para `"formulario/0"`
- Toque em item navega para `"formulario/{tarefa.id}"`
- `TarefaItem` (componente, já previewable): exibe uma `Tarefa` isolada — previews para estado concluído e pendente
- Previews de `ListaTarefasContent`: lista vazia, lista com tarefas (concluída + pendente)

### FormularioTarefaScreen
- `FormularioTarefaScreen` (stateful): recebe `tarefaId: Int` e `viewModel`; busca a tarefa existente na `StateFlow` (se `tarefaId > 0`) e delega para `FormularioTarefaContent`
- `FormularioTarefaContent` (previewable): recebe `isEdicao: Boolean`, `tituloInicial: String`, `descricaoInicial: String` e os callbacks `onSalvar(titulo, descricao)`, `onVoltar`
- Campos: `TextField` para `titulo` (obrigatório) e `descricao`
- Botão "Salvar" chama `onSalvar(titulo.trim(), descricao.trim())`:
  - Na versão stateful, `tarefaId == 0` → `viewModel.inserir(Tarefa(titulo=..., descricao=...))`; `tarefaId > 0` → `viewModel.atualizar(tarefaExistente.copy(titulo=..., descricao=...))`
  - Após salvar: `navController.popBackStack()`
- Botão "Voltar" (ou seta na TopAppBar): `onVoltar()` sem salvar
- Previews de `FormularioTarefaContent`: "Nova tarefa" (campos vazios) e "Editar tarefa" (campos pré-preenchidos)

### MainActivity
- Cria o `TarefaViewModel` via `viewModel(factory = TarefaViewModel.factory(applicationContext))`
- Chama `AppNavigation(viewModel)` dentro do tema

---

## Dependências a Adicionar

| Dependência | Motivo |
|---|---|
| `room-runtime` + `room-ktx` | Banco de dados local |
| `room-compiler` (via KSP) | Geração de código das anotações Room |
| `navigation-compose` | NavHost e NavController |
| `lifecycle-viewmodel-compose` | `viewModel()` em Composables |
| Plugin KSP | Processador de anotações do Room |

---

## Conceitos Ensinados

- `@Entity`, `@PrimaryKey`, `@Database`
- `@Dao`, `@Insert`, `@Update`, `@Delete`, `@Query`
- `Flow` no DAO → `StateFlow` no ViewModel via `stateIn`
- `viewModelScope.launch` para operações suspensas
- `ViewModelProvider.Factory` manual (sem DI)
- `NavHost`, `composable`, `navController.navigate`, `popBackStack`
- `collectAsStateWithLifecycle` para coletar Flow na UI
- `LazyColumn` com lista reativa
- `@Preview(showBackground = true, name = "...")` — visualização isolada de telas e componentes no Android Studio, sem rodar o app
- Separação entre Composable "stateful" (consome ViewModel) e "content" (recebe dados/callbacks simples, é previewable)
