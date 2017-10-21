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
package com.devexperts.usages.server

import com.devexperts.usages.api.MemberType
import com.devexperts.usages.api.UsageKind
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

private val TABLES = arrayOf(Artifacts, ArtifactStatus, Packages, ArtifactPackages, Dependencies, ArtifactSources,
        Members, Derived, Locations, MemberUsages, MemberStructure);

fun initDatabase(file: String) {
    initDatabaseByFullUrl("jdbc:h2:file:$file")
}

fun initInMemoryDatabase() {
    initDatabaseByFullUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
}

fun dropDatabase() = transaction { drop(*TABLES) }

private fun initDatabaseByFullUrl(url: String) {
    Database.connect(url = url, driver = "org.h2.Driver")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
    transaction { create(*TABLES) }
}

// == ARTIFACT MANAGER ==

object Artifacts : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val groupId = varchar("groupId", 255)
    val artifactId = varchar("artifactId", 255)
    val version = varchar("version", 255)
    val type = varchar("type", 255).nullable()
    val classifier = varchar("classifier", 255).nullable()

    init {
         uniqueIndex(groupId, artifactId, version, type, classifier)
    }
}

object Packages : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val pkg = varchar("package", 255).index()
}

object ArtifactPackages : Table() {
    val artifactId = (integer("artifactId") references Artifacts.id).primaryKey().index()
    val packageId = (integer("packageId") references Packages.id).primaryKey().index()
}

object ArtifactSources : Table() {
    val artifactId = (integer("artifactId") references Artifacts.id).primaryKey()
    val indexerId = varchar("indexerId", 255)
}

object Dependencies : Table() {
    val artifactId = (integer("artifactId") references Artifacts.id).primaryKey().index()
    val dependencyArtifactId = (integer("dependencyArtifactId") references Artifacts.id).primaryKey().index()
}

// == ANALYZER ==

object Members : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val qualifiedName = varchar("qualifiedName", 255)
    val paramTypes = varchar("paramTypes", 512)
    val type = enumeration("type", MemberType::class.java)

    init {
        // todo uniqueIndex(qualifiedName, paramTypes, type)
    }
}

object Locations : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val artifactId = (integer("artifactId") references Artifacts.id).index()
    val memberId = (integer("memberId") references Members.id).index()
    val file = varchar("file", 255).nullable()
    val line = integer("line").nullable()

    init {
        // todo uniqueIndex(artifactId, memberId, file, line)
    }
}

object MemberUsages : Table() {
    val memberId = (integer("memberId") references Members.id).primaryKey().index()
    val usageKind = enumeration("usageKind", UsageKind::class.java).primaryKey()
    val locationId = (integer("locationId") references Locations.id).primaryKey()
}

// == CODE STRUCTURE ==

object Derived : Table() {
    val memberId = (integer("memberId") references Members.id).primaryKey().index() // class or method
    val derivedMemberId = (integer("derivedMemberId") references Members.id).primaryKey() // derived class or method
}

object MemberStructure : Table() {
    val memberId = (integer("memberId") references Members.id).primaryKey().index() // package or class
    val internalMemberId = (integer("internalMemberId") references Members.id).primaryKey() // class, or method, or field in the member
}

// == COMMON ==

object ArtifactStatus : Table() {
    val artifactId = (integer("artifactId") references Artifacts.id).primaryKey()
    val analyzed = bool("analyzed") // true if this artifact has been analyzed
    val hasPackages = bool("hasPackages") // true if packages information has been filled for this artifact todo supporting
}