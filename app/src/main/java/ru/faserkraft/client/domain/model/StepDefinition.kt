package ru.faserkraft.client.domain.model

data class StepDefinition(
    val id: Int,
    val order: Int,
    val name: String,      // из TemplateDto.name
    val nameGenitive: String, // из TemplateDto.nameGenitive
)