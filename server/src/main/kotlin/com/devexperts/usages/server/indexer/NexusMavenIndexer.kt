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
import com.devexperts.usages.server.analyzer.toClassName
import com.devexperts.usages.server.config.Configuration
import com.devexperts.usages.server.config.RepositorySetting
import org.apache.maven.index.ArtifactInfo
import org.apache.maven.index.Indexer
import org.apache.maven.index.context.IndexCreator
import org.apache.maven.index.context.IndexUtils
import org.apache.maven.index.updater.IndexUpdateRequest
import org.apache.maven.index.updater.IndexUpdater
import org.apache.maven.index.updater.WagonHelper
import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.apache.maven.wagon.authentication.AuthenticationInfo
import org.apache.maven.wagon.events.TransferEvent
import org.apache.maven.wagon.observers.AbstractTransferListener
import org.apache.maven.wagon.providers.http.LightweightHttpWagon
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator
import org.codehaus.plexus.DefaultContainerConfiguration
import org.codehaus.plexus.DefaultPlexusContainer
import org.codehaus.plexus.PlexusConstants
import org.codehaus.plexus.PlexusContainer
import org.eclipse.aether.AbstractRepositoryListener
import org.eclipse.aether.RepositoryEvent
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.impl.DefaultServiceLocator
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactDescriptorRequest
import org.eclipse.aether.resolution.ArtifactRequest
import org.eclipse.aether.resolution.ArtifactResolutionException
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transport.file.FileTransporterFactory
import org.eclipse.aether.transport.http.HttpTransporterFactory
import org.eclipse.aether.util.repository.AuthenticationBuilder
import java.io.File
import java.util.stream.Collectors

