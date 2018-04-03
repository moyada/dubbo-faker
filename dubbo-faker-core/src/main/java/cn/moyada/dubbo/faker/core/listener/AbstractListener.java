package cn.moyada.dubbo.faker.core.listener;

import cn.moyada.dubbo.faker.core.convert.LoggingConvert;
import cn.moyada.dubbo.faker.core.manager.FakerManager;
import cn.moyada.dubbo.faker.core.model.InvokeFuture;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import static cn.moyada.dubbo.faker.core.common.Constant.NANO_PER_MILLIS;

/**
 * 监听器
 * @author xueyikang
 * @create 2018-03-18 17:12
 */
public abstract class AbstractListener implements ListenerAction {

    protected final ExecutorService excutor;
    protected final LongAdder count;

    protected final LoggingConvert convert;

    protected final FakerManager fakerManager;

    protected final long total;

    protected final Queue<InvokeFuture> futureQueue;

    protected AbstractListener(int poolSize, int maxPoolSize, long total, String fakerId, int invokeId, FakerManager fakerManager,
                               boolean saveResult, String resultParam) {
        this.excutor = new ThreadPoolExecutor(poolSize, maxPoolSize, 5L, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        this.count = new LongAdder();
        this.fakerManager = fakerManager;
        this.convert = new LoggingConvert(fakerId, invokeId, saveResult, resultParam);
        this.total = total;
        this.futureQueue = new LinkedBlockingQueue<>();
    }

    /**
     * 记录
     * @param result
     */
    public void record(InvokeFuture result) {
        futureQueue.offer(result);
    }

    public void shutdownDelay() {
        // 是否全部记录完了
        while (count.longValue() != total) {
            LockSupport.parkNanos(NANO_PER_MILLIS);
        }
        this.excutor.shutdown();
    }
}
