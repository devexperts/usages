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
import com.devexperts.usages.analyzer.walker.walkers.ZipRecursiveWalker;
import com.devexperts.usages.api.Artifact;
import com.devexperts.usages.api.Location;
import com.devexperts.usages.api.Member;
import com.devexperts.usages.api.MemberUsage;
import com.devexperts.usages.api.UsageKind;
import com.devexperts.usages.server.config.Configuration;
import com.devexperts.usages.server.indexer.MavenIndexer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Analyzer0 {
    private static final Logger logger = LogManager.getLogger(Analyzer0.class);

    public List<MemberUsage> analyze(MavenIndexer indexer, Artifact artifact) {
        List<MemberUsage> result = new ArrayList<>();

        UsagesScanResult usagesScanResult = null;

        File artifactUsagesCacheFile = new File(getUsagesCacheFilePath(artifact));
        if (!artifactUsagesCacheFile.exists()) {
            File artifactFile = indexer.downloadArtifact(artifact);
            if (artifactFile == null) {
                logger.error("Artifact hasn't been downloaded");
                return Collections.emptyList();
            }
            try {
                usagesScanResult = analyzeFile(artifactFile);
                artifactUsagesCacheFile.getParentFile().mkdirs();
                usagesScanResult.writeReportAtomically(artifactUsagesCacheFile);
                boolean deleted = artifactFile.delete();
                if (!deleted) {
                    logger.warn(artifactFile + " has not been deleted");
                }
            } catch (IOException e) {
                logger.error("Error while analyzing " + artifact, e);
            }
        } else {
            try {
                usagesScanResult = analyzeFile(artifactUsagesCacheFile);
            } catch (IOException e) {
                logger.error("Error while parsing " + artifactUsagesCacheFile, e);
            }
        }

        if (usagesScanResult != null)
            return getMemberUsages(usagesScanResult, artifact);

        return Collections.emptyList();
    }

    static UsagesScanResult analyzeFile(File file) throws IOException {
        return new UsagesScanner(ZipRecursiveWalker.ofFile(file)).analyze();
    }

    static List<MemberUsage> getMemberUsages(UsagesScanResult usagesScanResult, Artifact artifact) {
        List<MemberUsage> result = new ArrayList<>();

        Set<Map.Entry<String, ClassUsages>> allClassUsages =
            usagesScanResult.getUsages().getUsages().allClassUsages();
        for (Map.Entry<String, ClassUsages> entry : allClassUsages) {
            String className = entry.getKey();
            Map<String, Map<MemberInternal, Set<Usage>>> classUsages = entry.getValue().getUsages();
            classUsages.forEach((memberName, memberEnumSetMap) -> {
                MemberInternal mmm = new MemberInternal(className, memberName);
                Member member = mmm.toMember();
                memberEnumSetMap.forEach((m, usages) -> {
                    for (Usage usage : usages) {
                        Location location = new Location(artifact, m.toMember(), usage.getFileName(), usage.getLineNumber());
                        MemberUsage memberUsage = new MemberUsage(member, convertUseKind(usage.getUseKind()), location);
                        result.add(memberUsage);
                    }
                });
            });
        }

        return result;
    }

//    static Member getMember(String className, String memberName) {
//        String qualifiedMemberName = className + "#" + memberName;
//        if (CLASS_MEMBER_NAME.equals(memberName)) {
//            return Member.createClassMember(className);
//        } else if (memberName.contains("(")) {
//            int indexOfLP = qualifiedMemberName.indexOf('(');
//            int indexOfRP = qualifiedMemberName.indexOf(')');
//            String[] parameterTypes = new String[0];
//            if (indexOfLP + 1 != indexOfRP) {
//                parameterTypes = qualifiedMemberName.substring(indexOfLP + 1, indexOfRP).split(",");
//            }
//            qualifiedMemberName = qualifiedMemberName.substring(0, indexOfLP);
//            if (qualifiedMemberName.endsWith("<init>")) {
//                String shortClassName = className.substring(className.lastIndexOf('.') + 1);
//                qualifiedMemberName = qualifiedMemberName.replace("<init>", shortClassName);
//            }
//            return Member.createMethodMember(qualifiedMemberName, parameterTypes);
//        } else {
//            return Member.createFieldMember(qualifiedMemberName);
//        }
//    }

    private static UsageKind convertUseKind(UseKind useKind) {
        switch (useKind) {
        case ANEWARRAY:
            return UsageKind.ANEWARRAY;
        case ANNOTATION:
            return UsageKind.ANNOTATION;
        case CATCH:
            return UsageKind.CATCH;
        case CHECKCAST:
            return UsageKind.CAST;
        case CONSTANT:
            return UsageKind.CONSTANT;
        case EXTEND:
        case IMPLEMENT:
            return UsageKind.EXTEND_OR_IMPLEMENT;
        case FIELD:
            return UsageKind.FIELD;
        case GETFIELD:
            return UsageKind.GETFIELD;
        case GETSTATIC:
            return UsageKind.GETSTATIC;
        case INSTANCEOF:
            return UsageKind.CAST;
        case INVOKEDYNAMIC:
            return UsageKind.INVOKEDYNAMIC;
        case INVOKEINTERFACE:
            return UsageKind.INVOKEINTERFACE;
        case INVOKESPECIAL:
            return UsageKind.INVOKESPECIAL;
        case INVOKESTATIC:
            return UsageKind.INVOKESTATIC;
        case INVOKEVIRTUAL:
            return UsageKind.INVOKEVIRTUAL;
        case NEW:
            return UsageKind.NEW;
        case OVERRIDE:
            return UsageKind.OVERRIDE;
        case PUTFIELD:
            return UsageKind.PUTFIELD;
        case PUTSTATIC:
            return UsageKind.PUTSTATIC;
        case RETURN:
            return UsageKind.METHOD_RETURN;
        case ARGUMENT:
        case SIGNATURE:
            return UsageKind.SIGNATURE;
        case THROW:
            return UsageKind.THROW;
        case UNKNOWN:
            return UsageKind.UNCLASSIFIED;
        default:
            throw new IllegalArgumentException("Wrong UseKind");
        }
    }

    static String getUsagesCacheFilePath(Artifact artifact) {
        StringBuilder builder = new StringBuilder()
            .append(getCacheDirectory())
            .append(File.separator)
            .append(artifact.getGroupId().replace('.', File.separatorChar))
            .append(File.separator)
            .append(artifact.getArtifactId())
            .append(File.separator)
            .append(artifact.getVersion());
        if (artifact.getClassifier() != null) {
            builder.append(" (")
                .append(artifact.getClassifier())
                .append(")");
        }
        builder.append(".zip");
        return builder.toString();
    }

    static String getCacheDirectory() {
        StringBuilder builder = new StringBuilder()
            .append(Configuration.INSTANCE.getWorkDir())
            .append(File.separator)
            .append("cache");
        return builder.toString();
    }
}
