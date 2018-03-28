package cn.moyada.dubbo.faker.core.invoke;

import cn.moyada.dubbo.faker.core.listener.CompletedListener;
import cn.moyada.dubbo.faker.core.model.FutureResult;
import cn.moyada.dubbo.faker.core.model.InvokeFuture;
import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberExecutorScheduler;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.SuspendableCallable;

import java.lang.invoke.MethodHandle;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class FiberInvoker extends AbstractInvoker implements AutoCloseable {

    private final FiberExecutorScheduler scheduler;

    public FiberInvoker(MethodHandle handle, Object service,
                        CompletedListener completedListener, int poolSize) {
        super(handle, service, completedListener, poolSize);
        this.scheduler = new FiberExecutorScheduler("fiber", super.excutor);
    }

    @Suspendable
    @Override
    public void invoke(Object[] argsValue) {
        super.count.increment();
        Timestamp invokeTime = Timestamp.from(Instant.now());

        Fiber<FutureResult> fiber = this.scheduler
                .newFiber((SuspendableCallable<FutureResult>) () -> {
                    FutureResult result;
                    long start = System.nanoTime();
                    try {
                        result = FutureResult.success(execute(argsValue));
                    } catch (Throwable e) {
                        result = FutureResult.failed(e.getMessage());
                    }
                    result.setSpend((System.nanoTime() - start) / 1000_000);
                    return result;
                })
                .start();

        for (;;) {
            if(fiber.isDone()) {
                try {
                    FutureResult result = fiber.get();
                    super.callback(new InvokeFuture(result, invokeTime, Arrays.toString(argsValue)));
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                super.count.decrement();
                break;
            }
        }
    }
}