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

import com.devexperts.usages.api.UsageKind
import com.devexperts.usages.server.*
import com.devexperts.usages.server.artifacts.ArtifactManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UsagesManagerTest {
    private var id1: Int = -1
    private var id2: Int = -1
    private var id3: Int = -1

    @Before
    fun setUp() {
        initInMemoryDatabase()
        id1 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact1,
                dependencies = listOf(artifact2), packages = pkg123).id
        id2 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact2,
                dependencies = emptyList(), packages = listOf(pkg4)).id
        id3 = ArtifactManager.storeArtifactInfo(indexerId = "jrc", artifact = artifact3,
                dependencies = emptyList(), packages = listOf(pkg2)).id
    }

    @After
    fun tearDown() = dropDatabase()

    @Test
    fun testAddSameMembers() {
        val id1 = UsagesManager.geOrCreateMember(member1).id
        val id2 = UsagesManager.geOrCreateMember(member1).id
        assertEquals(id1, id2)
    }

    @Test
    fun testAddSameLocations() {
        val memberId1 = UsagesManager.geOrCreateMember(member1).id
        val id1 = UsagesManager.getOrCreateLocationId(id1, memberId1, null, -1)
        val id2 = UsagesManager.getOrCreateLocationId(id1, memberId1, null, -1)
        assertEquals(id1, id2)
    }

    @Test
    fun testAddMemberUsagesWithDifferentKind() {
        val memberId1 = UsagesManager.geOrCreateMember(member1).id
        val location1 = UsagesManager.getOrCreateLocationId(id1, memberId1, null, -1)
        UsagesManager.addMemberUsage(memberId1, location1, UsageKind.UNCLASSIFIED)
        UsagesManager.addMemberUsage(memberId1, location1, UsageKind.CLASS_DECLARATION)
    }

    @Test
    fun testAddSameMemberUsages() {
        val memberId1 = UsagesManager.geOrCreateMember(member1).id
        val location1 = UsagesManager.getOrCreateLocationId(id1, memberId1, null, -1)
        UsagesManager.addMemberUsage(memberId1, location1, UsageKind.UNCLASSIFIED)
        UsagesManager.addMemberUsage(memberId1, location1, UsageKind.UNCLASSIFIED)
    }

    @Test
    fun testGetMemberUsages() {
        val memberId1 = UsagesManager.geOrCreateMember(member1).id
        val memberId2 = UsagesManager.geOrCreateMember(member2).id
        val location1 = UsagesManager.getOrCreateLocationId(id1, memberId1, null, -1)
        val location2 = UsagesManager.getOrCreateLocationId(id2, memberId1, null, -1)
        val location3 = UsagesManager.getOrCreateLocationId(id1, memberId2, null, -1)
        UsagesManager.addMemberUsage(memberId1, location1, UsageKind.CAST)
        UsagesManager.addMemberUsage(memberId1, location2, UsageKind.CAST)
        UsagesManager.addMemberUsage(memberId1, location3, UsageKind.OVERRIDE)
        UsagesManager.addMemberUsage(memberId2, location1, UsageKind.UNCLASSIFIED)
        UsagesManager.addMemberUsage(memberId2, location3, UsageKind.UNCLASSIFIED)
        UsagesManager.addMemberUsage(memberId1, location1, UsageKind.UNCLASSIFIED)
        val usages = UsagesManager.getMemberUsages(memberId1)
        assertEquals(4, usages.size)
    }
}