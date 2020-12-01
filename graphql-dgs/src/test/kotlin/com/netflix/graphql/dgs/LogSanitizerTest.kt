package com.netflix.graphql.dgs

import com.netflix.graphql.dgs.logging.internal.LogSanitizer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
class LogSanitizerTest {
    @Test
    fun testStringSanitation() {

        val input = mapOf<String, Any>(Pair("stringa", "secret"), Pair("stringb", "secret"))

        val sanitizeMap = LogSanitizer().sanitizeMap(input)
        assertEquals(input.size, sanitizeMap.size)
        assertEquals("***", sanitizeMap["stringa"])
        assertEquals("***", sanitizeMap["stringb"])
    }

    @Test
    fun testNestedMapSanitation() {

        val input = mapOf<String, Any>(Pair("map1", mapOf(Pair("a", "secret"))), Pair("map2", mapOf(Pair("nested", "secret"))))
        val sanitizeMap = LogSanitizer().sanitizeMap(input)
        assertEquals(input.size, sanitizeMap.size)
        val firstMap = sanitizeMap["map1"] as Map<*, *>
        val secondMap = sanitizeMap["map2"] as Map<*, *>
        val nestedValue = secondMap["nested"]

        assertEquals("***", firstMap["a"])
        assertEquals("***", nestedValue)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testListSanitation() {

        val input = mapOf<String, Any>(
                Pair("listOfStrings", listOf("secret1", "secret2")),
                Pair("listOfMaps", listOf(mapOf(Pair("a", "secret")))))

        val sanitizeMap = LogSanitizer().sanitizeMap(input)
        assertEquals(input.size, sanitizeMap.size)

        val listOfStrings = sanitizeMap["listOfStrings"] as List<*>
        assertEquals("***", listOfStrings[0])

        val listOfMaps = sanitizeMap["listOfMaps"] as List<Map<String, *>>
        assertEquals("***", listOfMaps[0]["a"])
    }

    @Test
    fun testQuerySanitize() {
        val sanitizeQuery = LogSanitizer().sanitizeQuery("""{ hello(name: "Test with _special_ characters!")}""")
        assertEquals("{ hello(name: \"***\")}", sanitizeQuery)
    }
}