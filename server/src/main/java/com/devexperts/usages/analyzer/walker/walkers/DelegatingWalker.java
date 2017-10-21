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

import com.devexperts.usages.analyzer.walker.info.FileInfo;

/**
 * Can be used at passing (left) side of link in delegation chain.
 *
 * @param <T> type of fileInfo which it provides directly to lower layer
 * @param <F> type of fileInfo which fileAnalyzer accepts
 */
public abstract class DelegatingWalker<T extends FileInfo, F extends FileInfo> implements Walker<F> {
    protected final Delegating<? super T, ? extends F> delegating;

    public DelegatingWalker(Delegating<? super T, ? extends F> delegating) {
        this.delegating = delegating;
    }

}
