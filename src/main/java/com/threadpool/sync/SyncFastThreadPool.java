package com.threadpool.sync;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncFastThreadPool {
    /**
     * the real job thread
     */
    private ThreadPoolExecutor executor;
    /**
     * this map is used to let the rejected job return to the sync thread
     */
    private ConcurrentHashMap<String, LinkedBlockingQueue<SyncFastThreadCallableJob>> rejectQueueMap
            = new ConcurrentHashMap<>();
    private static Field callableField = null;
    private static Field taskField = null;
    SyncFastThreadPool(int coreThread, int maxThread, long keepAliveMinutes) {
        executor = new ThreadPoolExecutor(coreThread, maxThread, keepAliveMinutes,
                TimeUnit.MINUTES, new LinkedBlockingDeque<>(1), new MayRejectThreadFactory(), (r, executor) -> {
                    SyncFastThreadCallableJob runnableJob;
                    if(r instanceof FutureTask) {
                        FutureTask f = (FutureTask) r;
                        try {
                            if(callableField == null) {
                                callableField = f.getClass().getDeclaredField("callable");
                                callableField.setAccessible(true);
                            }
                            Object io1 = callableField.get(f);                            runnableJob = (SyncFastThreadCallableJob) io1;
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                            r.run();
                            return;
                        }
                    } else {
                        runnableJob = (SyncFastThreadCallableJob) r;
                    }
                    rejectQueueMap.get(runnableJob.getName()).add(runnableJob);
                });
    }

    /**
     *
     * @param runnable runnable job with thread name
     * @param rejectQueue if runnable job is rejected, it will return to this rejectQueue
     * @return future
     */
    Future<?> submit(SyncFastThreadCallableJob runnable, LinkedBlockingQueue<SyncFastThreadCallableJob> rejectQueue) {
        rejectQueueMap.put(Thread.currentThread().getName(), rejectQueue);
        return executor.submit(runnable);
    }

    /**
     * is this executor available to run jobs
     * @return
     */
    boolean isAvailable() {
        return executor.getActiveCount() < executor.getMaximumPoolSize();
    }

    private static class MayRejectThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "SyncFastThread-"+threadNumber);
        }
    }

    public static void shutdown() {
        threadPoolExecutor.executor.shutdown();
    }

    /**
     * such like g1 threads number calculation
     */
    static int count = 3 + Runtime.getRuntime().availableProcessors() / 8 * 2;
    private static final SyncFastThreadPool threadPoolExecutor = new SyncFastThreadPool(count, count, 10);

    static SyncFastThreadPool getPool() {
        return threadPoolExecutor;
    }

}
