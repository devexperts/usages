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
import com.devexperts.usages.analyzer.tune.UsagesKeeper;
import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class Usages {
    private static final Logger logger = Logger.getLogger(Usages.class);

    private final Cache cache;
    private final UsagesKeeper usages;
    private final Map<String, Set<String>> descendants = new HashMap<>();
    private final Config config;
    private final Map<String, String> filesOfClasses = new HashMap<>();
    private boolean needPostprocessing = true;

    public Usages(Cache cache, UsagesKeeper usages, Config config) {
        this.cache = cache;
        this.usages = usages;
        this.config = config;
    }

    public void putFileOfClass(String className, String fileName) {
        filesOfClasses.put(className, fileName);
    }

    public void setNeedPostprocessing(boolean needPostprocessing) {
        this.needPostprocessing = needPostprocessing;
    }

    public Cache getCache() {
        return cache;
    }

    private Set<String> getDescendantsRec(String className) {
        Set<String> ds = descendants.get(className);
        if (ds != null) {
            return ds;
        }
        descendants.put(className, Collections.emptySet()); // avoid infinite recursion
        ClassUsages classUsage = usages.get(className);
        Set<String> result = new HashSet<>();
        if (classUsage != null) {
            Set<String> immediateDescendants = classUsage.getDescendantClasses();
            result.addAll(immediateDescendants);
            for (String descendant : immediateDescendants) {
                result.addAll(getDescendantsRec(descendant));
            }
        }
        if (!result.isEmpty()) {
            descendants.put(className, result);
        }
        return result;
    }

    public void analyze() {
        if (needPostprocessing) {
            Set<Map.Entry<String, ClassUsages>> usageEntries = usages.allClassUsages();

            // setting information about source files
            usageEntries.forEach(entry -> entry.getValue().getUsages()
                .forEach((s, memberSetMap) -> memberSetMap.forEach((member, usages1) -> usages1
                    .forEach(usage -> usage
                        .setFileName(filesOfClasses.getOrDefault(usage.getFileName(), usage.getFileName()))))));

            logger.info("Analyzing overrides");
            // build descendants map recursively with memorization
            for (Map.Entry<String, ClassUsages> entry : usageEntries) {
                String className = entry.getKey();
                ClassUsages cu = entry.getValue();
                Set<String> classMembers = cu.getInheritableMembers();
                Set<String> classDescendants = getDescendantsRec(className);
                for (String descendantClassName : classDescendants) {
                    ClassUsages descendantCU = usages.get(descendantClassName);
                    if (descendantCU != null) {
                        for (String descendantMember : descendantCU.getInheritableMembers()) {
                            if (classMembers.contains(descendantMember) && descendantMember.endsWith(")")) {

                                cu.addMemberUsage(descendantMember,
                                    cache.resolveMember(descendantClassName, descendantMember),
                                    new Usage(UseKind.OVERRIDE, filesOfClasses.get(className), -1));
                            }
                        }
                    }
                }
            }

            logger.info("Analyzing inheritance");
            // build ancestors map by reversing descendants map
            Map<String, Set<ClassUsages>> ancestors = new HashMap<>();
            for (Map.Entry<String, Set<String>> entry : descendants.entrySet()) {
                ClassUsages ancestor = usages.get(entry.getKey());
                for (String descendant : entry.getValue()) {
                    Set<ClassUsages> classAncestors = ancestors.get(descendant);
                    if (classAncestors == null) {
                        ancestors.put(descendant, classAncestors = new HashSet<>());
                    }
                    classAncestors.add(ancestor);
                }
            }
            for (Map.Entry<String, ClassUsages> entry : usageEntries) {
                String className = entry.getKey();
                Set<ClassUsages> classAncestors = ancestors.get(className);
                if (classAncestors == null) {
                    continue;
                }
                ClassUsages cu = entry.getValue();
                cu.inheritUseTo(classAncestors);
            }

            logger.info("Cleaning up inner class usages");
            for (Map.Entry<String, ClassUsages> entry : usageEntries) {
                entry.getValue().cleanupInnerUsages();
            }
        }
    }

    public void removeUsesFromClasses(Set<String> implClasses) {
        for (Map.Entry<String, ClassUsages> entry : usages.allClassUsages()) {
            entry.getValue().removeUsesFromClasses(implClasses);
        }
    }

    public boolean isClassUsed(String className) {
        ClassUsages cu = usages.get(className);
        return cu != null && !cu.isEmpty();
    }

    public boolean isMemberUsed(MemberInternal member) {
        ClassUsages cu = usages.get(member.getClassName());
        return cu != null && cu.isMemberUsed(member.getMemberName());
    }

    public void writeToZipFile(OutputStream outputStream) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(outputStream);
        try {
            Set<Map.Entry<String, ClassUsages>> usagesToWrite = usages.allClassUsages();
            int size = usagesToWrite.size();
            int done = 0;
            int lastPercent = 0;
            long lastTime = System.currentTimeMillis();

            for (Map.Entry<String, ClassUsages> entry : usagesToWrite) {
                String name = entry.getKey();
                ClassUsages usages = entry.getValue();
                if (usages.isEmpty()) {
                    continue;
                }
                zos.putNextEntry(new ZipEntry(name.replace('.', '/') + Constants.USAGES_SUFFIX));
                usages.writeToStream(zos);
                zos.closeEntry();
                done++;
                int percent = 100 * done / size;
                long time = System.currentTimeMillis();
                if (done == size || percent > lastPercent && time > lastTime + 1000) {
                    lastPercent = percent;
                    lastTime = time;
                    logger.info(percent + "% done");
                }
            }
            logger.info("Writing zip file directory");
            zos.finish();
        } finally {
            zos.close();
        }
    }

    public UsagesKeeper getUsages() {
        return usages;
    }

    public ClassUsages getUsagesForClass(String className) {
        return usages.getOrEmpty(className);
    }

    public void parseClass(String className, InputStream inStream) throws IOException {
        ClassReader cr = new ClassReader(inStream);
        if (!className.equals(cr.getClassName().replace('/', '.'))) {
            logger.info("Unexpected class name: " + cr.getClassName() + " for class " + className);
        } else {
            cr.accept(new ClassUsagesAnalyzer(this, className, config), ClassReader.SKIP_FRAMES);
        }
    }

}
