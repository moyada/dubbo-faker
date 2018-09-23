package cn.moyada.dubbo.faker.core.invoke;

import cn.moyada.dubbo.faker.core.model.InvokeFuture;
import cn.moyada.dubbo.faker.core.model.InvokerInfo;
import cn.moyada.dubbo.faker.core.model.MethodProxy;
import cn.moyada.dubbo.faker.core.model.queue.AbstractQueue;
import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.Suspendable;

/**
 * 纤线程调用器
 */
public class FiberInvoker extends AbstractInvoker {

    private final FiberExecutorScheduler scheduler;

    public FiberInvoker(MethodProxy proxy, AbstractQueue<InvokeFuture> queue, InvokerInfo invokerInfo) {
        super(proxy, queue, invokerInfo);
        this.scheduler = new FiberExecutorScheduler("fiber", super.excutor);
    }

    @Suspendable
    @Override
    public void invoke(Object[] argsValue) {
        super.count.increment();
//        Timestamp invokeTime = Timestamp.from(Instant.now());

        this.scheduler
                .newFiber(() ->  {
                    execute(argsValue);
                    return null;
                })
                .setPriority(Thread.MAX_PRIORITY)
                .start();

//        for (;;) {
//            if(fiber.isDone()) {
//                try {
//                    FutureResult result = fiber.get();
//                    super.callback(new InvokeFuture(result, invokeTime, Arrays.toString(argsValue)));
//                } catch (ExecutionException | InterruptedException e) {
//                    e.printStackTrace();
//                }
//                super.count.decrement();
//                break;
//            }
//        }
    }
}