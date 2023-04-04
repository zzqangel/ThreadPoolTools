package com.threadpool.sync;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncFastThreadPool {
    final static String defaultThreadName = "SyncFastThread-";
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
    SyncFastThreadPool(int coreThread, int maxThread, long keepAliveMinutes, String threadPoolName) {
        executor = new ThreadPoolExecutor(coreThread, maxThread, keepAliveMinutes,
                TimeUnit.MINUTES, new LinkedBlockingDeque<>(1), new MayRejectThreadFactory(threadPoolName), (r, executor) -> {
            SyncFastThreadCallableJob runnableJob;
            if(r instanceof FutureTask) {
                FutureTask f = (FutureTask) r;
                try {
                    if(callableField == null) {
                        callableField = f.getClass().getDeclaredField("callable");
                        callableField.setAccessible(true);
                    }
                    Object io1 = callableField.get(f);
                    runnableJob = (SyncFastThreadCallableJob) io1;
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                    //if get callable fail then invoke it now
                    f.run();
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
        private String name;

        MayRejectThreadFactory(String threadName) {
            this.name = threadName;
        }
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name + threadNumber.getAndIncrement());
        }
    }

    public static void shutdown() {
        threadPoolExecutor.executor.shutdown();
    }

    /**
     * such like g1 threads number calculation
     */
    static int count = 3 + Runtime.getRuntime().availableProcessors() / 8 * 5;
    private static final SyncFastThreadPool threadPoolExecutor = new SyncFastThreadPool(count, count, 10, defaultThreadName);

    static SyncFastThreadPool getPool() {
        return threadPoolExecutor;
    }

    private static final Map<String, SyncFastThreadPool> registerPoolMap = new ConcurrentHashMap<>();

    static SyncFastThreadPool getPrivatePool(String poolRegisterKey) {
        return registerPoolMap.get(poolRegisterKey);
    }

    static boolean register(String poolRegisterKey, int poolSize) {
        SyncFastThreadPool threadPool = registerPoolMap.get(poolRegisterKey);
        if(threadPool == null) {
            synchronized(registerPoolMap) {
                threadPool = registerPoolMap.get(poolRegisterKey);
                if(threadPool == null) {
                    registerPoolMap.put(poolRegisterKey, new SyncFastThreadPool(poolSize, poolSize, 10, poolRegisterKey));
                    return true;
                }
            }
        }
        return false;
    }

    static void shutdown(String poolRegisterKey) {
        SyncFastThreadPool threadPool = registerPoolMap.get(poolRegisterKey);
        if(threadPool != null) {
            threadPool.executor.shutdown();
        }
    }

}
