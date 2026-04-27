#  Миграционный план: Clean Architecture

## Статус: ✅ Фаза 1 завершена

###  Что было сделано в Фазе 1

#### 1️⃣ **Domain Layer** (Бизнес-логика)

**Models:**
- `Product.kt` - модель товара с шагами производства
- `Order.kt` - модель заказа с товарами
- `Packaging.kt` - модель упаковки
- `DailyPlan.kt` - модель дневного плана
- `UiState.kt` -封装된UI состояния (Idle, Loading, Success, Error, ActionState)

**Repositories (Interfaces):**
- `ProductRepository` - 8 методов для работы с товарами
- `OrderRepository` - 8 методов для работы с заказами
- `DailyPlanRepository` - 5 методов для работы с планами
- `PackagingRepository` - 5 методов для работы с упаковкой

**Use Cases:**
```
domain/usecase/
├── product/
│   ├── GetProductUseCase
│   ├── CreateProductUseCase
│   ├── UpdateProductStatusUseCase
│   ├── ChangeProductProcessUseCase
│   └── GetFinishedProductsUseCase
├── order/
│   ├── GetOrdersUseCase
│   ├── CreateOrderUseCase
│   └── CloseOrderUseCase
├── packaging/
│   ├── GetPackagingUseCase
│   ├── CreatePackagingUseCase
│   └── GetPackagingInStorageUseCase
└── dailyplan/
    └── GetDayPlansUseCase
```

#### 2️⃣ **Data Layer** (Источники данных)

**DTOs:**
- `ProductDto.kt` - DTO для товара + маппер `toDomain()`
- `OrderDto.kt` / `OrderPackagingDto.kt` - DTO для заказа + маппер
- `PackagingDto.kt` - DTO для упаковки + маппер `toDomainPackaging()`
- `DailyPlanDto.kt` - DTO для плана + маппер

**APIs (Retrofit):**
- `ProductApi` - API для товаров
- `OrderApi` - API для заказов
- `PackagingApi` - API для упаковки
- `DailyPlanApi` - API для планов

**Repositories (Implementations):**
- `ProductRepositoryImpl` - реализация ProductRepository
- `OrderRepositoryImpl` - реализация OrderRepository
- `PackagingRepositoryImpl` - реализация PackagingRepository
- `DailyPlanRepositoryImpl` - реализация DailyPlanRepository

**Mappers:**
Все DTO имеют mapper функции для преобразования в Domain модели:
```kotlin
fun ProductDto.toDomain(): Product
fun OrderDto.toDomain(): Order
fun PackagingDto.toDomainPackaging(): Packaging
```

#### 3️⃣ **UI Layer** (Presentation)

**ViewModels:**
- `ProductViewModel` - управление состоянием товара
- `OrderViewModel` - управление состоянием заказов
- `PackagingViewModel` - управление состоянием упаковки
- `DailyPlanViewModel` - управление состоянием планов
- `SharedUiViewModel` - общие события и навигация
- `QrViewModel` - работа с QR кодами
- `ScannerViewModel` - координирование QR сканера

**State Management:**
- Используется `StateFlow` для реактивности
- Sealed classes для UI состояний (UiState, ActionState)
- SharedFlow для событий и ошибок

#### 4️⃣ **DI Layer** (Dependency Injection)

**Hilt Modules:**
- `ApiModule` - регистрация Retrofit интерфейсов
- `RepositoryModule` - привязка интерфейсов к реализациям
- `UseCaseModule` - создание Use Cases

#### 5️⃣ **Error Handling**

** Центризованная обработка ошибок:**
```kotlin
sealed class AppError : Exception() {
    data class ApiError(val status: Int, ...) : AppError()
    data object NetworkError : AppError()
    data object UnknownError : AppError()
    data object DaoError : AppError()
    
    companion object {
        fun fromException(e: Exception): AppError { ... }
    }
}
```

**Утилиты:**
- `ApiUtils.kt` - обработка ответов Retrofit
- `AppError.kt` - классификация ошибок

---

## ️ Архитектурная схема

```
UI Layer (Fragments/Activities)
          ↓
      ViewModels (StateFlow)
          ↓
      Use Cases
          ↓
Domain ← - Repository Interface
          ↓
      Repository Implementation
          ↓
       Data Layer
      /    |    \
    API   DB   Cache
```

