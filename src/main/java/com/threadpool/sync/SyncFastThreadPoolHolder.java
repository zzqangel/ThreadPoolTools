package com.threadpool.sync;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * this class is to offer a private sync thread pool not global
 */
public class SyncFastThreadPoolHolder implements Closeable {

    private final String threadPoolName;

    /**
     * you can let this class be a static field to reuse
     * If it's a local field, let it init in try-with-resources
     * @param threadPoolName the name of this thread pool, don't let it be conflicted and blank
     * @param threadPoolSize the size of this thread pool. The core size is equals to the max size
     */
    public SyncFastThreadPoolHolder(String threadPoolName, int threadPoolSize) {
        if(threadPoolName == null || threadPoolName.trim().length() == 0) {
            throw new RuntimeException("thread pool name is not valid");
        }
        if(threadPoolSize < 0 || threadPoolSize > (3 + Runtime.getRuntime().availableProcessors() * 5 /8)) {
            throw new RuntimeException("threadPoolSize is not valid");
        }
        SyncFastThreadPool.register(threadPoolName, threadPoolSize);
        this.threadPoolName = threadPoolName;
    }

    /**
     * to fire the jobs
     * @param callableList jobs list
     * @return the return of these jobs
     * @param <T>
     */
    public <T> List<T> doJobsNow(List<? extends Callable<T>> callableList) {
        return new SyncFastJob<>(callableList, threadPoolName).doJobsNow();
    }

    /**
     * If you want to close this pool, do with close method
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        SyncFastThreadPool.shutdown(this.threadPoolName);
    }
}
