/**
 * Copyright 2016 Xiaofei
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.library.hermes;

import android.content.Context;
import android.util.Log;

import com.library.hermes.internal.Channel;
import com.library.hermes.internal.HermesInvocationHandler;
import com.library.hermes.internal.Reply;
import com.library.hermes.sender.Sender;
import com.library.hermes.sender.SenderDesignator;
import com.library.hermes.util.HermesException;
import com.library.hermes.util.HermesGc;
import com.library.hermes.util.TypeCenter;
import com.library.hermes.util.TypeUtils;
import com.library.hermes.wrapper.ObjectWrapper;

import java.lang.reflect.Proxy;

/**
 * Created by Xiaofei on March 31, 2016.
 */
public class Hermes {
    //日志
    private static final String TAG = "HERMES";

    //缓存需要挂进程的class和method
    private static final TypeCenter TYPE_CENTER = TypeCenter.getInstance();

    //通道
    private static final Channel CHANNEL = Channel.getInstance();

    private static final HermesGc HERMES_GC = HermesGc.getInstance();

    private static Context sContext = null;

    //服务端，主进程注册class，这里参数是object
    public static void register(Object object) {
        register(object.getClass());
    }

    //检查是否在主进程调用了init(conetxt)方法
    private static void checkInit() {
        if (sContext == null) {
            throw new IllegalStateException("Hermes has not been initialized.");
        }
    }

    /**
     * 不需要在当地注册类process!
     * <p>
     * 但如果一个方法的返回类型是不完全相同的返回类型的方法,它应该是注册。
     *
     * @param clazz 需要注册的class
     */
    public static void register(Class<?> clazz) {
        //注册之前先检查是否调用了init（）
        checkInit();
        //缓存类注册
        TYPE_CENTER.register(clazz);
    }

    public static Context getContext() {
        return sContext;
    }

    //首先在主进程中使用init来初始化hermes，传入context
    public static void init(Context context) {
        //如果已经有了ApplicationContext则不重新赋值
        if (sContext != null) {
            return;
        }
        sContext = context.getApplicationContext();
    }

    private static void checkBound(Class<? extends HermesService> service) {
        if (!CHANNEL.getBound(service)) {
            throw new IllegalStateException("Service Unavailable: You have not connected the service "
                    + "or the connection is not completed. You can set HermesListener to receive a callback "
                    + "when the connection is completed.");
        }
    }

