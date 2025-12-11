package ru.yarsu

import java.math.BigDecimal
import java.util.UUID

data class Equipment(
    val Id: UUID,
    val Equipment: String,
    val Category: String,
    val GuaranteeDate: String,
    val IsUsed: Boolean,
    val Price: BigDecimal,
    val Location: String,
    val ResponsiblePerson: String,
    val User: String?,
)
