package ru.yarsu.http.handlers

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JsonMapper {
    private val mapper =
        ObjectMapper()
            .registerKotlinModule()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)

    fun toJson(obj: Any): String {
        val prettyPrinter = DefaultPrettyPrinter()
        val indenter = DefaultIndenter("  ", "\n")
        prettyPrinter.indentObjectsWith(indenter)
        prettyPrinter.indentArraysWith(indenter)
        return mapper.writer(prettyPrinter).writeValueAsString(obj)
    }

    fun readTree(json: String): JsonNode = mapper.readTree(json)

    fun nullNode(): JsonNode = mapper.nullNode()
}
