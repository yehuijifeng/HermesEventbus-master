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

public class ServiceConnectionManager {
    private static final ServiceConnectionManager ourInstance = new ServiceConnectionManager();

    //对应得binder 对象保存到hasmap中
    private final ConcurrentHashMap<Class,EventBusService> mHermesService = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class,HermesServiceConnection> mHermesServiceConnection = new ConcurrentHashMap<>();

    public static ServiceConnectionManager getInstance(){
        return ourInstance;
    }

    public ServiceConnectionManager() {
    }

    //绑定
    public void bind(Context context, String packageName,Class hermesServiceClass) {
        HermesServiceConnection hermesServiceConnection =
                new HermesServiceConnection(hermesServiceClass);
        //添加缓存
        mHermesServiceConnection.put(hermesServiceClass, hermesServiceConnection);
        Intent intent;
        //这里判断packagename的目的是，如果两个进程是两个app，则这里需要传递进来一个另外一个app的报名
        if(TextUtils.isEmpty(packageName)){
            intent = new Intent(context, hermesServiceClass);
        }else {
            intent = new Intent();
            intent.setClassName(packageName, hermesServiceClass.getName());
        }
        //当前app内绑定这个service
        context.bindService(intent, hermesServiceConnection, Context.BIND_AUTO_CREATE);
    }

    //获得请求结果
    public Responce request(Class hermesServiceClass, Request request) {
        EventBusService myEventBusService = mHermesService.get(hermesServiceClass);
        if(myEventBusService != null){
            try {
                Responce responce = myEventBusService.send(request);
                return responce;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    //服务链接
    private class HermesServiceConnection implements ServiceConnection {
        private Class mClass;

        HermesServiceConnection(Class service){
            this.mClass = service;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //通过aidl拿到跨进程的ibinder
            EventBusService myEventBusService = EventBusService.Stub.asInterface(service);
            mHermesService.put(mClass,myEventBusService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mHermesService.remove(mClass);
        }
    }
}
