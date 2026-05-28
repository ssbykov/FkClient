package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.data.dto.FinishedProcessDto
import ru.faserkraft.client.data.dto.ProcessDto
import ru.faserkraft.client.data.dto.ProcessShortDto
import ru.faserkraft.client.data.dto.StepDefinitionDto
import ru.faserkraft.client.data.dto.StepDefinitionWithProcessDto
import ru.faserkraft.client.domain.model.FinishedProcess
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.ProcessShort
import ru.faserkraft.client.domain.model.StepDefinition
import ru.faserkraft.client.domain.model.StepDefinitionWithProcess

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
    sizeTypeId = type?.id,
    sizeTypeName = type?.name,
    packagingCount = type?.packagingCount,
)

fun ProcessShortDto.toDomain(): ProcessShort {
    return ProcessShort(
        id = id,
        name = name,
    )
}

fun ProcessShort.toDto(): ProcessShortDto {
    return ProcessShortDto(
        id = id,
        name = name,
    )
}

fun StepDefinitionWithProcessDto.toDomain(): StepDefinitionWithProcess {
    return StepDefinitionWithProcess(
        id = id,
        order = order,
        name = template.name,
        nameGenitive = template.nameGenitive,
        process = process.toDomain(),
    )
}
