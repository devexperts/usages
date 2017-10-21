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
package com.devexperts.usages.analyzer.internal;

import com.devexperts.usages.api.Member;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemberInternal implements Comparable<MemberInternal> {
    public static final String CLASS_MEMBER_NAME = "<class>";

    private final String className;
    private final String memberName;

    public MemberInternal(String className, String memberName) {
        this.className = className;
        this.memberName = memberName;
    }

    public static MemberInternal valueOf(String memberFullName) {
        int hashIndex = memberFullName.lastIndexOf("#");
        return new MemberInternal(memberFullName.substring(0, hashIndex), memberFullName.substring(hashIndex + 1));
    }

    public static String methodMemberName(String methodName, Type type) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName);
        sb.append('(');
        Type[] argumentTypes = type.getArgumentTypes();
        for (int i = 0; i < argumentTypes.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(argumentTypes[i].getClassName());
        }
        sb.append(')');
        return sb.toString();
    }

    public Member toMember() {
        if (memberName.equals(CLASS_MEMBER_NAME)) {
            return Member.Companion.fromClass(className);
        }
        int i = memberName.indexOf('(');
        if (i < 0) { // field
            if (memberName.equals("<init>"))
                return Member.Companion.fromMethod(className, memberName, Collections.emptyList());
            return Member.Companion.fromField(className, memberName);
        }
        String mname = memberName.substring(0, i);
        List<String> params = Arrays.asList(memberName.substring(i + 1, memberName.length() - 1).split(","));
        return Member.Companion.fromMethod(className, mname, params);
    }

    public String getClassName() {
        return className;
    }

    public String getMemberName() {
        return memberName;
    }

    public int compareTo(MemberInternal o) {
        int i = className.compareTo(o.className);
        if (i != 0) {
            return i;
        }
        return memberName.compareTo(o.memberName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemberInternal)) {
            return false;
        }
        MemberInternal member = (MemberInternal) o;
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
