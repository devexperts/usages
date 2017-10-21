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
package com.devexperts.usages.analyzer;

import com.devexperts.usages.analyzer.internal.MemberInternal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ClassUsages {
    private final Cache cache;
    private final String className;

    private final Map<String, Map<MemberInternal, Set<Usage>>> usages = new TreeMap<>();
    private final Set<String> inheritableMembers = new TreeSet<>(); // public and protected instance methods only

    public ClassUsages(Cache cache, String className) {
        this.cache = cache;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Map<MemberInternal, Set<Usage>>> getUsages() {
        return usages;
    }

    // don't write classes that has not actual (don't store for the sake of overridableMethods only)
    public boolean isEmpty() {
        return usages.isEmpty();
    }

    public Set<String> getDescendantClasses() {
        Set<String> result = new HashSet<>();
        Map<MemberInternal, Set<Usage>> typeUseMap = usages.get(MemberInternal.CLASS_MEMBER_NAME);
        if (typeUseMap != null) {
            for (Map.Entry<MemberInternal, Set<Usage>> entry : typeUseMap.entrySet()) {
                Set<Usage> usages = entry.getValue();
                if (usages.stream()
                    .anyMatch(usage -> usage.getUseKind() == UseKind.EXTEND || usage.getUseKind() == UseKind.IMPLEMENT))
                {
                    result.add(entry.getKey().getClassName());
                }
            }
        }
        return result;
    }

    public void inheritUseTo(Collection<ClassUsages> classAncestors) {
        for (Map.Entry<String, Map<MemberInternal, Set<Usage>>> entry : usages.entrySet()) {
            String member = entry.getKey();
            Map<MemberInternal, Set<Usage>> use = entry.getValue();
            for (Map.Entry<MemberInternal, Set<Usage>> useEntry : use.entrySet()) {
                MemberInternal usedFrom = useEntry.getKey();
                Set<Usage> usages = useEntry.getValue();
                for (Usage usage : usages) {
                    if (usage.getUseKind().inheritedUse) {
                        for (ClassUsages ancestor : classAncestors) {
                            if (ancestor.inheritableMembers.contains(member) && member.endsWith(")")) {
                                ancestor.addMemberUsage(member, usedFrom, usage);
                            }
                        }
                    }
                }
            }
        }
    }

    private static String getOuterClassName(String className) {
        int i = className.indexOf('$');
        if (i < 0) {
            return className;
        }
        return className.substring(0, i);
    }

    public void cleanupInnerUsages() {
        String outerClassName = getOuterClassName(className);
        for (Iterator<Map<MemberInternal, Set<Usage>>> useIt = usages.values().iterator(); useIt.hasNext(); ) {
            Map<MemberInternal, Set<Usage>> use = useIt.next();
            for (Iterator<MemberInternal> memberIt = use.keySet().iterator(); memberIt.hasNext(); ) {
                MemberInternal member = memberIt.next();
                if (outerClassName.equals(getOuterClassName(member.getClassName()))) {
                    memberIt.remove();
                }
            }
            if (use.isEmpty()) {
                useIt.remove();
            }
        }
    }


    public void removeUsesFromClasses(Set<String> implClasses) {
        for (Iterator<Map<MemberInternal, Set<Usage>>> useIt = usages.values().iterator(); useIt.hasNext(); ) {
            Map<MemberInternal, Set<Usage>> use = useIt.next();
            for (Iterator<MemberInternal> memberIt = use.keySet().iterator(); memberIt.hasNext(); ) {
                MemberInternal member = memberIt.next();
                if (implClasses.contains(member.getClassName())) {
                    memberIt.remove();
                }
            }
            if (use.isEmpty()) {
                useIt.remove();
            }
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

    public Set<Usage> getAllUsages() {
        Set<Usage> result = new HashSet<>();
        for (Map<MemberInternal, Set<Usage>> use : usages.values()) {
            for (Set<Usage> usages : use.values()) {
                result.addAll(usages);
            }
        }
        return result;
    }

    public Map<MemberInternal, Set<Usage>> getAllUse() {
        Map<MemberInternal, Set<Usage>> result = new TreeMap<>();
        for (Map<MemberInternal, Set<Usage>> use : usages.values()) {
            for (Map.Entry<MemberInternal, Set<Usage>> useEntry : use.entrySet()) {
                MemberInternal member = useEntry.getKey();
                Set<Usage> set = result.get(member);
                if (set == null) {
                    result.put(member, set = new HashSet<>());
                }
                set.addAll(useEntry.getValue());
            }
        }
        return result;
    }

    public Set<Usage> getAllMemberUsages(String member) {
        Set<Usage> result = new HashSet<>();
        Map<MemberInternal, Set<Usage>> use = usages.get(member);
        if (use != null) {
            use.values().forEach(result::addAll);
        }
        return result;
    }

    public Map<MemberInternal, EnumSet<UseKind>> getMemberUse(String member) {
        Map<MemberInternal, Set<Usage>> use = usages.get(member);
        if (use == null) {
            usages.put(cache.resolveString(member), use = new TreeMap<>());
        }
        Map<MemberInternal, EnumSet<UseKind>> result = new TreeMap<>();
        use.forEach((m, u) -> {
            EnumSet<UseKind> useKinds = EnumSet.noneOf(UseKind.class);
            useKinds.addAll(u.stream().map(Usage::getUseKind).collect(Collectors.toSet()));
            result.put(m, useKinds);
        });
        return result;
    }

    public Map<MemberInternal, Set<Usage>> getMemberUsages(String member) {
        Map<MemberInternal, Set<Usage>> use = usages.get(member);
        if (use == null) {
            usages.put(cache.resolveString(member), use = new TreeMap<>());
        }
        return use;
    }


    private <K> EnumSet<UseKind> getUseKinds(Map<K, EnumSet<UseKind>> use, K key) {
        EnumSet<UseKind> useKinds = use.get(key);
        if (useKinds == null) {
            use.put(key, useKinds = EnumSet.noneOf(UseKind.class));
        }
        return useKinds;
    }

    private <K> Set<Usage> getUsages(Map<K, Set<Usage>> use, K key) {
        Set<Usage> usages = use.get(key);
        if (usages == null) {
            use.put(key, usages = new HashSet<>());
        }
        return usages;
    }

    public void addTypeUsage(MemberInternal usedFrom, Usage usage) {
        addMemberUsage(MemberInternal.CLASS_MEMBER_NAME, usedFrom, usage);
    }

    public void addMemberUsage(String member, MemberInternal usedFrom, Usage usage) {
        getUsages(getMemberUsages(member), usedFrom).add(usage);
    }

    public void readFromStream(InputStream inStream) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream, Fmt.CHARSET));
        boolean inheritableMember = false;
        String useFor = null;
        Map<MemberInternal, Set<Usage>> use = null;
        String className = null;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() == 0 || line.startsWith(Fmt.COMMENT_PREFIX)) {
                continue;
            }
            if (line.startsWith(Fmt.MEMBER_PREFIX)) {
                if (use == null || className == null) {
                    throw new IOException("Invalid format -- header lines expected");
                }
                String rest = line.substring(Fmt.MEMBER_PREFIX.length());
                int i = rest.indexOf(Fmt.USE_KINDS_PREFIX);
                if (i < 0) {
                    throw new IOException("Invalid format -- missing member use kinds");
                }
                String memberName = rest.substring(0, i);
                Set<Usage> useKinds = Usage.parseUsages(rest.substring(i + Fmt.USE_KINDS_PREFIX.length()));
                MemberInternal m = cache.resolveMember(className, memberName);
                getUsages(use, m).addAll(useKinds);
            } else if (line.startsWith(Fmt.CLASS_PREFIX)) {
                if (use == null) {
                    throw new IOException("Invalid format -- header lines expected");
                }
                inheritableMember = false;
                String rest = line.substring(Fmt.CLASS_PREFIX.length());
                int i = rest.indexOf(Fmt.USE_KINDS_PREFIX);
                if (i < 0) {
                    className = rest;
                } else {
                    className = rest.substring(0, i);
                    Set<Usage> useKinds =
                        Usage.parseUsages(rest.substring(i + Fmt.USE_KINDS_PREFIX.length()));
                    getUsages(use, cache.resolveMember(className, MemberInternal.CLASS_MEMBER_NAME)).addAll(useKinds);
                }
            } else {
                if (inheritableMember) {
                    addInheritableMember(useFor);
                }
                inheritableMember = true;
                useFor = line;
                use = getMemberUsages(line);
            }
        }
        if (inheritableMember) {
            addInheritableMember(useFor);
        }
    }

    public void writeToStream(OutputStream outStream) {
        PrintWriter out = new PrintWriter(new OutputStreamWriter(outStream, Fmt.CHARSET));
        out.print(Fmt.COMMENT_PREFIX + " Kinds of uses of class " + className);
        Usage.printUsages(out, getAllUsages());
        out.println();
        out.println();
        out.println(Fmt.COMMENT_PREFIX + " ---- Summary of all classes that are using class " + className);
        out.println(Fmt.COMMENT_PREFIX);
        for (Map.Entry<String, Set<Usage>> useEntry : getUsagesOfClasses().entrySet()) {
            String useClassName = useEntry.getKey();
            out.print(Fmt.COMMENT_PREFIX);
            out.print(' ');
            out.print(useClassName);
            Usage.printUsages(out, useEntry.getValue());
            out.println();
        }
        out.println();
        out.println(Fmt.COMMENT_PREFIX + " ---- Detailed uses of class " + className + " in the following format:");
        out.println(Fmt.COMMENT_PREFIX + " <member>");
        out.println(
            Fmt.COMMENT_PREFIX + " " + Fmt.CLASS_PREFIX + "<used-by-class>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use>");
        out.println(Fmt.COMMENT_PREFIX + " " + Fmt.MEMBER_PREFIX + "<used-by-member>" + Fmt.USE_KINDS_PREFIX +
            "<kinds-of-use>");
        out.println();
        for (Map.Entry<String, Map<MemberInternal, Set<Usage>>> entry : usages.entrySet()) {
            Map<MemberInternal, Set<Usage>> use = entry.getValue();
            if (use.isEmpty()) {
                continue;
            }
            out.println(entry.getKey());
            printUse(out, "", use);
        }
        if (!inheritableMembers.isEmpty()) {
            out.println();
            out.println(
                Fmt.COMMENT_PREFIX + " ---- Public and protected members (potentially inheritable and overridable)");
            out.println();
            for (String member : inheritableMembers) {
                out.println(member);
            }
        }
        out.flush();
    }

    public void printUse(PrintWriter out, String prefix, Map<MemberInternal, Set<Usage>> use) {
        String className = null;
        for (Map.Entry<MemberInternal, Set<Usage>> useEntry : use.entrySet()) {
            MemberInternal m = useEntry.getKey();
            if (!m.getClassName().equals(className)) {
                out.print(prefix);
                out.print(Fmt.CLASS_PREFIX);
                out.print(className = m.getClassName());
                Set<Usage> classUseKinds = use.get(new MemberInternal(className, MemberInternal.CLASS_MEMBER_NAME));
                if (classUseKinds != null && !classUseKinds.isEmpty()) {
                    Usage.printUsages(out, classUseKinds);
                }
                out.println();
            }
            if (!MemberInternal.CLASS_MEMBER_NAME.equals(m.getMemberName())) {
                out.print(prefix);
                out.print(Fmt.MEMBER_PREFIX);
                out.print(m.getMemberName());
                Usage.printUsages(out, useEntry.getValue());
                out.println();
            }
        }
    }

    public Map<String, EnumSet<UseKind>> getUsingClasses() {
        Map<String, EnumSet<UseKind>> result = new TreeMap<String, EnumSet<UseKind>>();
        for (Map<MemberInternal, Set<Usage>> use : usages.values()) {
            for (Map.Entry<MemberInternal, Set<Usage>> useEntry : use.entrySet()) {
                getUseKinds(result, useEntry.getKey().getClassName())
                    .addAll(useEntry.getValue().stream().map(Usage::getUseKind).collect(Collectors.toSet()));
            }
        }
        return result;
    }

    public Map<String, Set<Usage>> getUsagesOfClasses() {
        Map<String, Set<Usage>> result = new TreeMap<>();
        for (Map<MemberInternal, Set<Usage>> use : usages.values()) {
            for (Map.Entry<MemberInternal, Set<Usage>> useEntry : use.entrySet()) {
                getUsages(result, useEntry.getKey().getClassName()).addAll(useEntry.getValue());
            }
        }
        return result;
    }

    public void fetchFrom(ClassUsages other) {
        if (!className.equals(other.className)) {
            throw new AssertionError("Different class names");
        }

        for (Map.Entry<String, Map<MemberInternal, Set<Usage>>> entry : other.usages.entrySet()) {
            Map<MemberInternal, Set<Usage>> v = usages.get(entry.getKey());
            if (v == null) {
                usages.put(entry.getKey(), entry.getValue());
                continue;
            }
            for (Map.Entry<MemberInternal, Set<Usage>> entry1 : entry.getValue().entrySet()) {
                Set<Usage> useKinds = v.get(entry1.getKey());
                if (useKinds == null) {
                    v.put(entry1.getKey(), entry1.getValue());
                } else {
                    useKinds.addAll(entry1.getValue());
                }
            }
        }
    }

}
