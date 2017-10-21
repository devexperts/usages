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

import com.devexperts.usages.analyzer.tune.SimpleUsagesKeeper;
import com.devexperts.usages.analyzer.tune.UsagesKeeper;
import com.devexperts.usages.analyzer.walker.walkers.Walker;
import com.google.common.collect.HashMultimap;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class UsagesScanner {
    private static final Logger logger = Logger.getLogger(UsagesScanner.class);

    public static final String CLASS_SUFFIX = Constants.CLASS_SUFFIX;
    public static final String USAGES_SUFFIX = Constants.USAGES_SUFFIX;

    protected final Cache cache = new Cache();
    protected final Config config = new Config();

    protected final Usages usages;

    private final Walker<?> walker;

    public UsagesScanner(Walker walker) {
        this.walker = walker;
        this.usages = new Usages(cache, new SimpleUsagesKeeper(cache), config);
    }

    public UsagesScanner(Walker walker, UsagesKeeper usagesKeeper) {
        this.walker = walker;
        this.usages = new Usages(cache, usagesKeeper, config);
    }

    public UsagesScanResult analyze() throws IOException {
//        logger.info("Processing usages");

        HashMultimap<String, Processor> processors = HashMultimap.create();
        processors.put(CLASS_SUFFIX, new Usages4ClassProcessor());
        processors.put(USAGES_SUFFIX, new Usages4UsagesProcessor());
        walker.walk(new MainAnalyzer(cache, config, processors));
        usages.analyze();
        return new UsagesScanResult(usages);
    }

    private class Usages4ClassProcessor implements Processor {
        @Override
        public void process(String className, InputStream in) throws IOException {
            usages.parseClass(className, in);
        }
    }

    private class Usages4UsagesProcessor implements Processor {
        @Override
        public void process(String className, InputStream in) throws IOException {
            usages.setNeedPostprocessing(false);
            usages.getUsagesForClass(className).readFromStream(in);
        }
    }

}
