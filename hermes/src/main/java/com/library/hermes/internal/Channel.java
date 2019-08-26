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

package com.library.hermes.internal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.library.hermes.HermesListener;
import com.library.hermes.HermesService;
import com.library.hermes.util.CallbackManager;
import com.library.hermes.util.CodeUtils;
import com.library.hermes.util.ErrorCodes;
import com.library.hermes.util.HermesException;
import com.library.hermes.util.TypeCenter;
import com.library.hermes.wrapper.ParameterWrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通道
 */
public class Channel {

    private static final String TAG = "CHANNEL";

    private static volatile Channel sInstance = null;

    private final ConcurrentHashMap<Class<? extends HermesService>, IHermesService> mHermesServices = new ConcurrentHashMap<Class<? extends HermesService>, IHermesService>();

    private final ConcurrentHashMap<Class<? extends HermesService>, HermesServiceConnection> mHermesServiceConnections = new ConcurrentHashMap<Class<? extends HermesService>, HermesServiceConnection>();

    private final ConcurrentHashMap<Class<? extends HermesService>, Boolean> mBindings = new ConcurrentHashMap<Class<? extends HermesService>, Boolean>();

    private final ConcurrentHashMap<Class<? extends HermesService>, Boolean> mBounds = new ConcurrentHashMap<Class<? extends HermesService>, Boolean>();

    private HermesListener mListener = null;

    private Handler mUiHandler = new Handler(Looper.getMainLooper());

    private static final CallbackManager CALLBACK_MANAGER = CallbackManager.getInstance();

    private static final TypeCenter TYPE_CENTER = TypeCenter.getInstance();

    private IHermesServiceCallback mHermesServiceCallback = new IHermesServiceCallback.Stub() {

        private Object[] getParameters(ParameterWrapper[] parameterWrappers) throws HermesException {
            if (parameterWrappers == null) {
                parameterWrappers = new ParameterWrapper[0];
            }
            int length = parameterWrappers.length;
            Object[] result = new Object[length];
            for (int i = 0; i < length; ++i) {
                ParameterWrapper parameterWrapper = parameterWrappers[i];
                if (parameterWrapper == null) {
                    result[i] = null;
                } else {
                    Class<?> clazz = TYPE_CENTER.getClassType(parameterWrapper);

                    String data = parameterWrapper.getData();
                    if (data == null) {
                        result[i] = null;
                    } else {
                        result[i] = CodeUtils.decode(data, clazz);
                    }
                }
            }
            return result;
        }

        public Reply callback(CallbackMail mail) {
            final Pair<Boolean, Object> pair = CALLBACK_MANAGER.getCallback(mail.getTimeStamp(), mail.getIndex());
            if (pair == null) {
                return null;
            }
            final Object callback = pair.second;
            if (callback == null) {
                return new Reply(ErrorCodes.CALLBACK_NOT_ALIVE, "");
            }
            boolean uiThread = pair.first;
            try {
                // TODO Currently, the callback should not be annotated!
                final Method method = TYPE_CENTER.getMethod(callback.getClass(), mail.getMethod());
                final Object[] parameters = getParameters(mail.getParameters());
                Object result = null;
                Exception exception = null;
                if (uiThread) {
                    boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
                    if (isMainThread) {
                        try {
                            result = method.invoke(callback, parameters);
                        } catch (IllegalAccessException e) {
                            exception = e;
                        } catch (InvocationTargetException e) {
                            exception = e;
                        }
                    } else {
                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    method.invoke(callback, parameters);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        return null;
                    }
                } else {
                    try {
                        result = method.invoke(callback, parameters);
                    } catch (IllegalAccessException e) {
                        exception = e;
                    } catch (InvocationTargetException e) {
                        exception = e;
                    }
                }
                if (exception != null) {
                    exception.printStackTrace();
                    throw new HermesException(ErrorCodes.METHOD_INVOCATION_EXCEPTION,
                            "Error occurs when invoking method " + method + " on " + callback, exception);
                }
                if (result == null) {
                    return null;
                }
                return new Reply(new ParameterWrapper(result));
            } catch (HermesException e) {
                e.printStackTrace();
                return new Reply(e.getErrorCode(), e.getErrorMessage());
            }
        }

        @Override
        public void gc(List<Long> timeStamps, List<Integer> indexes) throws RemoteException {
            int size = timeStamps.size();
            for (int i = 0; i < size; ++i) {
                CALLBACK_MANAGER.removeCallback(timeStamps.get(i), indexes.get(i));
            }
        }
    };

    private Channel() {

    }

    public static Channel getInstance() {
        if (sInstance == null) {
            synchronized (Channel.class) {
                if (sInstance == null) {
                    sInstance = new Channel();
                }
            }
        }
        return sInstance;
    }

