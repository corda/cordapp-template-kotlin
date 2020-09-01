package com.template.schemas

import net.corda.core.crypto.SecureHash
import javax.persistence.AttributeConverter

class SecureHashConverter : AttributeConverter<SecureHash?, String> {
    override fun convertToDatabaseColumn(attribute: SecureHash?): String {
        return attribute.toString()
    }

    override fun convertToEntityAttribute(dbData: String): SecureHash? {
        return SecureHash.parse(dbData)
    }
}