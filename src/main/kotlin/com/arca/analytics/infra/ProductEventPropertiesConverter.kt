package com.arca.analytics.infra

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

@Converter
class ProductEventPropertiesConverter : AttributeConverter<Map<String, String>, String> {

    override fun convertToDatabaseColumn(attribute: Map<String, String>): String {
        return JSON_MAPPER.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String): Map<String, String> {
        return JSON_MAPPER.readValue(dbData, PROPERTIES_TYPE)
    }

    companion object {
        private val JSON_MAPPER = JsonMapper.builder().build()
        private val PROPERTIES_TYPE = object : TypeReference<Map<String, String>>() {}
    }
}
