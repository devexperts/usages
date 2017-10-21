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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SimpleUsagesKeeper implements UsagesKeeper {
    private final Map<String, ClassUsages> usages;
    private final Cache cache;

    public SimpleUsagesKeeper(Map<String, ClassUsages> container, Cache cache) {
        this.usages = container;
        this.cache = cache;
    }

    public SimpleUsagesKeeper(Cache cache) {
        this(new HashMap<String, ClassUsages>(), cache);
    }

    @Override
    public ClassUsages get(String className) {
        return usages.get(className);
    }

    @Override
    public ClassUsages getOrEmpty(String className) {
        ClassUsages result = usages.get(className);
        if (result == null) {
            if (className.endsWith("]") || className.endsWith(";") || className.contains("/"))
                throw new AssertionError("Not a class name: " + className);
            usages.put(cache.resolveString(className), result = new ClassUsages(cache, className));
        }
        return result;
    }

    @Override
    public Set<Map.Entry<String, ClassUsages>> allClassUsages() {
        return usages.entrySet();
    }
}
