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

import org.apache.log4j.Logger;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Attempts to perform a pack of actions. To execute an action, first create a <tt>slave</tt>, then use its
 * <tt>submit</tt> method to perform actions. After all actions submitted, invoke <tt>await</tt>, <tt>unsafeAwait</tt>
 * or <tt>complete</tt> to wait for execution completion, or <tt>finish</tt> to just free Slave as soon as it complete
 * all tasks.
 * <p/>
 * All slaves share a same thread pool, but await for only their own actions.
 * <p/>
 * By default, if action failed, it will be repeated in at least <tt>repetitionDelay</tt> milliseconds. If second
 * execution failed, <tt>await</tt>, <tt>unsafeAwait</tt> and <tt>complete</tt> throw. Exact scenarios description see
 * in <tt>await</tt>.
 * <p/>
 * This behaviour may be changed, see <tt>InsistentExecutor.Action</tt> for details.
 * <p/>
 * Once all actions are performed and <tt>await</tt> or similar method is invoked, <tt>submit</tt> becomes to do
 * nothing. So, submitting an action after <tt>await</tt> or similar method was invoked will not grantee its execution
 * (excluding a case when action is submitted inside another action).
 */
public class InsistentExecutor {
    private static final Logger logger = Logger.getLogger(InsistentExecutor.class);

    public static final int DEFAULT_THREAD_POOL_SIZE = 10;
    public static final int THREAD_NUM_PER_SLAVE = 2;

    public static final long SLEEP_TIME_THRESHOLD_FOR_LOGGING = TimeUnit.SECONDS.toMillis(5);
    public static final int DEFAULT_REPETITION_DELAY = (int) TimeUnit.SECONDS.toMillis(1);

    private final ThreadPoolExecutor executorService;
    private final int repetitionDelay;

    private final Semaphore activeSlaves;

    private InsistentExecutor(ThreadPoolExecutor executorService, int repetitionDelay, int maxSlaveNum) {
        this.executorService = executorService;
        executorService.setThreadFactory(new DaemonThreadFactory());

        this.repetitionDelay = repetitionDelay;
        activeSlaves = new Semaphore(maxSlaveNum);
    }

    public InsistentExecutor(int maximumPoolSize, int repetitionDelay, int maxSlaveNum) {
        this(
                new ThreadPoolExecutor(
                        maximumPoolSize + THREAD_NUM_PER_SLAVE * maxSlaveNum,
                        maximumPoolSize + THREAD_NUM_PER_SLAVE * maxSlaveNum,
                        0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingDeque<Runnable>()
                ),
                repetitionDelay,
                maxSlaveNum
        );
    }

    public InsistentExecutor(int threadNum, int repetitionDelay) {
        this(threadNum, repetitionDelay, 1);
    }

    public InsistentExecutor(int threadNum) {
        this(threadNum, DEFAULT_REPETITION_DELAY);
    }

    public InsistentExecutor() {
        this(DEFAULT_THREAD_POOL_SIZE);
    }


    public Slave newSlave() {
        return new Slave();
    }

