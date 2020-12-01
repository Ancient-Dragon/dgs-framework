package com.netflix.graphql.dgs.internal.utils

import org.springframework.web.multipart.MultipartFile
import java.util.regex.Pattern


/**
 * This implementation has borrowed heavily from graphql-servlet-java implementation of the variable mapper.
 * It handles populating the query variables with the files specified by object paths in the multi-part request.
 * Specifically, it takes each entry here '-F map='{ "0": ["variables.input.files.0"], "1": ["variables.input.files.1"] }',
 * and uses the object path, e.g., variables.input.files.0, to navigate to the appropriate path in the query variables, i.e.,
 * "variables": { "input": { "description": "test", "files": [null, null] } } }' and sets it to the file specified as
 * -F '0=@file1.txt' -F '1=@file2.txt'
 *
 * The resulting map of populated query variables is the output.
 * Original => "variables": { "input": { "description": "test", "files": [null, null] } }
 * Transformed => "variables": { "input": { "description": "test", "files": [file1.txt, file2.txt] } }
 */
object MultipartVariableMapper {
    private val PERIOD = Pattern.compile("\\.")

    private val MAP_MAPPER = object : Mapper<MutableMap<String, Any>> {
        override operator fun set(location: MutableMap<String, Any>, target: String, value: MultipartFile): Any? {
            return location.put(target, value)
        }

        override fun recurse(location: MutableMap<String, Any>, target: String): Any {
            return location[target]?: error("")
        }
    }

    private val LIST_MAPPER = object : Mapper<MutableList<Any>> {
        override operator fun set(location: MutableList<Any>, target: String, value: MultipartFile): Any {
            return location.set(Integer.parseInt(target), value)
        }

        override fun recurse(location: MutableList<Any>, target: String): Any {
            return location[Integer.parseInt(target)]
        }
    }

    internal interface Mapper<T> {
        operator fun set(location: T, target: String, value: MultipartFile): Any?
        fun recurse(location: T, target: String): Any
    }

    @Suppress("UNCHECKED_CAST")
    fun mapVariable(objectPath: String, variables: Map<String, Any>, part: MultipartFile) {
        val segments = PERIOD.split(objectPath)

        if (segments.size < 2) {
            throw RuntimeException("object-path in map must have at least two segments")
        } else if ("variables" != segments[0]) {
            throw RuntimeException("can only map into variables")
        }

        var currentLocation: Any = variables
        for (i in 1 until segments.size) {
            val segmentName = segments[i]
            if (i == segments.size - 1) {
                if (currentLocation is Map<*, *>) {
                    if (null != MAP_MAPPER.set(currentLocation as MutableMap<String, Any>, segmentName, part)) {
                        throw RuntimeException("expected null value when mapping $objectPath")
                    }
                } else {
                    if (null != LIST_MAPPER.set(currentLocation as MutableList<Any>, segmentName, part)) {
                        throw RuntimeException("expected null value when mapping $objectPath")
                    }
                }
            } else {
                if (currentLocation is Map<*, *>) {
                    currentLocation = MAP_MAPPER.recurse(currentLocation as MutableMap<String, Any>, segmentName)
                } else {
                    currentLocation = LIST_MAPPER.recurse(currentLocation as MutableList<Any>, segmentName)
                }
                if (null == currentLocation) {
                    throw RuntimeException("found null intermediate value when trying to map $objectPath")
                }
            }
        }
    }
}