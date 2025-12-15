package ru.yarsu.http.handlers

import com.fasterxml.jackson.annotation.JsonPropertyOrder

data class EquipmentListItem(
    val Id: String,
    val Equipment: String,
    val IsUsed: Boolean,
)

data class EquipmentUnusedItem(
    val Id: String,
    val Equipment: String,
)

data class EquipmentByTimeItem(
    val Id: String,
    val Equipment: String,
    val GuaranteeDate: String,
)

@JsonPropertyOrder(
    *[
        "id",
        "equipment",
        "category",
        "guaranteeDate",
        "isUsed",
        "price",
        "location",
        "responsiblePerson",
        "responsiblePersonName",
        "user",
        "userName",
        "log",
    ],
)
data class EquipmentDetail(
    val Id: String,
    val Equipment: String,
    val Category: String,
    val GuaranteeDate: String,
    val IsUsed: Boolean,
    val Price: Double,
    val Location: String,
    val ResponsiblePerson: String,
    val ResponsiblePersonName: String?,
    val User: String?,
    val UserName: String?,
    val Log: List<LogRef>,
)

data class LogRef(
    val Id: String,
    val ResponsiblePerson: String,
    val Operation: String,
    val Text: String,
    val LogDateTime: String,
)

data class EquipmentUnused(
    val Id: String,
    val Equipment: String,
)

data class EquipmentByTime(
    val Id: String,
    val Equipment: String,
    val GuaranteeDate: String,
)

data class CategoryStat(
    val Category: String,
    val Count: Int,
    val CountUsers: Int,
    val Price: Double,
)

data class PersonStat(
    val Person: String,
    val Count: Int,
    val CountUsers: Int,
    val Price: Double,
)

data class Statistics(
    val StatisticsByCategory: List<CategoryStat>? = null,
    val StatisticsByPerson: List<PersonStat>? = null,
)

data class LogDetail(
    val Id: String,
    val Equipment: String,
    val ResponsiblePerson: String,
    val Operation: String,
    val Text: String,
    val LogDateTime: String,
)

data class LocationWithEquipment(
    val Location: String,
    val Equipment: List<EquipmentListItem>,
)

data class EquipmentResponse(
    val EquipmentId: String,
    val LogId: String? = null,
)
