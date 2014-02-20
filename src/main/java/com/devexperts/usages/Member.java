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
