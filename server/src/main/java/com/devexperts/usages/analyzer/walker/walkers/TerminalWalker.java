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

import java.io.IOException;

/**
 * Just passes given info to processor, thus taking role of terminate walker in delegation chain.
 *
 * @param <I> type of operated fileInfo
 */
public class TerminalWalker<I extends FileInfo> implements Walker<I> {
    private final I fileInfo;

    public TerminalWalker(I fileInfo) {
        this.fileInfo = fileInfo;
    }

    @Override
    public void walk(FileAnalyzer<? super I> fileAnalyzer) throws IOException {
        fileAnalyzer.process(fileInfo);
    }


    // often used
    private static final Delegating DELEGATING = new TheDelegating();

    public static <I extends FileInfo> Delegating<I, I> getDelegating() {
        //noinspection unchecked
        return DELEGATING;
    }

    private static class TheDelegating<I extends FileInfo> implements Delegating<I, I> {
        @Override
        public Walker<I> makeDelegate(I info) {
            return new TerminalWalker<I>(info);
        }
    }
}
