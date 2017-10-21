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
 * Delegating which appears not at the end of the chain.
 *
 * @param <K> type of fileInfo which the walker passes to next walker
 * @param <V> type of fileInfo directly passed to next layer by next walker
 * @param <I> type of fileInfo which would be passed to processor at last layer
 */
public interface MidDelegating<K extends FileInfo, V extends FileInfo, I extends FileInfo> extends Delegating<K, I> {
    DelegatingWalker<? extends V, I> makeDelegate(K info);
}
