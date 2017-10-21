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
package com.devexperts.usages.analyzer.walker.info;

import java.io.IOException;
import java.io.InputStream;

abstract class FileInfoBase implements FileInfo {
    public abstract String getName();

    public abstract String getPath();

    public String getSuffix() {
        String name = getName();
        int i = name.lastIndexOf('.');
        return i == -1 ? "" : name.substring(i);
    }

    public String getExtension() {
        String name = getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    public String getBaseName() {
        String name = getName();
        int i = name.lastIndexOf('.');
        return i == -1 ? name : name.substring(0, i);
    }

    public abstract InputStream openInputStream() throws IOException;

}
