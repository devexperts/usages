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

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Usage {
    private UseKind useKind;
    private String fileName;
    private int lineNumber;

    public Usage(UseKind useKind, String fileName, int lineNumber) {
        this.useKind = useKind;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public UseKind getUseKind() {
        return useKind;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Usage usage = (Usage) o;

        if (lineNumber != usage.lineNumber) {
            return false;
        }
        if (useKind != usage.useKind) {
            return false;
        }
        return fileName != null ? fileName.equals(usage.fileName) : usage.fileName == null;
    }

    @Override
    public int hashCode() {
        int result = useKind != null ? useKind.hashCode() : 0;
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        result = 31 * result + lineNumber;
        return result;
    }

    public static void printUsages(PrintWriter out, Set<Usage> useKinds) {
        out.print(Fmt.USE_KINDS_PREFIX);
        boolean firstKind = true;
        for (Usage usage : useKinds) {
            if (firstKind)
                firstKind = false;
            else
                out.print(Fmt.USE_KINDS_SEPARATOR);
            out.print(usage.getUseKind() + "(" + usage.getFileName() + ":" + usage.getLineNumber() + ")");
        }
    }

    public static Set<Usage> parseUsages(String s) {
        Set<Usage> usages = new HashSet<>();
        StringTokenizer st = new StringTokenizer(s, Fmt.USE_KINDS_SEPARATOR);
        while (st.hasMoreTokens()) {
            Pattern pattern = Pattern.compile("(\\w+)\\(([^():]*):(-?\\d+)\\)");
            Matcher matcher = pattern.matcher(st.nextToken());
            if (matcher.matches()) {
                UseKind useKind = UseKind.valueOf(matcher.group(1));
                String fileName = matcher.group(2);
                int lineNumber = Integer.parseInt(matcher.group(3));
                usages.add(new Usage(useKind, fileName, lineNumber));
            }
        }
        return usages;
    }
}
