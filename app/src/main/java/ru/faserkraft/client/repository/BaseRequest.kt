package ru.faserkraft.client.repository

import retrofit2.Response
import ru.faserkraft.client.dto.ItemDto
import ru.faserkraft.client.entity.ItemEntity
import ru.faserkraft.client.error.ApiError
import ru.faserkraft.client.error.NetworkError
import ru.faserkraft.client.error.UnknownError

import java.io.IOException

class BaseRequest {
//    suspend fun <T : ItemDto, V : ItemEntity> getList(
    suspend fun <T : ItemDto> get(
    id: String,
    responseApi: suspend (id: String) -> Response<T>,
//        mapper: (List<T>) -> List<V>,
//        insert: suspend (List<V>) -> Unit
    ): T {
        try {
            val response = responseApi(id)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            val item = response.body() ?: throw UnknownError
//            insert(mapper(items))
            return item
        } catch (e: IOException) {
            println(e)
            throw NetworkError

        } catch (e: ApiError) {
            throw e

        } catch (e: Exception) {
            println(e)
            throw UnknownError
        }

    }


    suspend fun <T : ItemDto, V : ItemDto> post(
        responseApi: suspend (T) -> Response<V>, item: T
    ): V? {
        try {
            val response = responseApi(item)
            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
            return response.body()
        } catch (e: IOException) {
            throw NetworkError

        } catch (e: ApiError) {
            throw e

        } catch (e: Exception) {
            println(e)
            throw UnknownError
        }

    }

//    suspend fun <T : ItemDto, V : ItemDto> put(
//        responseApi: suspend (id: String, T) -> Response<V>, item: T
//    ): Boolean {
//        try {
//            val id = item.id ?: throw UnknownError
//            val response = responseApi(id, item)
//            if (!response.isSuccessful) throw ApiError(response.code(), response.message())
//            return true
//
//        } catch (e: IOException) {
//            throw NetworkError
//
//        } catch (e: ApiError) {
//            throw e
//
//        } catch (e: Exception) {
//            throw UnknownError
//        }
//    }
}