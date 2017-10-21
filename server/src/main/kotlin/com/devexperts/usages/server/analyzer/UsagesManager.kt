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

import com.devexperts.usages.api.*
import com.devexperts.usages.server.Locations
import com.devexperts.usages.server.MemberUsages
import com.devexperts.usages.server.Members
import com.devexperts.usages.server.WithId
import com.devexperts.usages.server.artifacts.ArtifactManager
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object UsagesManager {

    // === PUBLIC API ===

    fun geOrCreateMember(member: Member) = transaction { getOrCreateMemberInternal(member) }

    fun getOrCreateLocationId(artifactId: Int, memberId: Int, file: String?, line: Int): Int = transaction {
        getOrCreateLocationIdInternal(artifactId, memberId, file, line)
    }

    fun addMemberUsage(memberId: Int, locationId: Int, usageKind: UsageKind) = transaction {
        addMemberUsageInternal(memberId, locationId, usageKind)
    }

    @Synchronized
    fun getMemberUsages(memberId: Int): List<MemberUsage> {
        // Caches for members, locations, artifacts
        val members = HashMap<Int, Member>()
        val locations = HashMap<Int, Location>()
        val artifacts = HashMap<Int, Artifact>()
        return transaction {
            // todo in the specified artifacts only???
            // Get member associated with the specified memberId
            val member = members.computeIfAbsent(memberId, { getMemberInternal(it) })
            MemberUsages.select { MemberUsages.memberId.eq(memberId) }.toList().map {
                val usageKind = it[MemberUsages.usageKind]
                val location = locations.computeIfAbsent(it[MemberUsages.locationId], { locationId ->
                    val q = Locations.select { Locations.id.eq(locationId) }.limit(1).first()
                    Location(artifact = artifacts.computeIfAbsent(q[Locations.artifactId], { ArtifactManager.getArtifactInternal(it).value }),
                            member = members.computeIfAbsent(q[Locations.memberId], { getMemberInternal(it) }),
                            file = q[Locations.file],
                            lineNumber = q[Locations.line]
                    )
                })
                MemberUsage(member, usageKind, location)
            }
        }
    }

    // === END PUBLIC API ===

    private fun getMemberInternal(memberId: Int): Member {
        val q = Members.select { Members.id.eq(memberId) }.limit(1).first()
        return Member(qualifiedMemberName = q[Members.qualifiedName],
                parameterTypes = parseParameterTypes(q[Members.paramTypes]),
                type = q[Members.type])
    }

    private fun getOrCreateMemberInternal(member: Member): WithId<Member> {
        val parameterTypesStr = parameterTypesToString(member.parameterTypes)
        var id = Members.slice(Members.id).select {
            Members.qualifiedName.eq(member.qualifiedMemberName) and
                    Members.paramTypes.eq(parameterTypesStr) and
                    Members.type.eq(member.type)
        }.limit(1).firstOrNull()?.get(Members.id)
        if (id == null) {
            id = Members.insert {
                it[Members.qualifiedName] = member.qualifiedMemberName
                it[Members.paramTypes] = parameterTypesStr
                it[Members.type] = member.type
            }[Members.id]
        }
        return WithId(id, member)
    }

    private fun getOrCreateLocationIdInternal(artifactId: Int, memberId: Int, file: String?, line: Int): Int {
        val id = Locations.slice(Locations.id).select {
            Locations.artifactId.eq(artifactId) and Locations.file.eq(file) and Locations.line.eq(line)
        }.limit(1).firstOrNull()?.get(Locations.id)
        if (id != null)
            return id
        return Locations.insert {
            it[Locations.artifactId] = artifactId
            it[Locations.memberId] = memberId
            it[Locations.file] = file
            it[Locations.line] = line
        }[Locations.id]
    }

    private fun addMemberUsageInternal(memberId: Int, locationId: Int, usageKind: UsageKind) {
        val exists = MemberUsages.select {
            MemberUsages.memberId.eq(memberId) and
                    MemberUsages.locationId.eq(locationId) and
                    MemberUsages.usageKind.eq(usageKind)
        }.limit(1).count() > 0
        if (!exists) {
            MemberUsages.insert {
                it[MemberUsages.memberId] = memberId
                it[MemberUsages.locationId] = locationId
                it[MemberUsages.usageKind] = usageKind
            }
        }
    }

    private fun parameterTypesToString(paramTypes: List<String>): String = paramTypes.joinToString(separator = ",")
    private fun parseParameterTypes(parameterTypes: String): List<String> = parameterTypes.split(",")
}