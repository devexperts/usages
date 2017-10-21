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
import java.util.Collection;

/**
 * Unites several walkers into one
 */
public class PackWalker<I extends FileInfo> implements Walker<I> {
    private final Collection<Walker<? extends I>> walkers;

    public PackWalker(Collection<Walker<? extends I>> walkers) {
        this.walkers = walkers;
    }

    @Override
    public void walk(FileAnalyzer<? super I> fileAnalyzer) throws IOException {
        for (Walker<? extends I> walker : walkers) {
            walker.walk(fileAnalyzer);
        }
    }
}
