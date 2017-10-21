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

import com.devexperts.logging.Logging
import com.devexperts.usages.analyzer.Analyzer0
import com.devexperts.usages.api.*
import com.devexperts.usages.server.artifacts.ArtifactManager
import com.devexperts.usages.server.config.Configuration
import com.devexperts.usages.server.config.readSettings
import com.devexperts.usages.server.indexer.MavenIndexer
import com.devexperts.usages.server.indexer.createIndexers
import org.eclipse.aether.util.version.GenericVersionScheme
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.util.concurrent.Executors
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.concurrent.fixedRateTimer
import kotlin.streams.toList

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    initDatabase(Configuration.dbFile)
//    Server.scheduleScan()
    SpringApplication.run(Application::class.java, *args)
}

private val log = Logging.getLogging(Server::class.java)

object Server {
    val indexerPool = Executors.newFixedThreadPool(1)

    val settings = readSettings()
    val indexers = settings.createIndexers()
    val indexerIdToIndexerMap: Map<String, MavenIndexer>

    init {
        indexerIdToIndexerMap = indexers.stream().collect(Collectors.toMap(MavenIndexer::id, { i -> i }))
    }

    fun scheduleScan() {
        indexers.forEach { indexer ->
            fixedRateTimer(name = "MavenIndexer-${indexer.id}", period = indexer.scanDelay.time) {
                indexerPool.submit { indexer.scan() }
            }
        }
    }
}

@RestController
class RestController {



//    fun cancel(@RequestHeader(UUID_HEADER_NAME) uuid: String) {
//        TODO("Cancellation is not implemented")
//    }

    @PostMapping(produces = arrayOf(MediaType.APPLICATION_STREAM_JSON_VALUE), value = "/usages")
    fun findUsages(@RequestHeader(UUID_HEADER_NAME) uuid: String, @RequestBody request: MemberUsageRequest): Flux<MemberUsage> {
        println("[$uuid] REQUEST=$request")
        val pkg = request.member.packageName()
        val artifactsWithPackage = ArtifactManager.artifactsWithPackage(pkg)
        var artifactsToAnalyze = (artifactsWithPackage + ArtifactManager.artifactIdsWithAnyDependency(artifactsWithPackage))
                .map { artifactId -> ArtifactManager.getArtifact(artifactId) }
        artifactsToAnalyze = filterArtifacts(artifactsToAnalyze, request.searchScope)
//        println("[$uuid] ARTIFACTS TO ANALYZE=" + artifactsToAnalyze.map { it.value })
        // Analyze artifacts if needed
        var isCancelled = false
        val stream = artifactsToAnalyze.stream().parallel().map { artifact ->
            if (isCancelled)
                return@map Stream.empty<MemberUsage>()
            val indexer = Server.indexerIdToIndexerMap[ArtifactManager.getSourceIndexerName(artifact.id)]
            if (indexer == null) {
                log.warn("Artifact ${artifact.value} has not been indexed yet and cannot be analyzed")
                return@map Stream.empty<MemberUsage>()
            }
            val usages = Analyzer0().analyze(indexer, artifact.value)
//            println("[$uuid] ARTIFACT ${artifact.value} ANALYZED, USAGES=$usages")
            usages.stream()
//            Analyzer.analyzeIfNeeded(indexer, artifact)
        }.flatMap { it }.filter { usage ->
            if (usage.member == request.member)
                return@filter true
            if (request.member.type == MemberType.PACKAGE) {
                if (request.findClasses && usage.member.type == MemberType.CLASS && usage.member.packageName() == request.member.packageName())
                    return@filter true
                if (request.findClasses && request.findFields && usage.member.type == MemberType.FIELD && usage.member.packageName() == request.member.packageName())
                    return@filter true
                if (request.findClasses && request.findMethods && usage.member.type == MemberType.METHOD && usage.member.packageName() == request.member.packageName())
                    return@filter true
            }
            if (request.member.type == MemberType.CLASS) {
                if (request.findFields && usage.member.type == MemberType.FIELD && usage.member.className() == request.member.className())
                    return@filter true
                if (request.findMethods && usage.member.type == MemberType.METHOD && usage.member.className() == request.member.className())
                    return@filter true
            }
            return@filter false
        }
        return Flux.fromStream(stream).doOnCancel {
            isCancelled = true
        }
        // todo internal members (fields, methods) and derived members (derived classes, overridden methods)
    }
}

private fun filterArtifacts(artifacts: List<WithId<Artifact>>, searchScope: ArtifactMask): List<WithId<Artifact>> {
    // Filter artifacts by coordinates (ignore last versions number restriction here)
    val groupIdRegex = globPattern(searchScope.groupId)
    val artifactIdRegex = globPattern(searchScope.artifactId)
    val classifierRegex = globPattern(searchScope.classifier)
    val packagingRegex = globPattern(searchScope.packaging)
    val versionRegex = globPattern(searchScope.version)
    val goodArtifacts = artifacts.stream().filter {
        val a = it.value
        groupIdRegex.matches(a.groupId)
                && artifactIdRegex.matches(a.artifactId)
                && classifierRegex.matches(a.classifier.toString()) // toString here because it can be null
                && packagingRegex.matches(a.type.toString()) // toString here because it can be null
                && versionRegex.matches(a.version)
    }
    // Return filtered artifacts if they should not be restricted by last versions number
    if (searchScope.numberOfLastVersions < 0)
        return goodArtifacts.toList()
    // Split artifacts to lists with same coordinates except for version.
    // Therefore, every list contains all versions of artifact.
    data class ArtifactKey(val groupId: String, val artifactId: String, val classifier: String?, val type: String?)

    val artifactsByKey = goodArtifacts.collect(
            Collectors.groupingBy<WithId<Artifact>, ArtifactKey> {
                val a = it.value
                ArtifactKey(groupId = a.groupId, artifactId = a.artifactId,
                        classifier = a.classifier, type = a.type)
            })
    // Restrict by last versions number
    val versionScheme = GenericVersionScheme()
    return artifactsByKey.values.stream().flatMap {
        it.stream().sorted({ a1, a2 ->
            // todo do not create versions on each comparision!
            val v1 = versionScheme.parseVersion(a1.value.version)
            val v2 = versionScheme.parseVersion(a2.value.version)
            v2.compareTo(v1)
        }).limit(searchScope.numberOfLastVersions.toLong())
    }.toList()
}

// todo verify this transformation
private fun globPattern(glob: String): Regex {
    val regex = StringBuilder()
    for (i in 0 until glob.length) {
        val c = glob[i]
        when (c) {
            '*' -> regex.append(".*")
            ',' -> regex.append('|')
            else -> regex.append(Pattern.quote(glob.substring(i, i + 1)))
        }
    }
    return Pattern.compile(regex.toString()).toRegex()
}

data class WithId<out T>(val id: Int, val value: T)