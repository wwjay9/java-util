package com.wwj.util.java;

import com.wwj.util.java.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 线程工具类
 *
 * @author wwj
 */
@Slf4j
@SuppressWarnings("unused")
public class ThreadUtil {

    private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool(POOL_SIZE);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!FORK_JOIN_POOL.isShutdown()) {
                FORK_JOIN_POOL.shutdown();
            }
            log.info("ThreadUtil线程池已被关闭");
        }));
    }

    private ThreadUtil() {
    }

    /**
     * 使用ForkJoinPool线程池来执行批量操作
     *
     * @param list     任务列表
     * @param consumer 任务执行
     */
    public static <T> void forkJoinTasks(List<T> list, Consumer<T> consumer) {
        forkJoinTasks(list, t -> {
            consumer.accept(t);
            return t;
        });
    }

    /**
     * 使用ForkJoinPool线程池来执行批量操作
     *
     * @param list   任务列表
     * @param mapper 任务转换
     * @return 任务结果
     */
    public static <T, R> List<R> forkJoinTasks(Collection<T> list, Function<T, R> mapper) {
        return forkJoinTasks(list, FORK_JOIN_POOL, mapper);
    }

    /**
     * 使用ForkJoinPool线程池来执行批量操作，该方法谨慎使用，频繁创建线程池影响性能
     *
     * @param list        任务列表
     * @param parallelism 线程并发数
     * @param mapper      任务转换
     * @return 任务结果
     */
    public static <T, R> List<R> forkJoinTasks(Collection<T> list, int parallelism, Function<T, R> mapper) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(parallelism);
        try {
            return forkJoinTasks(list, forkJoinPool, mapper);
        } finally {
            forkJoinPool.shutdown();
        }
    }

    /**
     * 使用ForkJoinPool线程池来执行批量操作，此方法使用一个技巧：Stream的parallelStream方法默认使用的是ForkJoinPool.commonPool线程池
     * 该线程池的大小默认是cpu的核心数，当执行IO密集型任务时可以自定义一个ForkJoinPool，当ForkJoinPool执行任务时检测到自己已经处于
     * ForkJoinPool的线程中时不会去使用公共的线程池，转而使用自定义的ForkJoinPool
     *
     * @param list         任务列表
     * @param forkJoinPool 自定义的ForkJoinPool
     * @param mapper       任务转换
     * @return 任务结果
     * @see ForkJoinPool#commonPool
     * @see ForkJoinTask#fork
     */
    public static <T, R> List<R> forkJoinTasks(Collection<T> list, ForkJoinPool forkJoinPool, Function<T, R> mapper) {
        try {
            return forkJoinPool.submit(() -> list.parallelStream().map(mapper).collect(Collectors.toList())).get();
        } catch (InterruptedException e) {
            // 标记当前线程已被打断，保持线程打断信息
            Thread.currentThread().interrupt();
            log.error("任务执行失败:", e);
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            log.error("任务执行失败:", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * 将List拆分成多个大小为1000的子List，多线程转换处理
     *
     * @param list      原始List
     * @param convertor 转换器
     * @return 转换后的List
     */
    public static <T, R> List<R> parallelConvert(List<T> list, Function<List<T>, List<R>> convertor) {
        return parallelConvert(1000, list, convertor);
    }

    /**
     * 将List拆分成多个子List，多线程转换处理
     *
     * @param size      每个子List的大小
     * @param list      原始List
     * @param convertor 转换器
     * @return 转换后的List
     */
    public static <T, R> List<R> parallelConvert(int size, List<T> list, Function<List<T>, List<R>> convertor) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        // 对原始List进行去重，防止产生重复数据
        List<T> distinctList = new ArrayList<>(new LinkedHashSet<>(list));
        if (distinctList.size() <= size) {
            return convertor.apply(distinctList);
        }
        return BeanUtil.listPartition(distinctList, size).parallelStream()
                .map(convertor)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
