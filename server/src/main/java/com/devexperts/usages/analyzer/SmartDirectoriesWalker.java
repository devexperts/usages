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

import com.devexperts.usages.analyzer.walker.FileAnalyzer;
import com.devexperts.usages.analyzer.walker.info.FileInfo;
import com.devexperts.usages.analyzer.walker.info.PlainFileInfo;
import com.devexperts.usages.analyzer.walker.walkers.Delegating;
import com.devexperts.usages.analyzer.walker.walkers.DelegatingWalker;
import com.devexperts.usages.analyzer.walker.walkers.MidDelegating;
import com.devexperts.usages.analyzer.walker.walkers.Passing;
import com.devexperts.usages.analyzer.walker.walkers.TerminalWalker;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Walks through filesystem.
 * Understands constructions like "./fil?name.txt", "./*.txt" or "./**.txt".
 *
 * @param <F> type of fileInfo passed to analyser at the end of delegation chain
 */
class SmartDirectoriesWalker<F extends FileInfo> extends DelegatingWalker<PlainFileInfo, F> {
    private static final Logger logger = Logger.getLogger(SmartDirectoriesWalker.class);

    private final File file;

    public static SmartDirectoriesWalker<FileInfo> ofFile(File file) {
        return new SmartDirectoriesWalker<FileInfo>(file, TerminalWalker.getDelegating());
    }

    public SmartDirectoriesWalker(File rootFile, Delegating<? super PlainFileInfo, ? extends F> delegating) {
        super(delegating);
        this.file = rootFile;
    }

    @Override
    public void walk(FileAnalyzer<? super F> analyzer) throws IOException {
//        logger.info("Processing " + file);
        String name = file.getName();
        if (!name.contains("*") && !name.contains("?")) {
            delegating.makeDelegate(new PlainFileInfo(file))
                    .walk(analyzer);
            return;
        }
        File parentFile = file.getParentFile();
        if (name.contains("**")) {
            File[] dirs = parentFile.listFiles();
            if (dirs != null) {
                for (File dir : dirs) {
                    if (dir.isDirectory()) {
                        new SmartDirectoriesWalker<F>(new File(dir, name), delegating)
                                .walk(analyzer);
                    }
                }
            }
            name = name.replace("**", "*");
        }
        final Pattern pattern = Config.globToPattern(name, false);
        String[] fileNames = parentFile.list(new FilenameFilter() {
            public boolean accept(File dir, String fileName) {
                return pattern.matcher(fileName).matches();
            }
        });
        if (fileNames != null) {
            for (String fileName : fileNames) {
                File f = new File(parentFile, fileName);
//                logger.info("Processing " + f);
                new SmartDirectoriesWalker<F>(f, delegating)
                        .walk(analyzer);
            }
        }
    }

    public static class D extends Passing<PlainFileInfo, PlainFileInfo> {
        @Override
        public <F extends FileInfo> MidDelegating<? super PlainFileInfo, PlainFileInfo, ? extends F> delegateTo(
                final Delegating<? super PlainFileInfo, F> nextDelegating) {
            return new MidDelegating<PlainFileInfo, PlainFileInfo, F>() {
                @Override
                public DelegatingWalker<? extends PlainFileInfo, F> makeDelegate(PlainFileInfo info) {
                    return new SmartDirectoriesWalker<F>(info.getFile(), nextDelegating);
                }
            };
        }
    }
}
