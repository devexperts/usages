package com.devexperts.usages;

import java.io.*;
import java.util.*;

class ClassUsages {
	private final Cache cache;
	private final String className;

	private final Map<String, Map<Member, EnumSet<UseKind>>> usages = new TreeMap<String, Map<Member, EnumSet<UseKind>>>();
	private final Set<String> inheritableMembers = new TreeSet<String>(); // public and protected instance methods only

	ClassUsages(Cache cache, String className) {
		this.cache = cache;
		this.className = className;
	}

	// don't write classes that has not actual (don't store for the sake of overridableMethods only)
	public boolean isEmpty() {
		return usages.isEmpty();
	}

	public Set<String> getDescendantClasses() {
		Set<String> result = new HashSet<String>();
		Map<Member, EnumSet<UseKind>> typeUseMap = usages.get(Member.CLASS_MEMBER_NAME);
		if (typeUseMap != null)
			for (Map.Entry<Member, EnumSet<UseKind>> entry : typeUseMap.entrySet()) {
				EnumSet<UseKind> useKinds = entry.getValue();
				if (useKinds.contains(UseKind.EXTEND) || useKinds.contains(UseKind.IMPLEMENT))
					result.add(entry.getKey().getClassName());
			}
		return result;
	}

	public void inheritUseTo(Collection<ClassUsages> classAncestors) {
		for (Map.Entry<String, Map<Member, EnumSet<UseKind>>> entry : usages.entrySet()) {
			String member = entry.getKey();
			Map<Member, EnumSet<UseKind>> use = entry.getValue();
			for (Map.Entry<Member, EnumSet<UseKind>> useEntry : use.entrySet()) {
				Member usedFrom = useEntry.getKey();
				EnumSet<UseKind> useKinds = useEntry.getValue();
				for (UseKind useKind : useKinds)
					if (useKind.inheritedUse) {
						for (ClassUsages ancestor : classAncestors) {
							if (ancestor.inheritableMembers.contains(member))
								ancestor.addMemberUsage(member, usedFrom, useKind);
						}
					}
			}
		}
	}

	private static String getOuterClassName(String className) {
		int i = className.indexOf('$');
		if (i < 0)
			return className;
		return className.substring(0, i);
	}

	public void cleanupInnerUsages() {
		String outerClassName = getOuterClassName(className);
		for (Iterator<Map<Member, EnumSet<UseKind>>> useIt = usages.values().iterator(); useIt.hasNext(); ) {
			Map<Member, EnumSet<UseKind>> use = useIt.next();
			for (Iterator<Member> memberIt = use.keySet().iterator(); memberIt.hasNext(); ) {
				Member member = memberIt.next();
				if (outerClassName.equals(getOuterClassName(member.getClassName())))
					memberIt.remove();
			}
			if (use.isEmpty())
				useIt.remove();
		}
	}


	public void removeUsesFromClasses(Set<String> implClasses) {
		for (Iterator<Map<Member, EnumSet<UseKind>>> useIt = usages.values().iterator(); useIt.hasNext(); ) {
			Map<Member, EnumSet<UseKind>> use = useIt.next();
			for (Iterator<Member> memberIt = use.keySet().iterator(); memberIt.hasNext(); ) {
				Member member = memberIt.next();
				if (implClasses.contains(member.getClassName())) {
					memberIt.remove();
				}
			}
			if (use.isEmpty())
				useIt.remove();
		}
	}

	public boolean isMemberUsed(String memberName) {
		return usages.containsKey(memberName);
	}

	// public and protected instance methods only
	public Set<String> getInheritableMembers() {
		return inheritableMembers;
	}

	public void addInheritableMember(String member) {
		inheritableMembers.add(cache.resolveString(member));
	}

	public EnumSet<UseKind> getAllUseKinds() {
		EnumSet<UseKind> result = EnumSet.noneOf(UseKind.class);
		for (Map<Member, EnumSet<UseKind>> use : usages.values())
			for (EnumSet<UseKind> useKinds : use.values())
				result.addAll(useKinds);
		return result;
	}

	public Map<Member, EnumSet<UseKind>> getAllUse() {
		Map<Member, EnumSet<UseKind>> result = new TreeMap<Member, EnumSet<UseKind>>();
		for (Map<Member, EnumSet<UseKind>> use : usages.values()) {
			for (Map.Entry<Member, EnumSet<UseKind>> useEntry : use.entrySet()) {
				Member member = useEntry.getKey();
				EnumSet<UseKind> set = result.get(member);
				if (set == null)
					result.put(member, set = EnumSet.noneOf(UseKind.class));
				set.addAll(useEntry.getValue());
			}
		}
		return result;
	}

	public EnumSet<UseKind> getAllMemberUseKinds(String member) {
		EnumSet<UseKind> result = EnumSet.noneOf(UseKind.class);
		Map<Member, EnumSet<UseKind>> use = usages.get(member);
		if (use != null)
			for (EnumSet<UseKind> useKinds : use.values())
				result.addAll(useKinds);
		return result;
	}

	public Map<Member, EnumSet<UseKind>> getMemberUse(String member) {
		Map<Member, EnumSet<UseKind>> use = usages.get(member);
		if (use == null)
			usages.put(cache.resolveString(member), use = new TreeMap<Member, EnumSet<UseKind>>());
		return use;
	}

	private <K> EnumSet<UseKind> getUseKinds(Map<K, EnumSet<UseKind>> use, K key) {
		EnumSet<UseKind> useKinds = use.get(key);
		if (useKinds == null)
			use.put(key, useKinds = EnumSet.noneOf(UseKind.class));
		return useKinds;
	}

