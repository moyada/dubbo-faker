package cn.moyada.dubbo.faker.api.listener;

import cn.moyada.dubbo.faker.api.annotation.Fetch;
import cn.moyada.dubbo.faker.api.common.Context;
import cn.moyada.dubbo.faker.api.domain.RealParamDO;
import cn.moyada.dubbo.faker.api.manager.FakerManager;
import cn.moyada.dubbo.faker.api.utils.JsonUtil;
import com.alibaba.dubbo.rpc.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 参数监听器
 * @author xueyikang
 * @create 2018-03-30 02:44
 */
public class BatchRecordListener {
    private static final Logger log = LoggerFactory.getLogger(BatchRecordListener.class);

    // 参数临时保存空间
    private int capacity = 1000;

    private final Set<RealParamDO> list1;
    private final Set<RealParamDO> list2;

    // 保存位置开关
    private final Switch lock;

    // 最大线程数
    private int maxThread = 10;

    @Autowired
    private FakerManager fakerManager;

    private final ExecutorService excutor;

    public BatchRecordListener() {
        this.excutor = new ThreadPoolExecutor(1, maxThread, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        this.list1 = new HashSet<>(capacity);
        this.list2 = new HashSet<>(capacity);
        this.lock = new Switch(true);
        new Thread(new Consumer()).start();
    }

    public void saveRequest(Class<?> invokerInterface, Invocation invocation) {
        excutor.submit(new Recorder(invokerInterface, invocation));
    }

    /**
     * 保存参数
     */
    class Consumer implements Runnable {

        @Override
        public void run() {
            for (;;) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                if (lock.close()) {
                    save(list1);
                }
                else if (lock.open()) {
                    save(list2);
                }
            }
        }

        private void save(Set<RealParamDO> paramDOs) {
            if(paramDOs.isEmpty()) {
                return;
            }
            log.info("batch save request size: " + paramDOs.size());
            fakerManager.batchSave(paramDOs);
            paramDOs.clear();
        }
    }

    /**
     * 记录参数
     */
    class Recorder implements Runnable {

        private Class<?> invokerInterface;
        private Invocation invocation;

        Recorder(Class<?> invokerInterface, Invocation invocation) {
            this.invokerInterface = invokerInterface;
            this.invocation = invocation;
        }

        @Override
        public void run() {
            Method method = check(invokerInterface, invocation);
            if (null == method) {
                return;
            }

            RealParamDO realParamDO = new RealParamDO();
            realParamDO.setAppId(Context.getAppInfo().getAppId());
            realParamDO.setType(method.getAnnotation(Fetch.class).value());
            String args = getArgs(invocation.getArguments());
            if(null == args) {
                return;
            }
            realParamDO.setParamValue(args);
            save(realParamDO);
        }

        private void save(RealParamDO realParamDO) {
            if(lock.isOpen()) {
                if(list1.size() < capacity) {
                    list1.add(realParamDO);
                }
                else if(list2.size() < capacity) {
                    lock.close();
                    list2.add(realParamDO);
                }
            }
            else {
                if(list2.size() < capacity) {
                    list2.add(realParamDO);
                }
                else if(list1.size() < capacity) {
                    lock.open();
                    list1.add(realParamDO);
                }
            }
        }

        /**
         * 检测是否需要进行参数保存
         * @param invokerInterface
         * @param invocation
         * @return
         */
        private Method check(Class<?> invokerInterface, Invocation invocation) {
            Method method;
            try {
                method = invokerInterface.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return null;
            }

            if (!method.isAnnotationPresent(Fetch.class)) {
                return null;
            }
            return method;
        }

        /**
         * 转换参数
         * @param arguments
         * @return
         */
        private String getArgs(Object[] arguments) {
            String args;
            if (null == arguments) {
                args = null;
            } else {
                if (arguments.length == 1) {
                    args = JsonUtil.toJson(arguments[0]);
                } else {
                    args = JsonUtil.toJson(arguments);
                }
            }
            return args;
        }
    }

    /**
     * 开关
     */
    class Switch {

        private final AtomicBoolean status;

        Switch(boolean initStatus) {
            this.status = new AtomicBoolean(initStatus);
        }

        public boolean isOpen() {
            return this.status.compareAndSet(true, true);
        }

        public boolean open() {
            return checkout(true);
        }

        public boolean close() {
            return checkout(false);
        }

        /**
         * 切换状态
         * @param status
         * @return
         */
        private boolean checkout(boolean status) {
            return this.status.compareAndSet(!status, status);
        }
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setMaxThread(int maxThread) {
        this.maxThread = maxThread;
    }
}
