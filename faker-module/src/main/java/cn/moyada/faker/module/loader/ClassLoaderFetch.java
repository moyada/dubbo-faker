package cn.moyada.faker.module.loader;

import java.lang.invoke.MethodHandles;

public interface ClassLoaderFetch {

    /**
     * 查找类，先从当前类加载器中寻找，再通过双亲委托模型获取
     * @param name 类名
     * @return
     * @throws ClassNotFoundException
     */
    Class<?> loadLocalClass(String name) throws ClassNotFoundException;

    /**
     * 获取当前类加载器的方法具柄Lookup
     * @return
     */
    MethodHandles.Lookup getMethodLookup();
}