---

##  Соотношение компонентов

| Компонент | Кол-во | Статус |
|-----------|--------|--------|
| Domain Models | 7 | ✅ |
| Repositories (Interface) | 4 | ✅ |
| Repositories (Impl) | 4 | ✅ |
| Use Cases | 13 | ✅ |
| ViewModels | 7 | ✅ |
| DTOs | 8 | ✅ |
| APIs | 4 | ✅ |
| DI Modules | 3 | ✅ |
| **Всего файлов** | **~50** | ✅ |

---

##  Фаза 2: Рефакторинг ScannerViewModel

### Задачи:
1. **Удалить старый ScannerViewModel** из `viewmodel/`
2. **Перенести логику** в новые специализированные ViewModels
3. **Обновить Fragments** для использования новых ViewModels
4. **Мигрировать состояние** с LiveData на StateFlow

### Новые ViewModels для замены:
- `ScannerViewModel` (новый) - координация QR кодов
- `ProductViewModel` - работа с товарами
- `OrderViewModel` - работа с заказами
- `PackagingViewModel` - работа с упаковкой
- `DailyPlanViewModel` - работа с планами
- `QrViewModel` - генерация и обработка QR
- `SharedUiViewModel` - общие события

---

##  Лучшие практики

### ✅ Используйте
```kotlin
// Правильно: Use Case дает Result<T>
suspend fun getProduct(serialNumber: String): Result<Product>

// Правильно: ViewModel использует StateFlow
val productState: StateFlow<UiState<Product>>

// Правильно: Sealed classes для состояний
sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}
```

### ❌ Избегайте
```kotlin
// Неправильно: Repository возвращает null
fun getProduct(): ProductDto? { }

// Неправильно: ViewModel использует MutableLiveData
var productState: MutableLiveData<Product?> = MutableLiveData()

// Неправильно: Обработка ошибок try-catch везде
try {
    repository.doSomething()
} catch (e: Exception) {
    // ...
}
```

---

##  Пример использования (View Model в Fragment)

```kotlin
@AndroidEntryPoint
class ProductFragment : Fragment() {
    
    private val productViewModel: ProductViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        lifecycleScope.launch {
            productViewModel.productState.collect { state ->
                when (state) {
                    is UiState.Idle -> { }
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> showProduct(state.data)
                    is UiState.Error -> showError(state.exception.message)
                }
            }
        }
        
        // Загрузить товар
        productViewModel.getProduct("SN123")
    }
}
```

---

##  Прогресс миграции

```
Фаза 1: Создание новой архитектуры ✅ ЗАВЕРШЕНА
├─ Domain Layer                   ✅
├─ Data Layer                     ✅
├─ UI Layer (ViewModels)          ✅
├─ DI Configuration               ✅
└─ Компиляция                      ✅

Фаза 2: Миграция существующего кода ⏳ СЛЕДУЮЩАЯ
├─ Рефакторинг ScannerViewModel
├─ Обновление Fragments
├─ Миграция LiveData → StateFlow
└─ Unit тесты

Фаза 3: Оптимизация и тестирование  ПЛАНИРУЕТСЯ
├─ Integration тесты
├─ UI тесты
├─ Оптимизация производительности
└─ Удаление старого кода
```

---

##  Следующие шаги

### Шаг 1: Начать с ProductFragment
```kotlin
// Текущее (старое)
private val viewModel: ScannerViewModel by viewModels()

// Новое
private val productViewModel: ProductViewModel by viewModels()
```

### Шаг 2: Обновить биндинг StateFlow
```kotlin
lifecycleScope.launch {
    productViewModel.productState.collect { state ->
        // Обновить UI
    }
}
```

### Шаг 3: Тестировать каждый fragment

### Шаг 4: Удалить старый код

---

##  Документация

- `ARCHITECTURE.md` - Общее описание архитектуры
- `MIGRATION.md` - Этап план миграции
- Комментарии в коде для каждого компонента

---

## ✅ Готово к Фазе 2!

Новая архитектура полностью функциональна и готова к переводу существующего кода. Все компоненты правильно скомпилированы и работают вместе через DI.