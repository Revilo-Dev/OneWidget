package com.example.blurwidgetdemo.studio.json

sealed class JsonValue
data class JsonObjectValue(val values: LinkedHashMap<String, JsonValue>) : JsonValue()
data class JsonArrayValue(val values: List<JsonValue>) : JsonValue()
data class JsonStringValue(val value: String) : JsonValue()
data class JsonNumberValue(val value: String) : JsonValue()
data class JsonBooleanValue(val value: Boolean) : JsonValue()
data object JsonNullValue : JsonValue()

class JsonParseException(message: String) : Exception(message)
class JsonValueException(message: String) : Exception(message)

/** Small deterministic JSON implementation for Studio's versioned app-private files. */
object SimpleJson {
    fun parse(source: String): JsonValue = Parser(source).parse()

    fun stringify(value: JsonValue): String = buildString { writeValue(value, 0) }

    private fun StringBuilder.writeValue(value: JsonValue, indent: Int) {
        when (value) {
            is JsonObjectValue -> {
                append('{')
                if (value.values.isNotEmpty()) {
                    value.values.entries.forEachIndexed { index, (name, child) ->
                        if (index == 0) append('\n') else append(",\n")
                        append("  ".repeat(indent + 1))
                        writeString(name)
                        append(": ")
                        writeValue(child, indent + 1)
                    }
                    append('\n').append("  ".repeat(indent))
                }
                append('}')
            }
            is JsonArrayValue -> {
                append('[')
                if (value.values.isNotEmpty()) {
                    value.values.forEachIndexed { index, child ->
                        if (index == 0) append('\n') else append(",\n")
                        append("  ".repeat(indent + 1))
                        writeValue(child, indent + 1)
                    }
                    append('\n').append("  ".repeat(indent))
                }
                append(']')
            }
            is JsonStringValue -> writeString(value.value)
            is JsonNumberValue -> append(value.value)
            is JsonBooleanValue -> append(value.value)
            JsonNullValue -> append("null")
        }
    }

