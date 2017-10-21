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

import org.aeonbits.owner.Config
import org.aeonbits.owner.ConfigFactory
import java.io.File

@Config.Sources("classpath:usages.properties")
interface PropertiesConfiguration : Config {
    @Config.Key("usages.workDir")
    @Config.DefaultValue("~/.usages")
    fun workDir(): String
}

private val configuration = ConfigFactory.create(PropertiesConfiguration::class.java, System.getProperties())

object Configuration {
    val workDir = resolvePath(configuration.workDir())
    val settingsFile = workDirFile("settings.xml")
    val dbFile = workDirFile("usages_db")

    private fun resolvePath(file: String): String {
        var f = file
        if (f.startsWith('~'))
            f = System.getProperty("user.home") + f.substring(1)
        return f
    }
}

private fun workDirFile(file: String) = Configuration.workDir + File.separator + file





