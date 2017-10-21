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

import com.devexperts.usages.api.Artifact
import com.devexperts.usages.api.Member
import com.devexperts.usages.api.MemberType

val artifact1 = Artifact(
        groupId = "com.devexperts.usages",
        artifactId = "usages",
        version = "2017",
        type = null,
        classifier = null
)
val artifact2 = Artifact(
        groupId = "com.devexperts.qd",
        artifactId = "dxlib",
        version = "3.154",
        type = null,
        classifier = null
)
val artifact3 = Artifact(
        groupId = "com.devexperts.qd",
        artifactId = "qd-core",
        version = "3.155",
        type = null,
        classifier = null
)


val pkg1 = "com"
val pkg2 = "com.devexperts"
val pkg3 = "com.devexperts.usages"
val pkg4 = "com.devexperts.util"

val pkg123 = arrayListOf(pkg1, pkg2, pkg3)


val member1 = Member("com.devexperts.usages.server.ServerKt", emptyList(), MemberType.CLASS)
val member2 = Member("com.devexperts.usages.server.ServerKt#fff",
        listOf("java.lang.String", "com.devexperts.usages.MyClass"), MemberType.METHOD)
