package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.dto.EmployeeDto

fun EmployeeDto.toDomain(): Employee = Employee(
    id = id,
    name = name,
    email = user.email,
)