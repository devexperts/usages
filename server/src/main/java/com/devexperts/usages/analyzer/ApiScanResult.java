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

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ApiScanResult {
    private static final Logger logger = Logger.getLogger(ApiScanResult.class);

    private final Usages usages;
    private final PublicApi api;

    public ApiScanResult(Usages usages, PublicApi api) {
        this.usages = usages;
        this.api = api;
    }

    public void writeReport(File file) throws IOException {
        logger.info("Writing " + file);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), Fmt.CHARSET));
        try {
            api.writeReportTo(out, usages);
        } finally {
            out.close();
        }
    }
}
