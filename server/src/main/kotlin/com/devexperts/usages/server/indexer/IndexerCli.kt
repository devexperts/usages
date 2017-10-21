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
package com.devexperts.usages.server.indexer

import com.devexperts.logging.Logging
import com.devexperts.usages.api.Artifact
import com.devexperts.usages.server.initDatabase
import com.devexperts.usages.server.config.Configuration
import com.devexperts.usages.server.config.readSettings
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

val cliLog: Logging = Logging.getLogging("IndexerCli")

fun main(args: Array<String>) {
    // Create indexers
    val settings = readSettings()
    val indexers = settings.createIndexers()
    // Init database
    initDatabase(Configuration.dbFile)
    // Parse operation to execute
    when (args[0]) {
        "scan" -> scan(indexers)
        "download" -> download(indexers, args[1])
    }
}

fun scan(indexers: List<MavenIndexer>) = runBlocking {
    val jobs = arrayListOf<Job>()
    indexers.forEach { indexer ->
        try {
            jobs += launch(CommonPool) {
                indexer.scan()
            }
        } catch (e: Throwable) {
            cliLog.error("Error during ${indexer.id} repository indexing", e)
        }
    }
    jobs.forEach { it.join() }
}

fun download(indexers: List<MavenIndexer>, artifactDesc: String) {
    val props = artifactDesc.split(":")
    val artifact = when {
        props.size == 3 -> // groupId:artifactId:version
            Artifact(groupId = props[0], artifactId = props[1], version = props[2],
                    type = null, classifier = null)
        props.size == 5 -> // groupId:artifactId:type:classifier:version
            Artifact(groupId = props[0], artifactId = props[1],
                    type = props[2], classifier = props[3], version = props[4])
        else -> throw IllegalArgumentException("Invalid artifact description: " + artifactDesc)
    }
}