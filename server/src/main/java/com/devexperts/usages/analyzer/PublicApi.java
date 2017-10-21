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
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

class PublicApi {
    private static final Logger logger = Logger.getLogger(PublicApi.class);

    private final Cache cache;

    private final Set<String> implClasses = new HashSet<String>();
    private final Set<MemberInternal> apiMembers = new TreeSet<MemberInternal>();
    private final Set<MemberInternal> deprecatedMembers = new TreeSet<MemberInternal>();

    public PublicApi(Cache cache) {
        this.cache = cache;
    }

    public Set<String> getImplClasses() {
        return implClasses;
    }

    public void addImplClass(String className) {
        implClasses.add(className);
    }

    public void addApiMember(String className, String memberName, boolean deprecated) {
        MemberInternal member = cache.resolveMember(className, memberName);
        apiMembers.add(member);
        if (deprecated)
            deprecatedMembers.add(member);
    }

    public void parseClass(String className, InputStream inStream) throws IOException {
        ClassReader cr = new ClassReader(inStream);
        if (!className.equals(cr.getClassName().replace('/', '.')))
            logger.warn("Unexpected class name: " + cr.getClassName()  + " for class " + className);
        else
            cr.accept(
                    new PublicApiAnalyzer(this, className),
                    ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE);
    }

    public void writeReportTo(PrintWriter out, Usages usages) {
        out.println(Fmt.COMMENT_PREFIX + " ---- Unused deprecated api classes");
        out.println();
        Set<String> unusedClasses = new HashSet<String>();
        for (MemberInternal member : deprecatedMembers) {
            if (member.getMemberName().equals(MemberInternal.CLASS_MEMBER_NAME)) {
                if (!usages.isClassUsed(member.getClassName())) {
                    unusedClasses.add(member.getClassName());
                    out.println(member.getClassName());
                }
            }
        }

        out.println();
        out.println(Fmt.COMMENT_PREFIX + " ---- Unused deprecated api members in the following format:");
        out.println(Fmt.COMMENT_PREFIX + " <class>");
        out.println(Fmt.COMMENT_PREFIX + " \t<member>");
        out.println();
        String className = null;
        for (MemberInternal member : deprecatedMembers) {
            if (unusedClasses.contains(member.getClassName()))
                continue;
            if (member.getMemberName().equals(MemberInternal.CLASS_MEMBER_NAME))
                continue;
            if (!usages.isMemberUsed(member)) {
                if (!member.getClassName().equals(className)) {
                    className = member.getClassName();
                    out.println(className);
                }
                out.println("\t" + member.getMemberName());
            }
        }

        out.println();
        out.println(Fmt.COMMENT_PREFIX + " ---- Still used deprecated api classes in the following format:");
        out.println(Fmt.COMMENT_PREFIX + " <class>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use> # summary of all uses");
        out.println(Fmt.COMMENT_PREFIX + " " + Fmt.CLASS_PREFIX + "<used-by-class>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use>");
        out.println(Fmt.COMMENT_PREFIX + " " + Fmt.MEMBER_PREFIX + "<used-by-member>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use>");
        out.println();
        for (MemberInternal member : deprecatedMembers) {
            if (member.getMemberName().equals(MemberInternal.CLASS_MEMBER_NAME)) {
                if (usages.isClassUsed(member.getClassName())) {
                    ClassUsages cu = usages.getUsagesForClass(member.getClassName());
                    out.print(member.getClassName());
                    Usage.printUsages(out, cu.getAllUsages());
                    out.println();
                    cu.printUse(out, "", cu.getAllUse());
                }
            }
        }

        out.println();
        out.println(Fmt.COMMENT_PREFIX + " ---- Still used deprecated api members in the following format:");
        out.println(Fmt.COMMENT_PREFIX + " <class>");
        out.println(Fmt.COMMENT_PREFIX + " \t<member>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use> # summary of all uses");
        out.println(Fmt.COMMENT_PREFIX + " \t" + Fmt.CLASS_PREFIX + "<used-by-class>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use>");
        out.println(Fmt.COMMENT_PREFIX + " \t" + Fmt.MEMBER_PREFIX + "<used-by-member>" + Fmt.USE_KINDS_PREFIX + "<kinds-of-use>");
        out.println();
        className = null;
        for (MemberInternal member : deprecatedMembers) {
            if (member.getMemberName().equals(MemberInternal.CLASS_MEMBER_NAME))
                continue;
            if (!usages.isMemberUsed(member))
                continue;
            if (!member.getClassName().equals(className)) {
                className = member.getClassName();
                out.println(className);
            }
            ClassUsages cu = usages.getUsagesForClass(member.getClassName());
            out.print("\t" + member.getMemberName());
            Usage.printUsages(out, cu.getAllMemberUsages(member.getMemberName()));
            out.println();
            cu.printUse(out, "\t", cu.getMemberUsages(member.getMemberName()));
        }
    }
}
