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

import com.devexperts.usages.analyzer.walker.FileAnalyzer;
import com.devexperts.usages.analyzer.walker.info.FileInfo;
import com.google.common.collect.Multimap;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

class MainAnalyzer implements FileAnalyzer<FileInfo> {
    private static final Logger logger = Logger.getLogger(MainAnalyzer.class);

    private final Cache cache;
    private final Config config;
    private final Multimap<String, Processor> processors;

    public MainAnalyzer(Cache cache, Config config, Multimap<String, Processor> processors) {
        this.cache = cache;
        this.config = config;
        this.processors = processors;
    }

    @Override
    public void process(FileInfo fileInfo) throws IOException {
        String className = pathToClassName(fileInfo.getBaseName());
        if (config.excludesClassName(className))
            return;

        Collection<Processor> curProcessors = this.processors.get(fileInfo.getSuffix());
        if (!curProcessors.isEmpty()) {
//            logger.info("Processing " + fileInfo.getPath());
            for (Processor processor : curProcessors) {
                InputStream in = fileInfo.openInputStream();
                try {
                    processor.process(className, in);
                } finally {
                    in.close();
                }
            }
        }
    }

    private String pathToClassName(String path) {
        return cache.resolveString(path.replace('/', '.'));
    }
}
