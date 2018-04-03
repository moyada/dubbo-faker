package cn.moyada.dubbo.faker.core.listener;

import cn.moyada.dubbo.faker.core.common.Switch;
import cn.moyada.dubbo.faker.core.manager.FakerManager;
import cn.moyada.dubbo.faker.core.model.InvokeFuture;
import cn.moyada.dubbo.faker.core.model.domain.LogDO;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static cn.moyada.dubbo.faker.core.common.Constant.NANO_PER_MILLIS;

/**
 * 批量保存结果监听器
 * @author xueyikang
 * @create 2018-03-18 17:32
 */
public class BatchLoggingListener extends AbstractListener {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(BatchLoggingListener.class);

    private final List<LogDO> list1;
    private final List<LogDO> list2;

    private final Switch lock;
    private volatile boolean stop;

    public BatchLoggingListener(String fakerId, int invokeId, long total, FakerManager fakerManager,
                                boolean saveResult, String resultParam) {
        super(1, 1, total, fakerId, invokeId, fakerManager, saveResult, resultParam);
        Long r = total / 1000;
        int num = r == 0 ? 1: r.intValue();
        this.list1 = new ArrayList<>(1000 * num);
        this.list2 = new ArrayList<>(1000 * num);
        this.lock = new Switch(true);
        this.stop = false;
        this.excutor.submit(new Converter());
        new Thread(new Logger()).start();
    }

    public void shutdownDelay() {
        super.shutdownDelay();
        this.stop = true;
    }

    @Override
    public void shutdownNow() {
        for (;;) {
            LockSupport.parkNanos(1_000 * NANO_PER_MILLIS);
            if(list1.isEmpty() && list2.isEmpty()) {
                excutor.shutdownNow();
                break;
            }
        }
    }

    class Converter implements Runnable {

        @Override
        public void run() {
            for (;;) {
                InvokeFuture future = futureQueue.poll();
                if(null == future) {
                    LockSupport.parkNanos(1_000 * NANO_PER_MILLIS);
                    continue;
                }
                count.increment();
                if (lock.isOpen()) {
                    list1.add(convert.convertToLog(future));
                } else {
                    list2.add(convert.convertToLog(future));
                }
            }
        }
    }

    class Logger implements Runnable {

        @Override
        public void run() {
            for (;;) {
                // 每秒批量保存一次
                LockSupport.parkNanos(1_000 * NANO_PER_MILLIS);
                if (lock.close()) {
                    save(list1);
                }
                else if (lock.open()) {
                    save(list2);
                }

                if(stop) {
                    break;
                }
            }
        }

        private void save(List<LogDO> logDOs) {
            if(logDOs.isEmpty()) {
                return;
            }
            log.info("batch save invoke result: " + convert.getFakerId() + ", size: " + logDOs.size());
            fakerManager.saveLog(logDOs);
            logDOs.clear();
        }
    }
}
