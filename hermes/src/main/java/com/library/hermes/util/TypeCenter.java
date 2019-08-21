/**
 *
 * Copyright 2016 Xiaofei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.library.hermes.util;

import android.text.TextUtils;

import com.library.hermes.annotation.ClassId;
import com.library.hermes.annotation.MethodId;
import com.library.hermes.wrapper.BaseWrapper;
import com.library.hermes.wrapper.MethodWrapper;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Xiaofei on 16/4/7.
 * 缓存类
 */
public class TypeCenter {
    
    private static volatile TypeCenter sInstance = null;

    //带注解的class
    private final ConcurrentHashMap<String, Class<?>> mAnnotatedClasses;

    //原生的class
    private final ConcurrentHashMap<String, Class<?>> mRawClasses;

    //带注解的class的方法
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> mAnnotatedMethods;

    //原生的class的方法
    private final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>> mRawMethods;

    private TypeCenter() {
        mAnnotatedClasses = new ConcurrentHashMap<String, Class<?>>();
        mRawClasses = new ConcurrentHashMap<String, Class<?>>();
        mAnnotatedMethods = new ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>>();
        mRawMethods = new ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, Method>>();
    }

    public static TypeCenter getInstance() {
        if (sInstance == null) {
            synchronized (TypeCenter.class) {
                if (sInstance == null) {
                    sInstance = new TypeCenter();
                }
            }
        }
        return sInstance;
    }

    //注册类
    private void registerClass(Class<?> clazz) {
        //是否带有classid注解
        ClassId classId = clazz.getAnnotation(ClassId.class);
        if (classId == null) {
            //没有则存入原始classmap中
            String className = clazz.getName();
            mRawClasses.putIfAbsent(className, clazz);
        } else {
            //如多有class注解，则存储注解classmap中
            String className = classId.value();
            mAnnotatedClasses.putIfAbsent(className, clazz);
        }
    }

    //注册方法
    private void registerMethod(Class<?> clazz) {
        //获得所有方法
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            //是否带有methodid注解
            MethodId methodId = method.getAnnotation(MethodId.class);
            if (methodId == null) {
                //没有，则放入
                mRawMethods.putIfAbsent(clazz, new ConcurrentHashMap<String, Method>());
                ConcurrentHashMap<String, Method> map = mRawMethods.get(clazz);
                //生成方法id作为map的key
                String key = TypeUtils.getMethodId(method);
                map.putIfAbsent(key, method);
            } else {
                //有注解，则放入
                mAnnotatedMethods.putIfAbsent(clazz, new ConcurrentHashMap<String, Method>());
                ConcurrentHashMap<String, Method> map = mAnnotatedMethods.get(clazz);
                String key = TypeUtils.getMethodId(method);
                map.putIfAbsent(key, method);
            }
        }
    }

    //注册
    public void register(Class<?> clazz) {
        //检查类，如果不合法则抛出异常
        TypeUtils.validateClass(clazz);
        //注册类
        registerClass(clazz);
        //注册方法
        registerMethod(clazz);
    }

    public Class<?> getClassType(BaseWrapper wrapper) throws HermesException {
        String name = wrapper.getName();
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        if (wrapper.isName()) {
            Class<?> clazz = mRawClasses.get(name);
            if (clazz != null) {
                return clazz;
            }
            //boolean, byte, char, short, int, long, float, and double void
            if (name.equals("boolean")) {
                clazz = boolean.class;
            } else if (name.equals("byte")) {
                clazz = byte.class;
            } else if (name.equals("char")) {
                clazz = char.class;
            } else if (name.equals("short")) {
                clazz = short.class;
            } else if (name.equals("int")) {
                clazz = int.class;
            } else if (name.equals("long")) {
                clazz = long.class;
            } else if (name.equals("float")) {
                clazz = float.class;
            } else if (name.equals("double")) {
                clazz = double.class;
            } else if (name.equals("void")) {
                clazz = void.class;
            } else {
                try {
                    clazz = Class.forName(name);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new HermesException(ErrorCodes.CLASS_NOT_FOUND,
                            "Cannot find class " + name + ". Classes without ClassId annotation on it "
                                    + "should be located at the same package and have the same name, "
                                    + "EVEN IF the source code has been obfuscated by Proguard.");
                }

            }
            mRawClasses.putIfAbsent(name, clazz);
            return clazz;
        } else {
            Class<?> clazz = mAnnotatedClasses.get(name);
            if (clazz == null) {
                throw new HermesException(ErrorCodes.CLASS_NOT_FOUND,
                        "Cannot find class with ClassId annotation on it. ClassId = " + name
                                + ". Please add the same annotation on the corresponding class in the remote process"
                                + " and register it. Have you forgotten to register the class?");
            }
            return clazz;
        }
    }

    public Class<?>[] getClassTypes(BaseWrapper[] wrappers) throws HermesException {
        Class<?>[] classes = new Class<?>[wrappers.length];
        for (int i = 0; i < wrappers.length; ++i) {
            classes[i] = getClassType(wrappers[i]);
        }
        return classes;
    }

    public Method getMethod(Class<?> clazz, MethodWrapper methodWrapper) throws HermesException {
        String name = methodWrapper.getName();
        if (methodWrapper.isName()) {
            mRawMethods.putIfAbsent(clazz, new ConcurrentHashMap<String, Method>());
            ConcurrentHashMap<String, Method> methods = mRawMethods.get(clazz);
            Method method = methods.get(name);
            if (method != null) {
                TypeUtils.methodReturnTypeMatch(method, methodWrapper);
                return method;
            }
            int pos = name.indexOf('(');
            method = TypeUtils.getMethod(clazz, name.substring(0, pos), getClassTypes(methodWrapper.getParameterTypes()), getClassType(methodWrapper.getReturnType()));
            if (method == null) {
                throw new HermesException(ErrorCodes.METHOD_NOT_FOUND,
                        "Method not found: " + name + " in class " + clazz.getName());
            }
            methods.put(name, method);
            return method;
        } else {
            ConcurrentHashMap<String, Method> methods = mAnnotatedMethods.get(clazz);
            Method method = methods.get(name);
            if (method != null) {
                TypeUtils.methodMatch(method, methodWrapper);
                return method;
            }
            throw new HermesException(ErrorCodes.METHOD_NOT_FOUND,
                    "Method not found in class " + clazz.getName() + ". Method id = " + name + ". "
                            + "Please add the same annotation on the corresponding method in the remote process.");
        }
    }
}
