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
 * Visits some files, passing them to analyzer.
 * There is no need to implement this class, implement SimpleWalker or DelegatingWalker instead.
 *
 * @param <T> type of fileInfo which it provides directly to lower layer
 * @param <I> type of fileInfo which fileAnalyzer accepts
 */
public interface Walker<I extends FileInfo> {
    void walk(FileAnalyzer<? super I> fileAnalyzer) throws IOException;
}
