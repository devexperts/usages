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
import java.util.*

data class Member(
        @JsonProperty("memberName") val qualifiedMemberName: String,
        @JsonProperty("paramTypes") val parameterTypes: List<String>,
        @JsonProperty("type") val type: MemberType
) {

    fun packageName(): String {
        if (type == MemberType.PACKAGE)
            return qualifiedMemberName;
        val className = className();
        val lastDotIndex = className.lastIndexOf('.')
        return if (lastDotIndex == -1) qualifiedMemberName else qualifiedMemberName.substring(0, lastDotIndex)
    }

    fun className() = when (type) {
        MemberType.PACKAGE -> throw IllegalStateException("Cannot return class name for package member")
        MemberType.CLASS -> qualifiedMemberName
        else -> qualifiedMemberName.substring(0, qualifiedMemberName.indexOf('#'))
    }

    fun simpleClassName() = simpleNonPackageName(className())

    fun simpleName(): String {
        if (type == MemberType.PACKAGE)
            return qualifiedMemberName
        var simpleName = simpleNonPackageName(qualifiedMemberName)
        if (type == MemberType.METHOD)
            simpleName += "(${simpleParameters()})"
        return simpleName
    }

    fun simpleMemberName(): String {
        if (type == MemberType.PACKAGE || type == MemberType.CLASS)
            throw IllegalStateException("Simple member name is allowed for fields and methods only, current member is $this")
        var simpleMemberName = qualifiedMemberName.substring(qualifiedMemberName.indexOf("#") + 1)
        if (type == MemberType.METHOD && !parameterTypes.isEmpty())
            simpleMemberName += "(${simpleParameters()})"
        return simpleMemberName
    }

    private fun simpleNonPackageName(qualifiedName: String): String {
        val lastDotIndex = qualifiedName.lastIndexOf('.')
        return if (lastDotIndex < 0) qualifiedName else qualifiedName.substring(lastDotIndex + 1)
    }

    private fun simpleParameters(): String {
        val paramsJoiner = StringJoiner(", ")
        for (p in parameterTypes) {
            paramsJoiner.add(simpleNonPackageName(p))
        }
        return paramsJoiner.toString()
    }

    override fun toString(): String {
        var res = qualifiedMemberName
        if (type == MemberType.METHOD) {
            res = "$res(${simpleParameters()})"
        }
        return res
    }

    override fun hashCode(): Int {
        var result = qualifiedMemberName.hashCode()
        result = 31 * result + parameterTypes.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Member

        if (qualifiedMemberName != other.qualifiedMemberName) return false
        if (parameterTypes != other.parameterTypes) return false
        if (type != other.type) return false

        return true
    }

    companion object {
        fun fromPackage(packageName: String) = Member(packageName, emptyList(), MemberType.PACKAGE)
        fun fromClass(className: String) = Member(className, emptyList(), MemberType.CLASS)
        fun fromField(className: String, fieldName: String) = Member(
                "$className#$fieldName", emptyList(), MemberType.FIELD)
        fun fromMethod(className: String, methodName: String, parameterTypes: List<String>) = Member(
                "$className#$methodName", parameterTypes, MemberType.METHOD)
    }
}


enum class MemberType constructor(val typeName: String) {
    PACKAGE("package"),
    CLASS("class"),
    METHOD("method"),
    FIELD("field");

    override fun toString(): String {
        return typeName
    }
}


data class MemberUsage(
        @JsonProperty("member") val member: Member,
        @JsonProperty("usageKind") val usageKind: UsageKind,
        @JsonProperty("location") val location: Location
)


data class Location(
        @JsonProperty("artifact") val artifact: Artifact,
        @JsonProperty("member") val member: Member, // class or method
        @JsonProperty("file") val file: String?, // content file, do not use it for equals and hashCode!
        @JsonProperty("line") val lineNumber: Int? // line number of usage in the file
) {
    init {
//        if (member.type != MemberType.CLASS && member.type != MemberType.METHOD)
//            throw IllegalArgumentException("Only methods and classes could be used as location, current member $member")
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Location

        if (artifact != other.artifact) return false
        if (member != other.member) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = artifact.hashCode()
        result = 31 * result + member.hashCode()
        if (lineNumber != null)
            result = 31 * result + lineNumber
        return result
    }


}


data class Artifact(
        @JsonProperty("groupId") val groupId: String,
        @JsonProperty("artifactId") val artifactId: String,
        @JsonProperty("version") val version: String,
        @JsonProperty("type") val type: String?,
        @JsonProperty("classifier") val classifier: String?
) {
    override fun toString() = "$groupId:$artifactId:${type ?: ""}:${classifier ?: ""}:$version"
}


// Do not rename enum values for backwards compatibility
enum class UsageKind constructor(val description: String) {
    UNCLASSIFIED("Unclassified"),
    SIGNATURE("Signature"),
    CLASS_DECLARATION("Class declaration"),
    EXTEND_OR_IMPLEMENT("Usage in inheritance (extends or implements)"),
    OVERRIDE("Method overriding"),
    METHOD_DECLARATION("Method declaration"),
    METHOD_PARAMETER("Method parameter"),
    METHOD_RETURN("Method return type"),
    ANNOTATION("Annotation"),
    THROW("Throw"),
    CATCH("Catch"),
    CONSTANT("Constant"),
    FIELD("Field"),
    ASTORE("Local variable"),
    NEW("New instance"),
    ANEWARRAY("New array"),
    CAST("Type cast"),
    GETFIELD("Read field"),
    PUTFIELD("Write field"),
    GETSTATIC("Read static field"),
    PUTSTATIC("Write static field"),
    INVOKEVIRTUAL("Invoke virtual method"),
    INVOKESPECIAL("Invoke special method"),
    INVOKESTATIC("Invoke static method"),
    INVOKEINTERFACE("Invoke interface method"),
    INVOKEDYNAMIC("Invoke dynamic")
}