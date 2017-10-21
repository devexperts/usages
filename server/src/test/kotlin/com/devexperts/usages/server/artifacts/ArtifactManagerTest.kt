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

import com.devexperts.usages.server.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ArtifactManagerTest {
    @Before
    fun setUp() = initInMemoryDatabase()

    @After
    fun tearDown() = dropDatabase()

    @Test
    fun testStoreArtifactInfo() {
        val id1 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact1,
                dependencies = listOf(artifact2), packages = pkg123).id
        val id2 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact2,
                dependencies = emptyList(), packages = listOf(pkg4)).id
        val toAnalyze = ArtifactManager.artifactIdsWithAnyDependency(listOf(id2))
        assertEquals(1, toAnalyze.size)
        assertTrue(toAnalyze.contains(id1))
        assertFalse(ArtifactManager.isAnalyzed(id1))
        assertEquals("jrc", ArtifactManager.getSourceIndexerName(id1))
    }

    @Test
    fun testAddSameArtifact() {
        val id1 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact1,
                dependencies = listOf(artifact2), packages = pkg123).id
        val id2 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact1,
                dependencies = listOf(artifact2), packages = pkg123).id
        assertEquals(id1, id2)
    }

    @Test
    fun testArtifactsWithPackage() {
        val id1 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact1,
                dependencies = listOf(artifact2), packages = pkg123).id
        val id2 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact2,
                dependencies = emptyList(), packages = listOf(pkg4)).id
        val id3 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact3,
                dependencies = emptyList(), packages = listOf(pkg2)).id
        val res = ArtifactManager.artifactsWithPackage(pkg2)
        assertEquals(2, res.size)
        assertTrue(res.contains(id1))
        assertFalse(res.contains(id2))
        assertTrue(res.contains(id3))
    }

    @Test
    fun testGetArtifact() {
        val id = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact1,
                dependencies = listOf(artifact2), packages = pkg123).id
        val a = ArtifactManager.getArtifact(id).value
        assertEquals(artifact1, a)
    }

    @Test
    fun testMarkAnalyzed() {
        val id = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact1,
                dependencies = listOf(artifact2), packages = pkg123).id
        ArtifactManager.markAnalyzed(id)
        assertTrue(ArtifactManager.isAnalyzed(id))
    }
}