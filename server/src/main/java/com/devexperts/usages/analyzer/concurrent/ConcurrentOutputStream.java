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
package com.devexperts.usages.analyzer.concurrent;

import com.devexperts.usages.analyzer.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides opportunity to write concurrently into same file with wait-free guarantee, output from
 * only a single thread will actually be written. File will not be seen before stream is closed.
 * <p/>
 * The main guarantee of this class is the following: when some threads write to the same file via
 * this stream and no one tries to delete it, one of those threads will successfully create and fill
 * the file.
 * <p/>
 * Note: while this stream is opened, some temporal file will exist in directory which
 * contains target file.
 * <p/>
 * Note: instance of this class cannot be used concurrently.
 */
public class ConcurrentOutputStream extends OutputStream implements Closeable {
    private final File targetFile;

    private final File tempFile;

    private final OutputStream os;

    private boolean failedToWrite = false;

    /**
     * Creates a stream. If file already exists, writing to stream have no sense, so
     * it is recommended to check target file presence on disk before calling this
     * constructor.
     *
     * @throws IOException if I/O error occurs
     */
    public ConcurrentOutputStream(String filename) throws IOException {
        this(new File(filename));
    }

    /**
     * Equivalent to calling {@code ConcurrentOutputStream}(file.getPath()).
     */
    public ConcurrentOutputStream(File file) throws IOException {
        targetFile = file;
        tempFile = FileUtils.createTempFile(targetFile.toString());
        os = new FileOutputStream(tempFile);
    }

    @Override
    public void write(byte[] b) throws IOException {
        try {
            os.write(b);
        } catch (IOException e) {
            failedToWrite = true;
            throw e;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            os.write(b, off, len);
        } catch (IOException e) {
            failedToWrite = true;
            throw e;
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            os.write(b);
        } catch (IOException e) {
            failedToWrite = true;
            throw e;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            os.flush();
        } catch (IOException e) {
            failedToWrite = true;
            throw e;
        }
    }

    /**
     * Closes the stream, target file appears at due location.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        try {
            os.close();
            if (!failedToWrite) {
                tempFile.renameTo(targetFile);
            }
        } finally {
            tempFile.delete();
        }
    }
}
