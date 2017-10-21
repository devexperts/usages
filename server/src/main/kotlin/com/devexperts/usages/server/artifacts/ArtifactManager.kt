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
package com.devexperts.usages.server.artifacts

import com.devexperts.logging.Logging
import com.devexperts.usages.api.Artifact
import com.devexperts.usages.server.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


/**
 * This component manages artifacts, their dependencies,
 * packages and other artifact-related information
 */
object ArtifactManager {
    val log: Logging = Logging.getLogging(ArtifactManager::class.java)

    // === PUBLIC API ===

    /**
     * Store information about the specified artifact in database.
     */
    fun storeArtifactInfo(indexerId: String, artifact: Artifact, dependencies: List<Artifact>,
                          packages: Collection<String>): WithId<Artifact> {
        return transaction {
            val artifactId = getOrCreateArtifact(artifact).id
            addPackagesInternal(artifactId, packages)
            addDependenciesInternal(artifactId, dependencies)
            setSourceIndexerNameInternal(artifactId, indexerId)
            WithId(artifactId, artifact)
        }
    }

    /**
     * Returns ids of artifacts contained the specified package.
     */
    fun artifactsWithPackage(pkg: String): List<Int> {
        // todo approximation by groupId
        return transaction {
            // Get id associated with the specified package,
            // return empty list if this package is not found
            val pkgId = getPackageIdInternal(pkg) ?: return@transaction emptyList()
            // Get list of ids of the artifacts which contain the specified package
            ArtifactPackages.slice(ArtifactPackages.artifactId).select {
                ArtifactPackages.packageId.eq(pkgId)
            }.toList().map { it[ArtifactPackages.artifactId] }
        }
    }

    /**
     * Return ids of artifacts which have any of the specified artifact as its dependency.
     */
    fun artifactIdsWithAnyDependency(artifactIds: List<Int>): List<Int> {
        // todo FIX ME FIX FIX FIX
        return transaction {
            artifactIds.map { artifactId ->
                Dependencies.slice(Dependencies.artifactId).select {
                    Dependencies.dependencyArtifactId.eq(artifactId)
                }.toList().map { it[Dependencies.artifactId] }
            }.flatMap { it }.distinct()
        }
    }

    /**
     * Returns the [Artifact] associated with the specified id.
     */
    fun getArtifact(id: Int): WithId<Artifact> = transaction {
        getArtifactInternal(id)
    }

    /**
     * Returns the [Artifact] associated with the specified id.
     * Should be invoked inside [transaction]
     */
    fun getArtifactInternal(id: Int): WithId<Artifact> {
        val res = Artifacts.select {
            Artifacts.id.eq(id)
        }.limit(1).first()
        val artifact = Artifact(groupId = res[Artifacts.groupId],
                artifactId = res[Artifacts.artifactId],
                version = res[Artifacts.version],
                type = res[Artifacts.type],
                classifier = res[Artifacts.classifier])
        return WithId(id, artifact)
    }

    fun getSourceIndexerName(artifactId: Int): String = transaction {
        ArtifactSources.slice(ArtifactSources.indexerId).select {
            ArtifactSources.artifactId.eq(artifactId)
        }.limit(1).first()[ArtifactSources.indexerId]
    }

    fun isAnalyzed(artifactId: Int) = transaction {
        ArtifactStatus.slice(ArtifactStatus.analyzed).select {
            ArtifactStatus.artifactId.eq(artifactId)
        }.limit(1).first()[ArtifactStatus.analyzed]
    }

    fun markAnalyzed(artifactId: Int) = transaction {
        ArtifactStatus.update({ ArtifactStatus.artifactId.eq(artifactId) }) {
            it[ArtifactStatus.analyzed] = true
        }
    }

    // === END PUBLIC API ===


    /**
     * Inserts the specified artifact to the database if needed and
     * returns an id associated with the added artifact.
     * Should be invoked under [transaction].
     */
    private fun getOrCreateArtifact(artifact: Artifact): WithId<Artifact> {
        var id = getArtifactIdInternal(artifact)
        if (id == null) {
            id = Artifacts.insert {
                it[artifactId] = artifact.artifactId
                it[groupId] = artifact.groupId
                it[version] = artifact.version
                it[type] = artifact.type
                it[classifier] = artifact.classifier
            }[Artifacts.id]
            addEmptyArtifactStatusInternal(id)
        }
        return WithId(id, artifact)
    }


