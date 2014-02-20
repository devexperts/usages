/*
 * usages - Usages Analysis Tool
 * Copyright (C) 2002-2014  Devexperts LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.devexperts.usages;

import org.objectweb.asm.*;

class PublicApiAnalyzer extends ClassVisitor {
	private final PublicApi api;
	private final String className;

	public PublicApiAnalyzer(PublicApi api, String className) {
		super(Opcodes.ASM4);
		this.api = api;
		this.className = className;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		api.addImplClass(className);
		if (isPublicApi(access))
			api.addApiMember(className, Member.CLASS_MEMBER_NAME, isDeprecated(access));
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (isPublicApi(access))
			api.addApiMember(className, name, isDeprecated(access));
		return null;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (isPublicApi(access))
			api.addApiMember(className, Member.methodMemberName(name, Type.getType(desc)), isDeprecated(access));
		return null;
	}

	private static boolean isPublicApi(int access) {
		return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0;
	}

	private static boolean isDeprecated(int access) {
		return (access & (Opcodes.ACC_DEPRECATED)) != 0;
	}
}
