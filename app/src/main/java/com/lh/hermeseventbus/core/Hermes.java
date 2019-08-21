package com.lh.hermeseventbus.core;

import android.content.Context;

import com.google.gson.Gson;
import com.lh.hermeseventbus.annotation.ClassId;
import com.lh.hermeseventbus.bean.RequestBean;
import com.lh.hermeseventbus.bean.RequestParameter;
import com.lh.hermeseventbus.result.Request;
import com.lh.hermeseventbus.result.Responce;
import com.lh.hermeseventbus.service.HermesService;
import com.lh.hermeseventbus.utils.TypeUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * User: LuHao
 * Date: 2019/8/20 23:25
 * Describe: 手写Hermes
 */
public class Hermes {

    //这里保存的是application的context对象，因为如果多个app之间传递信息，则需要用到的是具有包名识别的application对象
    private Context mContext;
    //类型缓存
    private TypeCenter typeCenter;
    //服务链接管理类
    private ServiceConnectionManager serviceConnectionManager;
    //gson
    private Gson gson = new Gson();
    //得到对象
    public static final int TYPE_NEW = 0;
    //得到单例
    public static final int TYPE_GET = 1;

    //单例对象
    private static Hermes ourInstance = new Hermes();

    private Hermes() {
        serviceConnectionManager = ServiceConnectionManager.getInstance();
        typeCenter = TypeCenter.getInstance();
    }

    public static Hermes getDefault() {
        return ourInstance;
    }

    //初始化获得当前app的application对象
    public void init(Context context) {
        this.mContext = context.getApplicationContext();
    }

    //----------------------------服务端需要注册-------------------------
    public void register(Class clazz) {
        typeCenter.register(clazz);
    }


    //---------------------------客户端需要链接-------------------------
    public void connect(Context context,
                        Class serviceClass) {
        connectApp(context, null, serviceClass);
    }

    //链接app
    private void connectApp(Context context, String packageName, Class serviceClass) {
        init(context);
        serviceConnectionManager.bind(context.getApplicationContext(), packageName, serviceClass);
    }

    public <T> T getInstance(Class<T> tClass, Object... parameters) {
        Responce responce = sendRequest(HermesService.class, tClass, null, parameters);
        return getProxy(HermesService.class, tClass);
    }

    //这里使用了java的动态代理
    private <T> T getProxy(Class serviceClass, Class<T> tClass) {
        ClassLoader classLoader = serviceClass.getClassLoader();
        T proxy = (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{tClass},
                new HermesInvocationHander(serviceClass, tClass));
        return proxy;
    }

    private <T> Responce sendRequest(Class hermersServiceClass, Class<T> tClass, Method method, Object[] parameters) {
        RequestBean requestBean = new RequestBean();

        //set全类名
        String className = null;
        if (tClass.getAnnotation(ClassId.class) == null) {
            requestBean.setClassName(tClass.getName());
            requestBean.setResultClassName(tClass.getName());
        } else {
            requestBean.setClassName(tClass.getAnnotation(ClassId.class).value());
            requestBean.setResultClassName(tClass.getAnnotation(ClassId.class).value());
        }


        //set方法
        if (method != null) {
            requestBean.setMethodName(TypeUtils.getMethodId(method));
        }

        //set参数
        RequestParameter[] requestParameters = null;
        if (parameters != null && parameters.length > 0) {
            requestParameters = new RequestParameter[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                String parameterClassName = parameter.getClass().getName();
                String parameterValue = gson.toJson(parameter);
                RequestParameter requestParameter = new RequestParameter(parameterClassName, parameterValue);
                requestParameters[i] = requestParameter;
            }
        }
        if (requestParameters != null) {
            requestBean.setRequestParameter(requestParameters);
        }

        Request request = new Request(gson.toJson(requestBean), TYPE_GET);

        return serviceConnectionManager.request(hermersServiceClass, request);
    }

    public <T> Responce sendObjectRequest(Class serviceClass, Class<T> aClass,
                                          Method method, Object[] args) {
        RequestBean requestBean = new RequestBean();

        //set全类名
        String className = null;
        if (aClass.getAnnotation(ClassId.class) == null) {
            requestBean.setClassName(aClass.getName());
            requestBean.setResultClassName(aClass.getName());
        } else {
            requestBean.setClassName(aClass.getAnnotation(ClassId.class).value());
            requestBean.setResultClassName(aClass.getAnnotation(ClassId.class).value());
        }

        //set方法
        if (method != null) {
            requestBean.setMethodName(TypeUtils.getMethodId(method));
        }

        //set参数
        RequestParameter[] requestParameters = null;
        if (args != null && args.length > 0) {
            requestParameters = new RequestParameter[args.length];
            for (int i = 0; i < args.length; i++) {
                Object parameter = args[i];
                String parameterClassName = parameter.getClass().getName();
                String parameterValue = gson.toJson(parameter);

                RequestParameter requestParameter = new RequestParameter(parameterClassName, parameterValue);
                requestParameters[i] = requestParameter;
            }
        }

        if (requestParameters != null) {
            requestBean.setRequestParameter(requestParameters);
        }

        Request request = new Request(gson.toJson(requestBean), TYPE_NEW);

        return serviceConnectionManager.request(serviceClass, request);
    }

}
