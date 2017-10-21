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
package com.devexperts.usages.api

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.lang.Integer.parseInt
import java.util.*

const val UUID_HEADER_NAME = "UUID"

/**
 * It is used to do [MemberUsageRequest] and process responses in stream format.
 * Note that all methods can be invoked concurrently, from different threads.
 */
abstract class MemberUsageRequestProcessor(val serverUrls: List<String>,
                                           val memberUsagesRequest: MemberUsageRequest) {
    private val uuid = UUID.randomUUID().toString()
    private @Volatile
    var cancelled = false
    abstract fun onNewUsages(serverUrl: String, usages: List<MemberUsage>)
    abstract fun onError(serverUrl: String, message: String, throwable: Throwable?)
    abstract fun onComplete()

    fun doRequest() {
        serverUrls.forEach { url ->
            try {
                val webClient = WebClient.builder()
                        .baseUrl(url)
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_STREAM_JSON_VALUE)
                        .defaultHeader(UUID_HEADER_NAME, uuid)
                        .build()
                val usagesFlux = webClient.post()
                        .uri("/usages")
                        .body(BodyInserters.fromObject(memberUsagesRequest))
                        .exchange().flatMapMany { it.bodyToFlux(MemberUsage::class.java) }
                usagesFlux.toStream().forEach{ onNewUsages(url, listOf(it)) }
//                val block = CountDownLatch(1)
//                usagesFlux.doFinally { block.countDown() }
//                block.await()
//                usagesFlux.subscribe(object : Subscriber<MemberUsage> {
//                    override fun onNext(usage: MemberUsage) {
//                        onNewUsages(url, listOf(usage))
//                    }
//
//                    override fun onSubscribe(s: Subscription) {
//                        subscriptions += s
//                    }
//
//                    override fun onError(t: Throwable) {
//                        block.countDown()
//                        onError(url, "Error during request to $url", t)
//                    }
//
//                    override fun onComplete() {
//                        block.countDown()
//                    }
//                })
//                block.await()
            } catch (e: Throwable) {
                onError(url, "Error during request to $url", e)
            }
        }
        onComplete()
    }

    fun cancel() {
        cancelled = true
    }
}

enum class CompleteMessage {
    COMPLETE, USAGES_NUMBER_EXCEED
}


/**
 * Parses artifact mask which should be in one of the following formats:
 * * `<groupId>:<artifactId>:<version>`
 * * `<groupId>:<artifactId>:<version>:<numberOfLastVersions>`
 * * `<groupId>:<artifactId>:<packaging>:<classifier>:<version>:<numberOfLastVersions>`
 */
fun createArtifactMaskFromString(config: String): ArtifactMask {
    val props = config.trim().split(":")
    if (props.size == 3) {
        // <groupId>:<artifactId>:<version>
        return ArtifactMask(groupId = props[0], artifactId = props[1], version = props[2])
    } else if (props.size == 4) {
        // <groupId>:<artifactId>:<version>:<numberOfLastVersions>
        return ArtifactMask(groupId = props[0], artifactId = props[1], version = props[2],
                numberOfLastVersions = parseInt(props[3]))
    } else if (props.size == 6) {
        // <groupId>:<artifactId>:<packaging>:<classifier>:<version>:<numberOfLastVersions>
        return ArtifactMask(groupId = props[0], artifactId = props[1], packaging = props[2],
                classifier = props[3], version = props[4], numberOfLastVersions = parseInt(props[5]))
    } else {
        throw IllegalArgumentException("Invalid configuration: " + config)
    }
}

const val ANY = "*"

data class ArtifactMask(
        @JsonProperty("groupId") val groupId: String = ANY,
        @JsonProperty("artifactId") val artifactId: String = ANY,
        @JsonProperty("packaging") val packaging: String = ANY,
        @JsonProperty("classifier") val classifier: String = ANY,
        @JsonProperty("version") val version: String = ANY,
        @JsonProperty("numberOfLastVersions") val numberOfLastVersions: Int = 1
)

