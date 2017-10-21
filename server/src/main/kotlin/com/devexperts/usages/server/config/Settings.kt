/**
 * Copyright (C) 2017 Devexperts LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package com.devexperts.usages.server.config

import com.devexperts.util.TimePeriod
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.convert.Convert
import org.simpleframework.xml.convert.Converter
import org.simpleframework.xml.core.Persister
import org.simpleframework.xml.stream.InputNode
import org.simpleframework.xml.stream.OutputNode
import java.io.File

private const val ID_TAG = "id"
private const val URL_TAG = "url"
private const val TYPE_TAG = "type"
private const val USER_TAG = "user"
private const val PASSWORD_TAG = "password"
private const val SCAN_TIME_PERIOD_TAG = "scanTimePeriod"

@Root(name = "settings")
class Settings {
    @field:Path("repositories")
    @field:ElementList(inline = true, type = RepositorySetting::class, empty = false)
    lateinit var repositorySettings: List<RepositorySetting>

    @field:Path("artifactTypes")
    @field:ElementList(inline = true, entry = "type", empty = false)
    lateinit var artifactTypes: List<String>
}

@Root(name = "repository")
class RepositorySetting {
    @field:Element(name = ID_TAG)
    lateinit var id: String

    @field:Element(name = URL_TAG)
    lateinit var url: String

    @field:Element(name = TYPE_TAG)
    @field:Convert(RepositoryTypeConverter::class)
    lateinit var type: RepositoryType

    @field:Element(name = USER_TAG, required = false)
    var user: String? = null

    @field:Element(name = PASSWORD_TAG, required = false)
    var password: String? = null

    @field:Element(name = SCAN_TIME_PERIOD_TAG)
    @field:Convert(TimePeriodConverter::class)
    var scanTimePeriod: TimePeriod = TimePeriod.valueOf("15m")
}

enum class RepositoryType(val typeName: String) {
    NEXUS("nexus"),
    ARTIFACTORY("artifactory");

    override fun toString(): String = typeName
}

private class RepositoryTypeConverter : Converter<RepositoryType> {
    override fun read(node: InputNode): RepositoryType {
        return RepositoryType.values().first { it.typeName == node.value }
    }

    override fun write(node: OutputNode, value: RepositoryType) {
        node.name = TYPE_TAG
        node.value = value.typeName
    }
}

private class TimePeriodConverter : Converter<TimePeriod> {
    override fun read(node: InputNode): TimePeriod {
        return TimePeriod.valueOf(node.value)
    }

    override fun write(node: OutputNode, value: TimePeriod) {
        node.name = SCAN_TIME_PERIOD_TAG
        node.value = value.toString()
    }
}

fun readSettings(): Settings {
    val serializer = Persister(AnnotationStrategy())
    return serializer.read(Settings::class.java, File(Configuration.settingsFile))
}