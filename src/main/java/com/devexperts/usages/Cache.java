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