data class MemberUsageRequest(
        @JsonProperty("member") val member: Member, // find usages of this member
        @JsonProperty("searchScope") val searchScope: ArtifactMask, // search in these artifacts only
        @JsonProperty("findClasses") val findClasses: Boolean, // find usages of classes from specified package (as member)
        @JsonProperty("findMethods") val findMethods: Boolean, // find method usages during finding usages of the class
        @JsonProperty("findFields") val findFields: Boolean,
//        @JsonProperty("findBaseClassesUsages") val findBaseClassesUsages: Boolean,
        @JsonProperty("findDerivedClassesUsages") val findDerivedClassesUsages: Boolean,
//        @JsonProperty("findBaseMethodsUsages") val findBaseMethodsUsages: Boolean,
        @JsonProperty("findDerivedMethodsUsages") val findDerivedMethodsUsages: Boolean
)


val usages = arrayListOf(
        MemberUsage(
                member = Member("com.devexperts.util.TimePeriod", emptyList(), MemberType.CLASS),
                usageKind = UsageKind.FIELD, // property type
                location = Location(
                        artifact = Artifact("com.devexperts.usages", "server", "3.0", null, null),
                        member = Member("com.devexperts.usages.config.RepositorySettings", emptyList(), MemberType.CLASS),
                        file = "com/devexperts/usages/config/Settings.kt", lineNumber = 54)),
        MemberUsage(
                member = Member("com.devexperts.util.TimePeriod", emptyList(), MemberType.CLASS),
                usageKind = UsageKind.METHOD_RETURN, // method return type
                location = Location(
                        artifact = Artifact("com.devexperts.usages", "server", "3.0", null, null),
                        member = Member("com.devexperts.usages.config.TimePeriodConverter#myMethod", emptyList(), MemberType.METHOD),
                        file = "com/devexperts/usages/config/Settings.kt", lineNumber = 76)),
        MemberUsage(
                member = Member("com.devexperts.util.TimePeriod", emptyList(), MemberType.CLASS),
                usageKind = UsageKind.METHOD_RETURN, // nested class/object
                location = Location(
                        artifact = Artifact("com.devexperts.usages", "server", "3.0", null, null),
                        member = Member("com.devexperts.usages.config.TimePeriodConverter", emptyList(), MemberType.CLASS),
                        file = "com/devexperts/usages/config/Settings.kt", lineNumber = 77)),
        MemberUsage(
                member = Member("com.devexperts.util.TimePeriod", emptyList(), MemberType.CLASS),
                usageKind = UsageKind.METHOD_RETURN, // nested class/object
                location = Location(
                        artifact = Artifact("com.devexperts.usages", "server", "3.0", null, null),
                        member = Member("com.devexperts.usages.config.RepositorySettings", emptyList(), MemberType.CLASS),
                        file = "com/devexperts/usages/config/Settings.kt", lineNumber = 54)),
        MemberUsage(
                member = Member("com.devexperts.util.IndexedSet", emptyList(), MemberType.CLASS),
                usageKind = UsageKind.NEW,
                location = Location(
                        artifact = Artifact("com.devexperts.qd", "qd-core", "3.257", null, null),
                        member = Member("com.devexperts.qd.core.MyIndexedSet", emptyList(), MemberType.CLASS),
                        file = "com/devexperts/qd/core/MyIndexedSet", lineNumber = 47)),
        MemberUsage(
                member = Member("com.devexperts.util.IndexedSet", emptyList(), MemberType.CLASS),
                usageKind = UsageKind.FIELD,
                location = Location(
                        artifact = Artifact("com.devexperts.qd", "qd-core", "3.256", null, null),
                        member = Member("com.devexperts.qd.core.MyIndexedSet", emptyList(), MemberType.CLASS),
                        file = "com/devexperts/qd/core/MyIndexedSet", lineNumber = 35))
)