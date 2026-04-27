# Новая архитектура проекта FKClient

## 🏗️ Clean Architecture + MVVM

Проект переведен на **Clean Architecture** с разделением на слои и **MVVM** паттерн для UI.

## 📁 Структура проекта

```
app/src/main/java/ru/faserkraft/client/
├── domain/                          # Бизнес-логика (независимая от фреймворков)
│   ├── model/                       # Domain модели (Product, Order, DailyPlan, UiState)
│   ├── repository/                  # Интерфейсы репозиториев
│   └── usecase/                     # Use Cases (бизнес-логика)
│       ├── product/                 # Use Cases для товаров
│       ├── order/                   # Use Cases для заказов
│       └── dailyplan/               # Use Cases для планов
├── data/                            # Data слой (внешние источники данных)
│   ├── api/                         # Retrofit API интерфейсы
│   ├── dto/                         # DTO для API + мапперы
│   └── repository/                  # Реализации репозиториев
├── ui/                              # Presentation слой (UI)
│   ├── product/                     # ProductViewModel + UI логика
│   ├── order/                       # OrderViewModel + UI логика
│   ├── dailyplan/                   # DailyPlanViewModel + UI логика
│   └── common/                      # Shared ViewModels
├── di/                              # Dependency Injection (Hilt модули)
│   ├── ApiModule.kt                 # API зависимости
│   ├── RepositoryModule.kt          # Repository зависимости
│   └── UseCaseModule.kt             # Use Case зависимости
├── error/                           # Обработка ошибок
├── utils/                           # Утилиты
└── [старые папки]                   # Постепенно будут удалены
```

## 🔄 Поток данных

```
UI (Fragment/Activity) → ViewModel → UseCase → Repository → API/DB
                              ↓
UI State (UiState<T>) ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←
```

## 📋 Основные компоненты

### Domain Layer
- **Model**: Независимые от фреймворков модели данных
- **Repository**: Интерфейсы для доступа к данным
- **UseCase**: Бизнес-логика, инкапсулированная в отдельные классы

### Data Layer
- **DTO**: Модели для API (с @SerializedName)
- **API**: Retrofit интерфейсы
- **RepositoryImpl**: Реализации репозиториев с маппингом DTO → Domain

### UI Layer
- **ViewModel**: Управление состоянием UI, подписка на Use Cases
- **UiState**: Sealed классы для состояний (Idle, Loading, Success, Error)

### DI Layer
- **Hilt модули** для внедрения зависимостей

## 🚀 Преимущества новой архитектуры

✅ **Тестируемость**: Каждый слой можно тестировать независимо
✅ **Масштабируемость**: Легко добавлять новые фичи
✅ **Поддерживаемость**: Четкое разделение ответственности
✅ **Гибкость**: Легко менять реализации (API, DB, etc.)
✅ **SOLID принципы**: Каждый класс имеет одну ответственность

## 📝 Как использовать

### 1. Создание нового Use Case
```kotlin
class MyUseCase @Inject constructor(
    private val repository: MyRepository
) {
    suspend operator fun invoke(params: Params): Result<Data> {
        // Бизнес-логика
        return repository.doSomething(params)
    }
}
```

### 2. Создание нового ViewModel
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val useCase: MyUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Data>>(UiState.Idle)
    val state: StateFlow<UiState<Data>> = _state

    fun loadData() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            useCase().fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { _state.value = UiState.Error(it) }
            )
        }
    }
}
```

### 3. Использование в Fragment
```kotlin
@AndroidEntryPoint
class MyFragment : Fragment() {

    private val viewModel: MyViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> showData(state.data)
                    is UiState.Error -> showError(state.exception)
                    UiState.Idle -> Unit
                }
            }
        }
    }
}
```

## 🔄 Миграционный план

### Фаза 1 ✅ (Текущая)
- [x] Создать новую структуру папок
- [x] Создать Domain модели и Repository интерфейсы
- [x] Создать Use Cases
- [x] Создать Data слой (DTO, API, RepositoryImpl)
- [x] Создать UI слой (ViewModels)
- [x] Настроить DI

### Фаза 2 (Следующая)
- [ ] Рефакторить ScannerViewModel → отдельные ViewModels
- [ ] Обновить Fragments для использования новых ViewModels
- [ ] Добавить Unit тесты
- [ ] Удалить старый код

### Фаза 3 (Финальная)
- [ ] Полная миграция всех компонентов
- [ ] Оптимизация производительности
- [ ] Добавление кэширования

## 🧪 Тестирование

```kotlin
// Unit тест для Use Case
@Test
fun `get product should return success`() = runTest {
    val repository = mockk<ProductRepository>()
    val useCase = GetProductUseCase(repository)

    coEvery { repository.getProductBySerialNumber("123") } returns
        Result.success(mockProduct)

    val result = useCase("123")

    assertTrue(result.isSuccess)
    assertEquals(mockProduct, result.getOrNull())
}
```

## 📚 Рекомендации

1. **Всегда используйте Result<T>** для обработки ошибок
2. **StateFlow вместо LiveData** для новых компонентов
3. **Sealed классы** для UI состояний
4. **Suspend функции** для асинхронных операций
5. **Hilt** для dependency injection

---

**Следующие шаги**: Начнем рефакторинг ScannerViewModel и перенос логики в новые ViewModels!
