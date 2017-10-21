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
import com.devexperts.usages.server.artifacts.ArtifactManager
import com.devexperts.usages.server.config.RepositorySetting
import com.devexperts.usages.server.config.RepositoryType
import com.devexperts.usages.server.config.Settings
import com.devexperts.util.TimePeriod
import java.io.File

abstract class MavenIndexer(
        repositorySetting: RepositorySetting, // delay between repository indexing runs.
        val supportedArtifactTypes: List<String>,
        val id: String = repositorySetting.id,
        val url: String = repositorySetting.url,
        val user: String? = repositorySetting.user,
        val password: String? = repositorySetting.password,
        val scanDelay: TimePeriod = repositorySetting.scanTimePeriod
) {

    protected abstract val log: Logging

    /**
     * Scans maven repository structure,
     * stores information about artifacts and their transitive dependencies.
     */
    abstract fun scan()

    /**
     * Downloads the specified artifact from the repository.
     * Returns null if the artifact has not been downloaded.
     */
    abstract fun downloadArtifact(artifact: Artifact): File?

    /**
     * Store information about the artifact into the [ArtifactManager]
     */
    protected fun storeArtifactInfo(artifact: Artifact, dependencies: List<Artifact>, packages: Collection<String>) {
        ArtifactManager.storeArtifactInfo(id, artifact, dependencies, packages)
        log.trace("Store information for $artifact, dependencies=$dependencies, packages=$packages")
    }
}

/**
 * Creates [MavenIndexer] according to this settings
 */
fun RepositorySetting.createIndexer(supportedArtifactTypes: List<String>): MavenIndexer {
    when (type) {
        RepositoryType.NEXUS -> return NexusMavenIndexer(
                repositorySetting = this,
                supportedArtifacts = supportedArtifactTypes
        )
        else -> throw IllegalStateException("Unsupported repository type: $type")
    }
}

/**
 * Creates a list of [MavenIndexer]s according to this settings
 */
fun Settings.createIndexers() = repositorySettings.map {
    it.createIndexer(artifactTypes)
}