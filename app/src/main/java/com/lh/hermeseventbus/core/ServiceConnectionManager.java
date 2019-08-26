package com.lh.hermeseventbus.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.lh.hermeseventbus.EventBusService;
import com.lh.hermeseventbus.result.Request;
import com.lh.hermeseventbus.result.Responce;

import java.util.concurrent.ConcurrentHashMap;

//服务连接管理类，专门管理ServiceConnection
public class ServiceConnectionManager {
    private static final ServiceConnectionManager ourInstance = new ServiceConnectionManager();

    //对应得binder 对象保存到hasmap中
    //这里两个并发map为缓存，一个缓存EventBusService，一个缓存ServiceConnection
    private final ConcurrentHashMap<Class, EventBusService> mHermesService = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class, HermesServiceConnection> mHermesServiceConnection = new ConcurrentHashMap<>();

    public static ServiceConnectionManager getInstance() {
        return ourInstance;
    }

    public ServiceConnectionManager() {
    }

    /**
     * 绑定
     *
     * @param context            上下文
     * @param packageName        app的包名，涉及到不同app进程间，需要用到包名
     * @param hermesServiceClass serviceClass，我们需要接收快进程信息的service类的class对象
     */
    public void bind(Context context, String packageName, Class hermesServiceClass) {
        HermesServiceConnection hermesServiceConnection =
                new HermesServiceConnection(hermesServiceClass);
        //添加缓存
        mHermesServiceConnection.put(hermesServiceClass, hermesServiceConnection);
        Intent intent;
        //这里判断packagename的目的是，如果两个进程是两个app，则这里需要传递进来一个另外一个app的报名
        if (TextUtils.isEmpty(packageName)) {
            intent = new Intent(context, hermesServiceClass);
        } else {
            //设置包名
            intent = new Intent();
            intent.setClassName(packageName, hermesServiceClass.getName());
        }
        //当前app内绑定这个service
        context.bindService(intent, hermesServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 发送请求
     *
     * @param hermesServiceClass serviceClass，我们需要接收快进程信息的service类的class对象
     * @param request 发送的对象
     * @return 返回接收对象responce
     */
    public Responce request(Class hermesServiceClass, Request request) {
        //从缓存中取出当前serviceclass对象对应的eventbusservice
        EventBusService myEventBusService = mHermesService.get(hermesServiceClass);
        if (myEventBusService != null) {
            try {
                //取到了缓存的service然后发送对象
                Responce responce = myEventBusService.send(request);
                //返回发送结果
                return responce;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //服务链接，这里和aidl生成的代码一致，相当于手写aidl自动生成的代码
    private class HermesServiceConnection implements ServiceConnection {
        private Class mClass;

        HermesServiceConnection(Class service) {
            this.mClass = service;
        }

        //服务连接
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //拿到跨进程的ibinder
            //这个自定义的eventbusservice实现了adil的代码
            EventBusService myEventBusService = EventBusService.Stub.asInterface(service);
            //将事件缓存起来
            mHermesService.put(mClass, myEventBusService);
        }

        //服务断开
        @Override
        public void onServiceDisconnected(ComponentName name) {
            //从缓存中移除
            mHermesService.remove(mClass);
        }
    }
}
