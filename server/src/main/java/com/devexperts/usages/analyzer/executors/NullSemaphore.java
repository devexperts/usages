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
package com.devexperts.usages.analyzer.executors;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Acts just like {@link Semaphore} with infinite permission number
 */
public class NullSemaphore extends Semaphore {
    public NullSemaphore() {
        super(0);
    }

    @Override
    public void acquire() throws InterruptedException {
    }

    @Override
    public void acquireUninterruptibly() {
    }

    @Override
    public boolean tryAcquire() {
        return true;
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public void release() {
    }

    @Override
    public void acquire(int permits) throws InterruptedException {
    }

    @Override
    public void acquireUninterruptibly(int permits) {
    }

    @Override
    public boolean tryAcquire(int permits) {
        return true;
    }

    @Override
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public void release(int permits) {
    }

    @Override
    public int availablePermits() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int drainPermits() {
        return 0;
    }

    @Override
    protected void reducePermits(int reduction) {
    }

    @Override
    public boolean isFair() {
        return false;
    }

    @Override
    protected Collection<Thread> getQueuedThreads() {
        return Collections.emptyList();
    }
}
