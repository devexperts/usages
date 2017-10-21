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

import com.devexperts.usages.analyzer.walker.walkers.Walker;
import com.google.common.collect.HashMultimap;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class ApiScanner {
    private static final Logger logger = Logger.getLogger(ApiScanner.class);

    public static final String CLASS_SUFFIX = Constants.CLASS_SUFFIX;

    protected final Cache cache = new Cache();
    protected final Config config = new Config();

    protected final PublicApi api = new PublicApi(cache);

    private final Walker<?> allJarWalker;
    private final Walker<?> apiJarWalker;

    public ApiScanner(Walker allJarWalker, Walker apiJarWalker) {
        this.allJarWalker = allJarWalker;
        this.apiJarWalker = apiJarWalker;
    }

    public ApiScanResult analyze() throws IOException {
        Usages usages = new UsagesScanner(allJarWalker).analyze().getUsages();

//        logger.info("Processing api");

        HashMultimap<String, Processor> processors = HashMultimap.create();
        processors.put(CLASS_SUFFIX, new Api4ClassProcessor());
        apiJarWalker.walk(new MainAnalyzer(cache, config, processors));

        logger.info("Removing uses from api classes");
        usages.removeUsesFromClasses(api.getImplClasses());

        return new ApiScanResult(usages, api);
    }

    private class Api4ClassProcessor implements Processor {
        @Override
        public void process(String className, InputStream in) throws IOException {
            api.parseClass(className, in);
        }
    }
}