    private fun StringBuilder.writeString(value: String) {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
            }
        }
        append('"')
    }

    private class Parser(private val source: String) {
        private var index = 0

        fun parse(): JsonValue {
            skipWhitespace()
            val result = readValue()
            skipWhitespace()
            if (index != source.length) fail("Unexpected trailing content")
            return result
        }

        private fun readValue(): JsonValue {
            if (index >= source.length) fail("Unexpected end of JSON")
            return when (source[index]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> JsonStringValue(readString())
                't' -> { expect("true"); JsonBooleanValue(true) }
                'f' -> { expect("false"); JsonBooleanValue(false) }
                'n' -> { expect("null"); JsonNullValue }
                '-', in '0'..'9' -> JsonNumberValue(readNumber())
                else -> fail("Unexpected character '${source[index]}'")
            }
        }

        private fun readObject(): JsonObjectValue {
            index++
            skipWhitespace()
            val values = linkedMapOf<String, JsonValue>()
            if (consume('}')) return JsonObjectValue(values)
            while (true) {
                if (index >= source.length || source[index] != '"') fail("Expected object key")
                val key = readString()
                if (values.containsKey(key)) fail("Duplicate object key: $key")
                skipWhitespace()
                requireCharacter(':')
                skipWhitespace()
                values[key] = readValue()
                skipWhitespace()
                if (consume('}')) break
                requireCharacter(',')
                skipWhitespace()
            }
            return JsonObjectValue(values)
        }

        private fun readArray(): JsonArrayValue {
            index++
            skipWhitespace()
            val values = mutableListOf<JsonValue>()
            if (consume(']')) return JsonArrayValue(values)
            while (true) {
                values += readValue()
                skipWhitespace()
                if (consume(']')) break
                requireCharacter(',')
                skipWhitespace()
            }
            return JsonArrayValue(values)
        }

        private fun readString(): String {
            requireCharacter('"')
            val result = StringBuilder()
            while (index < source.length) {
                val character = source[index++]
                when {
                    character == '"' -> return result.toString()
                    character == '\\' -> {
                        if (index >= source.length) fail("Unterminated escape sequence")
                        when (val escaped = source[index++]) {
                            '"', '\\', '/' -> result.append(escaped)
                            'b' -> result.append('\b')
                            'f' -> result.append('\u000C')
                            'n' -> result.append('\n')
                            'r' -> result.append('\r')
                            't' -> result.append('\t')
                            'u' -> result.append(readUnicodeEscape())
                            else -> fail("Invalid escape sequence: \\$escaped")
                        }
                    }
                    character.code < 0x20 -> fail("Unescaped control character in string")
                    else -> result.append(character)
                }
            }
            fail("Unterminated string")
        }

        private fun readUnicodeEscape(): Char {
            if (index + 4 > source.length) fail("Incomplete unicode escape")
            val digits = source.substring(index, index + 4)
            index += 4
            return digits.toIntOrNull(16)?.toChar() ?: fail("Invalid unicode escape: $digits")
        }

        private fun readNumber(): String {
            val start = index
            consume('-')
            if (consume('0')) {
                if (index < source.length && source[index].isDigit()) fail("Leading zero in number")
            } else {
                readDigits(required = true)
            }
            if (consume('.')) readDigits(required = true)
            if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
                index++
                if (index < source.length && (source[index] == '+' || source[index] == '-')) index++
                readDigits(required = true)
            }
            return source.substring(start, index)
        }

        private fun readDigits(required: Boolean) {
            val start = index
            while (index < source.length && source[index].isDigit()) index++
            if (required && index == start) fail("Expected digit")
        }

        private fun expect(value: String) {
            if (!source.startsWith(value, index)) fail("Expected $value")
            index += value.length
        }

        private fun requireCharacter(character: Char) {
            if (!consume(character)) fail("Expected '$character'")
        }

        private fun consume(character: Char): Boolean {
            if (index < source.length && source[index] == character) {
                index++
                return true
            }
            return false
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index] in charArrayOf(' ', '\t', '\r', '\n')) index++
        }

        private fun fail(message: String): Nothing =
            throw JsonParseException("$message at character $index")
    }
}

fun jsonObject(vararg fields: Pair<String, JsonValue>): JsonObjectValue =
    JsonObjectValue(linkedMapOf(*fields))

fun jsonArray(values: Iterable<JsonValue>): JsonArrayValue = JsonArrayValue(values.toList())
fun jsonString(value: String): JsonStringValue = JsonStringValue(value)
fun jsonNumber(value: Number): JsonNumberValue = JsonNumberValue(value.toString())
fun jsonBoolean(value: Boolean): JsonBooleanValue = JsonBooleanValue(value)

fun JsonObjectValue.required(name: String): JsonValue =
    values[name] ?: throw JsonValueException("Missing JSON field: $name")

fun JsonObjectValue.optional(name: String): JsonValue? = values[name]?.takeUnless { it == JsonNullValue }

fun JsonValue.asObject(field: String): JsonObjectValue =
    this as? JsonObjectValue ?: throw JsonValueException("$field must be an object")

fun JsonValue.asArray(field: String): JsonArrayValue =
    this as? JsonArrayValue ?: throw JsonValueException("$field must be an array")

fun JsonValue.asString(field: String): String =
    (this as? JsonStringValue)?.value ?: throw JsonValueException("$field must be a string")

fun JsonValue.asBoolean(field: String): Boolean =
    (this as? JsonBooleanValue)?.value ?: throw JsonValueException("$field must be a boolean")

fun JsonValue.asInt(field: String): Int =
    (this as? JsonNumberValue)?.value?.toIntOrNull() ?: throw JsonValueException("$field must be an integer")

fun JsonValue.asLong(field: String): Long =
    (this as? JsonNumberValue)?.value?.toLongOrNull() ?: throw JsonValueException("$field must be a long")

fun JsonValue.asFloat(field: String): Float =
    (this as? JsonNumberValue)?.value?.toFloatOrNull() ?: throw JsonValueException("$field must be a number")
