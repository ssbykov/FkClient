#  Фаза 2: Миграция старого кода на новую архитектуру

##  План миграции

### Этап 1: Подготовка базовых адаптеров (2шт)

#### 1.1 Создать Base Fragment класс с общей логикой

```kotlin
// ui/base/BaseFragment.kt
@AndroidEntryPoint
abstract class BaseFragment<VM : ViewModel> : Fragment() {
    protected abstract val viewModel: VM
    
    protected fun showDialog(message: String, onPositive: () -> Unit = {}) {
        // Общая логика диалогов
    }
    
    protected fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        // Общая логика подтверждения
    }
}
```

#### 1.2 Создать расширение для StateFlow

```kotlin
// utils/FlowExtensions.kt
fun <T> StateFlow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.lifecycle.repeatOnLifecycle(state) {
            collect { action(it) }
        }
    }
}
```

---

### Этап 2: Миграция компонентов по порядку

| Приоритет | Fragment | Зависит от | Сложность | Статус |
|-----------|----------|-----------|----------|--------|
| 1 | ProductFragment | ProductViewModel | ⭐⭐ | ⏳ TODO |
| 2 | OrdersFragment | OrderViewModel | ⭐⭐ | ⏳ TODO |
| 3 | PackagingFragment | PackagingViewModel | ⭐⭐ | ⏳ TODO |
| 4 | DayPlanFragment | DailyPlanViewModel | ⭐⭐ | ⏳ TODO |
| 5 | ScannerFragment | ScannerViewModel | ⭐⭐⭐ | ⏳ TODO |
| 6 | RegistrationFragment | SharedUiViewModel | ⭐ | ⏳ TODO |
| 7 | Остальные Fragments | Разные | ⭐⭐⭐ | ⏳ TODO |

---

##  Пример: Миграция ProductFragment

### **БЫЛО (старое):**
```kotlin
class ProductFragment : Fragment() {
    private val viewModel: ScannerViewModel by activityViewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // LiveData observe
        viewModel.productState.observe(viewLifecycleOwner) { productState ->
            product = productState ?: return@observe
            // Update UI
        }
        
        // Старые DTOs
        val isRework = product.status == ProductStatus.REWORK
    }
}
```

### **СТАЛО (новое):**
```kotlin
@AndroidEntryPoint
class ProductFragment : Fragment(R.layout.fragment_product) {

    private val productViewModel: ProductViewModel by viewModels()
    private val orderViewModel: OrderViewModel by viewModels()
    private val sharedUiViewModel: SharedUiViewModel by viewModels()
    
    private var _binding: FragmentProductBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Используем новый ProductViewModel с StateFlow
        productViewModel.productState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> showLoading()
                is UiState.Success -> {
                    val product = state.data
                    updateProductUI(product)
                }
                is UiState.Error -> {
                    showError(state.exception.message ?: "Unknown error")
                }
                UiState.Idle -> hideLoading()
            }
        }
        
        // Подписаться на ошибки
        sharedUiViewModel.errorMessages.collectIn(viewLifecycleOwner) { message ->
            showDialog(message)
        }
        
        binding.btnClose.setOnClickListener {
            productViewModel.closeStep()
        }
    }
    
    private fun updateProductUI(product: Product) {
        with(binding) {
            tvProductNumber.text = product.serialNumber
            tvProcess.text = "Process ${product.processId}"
            
            // Новые Domain models имеют четкую структуру
            val currentStep = product.steps.firstOrNull()
            currentStep?.let {
                tvStepName.text = it.stepDefinition.name
                tvStepOrder.text = "Step #${it.stepDefinition.order}"
            }
        }
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }
    
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        showDialog(message)
    }
    
    private fun showDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

---

##  Порядок миграции (step-by-step)

### **Шаг 1:** Изучить текущий код
- [ ] Прочитать ProductFragment полностью
- [ ] Понять какие ViewModel методы используются
- [ ] Найти все зависимости

### **Шаг 2:** Создать адаптеры
- [ ] BaseFragment с общей логикой
- [ ] FlowExtensions для удобства
- [ ] Common dialogs utility

### **Шаг 3:** Рефакторить ProductFragment
- [ ] Заменить ScannerViewModel на ProductViewModel + SharedUiViewModel
- [ ] Заменить observe на collectIn
- [ ] Заменить ProductDto на Product domain model
- [ ] Тестировать функциональность

### **Шаг 4:** Рефакторить OrdersFragment
- [ ] Применить те же принципы
- [ ] Заменить на OrderViewModel
- [ ] Тестировать

### **Шаг 5:** Рефакторить остальные
- [ ] Одинаково для всех
- [ ] Валидировать компиляцию
- [ ] Unit тесты

---

## ⚠️ Важные моменты

### 1. **Импорты**
```kotlin
// УДАЛИТЬ старые импорты
import ru.faserkraft.client.viewmodel.ScannerViewModel
import ru.faserkraft.client.dto.ProductDto