class NexusMavenIndexer(repositorySetting: RepositorySetting,
                        supportedArtifacts: List<String>)
    : MavenIndexer(repositorySetting, supportedArtifacts) {
    override val log: Logging = Logging.getLogging(NexusMavenIndexer::class.java)

    private val cacheDir = File(Configuration.workDir + File.separator + "cache" + File.separator + id)
    private val centralIndexDir = File(Configuration.workDir + File.separator + "central-index" + File.separator + id)
    private val localRepositoryDir = File(Configuration.workDir + File.separator + "local-repository" + File.separator + id)

    private val authenticationInfo: AuthenticationInfo = AuthenticationInfo()
    private val remoteRepository: RemoteRepository
    private val repositorySystem: RepositorySystem = newRepositorySystem()
    private val repositorySystemSession: RepositorySystemSession = newRepositorySystemSession(repositorySystem)
    private val wagon: LightweightHttpWagon

    private val plexusContainer: PlexusContainer
    private val indexer: Indexer
    private val indexUpdater: IndexUpdater

    init {
        val remoteRepoBuilder = RemoteRepository.Builder(id, "default", url)
        if (!user.isNullOrEmpty() && !password.isNullOrEmpty()) {
            authenticationInfo.userName = user
            authenticationInfo.password = password
            remoteRepoBuilder.setAuthentication(AuthenticationBuilder()
                    .addUsername(user).addPassword(password).build())
        }
        remoteRepository = remoteRepoBuilder.build()
        wagon = LightweightHttpWagon()
        wagon.authenticator = LightweightHttpWagonAuthenticator()
        val config = DefaultContainerConfiguration()
        config.classPathScanning = PlexusConstants.SCANNING_INDEX
        plexusContainer = DefaultPlexusContainer(config)
        indexer = plexusContainer.lookup(Indexer::class.java)
        indexUpdater = plexusContainer.lookup(IndexUpdater::class.java)
    }

    override fun scan() {
        log.info("[$id] Start repository indexing")
        val indexers = listOf("min", "jarContent", "maven-plugin").stream()
                .map({ roleHint -> plexusContainer.lookup(IndexCreator::class.java, roleHint) })
                .collect(Collectors.toList())
        val indexingContext = indexer.createIndexingContext(id, id, cacheDir, centralIndexDir, url,
                null, false, true, indexers)
        try {
            val resourceFetcher = WagonHelper.WagonFetcher(wagon, ResourceFetcherListener(), authenticationInfo, null)
            val updateResult = indexUpdater.fetchAndUpdateIndex(IndexUpdateRequest(indexingContext, resourceFetcher))
            if (updateResult.timestamp != null && updateResult.timestamp < indexingContext.timestamp) {
                log.info("[$id] Everything is up-to-date, scanning completed")
                return
            }
            log.info("[$id] Updating artifacts information...")
            val indexSearcher = indexingContext.acquireIndexSearcher()
            try {
                indexSearcher.use {
                    repeat(it.maxDoc()) { i ->
                        if (it.indexReader.isDeleted(i))
                            return@repeat
                        // Construct ArtifactInfo for current doc
                        val doc = it.indexReader.document(i)
                        val artifactInfo = IndexUtils.constructArtifactInfo(doc, indexingContext) ?: return@repeat
                        // Process artifacts with specified extensions only
                        if ("pom" != artifactInfo.fextension && !supportedArtifactTypes.contains(artifactInfo.fextension))
                            return@repeat
                        // Do not process sources and javadocs
                        if (artifactInfo.classifier == "sources" || artifactInfo.classifier == "javadoc")
                            return@repeat
                        // Get list of classes
                        val classes = doc.getFieldable("c")?.stringValue()
                        try {
                            processArtifact(artifactInfo, classes)
                        } catch (e: Throwable) {
                            log.warn("[$id] Error while processing artifact $artifactInfo", e)
                        }
                    }
                }
            } finally {
                indexingContext.releaseIndexSearcher(indexSearcher)
            }
            log.info("[$id] Repository indexing has completed successfully")
        } catch (e: Throwable) {
            log.warn("[$id] Repository indexing has failed with error", e)
        } finally {
            try {
                indexingContext.close(false)
            } catch (e: Throwable) {
                // Ignore
                log.warn("[$id] Error during indexing context closing", e)
            }
        }
    }

    private fun processArtifact(artifactInfo: ArtifactInfo, classes: String?) {
        val artifact = artifactInfoToArtifact(artifactInfo)
        // Retrieve packages
        val packages = HashSet<String>()
        classes?.split(Regex("\\s"))?.forEach { internalClassName ->
            // internal className starts with '/', remove it
            val className = toClassName(internalClassName.substring(1))
            // Add package of this class to the set if it is not empty
            val lastDotIndex = className.lastIndexOf('.')
            if (lastDotIndex > 0) {
                val pkg = className.substring(0, lastDotIndex)
                packages.add(pkg)
            }
        }
        // Retrieve dependencies
        val request = ArtifactDescriptorRequest()
                .setArtifact(artifactToAetherArtifact(artifact))
                .addRepository(remoteRepository)
        val dependencies = repositorySystem.readArtifactDescriptor(repositorySystemSession, request).dependencies
                .map { it.artifact }
                .filter { /*"pom" == it.extension || */supportedArtifactTypes.contains(it.extension) }
                .map { aetherArtifactToArtifact(it) }
        // Store artifact info
        storeArtifactInfo(artifact, dependencies, packages)
    }

    override fun downloadArtifact(artifact: Artifact): File? {
        try {
            // Create request in order to get aether's artifact
            val request = ArtifactRequest()
                    .setArtifact(artifactToAetherArtifact(artifact))
                    .addRepository(remoteRepository)
            // Perform the request,
            val aetherArtifact = repositorySystem
                    .resolveArtifact(repositorySystemSession, request).artifact
            // Get [File] or return null if artifact has not been resolved
            val file = aetherArtifact?.file
            if (file == null) {
                log.warn("Downloading $artifact failed, artifact is not resolved")
                return null
            }
            // todo fill package information here if needed
            return file
        } catch (e: ArtifactResolutionException) {
            log.warn("Downloading $artifact failed, error during resolution", e)
            return null
        }
    }

    private fun artifactInfoToArtifact(artifactInfo: ArtifactInfo) = Artifact(
            groupId = artifactInfo.groupId,
            artifactId = artifactInfo.artifactId,
            classifier = artifactInfo.classifier,
            type = artifactInfo.fextension,
            version = artifactInfo.version
    )

    private fun artifactToAetherArtifact(artifact: Artifact): org.eclipse.aether.artifact.Artifact = DefaultArtifact(
            artifact.groupId, artifact.artifactId, artifact.classifier, artifact.type, artifact.version
    )

    private fun aetherArtifactToArtifact(aetherArtifact: org.eclipse.aether.artifact.Artifact) = Artifact(
            groupId = aetherArtifact.groupId,
            artifactId = aetherArtifact.artifactId,
            classifier = aetherArtifact.classifier,
            type = aetherArtifact.extension,
            version = aetherArtifact.version
    )

    private fun newRepositorySystemSession(system: RepositorySystem): RepositorySystemSession {
        val session = MavenRepositorySystemUtils.newSession()
        val localRepo = LocalRepository(localRepositoryDir)
        session.localRepositoryManager = system.newLocalRepositoryManager(session, localRepo)
        session.repositoryListener = LoggedRepositoryListener()
        return session
    }

    private fun newRepositorySystem(): RepositorySystem {
        val locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, FileTransporterFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        locator.setErrorHandler(object : DefaultServiceLocator.ErrorHandler() {
            override fun serviceCreationFailed(type: Class<*>, impl: Class<*>, e: Throwable) {
                log.warn("Error while creating new repository system", e)
            }
        })
        return locator.getService(RepositorySystem::class.java)
    }

    private inner class LoggedRepositoryListener : AbstractRepositoryListener() {
        private val log: Logging = Logging.getLogging(LoggedRepositoryListener::class.java)

        override fun artifactDescriptorInvalid(event: RepositoryEvent) {
            log.warn("Invalid artifact descriptor, ${this@NexusMavenIndexer.id} indexing: $event")
        }

        override fun metadataInvalid(event: RepositoryEvent) {
            log.warn("Invalid metadata, ${this@NexusMavenIndexer.id} indexing: $event")
        }

        override fun artifactDescriptorMissing(event: RepositoryEvent) {
            log.warn("Invalid artifact descriptor,  ${this@NexusMavenIndexer.id} indexing: $event")
        }
    }

    private inner class ResourceFetcherListener : AbstractTransferListener() {
        private val log: Logging = Logging.getLogging(ResourceFetcherListener::class.java)

        override fun transferStarted(event: TransferEvent) {
            log.debug("[${this@NexusMavenIndexer.id}] Downloading ${event.resource.name}")
        }

        override fun transferCompleted(event: TransferEvent) {
            log.debug("[${this@NexusMavenIndexer.id}] Downloaded ${event.resource.name}")
        }

        override fun transferError(event: TransferEvent) {
            log.debug("[${this@NexusMavenIndexer.id}] Downloading failed ${event.resource.name}")
        }
    }
}