package cn.xueyikang.dubbo.faker.core.request;

import cn.xueyikang.dubbo.faker.core.consumer.InvokerConsumer;
import cn.xueyikang.dubbo.faker.core.handle.AbstractHandle;
import cn.xueyikang.dubbo.faker.core.handle.MethodInvokeHandle;
import cn.xueyikang.dubbo.faker.core.invoke.AbstractInvoke;
import cn.xueyikang.dubbo.faker.core.invoke.AsyncInvoke;
import cn.xueyikang.dubbo.faker.core.manager.FakerManager;
import cn.xueyikang.dubbo.faker.core.model.InvokeFuture;
import cn.xueyikang.dubbo.faker.core.model.MethodInvokeDO;
import cn.xueyikang.dubbo.faker.core.model.RebuildParam;
import cn.xueyikang.dubbo.faker.core.utils.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class FakerRequest {

    private ClassPathXmlApplicationContext context;

    @Autowired
    private FakerManager fakerManager;

    private final AbstractHandle handle;

    public FakerRequest() {
        this.handle = new MethodInvokeHandle();
    }

    public String request(int invokeId, String invokeParam,
                          Integer poolSize, Integer qps, Integer questNum) {
        if(null == context) {
            context = new ClassPathXmlApplicationContext(new String[]{"classpath:application-dubbo.xml"});
        }

        // get method invoke info
        MethodInvokeDO invokeInfo = fakerManager.getInvokeInfo(invokeId);
        if(null == invokeInfo) {
            return "该请求调用不存在";
        }

        // get param type
        Class<?>[] paramTypes;
        String[] argsType = invokeInfo.getParamType().split(",");
        int length = argsType.length;
        if(0 == length) {
            paramTypes = new Class[0];
        }
        else {
            paramTypes = new Class[length];
            try {
                for (int index = 0; index < length; index++) {
                    paramTypes[index] = ReflectUtil.getClassType(argsType[index]);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Take Parameter Class Error: " + e);
            }
        }

        // get method handle
        MethodHandle methodHandle;
        try {
            methodHandle = handle.fetchHandleInfo(invokeInfo.getClassName(), invokeInfo.getMethodName(),
                    invokeInfo.getReturnType(), paramTypes);
        }
        catch (Exception e) {
            return "方法句柄获取失败" + e;
        }

        // get service instance
//        Class classType;
//        try {
//            classType = ReflectUtil.getClassType(invokeInfo.getClassName());
//        } catch (ClassNotFoundException e) {
//            return "依赖类未找到" + e;
//        }
        Object service;
        try {
            service = BeanUtil.getBean(context, invokeInfo.getClassName());
        }
        catch (BeansException e) {
            return "依赖实例未找到" + e;
        }

        Object[] values = JsonUtil.toArray(invokeParam, Object.class);
        if(null == values) {
            return "参数输入有误";
        }
        RebuildParam rebuildParam = ParamUtil.getRebuildParam(values);
        Set<String> rebuildParamSet = rebuildParam.getRebuildParamSet();
        Map<Integer, List<String>> rebuildParamMap = rebuildParam.getRebuildParamMap();

        Map<String, List<String>> paramMap;
        if(rebuildParamSet.isEmpty()) {
            paramMap = null;
        }
        else {
            paramMap = Maps.newHashMapWithExpectedSize(rebuildParamSet.size());
            for (String param : rebuildParamSet) {
                paramMap.put(param, fakerManager.getFakerParamByRebuildParam(param));
            }
        }


        // init invoke thread pool
        AbstractInvoke invoke = new AsyncInvoke(null == poolSize ? 1 : poolSize);
        int timeout = null == qps ? 100 : 3600 / qps;
        Queue<InvokeFuture> queue = new ConcurrentLinkedQueue<>();

        // init logging thread poll
        ExecutorService excutor = Executors.newFixedThreadPool(5);

        // find param convert
        Map<Integer, Integer> convertMap = ConvertUtil.getConvertMap(paramTypes);

        int size = null == questNum ? 1 : questNum, i;

        // generator fakerId
        String fakerId = UUIDUtil.getUUID();
        Random random = new Random();

        // async result
        CompletableFuture<Object> future;
        Instant start;

        // start logging thread
        excutor.submit(new InvokerConsumer(fakerId, queue, fakerManager));
        excutor.submit(new InvokerConsumer(fakerId, queue, fakerManager));
        excutor.submit(new InvokerConsumer(fakerId, queue, fakerManager));
        excutor.submit(new InvokerConsumer(fakerId, queue, fakerManager));
        excutor.submit(new InvokerConsumer(fakerId, queue, fakerManager));



        length = values.length;
        String value;
        List<String> params, paramValues;

        // convert param and invoke method
        for (int index = 0; index < size; index++) {
            // random invoke param
            //values = fakerParam.get(index).split(",");

            if(timeout > 50) {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Object[] argsValue;

            // convert param value
            if(0 == length) {
                argsValue = new Object[]{service};
            }
            else {
                argsValue = new Object[length + 1];
                argsValue[0] = service;
                for (i = 0; i < length; i++) {
                    value = values[i].toString();

                    params = rebuildParamMap.get(i);
                    if(null != params) {
                        for (String p : params) {
                            paramValues = paramMap.get(p);
                            value = value.replace(p, paramValues.get(random.nextInt(paramValues.size())));
                        }
                    }

                    if(1 == convertMap.get(i)) {
                        argsValue[i + 1] = JsonUtil.toList(value, Object.class);
                    }
                    else {
                        argsValue[i + 1] = JsonUtil.toObject(value, paramTypes[i]);
                    }
                }
            }
            start = Instant.now();
            future = invoke.invoke(fakerId, methodHandle, argsValue);
            queue.add(new InvokeFuture(start, future));
        }
        invoke.destroy();
        excutor.shutdown();
        return "请求编号：" + fakerId;
    }

    public static void main(String[] args) {

        String invokeParam = "[\"${123.model}\", {\"action\":\"haha\",\"money\":1111}, \"wishenm\"]";
        Object[] array1 = JsonUtil.toArray(invokeParam, Object.class);
        System.out.println(array1[1] instanceof Map);
        System.out.println(array1);


        boolean array = String[].class.isArray();
        String[] strs = new String[]{"666", "xxx"};
        String str = JsonUtil.toJson(strs);
        String[] strings = JsonUtil.toObject(str, String[].class);
        List<MethodInvokeDO> list = Lists.newArrayList();
        MethodInvokeDO invokeDO = new MethodInvokeDO();
        invokeDO.setAppId(12);
        invokeDO.setAppName("rewre");
        invokeDO.setClassName("com.cdaf.cdsf.d.fsd");
        invokeDO.setMethodName("dsfsfdsfdsf");
        invokeDO.setParamType("ddfdf,dfd,sf,dsf,dsf");
        invokeDO.setReturnType("ddfdf,dfd,sf,dsf,dsf");
        list.add(invokeDO);
        str = JsonUtil.toJson(list);
        List<Object> list1 = JsonUtil.toList(str, Object.class);
        System.out.println(list.getClass().getSimpleName());
        System.out.println(List.class.getSimpleName());
    }
}
