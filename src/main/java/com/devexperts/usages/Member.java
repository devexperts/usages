package com.devexperts.usages;

import org.objectweb.asm.Type;

class Member implements Comparable<Member> {
	public static final String CLASS_MEMBER_NAME = "<class>";

	private final String className;
	private final String memberName;

	Member(String className, String memberName) {
		this.className = className;
		this.memberName = memberName;
	}

	public static String methodMemberName(String methodName, Type type) {
		StringBuilder sb = new StringBuilder();
		sb.append(methodName);
		sb.append('(');
		Type[] argumentTypes = type.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; i++) {
			if (i > 0)
				sb.append(',');
			sb.append(argumentTypes[i].getClassName());
		}
		sb.append(')');
		return sb.toString();
	}

	public String getClassName() {
		return className;
	}

	public String getMemberName() {
		return memberName;
	}

	public int compareTo(Member o) {
		int i = className.compareTo(o.className);
		if (i != 0)
			return i;
		return memberName.compareTo(o.memberName);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Member))
			return false;
		Member member = (Member)o;
		return className.equals(member.className) && memberName.equals(member.memberName);

	}

	@Override
	public int hashCode() {
		return 31 * className.hashCode() + memberName.hashCode();
	}

	@Override
	public String toString() {
		return className + "#" + memberName;
	}
}
