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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Walks through specified files.
 *
 * @param <F> type of fileInfo passed to analyser at the end of delegation chain
 */
public class FilePackWalker<N extends FileInfo, F extends FileInfo> extends DelegatingWalker<N, F> {
    private final Collection<N> infos;

    public FilePackWalker(Collection<N> infos, Delegating<? super N, ? extends F> delegating) {
        super(delegating);
        this.infos = infos;
    }

    public static <F extends FileInfo> FilePackWalker<PlainFileInfo, F> fromFiles(
            Collection<File> files, Delegating<? super PlainFileInfo, F> delegating) {
        ArrayList<PlainFileInfo> infos = new ArrayList<PlainFileInfo>();
        for (File file : files) {
            infos.add(new PlainFileInfo(file));
        }
        return new FilePackWalker<PlainFileInfo, F>(infos, delegating);
    }

    public static <F extends FileInfo> FilePackWalker<PlainFileInfo, F> fromFilenames(
            Collection<String> filenames, Delegating<? super PlainFileInfo, F> delegating) {
        ArrayList<PlainFileInfo> infos = new ArrayList<PlainFileInfo>();
        for (String filename : filenames) {
            infos.add(new PlainFileInfo(new File(filename)));
        }
        return new FilePackWalker<PlainFileInfo, F>(infos, delegating);
    }

    @Override
    public void walk(FileAnalyzer<? super F> analyzer) throws IOException {
        for (N info : infos) {
            delegating.makeDelegate(info)
                    .walk(analyzer);
        }
    }
}