    /**
     * TODO
     */
    private fun addEmptyArtifactStatusInternal(artifactId: Int) {
        ArtifactStatus.insert {
            it[ArtifactStatus.artifactId] = artifactId
            it[ArtifactStatus.analyzed] = false
            it[ArtifactStatus.hasPackages] = false
        }
    }

    /**
     * TODO
     */
    private fun setSourceIndexerNameInternal(artifactId: Int, indexerId: String) {
        val exist = ArtifactSources.select { ArtifactSources.artifactId.eq(artifactId) }
                .limit(1).count() > 0
        if (!exist) {
            ArtifactSources.insert {
                it[ArtifactSources.artifactId] = artifactId
                it[ArtifactSources.indexerId] = indexerId
            }
        }
    }

    /**
     * Add information of packages in the artifact with the specified id.
     * Should be invoked under [transaction].
     */
    private fun addPackagesInternal(artifactId: Int, packages: Collection<String>) {
        val packageIds = packages.map { pkg -> addPackageIfNeededInternal(pkg) }
        val alreadyStoredPackageIds = ArtifactPackages.slice(ArtifactPackages.packageId)
                .select {
                    ArtifactPackages.artifactId.eq(artifactId)
                }.map { it[ArtifactPackages.packageId] }
        ArtifactPackages.batchInsert((HashSet(packageIds) - alreadyStoredPackageIds)) { pkgId ->
            this[ArtifactPackages.artifactId] = artifactId
            this[ArtifactPackages.packageId] = pkgId
        }
    }

    /**
     * Add information of the specified artifact dependencies.
     * Should be invoked under [transaction].
     */
    private fun addDependenciesInternal(artifactId: Int, dependencies: List<Artifact>) {
        transaction {
            // Get ids of the specified dependencies
            val dependencyIds = dependencies.map { getOrCreateArtifact(it).id }
            // Get already added dependencies
            val curDependencyIds = HashSet(getDependenciesInternal(artifactId))
            // This set contains ids of dependencies
            Dependencies.batchInsert(dependencyIds - curDependencyIds) { depId ->
                this[Dependencies.artifactId] = artifactId
                this[Dependencies.dependencyArtifactId] = depId
            }
        }
    }

    /**
     * Returns ids of specified artifact dependencies.
     * Should be invoked under [transaction].
     */
    private fun getDependenciesInternal(artifactId: Int): List<Int> {
        val q = Dependencies.slice(Dependencies.dependencyArtifactId).select {
            Dependencies.artifactId.eq(artifactId)
        }
        return q.toList().map { it[Dependencies.dependencyArtifactId] }
    }

    /**
     * Get id of the specified [Artifact].
     * Should be invoked under [transaction].
     */
    private fun getArtifactIdInternal(artifact: Artifact): Int? {
        val q = Artifacts.slice(Artifacts.id).select {
            Artifacts.artifactId.eq(artifact.artifactId) and
                    Artifacts.groupId.eq(artifact.groupId) and
                    Artifacts.version.eq(artifact.version) and
                    Artifacts.type.eq(artifact.type) and
                    Artifacts.classifier.eq(artifact.classifier)
        }.limit(1)
        return q.firstOrNull()?.get(Artifacts.id)
    }

    /**
     * Get id of the specified package.
     * Should be invoked under [transaction].
     */
    private fun getPackageIdInternal(pkg: String) = Packages.slice(Packages.id)
            .select { Packages.pkg.eq(pkg) }.limit(1)
            .firstOrNull()?.get(Packages.id)

    /**
     * Inserts the specified package to the database if needed and
     * returns an id associated with the added package.
     * Should be invoked under [transaction].
     */
    private fun addPackageIfNeededInternal(pkg: String): Int {
        val pkgId = getPackageIdInternal(pkg)
        if (pkgId != null)
            return pkgId
        return Packages.insert {
            it[Packages.pkg] = pkg
        }[Packages.id]
    }
}