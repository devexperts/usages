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

import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Some methods of io.Files that java 6 lacks of
 */
public class FileUtils {
    private static final String tempFormat = "%s.%d.tmp";

    public static File createTempFile(String file) throws IOException {
        Random rand = new Random();
        while (true) {
            try {
                File tempFile = new File(String.format(tempFormat, file, Math.abs(rand.nextInt())));
                if (tempFile.createNewFile())
                    return tempFile;
            } catch (IOException e) {
                throw new IOException("Cannot create temp file for " + file);
            }
        }
    }

    public static File createTempFile(File file) throws IOException {
        return createTempFile(file.toString());
    }
}
