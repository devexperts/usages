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
package com.devexperts.usages.cclient

import com.devexperts.usages.api.*

fun main(args: Array<String>) {
    val memberType = MemberType.valueOf(getRequiredStringPropertyOrExit("memberType").toUpperCase())
    val memberDesc = getRequiredStringPropertyOrExit("member")
    val servers = getRequiredStringPropertyOrExit("servers").split(";")

    val member = when (memberType) {
        MemberType.PACKAGE -> Member(memberDesc, emptyList(), memberType)
        MemberType.CLASS -> Member(memberDesc, emptyList(), memberType)
        MemberType.FIELD -> Member(memberDesc, emptyList(), memberType)
        MemberType.METHOD -> {
            val openBracketIndex = memberDesc.indexOf('(');
            val qualifiedMemberName = memberDesc.substring(0, openBracketIndex)
            val parameters = memberDesc.substring(openBracketIndex + 1, memberDesc.length - 1).split(",")
            Member(qualifiedMemberName, parameters, memberType)
        }
    }
    val request = MemberUsageRequest(member = member,
            findClasses = getBooleanProperty("findClasses", true),
            findDerivedClassesUsages = getBooleanProperty("findDerivedClassesUsages", true),
            findFields = getBooleanProperty("findFields", true),
            findMethods = getBooleanProperty("findMethods", true),
            findDerivedMethodsUsages = getBooleanProperty("findDerivedMethodsUsages", true),
            searchScope = createArtifactMaskFromString(getStringProperty("searchScope", "*:*:*"))
    )
    val requestProcessor = object : MemberUsageRequestProcessor(servers, request) {
        override fun onNewUsages(serverUrl: String, usages: List<MemberUsage>) {
            usages.forEach { println(it) }
        }

        override fun onError(serverUrl: String, message: String, throwable: Throwable?) {
            println("ERROR: $message")
        }

        override fun onComplete() {
            println("COMPLETED")
        }
    }
    requestProcessor.doRequest()
}

private fun getBooleanProperty(key: String, defaultValue: Boolean): Boolean {
    return System.getProperty(key)?.toBoolean() ?: defaultValue;
}

private fun getStringProperty(key: String, defaultValue: String): String {
    return System.getProperty(key) ?: defaultValue;
}

private fun getRequiredStringPropertyOrExit(key: String): String {
    val value = System.getProperty(key)
    if (value == null) {
        println("Property $key should be specified")
        System.exit(0)
    }
    return value
}