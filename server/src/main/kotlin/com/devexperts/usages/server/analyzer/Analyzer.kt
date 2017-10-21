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
package com.devexperts.usages.server.analyzer

import com.devexperts.logging.Logging
import com.devexperts.usages.api.Artifact
import com.devexperts.usages.server.WithId
import com.devexperts.usages.server.artifacts.ArtifactManager
import com.devexperts.usages.server.indexer.MavenIndexer
import org.objectweb.asm.ClassReader
import java.util.zip.ZipFile

object Analyzer {
    private val log = Logging.getLogging(Analyzer::class.java)

    fun analyzeIfNeeded(indexer: MavenIndexer, artifact: WithId<Artifact>) {
        if (!ArtifactManager.isAnalyzed(artifact.id)) {
            downloadAndAnalyze(indexer, artifact)
            ArtifactManager.markAnalyzed(artifact.id)
        }
    }

    private fun downloadAndAnalyze(indexer: MavenIndexer, artifact: WithId<Artifact>) {
        val artifactFile = indexer.downloadArtifact(artifact.value)
        if (artifactFile == null) {
            log.error("Cannot analyze artifact $artifact, not downloaded")
            return
        }
        ZipFile(artifactFile).use { zip ->
            zip.entries().iterator().forEach { zipEntry ->
                if (zipEntry.isDirectory)
                    return@forEach
                if (zipEntry.name.endsWith(".class")) {
                    zip.getInputStream(zipEntry).use { classInputStream ->
                        val cr = ClassReader(classInputStream)
                        cr.accept(ClassAnalyzer(artifact.id), ClassReader.SKIP_FRAMES)
                    }
                }
            }
        }
    }
}