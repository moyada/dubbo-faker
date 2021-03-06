package io.moyada.sharingan.infrastructure.module;

import java.lang.invoke.MethodHandles;

/**
 * 元类信息获取器
 * @author xueyikang
 * @since 0.0.1
 */
public interface MetadataFetch {

    ClassLoader getClassLoader(Dependency dependency);

    /**
     * 根据依赖获取类
     * @param dependency 依赖
     * @param className 类全名
     * @return
     * @throws ClassNotFoundException
     */
    Class getClass(Dependency dependency, String className) throws ClassNotFoundException;

    /**
     * 获取对应依赖方法具柄
     * @param dependency
     * @return
     */
    MethodHandles.Lookup getMethodLookup(Dependency dependency);
}