    public Slave newSlave(int availableThreads) {
        return new Slave(availableThreads);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    private static final AtomicInteger working = new AtomicInteger();



    /**
     * Allows submitting tasks and wait until all of them are finished
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public class Slave {
        private final AtomicBoolean awaited = new AtomicBoolean(false);

        /**
         * At any time equals to (submitted task number - finished task number + !awaited)
         */
        private final AtomicInteger activeMeter = new AtomicInteger(1);

        /**
         * Gate opens when activeMeter == 0, hence awaiting threads get released
         */
        private CountDownLatch finished = new CountDownLatch(1);

        /**
         * Not null if slave completely failed
         */
        private AtomicReference<Throwable> actionException = new AtomicReference<Throwable>();

        private final ActionSubmitter actionFirstQueue;
        private final ActionSubmitter actionsRepetitionQueue;

        /**
         * Limits number of executing actions
         */
        private final Semaphore activeActionsSemaphore;

        private Slave(int activeNum) {
            this(new Semaphore(activeNum));
        }

        private Slave() {
            this(new NullSemaphore());
        }

        private Slave(Semaphore activeActionsSemaphore) {
            this.activeActionsSemaphore = activeActionsSemaphore;
            activeSlaves.acquireUninterruptibly();
            actionFirstQueue = new ActionSubmitter(this.activeActionsSemaphore);
            actionsRepetitionQueue = new ActionSubmitter(this.activeActionsSemaphore);
        }

        public void submit(Action action) {
            if (actionException.get() == null) {
                activeMeter.incrementAndGet();
                actionFirstQueue.submit(new ActionRunnable(action, true, 0));
            }
        }

        private void decrementActiveMeter() {
            if (activeMeter.decrementAndGet() == 0) {
                actionFirstQueue.stop();
                actionsRepetitionQueue.stop();
                activeSlaves.release();

                finished.countDown();
            }
        }

        public void finish() {
            if (awaited.compareAndSet(false, true))
                decrementActiveMeter();
        }

        public void await() throws InterruptedException, ActionExecutionException {
            finish();
            try {
                finished.await();
            } catch (InterruptedException e) {
                if (!actionException.compareAndSet(null, new CallerInterruptedException(e))) {
                    // when not throwing InterruptedException, set interrupted flag
                    Thread.currentThread().interrupt();
                }
            }

            Throwable e = this.actionException.get();
            if (e == null)
                return;

            if (e instanceof CallerInterruptedException)
                throw ((CallerInterruptedException) e).getCause();

            if (e instanceof Exception)
                throw new ActionExecutionException((Exception) e);

            if (e instanceof Error)
                throw (Error) e;

            else
                throw new RuntimeException("Throwable in action occurred", e);

        }

        public void awaitUninterruptedly() throws ActionExecutionException {
            try {
                await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void complete() throws ActionExecutionException, InterruptedException {
            try {
                await();
            } finally {
                InsistentExecutor.this.shutdown();
            }
        }

        public InsistentExecutor getFactory() {
            return InsistentExecutor.this;
        }

        private class ActionRunnable implements Runnable {
            private final Action action;
            private final boolean firstTime;
            private final long executeTime;

            public ActionRunnable(Action action, boolean firstTime, long executeTime) {
                this.action = action;
                this.firstTime = firstTime;
                this.executeTime = executeTime;
            }

            @Override
            public void run() {
                try {
                    if (actionException.get() != null) {
                        decrementActiveMeter();
                        return;
                    }
                    try {
                        if (finished.getCount() != 0) {
                            action.run();
                            decrementActiveMeter();
                        }
                    } catch (Exception e) {
                        //noinspection unchecked
                        if (firstTime) {
                            action.onFirstFail(e);
                            actionsRepetitionQueue.submit(new ActionRunnable(action, false, System.currentTimeMillis() + repetitionDelay));
                        } else {
                            action.onRepeatedFail(e);
                            decrementActiveMeter();
                        }
                    }
                } catch (Throwable e) {
                    actionException.compareAndSet(null, e);
                    decrementActiveMeter();
                } finally {
                    activeActionsSemaphore.release();
                }
            }

        }

        /**
         * Submits actions for execution, waiting if necessary
         */
        private class ActionSubmitter implements Runnable {
            private final BlockingDeque<ActionRunnable> queue = new LinkedBlockingDeque<ActionRunnable>();

            private final Semaphore semaphore;

            private final Future<?> future;

            public ActionSubmitter(Semaphore semaphore) {
                this.semaphore = semaphore;
                future = executorService.submit(this);
            }

            public void submit(ActionRunnable actionRunnable) {
                queue.add(actionRunnable);
            }

            public void stop() {
                future.cancel(true);
            }

            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Slave.ActionRunnable action = queue.take();

                        long remainingTime = action.executeTime - System.currentTimeMillis();
                        if (remainingTime > 0) {
                            if (remainingTime > SLEEP_TIME_THRESHOLD_FOR_LOGGING) {
                                logger.info(String.format("Next repetition in %.1f sec.", remainingTime / 1e3));
                            }
                            Thread.sleep(remainingTime);
                        }

                        semaphore.acquire();
                        executorService.submit(action);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                working.decrementAndGet();
            }
        }

    }


    /**
     * Action to perform.
     */
    public abstract static class Action {

        /**
         * Performed operation.
         */
        public abstract void run() throws Exception;

        /**
         * Invoked when action failed at first time. It is preferably to override this method with more particular
         * operation. You may throw from this method to stop processing remaining actions, exception will be rethrown
         * from <tt>await</tt> method.
         *
         * @param e exception thrown by action
         */
        public void onFirstFail(Exception e) throws Exception {
            logger.warn("Action failed, going to launch again later", e);
        }

        /**
         * Invoked when action failed again. It is preferably to override this method with more particular operation.
         * Possibly you would like to throw an exception here, it will be rethrown by <tt>await</tt> method and
         * remaining actions will not be performed. Otherwise, <tt>await</tt> will complete normally.
         *
         * @param e exception thrown by action
         */
        public void onRepeatedFail(Exception e) throws Exception {
            throw e;
        }

    }
}