// ДОБАВИТЬ новые
import ru.faserkraft.client.ui.product.ProductViewModel
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.UiState
import dagger.hilt.android.AndroidEntryPoint
```

### 2. **ViewModels**
```kotlin
// Если использовали activityViewModels() (общий для всей Activity)
// Теперь используем viewModels() (вложенный для Fragment)
private val productViewModel: ProductViewModel by viewModels()
private val sharedUiViewModel: SharedUiViewModel by viewModels()
```

### 3. **Навигация**.
```kotlin
// Старое (может остаться)
findNavController().navigate(R.id.action_productFragment_to_editProductFragment)

// Новое (лучше использовать SharedUiViewModel)
sharedUiViewModel.navigate(NavigationEvent.NavigateToEditProduct)
```

---

##  Тестирование во время миграции

### Перед миграцией
```bash
# Компилируем и убедимся что работает
./gradlew compileDebugKotlin
./gradlew assembleDebug
```

### После каждого Fragment
```bash
# Проверяем компиляцию
./gradlew compileDebugKotlin

# Запускаем на девайсе и проверяем функциональность
# - Загрузка данных
# - Отображение UI
# - Ошибки обработаны
# - Навигация работает
```

---

##  Прогресс отслеживания

### Фаза 2.1 - Подготовка (⏳ Current)
- [ ] Создать BaseFragment
- [ ] Создать FlowExtensions
- [ ] Обновить DI для нового структуры

### Фаза 2.2 - Миграция основных Fragments
- [ ] ProductFragment
- [ ] OrdersFragment
- [ ] PackagingFragment
- [ ] DailyPlanFragment

### Фаза 2.3 - Миграция вспомогательных
- [ ] ScannerFragment
- [ ] RegistrationFragment
- [ ] Остальные Fragments

### Фаза 2.4 - Валидация
- [ ] Все компилируется
- [ ] Все функционирует
- [ ] Unit тесты написаны
- [ ] Старый код удален

---

##  Чек-лист для каждого Fragment

Используйте этот список при миграции каждого Fragment:

- [ ] Старый ViewModel заменен на новый
- [ ] @AndroidEntryPoint добавлен
- [ ] Все импорты обновлены
- [ ] LiveData заменена на StateFlow
- [ ] DTOs заменены на Domain models
- [ ] Обработка ошибок через SharedUiViewModel
- [ ] Навигация через SharedUiViewModel события
- [ ] Компиляция успешна
- [ ] Функциональность проверена
- [ ] Unit тесты написаны

---

##  Советы для гладкой миграции

1. **Один Fragment за раз** - не мигрируйте все сразу
2. **Тестируйте после каждого** - убедитесь что работает
3. **Сохраняйте старый код** - удалите в конце Фазы 3
4. **Используйте IDE** - Let IntelliJ подсказывать
5. **Документируйте изменения** - помогает другим разработчикам

---

**Готовы начать Фазу 2.1? **