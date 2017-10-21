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
 * This class facilitates Delegating's creation.
 *
 * @param <K> available info types which can be passed to Delegating.
 * @param <N> info type which delegate walker directly passes to next layer.
 */
public abstract class Passing<K extends FileInfo, N extends FileInfo> {
    public abstract <F extends FileInfo> MidDelegating<? super K, N, ? extends F> delegateTo(
            Delegating<? super N, F> nextDelegating);

    public MidDelegating<? super K, N, ? extends N> passToProcessor() {
        return delegateTo(TerminalWalker.<N>getDelegating());
    }
}
