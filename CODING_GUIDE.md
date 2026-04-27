#  Руководство по использованию новой архитектуры

## 1. Как создать новый Use Case

```kotlin
//  domain/usecase/myfeature/MyUseCase.kt
package ru.faserkraft.client.domain.usecase.myfeature

import ru.faserkraft.client.domain.model.MyModel
import ru.faserkraft.client.domain.repository.MyRepository
import javax.inject.Inject

class MyUseCase @Inject constructor(
    private val myRepository: MyRepository
) {
    /**
     * Основной оператор invoke для удобства использования
     */
    suspend operator fun invoke(param: String): Result<MyModel> {
        return myRepository.doSomething(param)
    }
}
```

## 2. Как создать новый Repository Interface

```kotlin
//  domain/repository/MyRepository.kt
package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.MyModel

interface MyRepository {
    /**
     * Описание метода
     * @param param параметр
     * @return Result с моделью или ошибкой
     */
    suspend fun doSomething(param: String): Result<MyModel>
}
```

## 3. Как реализовать Repository

```kotlin
//  data/repository/MyRepositoryImpl.kt
package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.api.MyApi
import ru.faserkraft.client.data.dto.toDomain
import ru.faserkraft.client.domain.model.MyModel
import ru.faserkraft.client.domain.repository.MyRepository
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.callApi
import javax.inject.Inject

class MyRepositoryImpl @Inject constructor(
    private val myApi: MyApi
) : MyRepository {

    override suspend fun doSomething(param: String): Result<MyModel> {
        return try {
            val response = myApi.doSomething(param)
            callApi(response).map { it.toDomain() }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }
}
```

## 4. Как создать Retrofit API

```kotlin
//  data/api/MyApi.kt
package ru.faserkraft.client.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.data.dto.MyDto

interface MyApi {

    @GET(BuildConfig.BASE_URL + "endpoint")
    suspend fun doSomething(
        @Query("param") param: String
    ): Response<MyDto>
}
```

## 5. Как создать DTO с маппером

```kotlin
//  data/dto/MyDto.kt
package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName
import ru.faserkraft.client.domain.model.MyModel

data class MyDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String
)

/**
 * Преобразование DTO в Domain модель
 */
fun MyDto.toDomain(): MyModel {
    return MyModel(
        id = id,
        name = name,
        status = status
    )
}
```

## 6. Как создать Domain Model

```kotlin
//  domain/model/MyModel.kt
package ru.faserkraft.client.domain.model

data class MyModel(
    val id: Long,
    val name: String,
    val status: String
)
```

## 7. Как создать ViewModel

```kotlin
//  ui/myfeature/MyViewModel.kt
package ru.faserkraft.client.ui.myfeature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.MyModel
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.domain.usecase.myfeature.MyUseCase
import javax.inject.Inject

@HiltViewModel
class MyViewModel @Inject constructor(
    private val myUseCase: MyUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MyModel>>(UiState.Idle)
    val state: StateFlow<UiState<MyModel>> = _state

    fun loadData(param: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            myUseCase(param)
                .onSuccess { data ->
                    _state.value = UiState.Success(data)
                }
                .onFailure { exception ->
                    _state.value = UiState.Error(exception)
                }
        }
    }

    fun clearState() {
        _state.value = UiState.Idle
    }
}
```

## 8. Как регистрировать в DI

```kotlin
//  di/MyModule.kt
package ru.faserkraft.client.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import ru.faserkraft.client.data.api.MyApi
import ru.faserkraft.client.data.repository.MyRepositoryImpl
import ru.faserkraft.client.domain.repository.MyRepository
import ru.faserkraft.client.domain.usecase.myfeature.MyUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MyModule {

    //Bindидем интерфейс к реализации
    @Binds
    abstract fun bindMyRepository(
        impl: MyRepositoryImpl
    ): MyRepository
}

@Module
@InstallIn(SingletonComponent::class)
object MyApiModule {

    @Provides
    @Singleton
    fun provideMyApi(retrofit: Retrofit): MyApi {
        return retrofit.create(MyApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object MyUseCaseModule {

    @Provides
    @Singleton
    fun provideMyUseCase(
        repository: MyRepository
    ): MyUseCase {
        return MyUseCase(repository)
    }
}
```

## 9. Как использовать в Fragment

```kotlin
//  ui/myfeature/MyFragment.kt
package ru.faserkraft.client.ui.myfeature

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.domain.model.UiState

@AndroidEntryPoint
class MyFragment : Fragment(R.layout.fragment_my) {

    private val viewModel: MyViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Наблюдать за состоянием
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        // Скрыть лоадер, показать кнопку загрузки
                    }
                    is UiState.Loading -> {
                        // Показать прогресс
                        showLoading()
                    }
                    is UiState.Success -> {
                        // Показать данные
                        showData(state.data)
                    }
                    is UiState.Error -> {
                        // Показать ошибку
                        showError(state.exception)
                    }
                }
            }
        }

        // Инициировать загрузку
        viewModel.loadData("my_param")
    }

    private fun showLoading() {
        // TODO
    }

    private fun showData(data: Any) {
        // TODO
    }

    private fun showError(exception: Throwable) {
        // TODO
    }
}
```

## 10. Unit тестирование

```kotlin
//  domain/usecase/myfeature/MyUseCaseTest.kt
package ru.faserkraft.client.domain.usecase.myfeature

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.faserkraft.client.domain.model.MyModel
import ru.faserkraft.client.domain.repository.MyRepository

class MyUseCaseTest {

    private val repository = mockk<MyRepository>()
    private val useCase = MyUseCase(repository)

    @Test
    fun `should return success when repository returns data`() = runTest {
        // Given
        val mockModel = MyModel(
            id = 1L,
            name = "Test",
            status = "active"
        )
        coEvery { repository.doSomething("param") } returns Result.success(mockModel)

        // When
        val result = useCase("param")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(mockModel, result.getOrNull())
    }

    @Test
    fun `should return failure when repository throws exception`() = runTest {
        // Given
        coEvery { repository.doSomething("param") } returns 
            Result.failure(Exception("Test error"))

        // When
        val result = useCase("param")

        // Then
        assertTrue(result.isFailure)
    }
}
```

##  Чек-лист для нового компонента

- [ ] Создан Domain Model
- [ ] Создан Repository Interface
- [ ] Реализован Repository
- [ ] Создан DTO с маппером
- [ ] Создан Retrofit API интерфейс
- [ ] Реализован Repository Implementation
- [ ] Создан Use Case
- [ ] Создан ViewModel
- [ ] Зарегистрирован в DI
- [ ] Написаны unit тесты
- [ ] Успешно скомпилировано
- [ ] Используется в Fragment/Activity

##  Запуск тестов

```bash
./gradlew test                    # Все unit тесты
./gradlew connectedAndroidTest    # Android тесты
./gradlew testDebug               # Debug tests
```

##  Полезные ссылки

- Clean Architecture: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
- MVVM: https://developer.android.com/jetpack/guide
- Coroutines: https://kotlinlang.org/docs/coroutines-overview.html
- StateFlow: https://developer.android.com/kotlin/flow/stateflow-and-sharedflow