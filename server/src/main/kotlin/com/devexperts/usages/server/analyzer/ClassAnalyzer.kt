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

import com.devexperts.usages.api.Member
import com.devexperts.usages.api.MemberType
import com.devexperts.usages.api.UsageKind
import com.devexperts.usages.server.WithId
import org.objectweb.asm.*
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import java.io.File

class ClassAnalyzer(private val artifactId: Int) : ClassVisitor(ASM_VERSION) {
    private lateinit var classMember: WithId<Member>

    // Location parameters
    private lateinit var locationMember: WithId<Member>
    private var file: String? = null // defined in [visitSource]
    private var lineNumber = -1

    private fun addUsage(member: Member, usageKind: UsageKind) {
        addUsage(UsagesManager.geOrCreateMember(member), usageKind)
    }

    private fun addUsage(member: WithId<Member>, usageKind: UsageKind) {
        UsagesManager.addMemberUsage(member.id,
                UsagesManager.getOrCreateLocationId(artifactId, locationMember.id, file, lineNumber),
                usageKind)
    }

    private fun addTypeUsage(type: Type, usageKind: UsageKind) {
        var myType = type
        if (myType.sort == Type.METHOD) {
            addTypeUsage(myType.returnType, UsageKind.METHOD_RETURN)
            for (arg in myType.argumentTypes)
                addTypeUsage(arg, UsageKind.METHOD_PARAMETER)
            return
        }
        while (myType.sort == Type.ARRAY)
            myType = myType.elementType
        if (myType.sort == Type.OBJECT)
            addUsage(Member.fromClass(myType.className), usageKind)
    }

    private fun addHandleUsage(handle: Handle, usageKind: UsageKind) {

    }

    private fun addConstantUsage(constant: Any, usageKind: UsageKind) {
        when (constant) {
            is Type -> addTypeUsage(constant, usageKind)
            is Handle -> addHandleUsage(constant, usageKind)
        }
    }

    private fun processSignature(signature: String?) {
        if (signature != null)
            SignatureReader(signature).accept(SignatureAnalyzer())
    }

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        val m = Member(toClassName(name), emptyList(), MemberType.CLASS)
        classMember = UsagesManager.geOrCreateMember(m)
        locationMember = classMember
        addUsage(classMember.value, UsageKind.CLASS_DECLARATION)
        processSignature(signature)
    }

    override fun visitEnd() {
        // todo location at package???
    }

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor {
        val fieldMember = Member.fromField(classMember.value.qualifiedMemberName, name)
        addTypeUsage(Type.getType(desc), UsageKind.FIELD)
        processSignature(signature)
        return super.visitField(access, name, desc, signature, value)
    }

    override fun visitSource(source: String?, debug: String?) {
        // Define [file] here
        if (source == null)
            return
        var className = classMember.value.qualifiedMemberName
        val lastDotIndex = className.lastIndexOf('.')
        if (lastDotIndex > 0)
            className = className.substring(0, lastDotIndex)
        file = className.replace('.', File.separatorChar) + File.separator + source
    }

    private inner class SignatureAnalyzer : SignatureVisitor(ASM_VERSION) {
        override fun visitClassType(internalName: String) = addUsage(
                Member.fromClass(toClassName(internalName)), UsageKind.SIGNATURE)
    }
}

fun toClassName(internalName: String) = internalName.replace('/', '.')

private val ASM_VERSION = Opcodes.ASM6