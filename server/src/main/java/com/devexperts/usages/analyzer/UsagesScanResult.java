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

import com.devexperts.usages.analyzer.concurrent.ConcurrentOutputStream;
import com.devexperts.usages.analyzer.internal.MemberInternal;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class UsagesScanResult {
    private static final Logger logger = Logger.getLogger(UsagesScanResult.class);

    private final Usages usages;

    public UsagesScanResult(Usages usages) {
        this.usages = usages;
    }

    public Map<MemberInternal, EnumSet<UseKind>> getElementUsages(String name) throws IOException {
        MemberInternal member = MemberInternal.valueOf(name);
        Map<MemberInternal, EnumSet<UseKind>> answer = usages.getUsagesForClass(member.getClassName())
                .getMemberUse(member.getMemberName());

        HashMap<MemberInternal, EnumSet<UseKind>> clone = new HashMap<MemberInternal, EnumSet<UseKind>>();
        for (Map.Entry<MemberInternal, EnumSet<UseKind>> entry : answer.entrySet()) {
            clone.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
        }
        return clone;
    }

    public void writeReport(File zipFile) throws IOException {
        write(zipFile, false, false);
    }

    public void writeReportAtomically(File zipFile) throws IOException {
        write(zipFile, true, false);
    }

    public void writeReportAtomicallyForcely(File zipFile) throws IOException {
        write(zipFile, true, true);
    }

    private void write(File zipFile, boolean atomically, boolean force) throws IOException {
        if (isEmpty()) {
            logger.info("Nothing found");
            if (!force)
                return;
        }
        logger.info("Writing " + zipFile);
        OutputStream outputStream = atomically ? new ConcurrentOutputStream(zipFile) : new FileOutputStream(zipFile);
        usages.writeToZipFile(outputStream);
        logger.info("Completed");
    }

    private boolean isEmpty() {
        for (Map.Entry<String, ClassUsages> entry : usages.getUsages().allClassUsages()) {
            if (!entry.getValue().isEmpty())
                return false;
        }
        return true;
    }

    Usages getUsages() {
        return usages;
    }
}
