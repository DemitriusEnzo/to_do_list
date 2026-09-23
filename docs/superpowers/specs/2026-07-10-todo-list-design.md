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

## Telas

### ListaTarefasScreen
- Coleta `viewModel.tarefas` via `collectAsStateWithLifecycle()`
- `LazyColumn` com um item por `Tarefa`
- Cada item exibe: `titulo`, `descricao` (truncada), `Checkbox` para `concluida`
- Marcar checkbox dispara `viewModel.atualizar(tarefa.copy(concluida = !tarefa.concluida))`
- Botão de deletar (ícone lixeira) por item dispara `viewModel.deletar(tarefa)`
- `FloatingActionButton` com ícone `+` navega para `"formulario/0"`
- Toque em item navega para `"formulario/{tarefa.id}"`

### FormularioTarefaScreen
- Recebe `tarefaId: Int` e `viewModel`
- Se `tarefaId > 0`: busca a tarefa na lista de `StateFlow` e pré-preenche os campos
- Campos: `TextField` para `titulo` (obrigatório) e `descricao`
- Botão "Salvar":
  - `tarefaId == 0` → `viewModel.inserir(Tarefa(titulo=..., descricao=...))`
  - `tarefaId > 0` → `viewModel.atualizar(tarefaExistente.copy(titulo=..., descricao=...))`
  - Após salvar: `navController.popBackStack()`
- Botão "Voltar" (ou seta na TopAppBar): `navController.popBackStack()` sem salvar

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
