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
package com.devexperts.usages.analyzer.tune;

import com.devexperts.usages.analyzer.Cache;
import com.devexperts.usages.analyzer.ClassUsages;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AnalyzeSingleUsagesKeeper implements UsagesKeeper {
    private final Map<String, ClassUsages> interestingClasses;
    private final UsagesKeeper keeper;

    public AnalyzeSingleUsagesKeeper(String clazz, Cache cache) {
        this(clazz, cache, new HashMap<String, ClassUsages>());
    }

    public AnalyzeSingleUsagesKeeper(String clazz, Cache cache, Map<String, ClassUsages> container) {
        this.interestingClasses = Collections.singletonMap(clazz, new ClassUsages(cache, clazz));
        this.keeper = new SimpleUsagesKeeper(container, cache);
    }

    @Override
    public ClassUsages get(String clazz) {
        return keeper.get(clazz);
    }

    @Override
    public ClassUsages getOrEmpty(String clazz) {
        return keeper.getOrEmpty(clazz);
    }

    @Override
    public Set<Map.Entry<String, ClassUsages>> allClassUsages() {
        return interestingClasses.entrySet();
    }
}
