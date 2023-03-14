package com.threadpool.sync;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class SyncFastThreadCallableJob<T> implements Callable<T> {

    /**
     * init job thread name
     */
    private String name;
    /**
     * the real job
     */
    private Callable<T> job;
    /**
     * to stop the sync list
     */
    private CountDownLatch counter;

    public String getName() {
        return this.name;
    }

    private final List<T> returnList;

    /**
     * to set the current user to the thread local verb
     */
    SyncFastThreadCallableJob(Callable<T> job, CountDownLatch counter, List<T> returnList) {
        this.name = Thread.currentThread().getName();
        this.job = job;
        this.counter = counter;
        this.returnList = returnList;
    }

    @Override
    public T call() throws Exception {
        try {
            T result = job.call();
            returnList.add(result);
            return result;
        } finally {
            counter.countDown();
        }
    }


}