	public void addTypeUsage(Member usedFrom, UseKind useKind) {
		addMemberUsage(Member.CLASS_MEMBER_NAME, usedFrom, useKind);
	}

	public void addMemberUsage(String member, Member usedFrom, UseKind useKind) {
		getUseKinds(getMemberUse(member), usedFrom).add(useKind);
	}

	public void readFromStream(InputStream inStream) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(inStream, Fmt.CHARSET));
		boolean inheritableMember = false;
		String useFor = null;
		Map<Member, EnumSet<UseKind>> use = null;
		String className = null;
		String line;
		while ((line = in.readLine()) != null) {
			if (line.length() == 0 || line.startsWith(Fmt.COMMENT_PREFIX))
				continue;
			if (line.startsWith(Fmt.MEMBER_PREFIX)) {
				if (use == null || className == null)
					throw new IOException("Invalid format -- header lines expected");
				String rest = line.substring(Fmt.MEMBER_PREFIX.length());
				int i = rest.indexOf(Fmt.USE_KINDS_PREFIX);
				if (i < 0)
					throw new IOException("Invalid format -- missing member use kinds");
				String memberName = rest.substring(0, i);
				EnumSet<UseKind> useKinds = UseKind.parseUseKinds(rest.substring(i + Fmt.USE_KINDS_PREFIX.length()));
				Member m = cache.resolveMember(className, memberName);
				getUseKinds(use, m).addAll(useKinds);
			} else if (line.startsWith(Fmt.CLASS_PREFIX)) {
				if (use == null)
					throw new IOException("Invalid format -- header lines expected");
				inheritableMember = false;
				String rest = line.substring(Fmt.CLASS_PREFIX.length());
				int i = rest.indexOf(Fmt.USE_KINDS_PREFIX);
				if (i < 0) {
					className = rest;
				} else {
					className = rest.substring(0, i);
					EnumSet<UseKind> useKinds = UseKind.parseUseKinds(rest.substring(i + Fmt.USE_KINDS_PREFIX.length()));
					getUseKinds(use, cache.resolveMember(className, Member.CLASS_MEMBER_NAME)).addAll(useKinds);
				}
			} else {
				if (inheritableMember)
					addInheritableMember(useFor);
				inheritableMember = true;
				useFor = line;
				use = getMemberUse(line);
			}
		}
		if (inheritableMember)
			addInheritableMember(useFor);
	}

	public void writeToStream(OutputStream outStream) {
		PrintWriter out = new PrintWriter(new OutputStreamWriter(outStream, Fmt.CHARSET));
		out.print(Fmt.COMMENT_PREFIX + " Kinds of uses of class " + className);
		UseKind.printUseKinds(out, getAllUseKinds());
		out.println();
		out.println();
		out.println(Fmt.COMMENT_PREFIX + " ---- Summary of all classes that are using class " + className);
		out.println(Fmt.COMMENT_PREFIX);
		for (Map.Entry<String, EnumSet<UseKind>> useEntry : getUsingClasses().entrySet()) {
			String useClassName = useEntry.getKey();
			out.print(Fmt.COMMENT_PREFIX);
			out.print(' ');
			out.print(useClassName);
			UseKind.printUseKinds(out, useEntry.getValue());
			out.println();
		}
		out.println();
		out.println(Fmt.COMMENT_PREFIX + " ---- Detailed uses of class " + className + " in the following format:");
		out.println(Fmt.COMMENT_PREFIX + " <member>");
		out.println(Fmt.COMMENT_PREFIX + " " + Fmt.CLASS_PREFIX + "<used-by-class>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use>");
		out.println(Fmt.COMMENT_PREFIX + " " + Fmt.MEMBER_PREFIX + "<used-by-member>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use>");
		out.println();
		for (Map.Entry<String, Map<Member, EnumSet<UseKind>>> entry : usages.entrySet()) {
			Map<Member, EnumSet<UseKind>> use = entry.getValue();
			if (use.isEmpty())
				continue;
			out.println(entry.getKey());
			printUse(out, "", use);
		}
		if (!inheritableMembers.isEmpty()) {
			out.println();
			out.println(Fmt.COMMENT_PREFIX + " ---- Public and protected members (potentially inheritable and overridable)");
			out.println();
			for (String member : inheritableMembers)
				out.println(member);
		}
		out.flush();
	}

	public void printUse(PrintWriter out, String prefix, Map<Member, EnumSet<UseKind>> use) {
		String className = null;
		for (Map.Entry<Member, EnumSet<UseKind>> useEntry : use.entrySet()) {
			Member m = useEntry.getKey();
			if (!m.getClassName().equals(className)) {
				out.print(prefix);
				out.print(Fmt.CLASS_PREFIX);
				out.print(className = m.getClassName());
				EnumSet<UseKind> classUseKinds = use.get(new Member(className, Member.CLASS_MEMBER_NAME));
				if (classUseKinds != null && !classUseKinds.isEmpty())
					UseKind.printUseKinds(out, classUseKinds);
				out.println();
			}
			if (!Member.CLASS_MEMBER_NAME.equals(m.getMemberName())) {
				out.print(prefix);
				out.print(Fmt.MEMBER_PREFIX);
				out.print(m.getMemberName());
				UseKind.printUseKinds(out, useEntry.getValue());
				out.println();
			}
		}
	}

	private Map<String, EnumSet<UseKind>> getUsingClasses() {
		Map<String, EnumSet<UseKind>> result = new TreeMap<String, EnumSet<UseKind>>();
		for (Map<Member, EnumSet<UseKind>> use : usages.values()) {
			for (Map.Entry<Member, EnumSet<UseKind>> useEntry : use.entrySet())
				getUseKinds(result, useEntry.getKey().getClassName()).addAll(useEntry.getValue());
		}
		return result;
	}
}
