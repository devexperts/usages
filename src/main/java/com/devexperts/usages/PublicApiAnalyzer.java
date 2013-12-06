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
