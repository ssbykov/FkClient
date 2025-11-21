package ru.faserkraft.client.entity

import ru.faserkraft.client.activity.EMPTY
import ru.faserkraft.client.model.ItemModel

open class ItemEntity() {
    open fun toModel(itemEntity: ItemEntity) = ItemModel(EMPTY, EMPTY)
}