    private static <T> T getProxy(Class<? extends HermesService> service, ObjectWrapper object) {
        Class<?> clazz = object.getObjectClass();
        T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz},
                new HermesInvocationHandler(service, object));
        HERMES_GC.register(service, proxy, object.getTimeStamp());
        return proxy;
    }

    public static <T> T newInstance(Class<T> clazz, Object... parameters) {
        return newInstanceInService(HermesService.HermesService0.class, clazz, parameters);
    }

    /**
     * new一个跨进程的对象
     *
     * @param service    hermesservice
     * @param clazz      该对象对应的接口
     * @param parameters 如果当前对象的构造方法有多个，则这里需要传递对象的参数来指定调用哪一个构造方法
     * @param <T>        接口对象
     */
    public static <T> T newInstanceInService(Class<? extends HermesService> service, Class<T> clazz, Object... parameters) {
        //如果当前clazz不是接口，则抛出异常
        TypeUtils.validateServiceInterface(clazz);
        //检查当前调用的service是否已经注册过并且绑定了
        checkBound(service);
        //new一个ObjectWrapper，clazz对应的是new当前对象
        ObjectWrapper object = new ObjectWrapper(clazz, ObjectWrapper.TYPE_OBJECT_TO_NEW);
        //通过发送方指示器post一个sender对象
        Sender sender = SenderDesignator.getPostOffice(service, SenderDesignator.TYPE_NEW_INSTANCE, object);
        try {
            //得到回复对象reply，reply发送参数集合
            Reply reply = sender.send(null, parameters);
            if (reply != null && !reply.success()) {
                Log.e(TAG, "Error occurs during creating instance. Error code: " + reply.getErrorCode());
                Log.e(TAG, "Error message: " + reply.getMessage());
                return null;
            }
        } catch (HermesException e) {
            e.printStackTrace();
            return null;
        }
        object.setType(ObjectWrapper.TYPE_OBJECT);
        return getProxy(service, object);
    }

    public static <T> T getInstanceInService(Class<? extends HermesService> service, Class<T> clazz, Object... parameters) {
        return getInstanceWithMethodNameInService(service, clazz, "", parameters);
    }

    public static <T> T getInstance(Class<T> clazz, Object... parameters) {
        return getInstanceInService(HermesService.HermesService0.class, clazz, parameters);
    }

    public static <T> T getInstanceWithMethodName(Class<T> clazz, String methodName, Object... parameters) {
        return getInstanceWithMethodNameInService(HermesService.HermesService0.class, clazz, methodName, parameters);
    }

    public static <T> T getInstanceWithMethodNameInService(Class<? extends HermesService> service, Class<T> clazz, String methodName, Object... parameters) {
        TypeUtils.validateServiceInterface(clazz);
        checkBound(service);
        ObjectWrapper object = new ObjectWrapper(clazz, ObjectWrapper.TYPE_OBJECT_TO_GET);
        Sender sender = SenderDesignator.getPostOffice(service, SenderDesignator.TYPE_GET_INSTANCE, object);
        if (parameters == null) {
            parameters = new Object[0];
        }
        int length = parameters.length;
        Object[] tmp = new Object[length + 1];
        tmp[0] = methodName;
        for (int i = 0; i < length; ++i) {
            tmp[i + 1] = parameters[i];
        }
        try {
            Reply reply = sender.send(null, tmp);
            if (reply != null && !reply.success()) {
                Log.e(TAG, "Error occurs during getting instance. Error code: " + reply.getErrorCode());
                Log.e(TAG, "Error message: " + reply.getMessage());
                return null;
            }
        } catch (HermesException e) {
            e.printStackTrace();
            return null;
        }
        object.setType(ObjectWrapper.TYPE_OBJECT);
        return getProxy(service, object);
    }

    public static <T> T getUtilityClass(Class<T> clazz) {
        return getUtilityClassInService(HermesService.HermesService0.class, clazz);
    }

    public static <T> T getUtilityClassInService(Class<? extends HermesService> service, Class<T> clazz) {
        TypeUtils.validateServiceInterface(clazz);
        checkBound(service);
        ObjectWrapper object = new ObjectWrapper(clazz, ObjectWrapper.TYPE_CLASS_TO_GET);
        Sender sender = SenderDesignator.getPostOffice(service, SenderDesignator.TYPE_GET_UTILITY_CLASS, object);
        try {
            Reply reply = sender.send(null, null);
            if (reply != null && !reply.success()) {
                Log.e(TAG, "Error occurs during getting utility class. Error code: " + reply.getErrorCode());
                Log.e(TAG, "Error message: " + reply.getMessage());
                return null;
            }
        } catch (HermesException e) {
            e.printStackTrace();
            return null;
        }
        object.setType(ObjectWrapper.TYPE_CLASS);
        return getProxy(service, object);
    }

    //其他进程中调用该方法来实现连接
    public static void connect(Context context) {
        //该方法需要我们在主进程的AndroidManifest中注册HermesService.HermesService0.class
        connectApp(context, null, HermesService.HermesService0.class);
    }

    //如果当前app有多个进程，则需要调用该方法来指定哪个service、
    //当然，每个class都是继承HermesService，详见HermesService
    public static void connect(Context context, Class<? extends HermesService> service) {
        connectApp(context, null, service);
    }

    //当两个进程在多个app中的时候需要调用该方法来指定主进程的包名
    public static void connectApp(Context context, String packageName) {
        //主进程（packageName）中同样需要注册HermesService.HermesService0.class
        connectApp(context, packageName, HermesService.HermesService0.class);
    }

    /**
     * 连接两个进程的方法
     *
     * @param context     当前进程的context
     * @param packageName 主进程的包名，两个进程在不同的app中则需要指定该参数
     * @param service     挂进程需要使用到的service，都继承HermesService
     */
    public static void connectApp(Context context, String packageName, Class<? extends HermesService> service) {
        //初始化，将当前app的application赋值过来
        init(context);
        //绑定操作
        CHANNEL.bind(context.getApplicationContext(), packageName, service);
    }

    //断开连接
    public static void disconnect(Context context) {
        disconnect(context, HermesService.HermesService0.class);
    }

    //断开连接，传递进来application和hermesservice
    public static void disconnect(Context context, Class<? extends HermesService> service) {
        CHANNEL.unbind(context.getApplicationContext(), service);
    }

    //判断当前服务是否连接
    public static boolean isConnected() {
        return isConnected(HermesService.HermesService0.class);
    }

    //判断当前服务是否连接
    public static boolean isConnected(Class<? extends HermesService> service) {
        return CHANNEL.isConnected(service);
    }

    //添加hermes连接监听
    public static void setHermesListener(HermesListener listener) {
        CHANNEL.setHermesListener(listener);
    }

}
