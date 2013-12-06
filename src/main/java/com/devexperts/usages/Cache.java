package com.devexperts.usages;

import java.util.HashMap;
import java.util.Map;

class Cache {
	private Map<String, String> strings = new HashMap<String, String>();
	private Map<Member, Member> members = new HashMap<Member, Member>();

	public String resolveString(String s) {
		String result = strings.get(s);
		if (result != null)
			return result;
		strings.put(s, s);
		return s;
	}

	public Member resolveMember(String className, String memberName) {
		Member m = new Member(resolveString(className), resolveString(memberName));
		Member result = members.get(m);
		if (result != null)
			return result;
		members.put(m, m);
		return m;
	}
}
