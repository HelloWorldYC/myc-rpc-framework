package github.myc.utils.concurrent.threadpool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 创建 ThreadPool 线程池工具类
 */
@Slf4j
public final class ThreadPoolFactoryUtil {

    /**
     * 保存不同类型的线程池
     * 通过 threadNamePrefix 来区分不同线程池（我们可以把相同 threadNamePrefix 的线程池看作是为同一业务场景服务）
     * key：threadNamePrefix
     * value：threadPool
     */
    private static final Map<String, ExecutorService> THREAD_POOLS = new ConcurrentHashMap<>();

    private ThreadPoolFactoryUtil(){
    }

    public static ExecutorService createCustomThreadPoolIfAbsent(String threadNamePrefix){
        CustomThreadPoolConfig customThreadPoolConfig = new CustomThreadPoolConfig();
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);
    }

    public static ExecutorService createCustomThreadPoolIfAbsent(String threadNamePrefix, CustomThreadPoolConfig customThreadPoolConfig){
        return createCustomThreadPoolIfAbsent(customThreadPoolConfig, threadNamePrefix, false);
    }

    public static ExecutorService createCustomThreadPoolIfAbsent(CustomThreadPoolConfig customThreadPoolConfig, String threadNamePrefix, Boolean daemon){
        // 根据 threadNamePrefix 查看是否有对应的线程池，如果存在，就取出对应的线程池，如果没有，则创建一个线程池放入其中并返回该线程池
        ExecutorService threadPool = THREAD_POOLS.computeIfAbsent(threadNamePrefix, k -> createThreadPool(customThreadPoolConfig, threadNamePrefix, daemon));
        // 如果 threadPool 被 shutdown 的话就重新创建一个
        if(threadPool.isShutdown() || threadPool.isTerminated()){
            THREAD_POOLS.remove(threadNamePrefix);
            threadPool = createThreadPool(customThreadPoolConfig, threadNamePrefix, daemon);
            THREAD_POOLS.put(threadNamePrefix, threadPool);
        }
        return threadPool;
    }

    /**
     * 通过 ThreadFactory 创建线程池
     * @param customThreadPoolConfig    线程池相关配置
     * @param threadNamePrefix          线程池前缀
     * @param daemon                    是否为守护线程
     * @return
     */
    private static ExecutorService createThreadPool(CustomThreadPoolConfig customThreadPoolConfig, String threadNamePrefix, Boolean daemon){
        ThreadFactory threadFactory = createThreadFactory(threadNamePrefix, daemon);
        return new ThreadPoolExecutor(customThreadPoolConfig.getCorePoolSize(), customThreadPoolConfig.getMaximumPoolSize(),
                customThreadPoolConfig.getKeepAliveTime(), customThreadPoolConfig.getUnit(), customThreadPoolConfig.getWorkQueue(),
                threadFactory);
    }

    /**
     * 创建 ThreadFactory，如果 threadNamePrefix 不为空则使用自建的 ThreadFactory，否则使用 defaultThreadFactory
     * @param threadNamePrefix  作为创建的线程名字的前缀
     * @param daemon            指定是否为 Daemon Thread (守护线程)
     * @return ThreadFactory
     */
    public static ThreadFactory createThreadFactory(String threadNamePrefix, Boolean daemon){
        if(threadNamePrefix != null){
            if(daemon != null) {
                return new ThreadFactoryBuilder().setNameFormat(threadNamePrefix + "-%d")
                        .setDaemon(daemon).build();
            } else {
                return new ThreadFactoryBuilder().setNameFormat(threadNamePrefix + "-%d").build();
            }
        }
        return Executors.defaultThreadFactory();
    }

    /**
     * 关闭所有线程池
     */
    public static void shutDownAllThreadPool(){
        log.info("call shutDownAllThreadPool method");
        // 使用 parallelStream() 可以将集合视图转换为并行流，从而在处理键值对时可以并行执行操作，提高处理效率。
        THREAD_POOLS.entrySet().parallelStream().forEach(entry -> {
            ExecutorService executorService = entry.getValue();
            executorService.shutdown();
            log.info("shut down thread pool [{}] [{}]", entry.getKey(), executorService.isTerminated());
            try {
                // awaitTermination() 方法会阻塞当前线程，直到执行器服务的所有任务都已经执行完成或者达到超时时间。
                // 这里调用 awaitTermination 方法，当且仅当线程池关闭了才为 true，若达到时间还没关闭则抛出异常
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e){
                log.error("Thread pool never terminated");
                executorService.shutdown();
            }
        });
    }

    /**
     * 打印线程池的状态
     * @param threadPool 线程池对象
     */
    public static void printThreadPoolStatus(ThreadPoolExecutor threadPool) {
        // ScheduledThreadPoolExecutor 是 ThreadPoolExecutor 的子类，专门用于执行定时任务的线程池。
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, createThreadFactory("print-thread-pool-status", false));
        // 该定时任务以固定速率执行任务，initialDelay 参数表示初始延迟时间，period 参数表示连续任务之间的时间间隔
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            log.info("============ThreadPool Status===========");
            log.info("ThreadPool Size: [{}]", threadPool.getPoolSize());
            log.info("Active Threads: [{}]", threadPool.getActiveCount());
            log.info("Number of Tasks: [{}]", threadPool.getCompletedTaskCount());
            log.info("Number of Tasks in Queue: [{}]", threadPool.getQueue().size());
            log.info("========================================");
        }, 0, 1, TimeUnit.SECONDS);
    }
}
