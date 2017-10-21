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

import com.devexperts.usages.analyzer.walker.walkers.FilePackWalker;
import com.devexperts.usages.analyzer.walker.walkers.Walker;
import com.devexperts.usages.analyzer.walker.walkers.ZipRecursiveWalker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Processing {
    private final List<String> allJarFiles = new ArrayList<String>();
    private final List<String> apiJarFiles = new ArrayList<String>();

    public final String resultFileName;

    public Processing(List<String> allJarFiles, List<String> apiJarFiles, String resultFileName) {
        this.allJarFiles.addAll(allJarFiles);
        this.apiJarFiles.addAll(apiJarFiles);
        this.resultFileName = resultFileName;
    }

    public void go() throws IOException {
        Walker allJarWalkers = filesToWalkers(allJarFiles);

        if (apiJarFiles.isEmpty()) {
            new UsagesScanner(allJarWalkers).analyze()
                    .writeReport(new File(resultFileName));
        } else {
            Walker apiJarWalkers = filesToWalkers(apiJarFiles);

            new ApiScanner(allJarWalkers, apiJarWalkers).analyze()
                    .writeReport(new File(Config.getApi()));
        }
    }

    private static Walker filesToWalkers(List<String> files) {
        return FilePackWalker.fromFilenames(files,
                new SmartDirectoriesWalker.D().delegateTo(
                        new ZipRecursiveWalker.D().passToProcessor()));
    }

}
