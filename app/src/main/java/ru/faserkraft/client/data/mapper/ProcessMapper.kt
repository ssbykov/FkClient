package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.FinishedProcess
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.StepDefinition
import ru.faserkraft.client.dto.FinishedProcessDto
import ru.faserkraft.client.dto.ProcessDto
import ru.faserkraft.client.dto.StepDefinitionDto

fun StepDefinitionDto.toDomain(): StepDefinition = StepDefinition(
    id = id,
    order = order,
    name = template.name,
    nameGenitive = template.nameGenitive,
)

fun ProcessDto.toDomain(): Process = Process(
    id = id,
    name = name,
    description = description ?: "",
    steps = steps?.map { it.toDomain() } ?: emptyList(),
)

fun FinishedProcessDto.toDomain(): FinishedProcess = FinishedProcess(
    id = id,
    name = name,
    sizeTypeName = type?.name,
    packagingCount = type?.packagingCount,
)