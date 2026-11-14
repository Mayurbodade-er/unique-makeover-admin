package com.uniquemakeover.admin.core.database

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setUp() {
        converters = Converters()
    }

    @Test
    fun fromStringList_convertsValidListToJsonString() {
        val list = listOf("svc_1", "svc_2", "svc_3")
        val jsonString = converters.fromStringList(list)

        assertThat(jsonString).isEqualTo("[\"svc_1\",\"svc_2\",\"svc_3\"]")
    }

    @Test
    fun fromStringList_nullInput_returnsEmptyJsonArray() {
        val jsonString = converters.fromStringList(null)
        assertThat(jsonString).isEqualTo("[]")
    }

    @Test
    fun fromStringList_emptyList_returnsEmptyJsonArray() {
        val jsonString = converters.fromStringList(emptyList())
        assertThat(jsonString).isEqualTo("[]")
    }

    @Test
    fun toStringList_convertsValidJsonStringToList() {
        val json = "[\"svc_facial\",\"svc_haircut\"]"
        val list = converters.toStringList(json)

        assertThat(list).containsExactly("svc_facial", "svc_haircut").inOrder()
    }

    @Test
    fun toStringList_nullOrBlankInput_returnsEmptyList() {
        assertThat(converters.toStringList(null)).isEmpty()
        assertThat(converters.toStringList("")).isEmpty()
        assertThat(converters.toStringList("   ")).isEmpty()
    }

    @Test
    fun toStringList_malformedJson_returnsEmptyListGracefully() {
        val malformed = "{invalid_json}"
        val list = converters.toStringList(malformed)

        assertThat(list).isEmpty()
    }
}
