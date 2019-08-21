package com.lh.hermeseventbus.core;


import com.lh.hermeseventbus.utils.TypeUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型中心，缓存
 */
public class TypeCenter {

    //为了减少反射，所以保存起来，并发hasmap
    private final ConcurrentHashMap<Class<?>,ConcurrentHashMap<String, Method>> mRawMethods;
    private final ConcurrentHashMap<String,Class<?>> mClazz;

    private static final TypeCenter ourInstance = new TypeCenter();

    private TypeCenter() {
        mRawMethods = new ConcurrentHashMap<>();
        mClazz = new ConcurrentHashMap<>();
    }

    public static TypeCenter getInstance(){
        return ourInstance;
    }

    public void register(Class clazz) {
        //注册--》类， 注册--》方法
        registerClass(clazz);
        registerMethod(clazz);
    }

    //缓存class
    private void registerClass(Class clazz) {
        String name =  clazz.getName();
        //如果没有则添加
        mClazz.putIfAbsent(name, clazz);
    }

    private void registerMethod(Class clazz){
        Method[] methods = clazz.getMethods();
        for(Method method: methods){
            mRawMethods.putIfAbsent(clazz, new ConcurrentHashMap<String, Method>());
            ConcurrentHashMap<String, Method> map = mRawMethods.get(clazz);
            String methodId = TypeUtils.getMethodId(method);
            map.put(methodId, method);
        }
    }
}