    /**
     * 绑定服务
     *
     * @param context     上下文
     * @param packageName 不同app的包名
     * @param service     绑定的服务
     */
    public void bind(Context context, String packageName, Class<? extends HermesService> service) {
        //继承ServiceConnection
        HermesServiceConnection connection;
        synchronized (this) {
            //当前服务是否绑定，将建getBound(class)
            if (getBound(service)) {
                return;
            }
            //绑定服务的缓存中如果也有，且绑定过了，则不在绑定
            Boolean binding = mBindings.get(service);
            if (binding != null && binding) {
                return;
            }
            //当前service存入缓存，绑定的时候才将缓存中的状态改过来
            mBindings.put(service, true);
            //new一个HermesServiceConnection
            connection = new HermesServiceConnection(service);
            //将HermesServiceConnection存入缓存
            mHermesServiceConnections.put(service, connection);
        }
        Intent intent;
        //如果包名为null，则说明是同一个app的不同进程
        if (TextUtils.isEmpty(packageName)) {
            intent = new Intent(context, service);
        } else {
            //指定报名，进程在不同的app中
            intent = new Intent();
            intent.setClassName(packageName, service.getName());
        }
        //绑定服务
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 断开连接
     *
     * @param context 上下文
     * @param service 服务
     */
    public void unbind(Context context, Class<? extends HermesService> service) {
        synchronized (this) {
            //从缓存中拿到当前service的状态
            Boolean bound = mBounds.get(service);
            if (bound != null && bound) {
                //缓存中的service并没有被清除只是解除绑定
                HermesServiceConnection connection = mHermesServiceConnections.get(service);
                if (connection != null) {
                    //取消绑定
                    context.unbindService(connection);
                }
                //将缓存中的连接状态改成未绑定
                mBounds.put(service, false);
            }
        }
    }

    /**
     * 发送
     * @param service
     * @param mail
     * @return
     */
    public Reply send(Class<? extends HermesService> service, Mail mail) {
        IHermesService hermesService = mHermesServices.get(service);
        try {
            if (hermesService == null) {
                return new Reply(ErrorCodes.SERVICE_UNAVAILABLE,
                        "Service Unavailable: Check whether you have connected Hermes.");
            }
            return hermesService.send(mail);
        } catch (RemoteException e) {
            return new Reply(ErrorCodes.REMOTE_EXCEPTION, "Remote Exception: Check whether "
                    + "the process you are communicating with is still alive.");
        }
    }

    public void gc(Class<? extends HermesService> service, List<Long> timeStamps) {
        IHermesService hermesService = mHermesServices.get(service);
        if (hermesService == null) {
            Log.e(TAG, "Service Unavailable: Check whether you have disconnected the service before a process dies.");
        } else {
            try {
                hermesService.gc(timeStamps);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查是否绑定service
     *
     * @param service HermesService
     * @return bl
     */
    public boolean getBound(Class<? extends HermesService> service) {
        //从缓存中获取
        Boolean bound = mBounds.get(service);
        return bound != null && bound;
    }

    public void setHermesListener(HermesListener listener) {
        mListener = listener;
    }

    //判断当前service是否连接
    public boolean isConnected(Class<? extends HermesService> service) {
        //从缓存中拿到
        IHermesService hermesService = mHermesServices.get(service);
        return hermesService != null && hermesService.asBinder().pingBinder();
    }

    /**
     * 服务连接
     */
    private class HermesServiceConnection implements ServiceConnection {

        private Class<? extends HermesService> mClass;

        HermesServiceConnection(Class<? extends HermesService> service) {
            mClass = service;
        }

        //服务连接
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            synchronized (Channel.this) {
                //绑定对象service的缓存
                mBounds.put(mClass, true);
                //是否绑定了的缓存
                mBindings.put(mClass, false);
                //该方法与aidl的方法对应，属于跨进程的原生方法
                IHermesService hermesService = IHermesService.Stub.asInterface(service);
                //将当前class和得到的跨进程对象放入缓存
                mHermesServices.put(mClass, hermesService);
                try {
                    //注册回调，在当前线程中注册
                    hermesService.register(mHermesServiceCallback, Process.myPid());
                } catch (RemoteException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Remote Exception: Check whether "
                            + "the process you are communicating with is still alive.");
                    return;
                }
            }
            if (mListener != null) {
                mListener.onHermesConnected(mClass);
            }
        }

        //服务断开
        @Override
        public void onServiceDisconnected(ComponentName className) {
            synchronized (Channel.this) {
                //从缓存中断开连接
                mHermesServices.remove(mClass);
                //绑定的标识修改成未绑定
                mBounds.put(mClass, false);
                mBindings.put(mClass, false);
            }
            if (mListener != null) {
                mListener.onHermesDisconnected(mClass);
            }
        }
    }
}
