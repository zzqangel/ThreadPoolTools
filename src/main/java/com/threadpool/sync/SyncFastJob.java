package com.threadpool.sync;

import java.util.List;
import java.util.concurrent.*;

/**
 * this class is using in doing some batch jobs with the available computing capability
 * the current user has been set to thread local context
 * the worst result is to do batch jobs in current thread
 * and for the most time, u will run your batch jobs much faster
 *
 * the default number of threads in the pool is 3 + 5/8 * systemProcessNumber
 * just equals to the gc threads number
 * may be it will be changed some days later
 *
 * @Since 8.2
 */

public class SyncFastJob<T> {

    private final LinkedBlockingQueue<SyncFastThreadCallableJob> rejectJobList = new LinkedBlockingQueue<>();

    private final List<T> returnList = new CopyOnWriteArrayList<>();

    private CountDownLatch countDown;

    private String threadName = null;
    public SyncFastJob(List<? extends Callable<T>> callables) {
        if(callables == null) return;
        countDown = new CountDownLatch(callables.size());
        for (Callable<T> runnAble : callables) {
            rejectJobList.add(new SyncFastThreadCallableJob(runnAble, countDown, returnList, SyncFastThreadPool.defaultThreadName));
        }
    }

    SyncFastJob(List<? extends Callable<T>> callables, String threadName) {
        if(callables == null) return;
        countDown = new CountDownLatch(callables.size());
        for (Callable<T> runnAble : callables) {
            rejectJobList.add(new SyncFastThreadCallableJob(runnAble, countDown, returnList, threadName));
        }
        this.threadName = threadName;
    }

    public List<T> doJobsNow() {
        SyncFastThreadPool syncFastThreadPool;
        if(threadName == null) {
            syncFastThreadPool = SyncFastThreadPool.getPool();
        } else {
            syncFastThreadPool = SyncFastThreadPool.getPrivatePool(threadName);
        }
        while(!rejectJobList.isEmpty()) {
            try {
                SyncFastThreadCallableJob runnable = rejectJobList.take();
                if (syncFastThreadPool.isAvailable()) {
                    syncFastThreadPool.submit(runnable, rejectJobList);
                } else {
                    runnable.call();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            countDown.await(20, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            //如果20分钟没有执行完，会抛出异常，停止等待，以释放内存
            e.printStackTrace();
        }
        return returnList;
    }

    public static <T> List<T> doJobsNow(List<? extends Callable<T>> callables) {
        return  new SyncFastJob<>(callables).doJobsNow();
    }

}
