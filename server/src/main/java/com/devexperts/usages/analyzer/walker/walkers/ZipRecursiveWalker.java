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
package com.devexperts.usages.analyzer.walker.walkers;

import com.devexperts.usages.analyzer.walker.FileAnalyzer;
import com.devexperts.usages.analyzer.walker.info.FileInfo;
import com.devexperts.usages.analyzer.walker.info.PlainFileInfo;
import com.devexperts.usages.analyzer.walker.info.ZipEntryInfo;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Walks through zip entries. When encounters a zip inside, makes it out too.
 *
 * @param <F> type of fileInfo passed to analyser at the end of delegation chain
 */
public class ZipRecursiveWalker<F extends FileInfo> extends DelegatingWalker<ZipEntryInfo, F> {
    private static final Logger logger = Logger.getLogger(ZipRecursiveWalker.class);

    private final PlainFileInfo rootInfo;

    public ZipRecursiveWalker(PlainFileInfo file, Delegating<? super ZipEntryInfo, ? extends F> delegating) {
        super(delegating);
        this.rootInfo = file;
    }

    public ZipRecursiveWalker(File file, Delegating<? super ZipEntryInfo, ? extends F> delegating) {
        this(new PlainFileInfo(file), delegating);
    }

    public static ZipRecursiveWalker<ZipEntryInfo> ofFile(File file) {
        return new ZipRecursiveWalker<>(file, TerminalWalker.<ZipEntryInfo>getDelegating());
    }

    @Override
    public void walk(FileAnalyzer<? super F> analyzer) throws IOException {
        final ZipFile zip = new ZipFile(rootInfo.getPath());
        try {
            for (Enumeration<? extends ZipEntry> en = zip.entries(); en.hasMoreElements(); ) {
                final ZipEntry ze = en.nextElement();
                if (ze.isDirectory()) {
                    continue;
                }
                String entryName = ze.getName();
                String entryPath = rootInfo.getPath() + "!" + entryName;
                if (entryName.endsWith(".zip") || entryName.endsWith(".jar") || entryName.endsWith(".war")) {
                    File temp = new File("temp" + entryPath.replaceAll("[\\\\!/]", "~") + ".zip");
                    logger.debug("Extracting " + entryPath + " to " + temp);

                    try {
                        Files.copy(zip.getInputStream(ze), temp.toPath());
                        new ZipRecursiveWalker<F>(temp, delegating)
                            .walk(analyzer);
                    } finally {
                        temp.delete();
                    }
                }
                this.delegating.makeDelegate(new ZipEntryInfo(entryPath, zip, ze))
                    .walk(analyzer);
            }
        } finally {
            try {
                zip.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public static class D extends Passing<PlainFileInfo, ZipEntryInfo> {
        @Override
        public <F extends FileInfo> MidDelegating<? super PlainFileInfo, ZipEntryInfo, ? extends F> delegateTo(
            final Delegating<? super ZipEntryInfo, F> nextDelegating)
        {
            return new MidDelegating<PlainFileInfo, ZipEntryInfo, F>() {
                @Override
                public DelegatingWalker<? extends ZipEntryInfo, F> makeDelegate(PlainFileInfo info) {
                    return new ZipRecursiveWalker<F>(info, nextDelegating);
                }
            };
        }
    }
}
