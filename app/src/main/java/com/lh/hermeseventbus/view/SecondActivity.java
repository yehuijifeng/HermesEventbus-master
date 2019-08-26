package com.lh.hermeseventbus.view;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lh.hermeseventbus.R;
import com.lh.hermeseventbus.core.Hermes1;
import com.lh.hermeseventbus.interfaces.IUserManager;
import com.library.hermes.Hermes;
import com.library.hermes.HermesListener;
import com.library.hermes.HermesService;

/**
 * User: LuHao
 * Date: 2019/8/21 21:31
 * Describe:
 */
public class SecondActivity extends AppCompatActivity {
    private IUserManager iUerManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //在连接之前给Hermes设置监听器
        Hermes.setHermesListener(new HermesListener() {
            @Override
            public void onHermesConnected(Class<? extends HermesService> service) {
                //连接成功，首先获取单例
                try {
                    Hermes.newInstance(Object.class);
                    Hermes.getInstance(Object.class);
                    //静态方法没有限制,这里不清楚原因需要先调用getUtilityClass在调用newInstance才能正常使用
                    Hermes.getUtilityClass(IHermesOtherBean.class);
                    //如果是new的有限制
                    IHermesOtherBean iHermesOtherBeanNew = Hermes.newInstance(IHermesOtherBean.class);
                    if (iHermesOtherBeanNew == null)
                        Log.i("appjson", "当前IHermesOtherBean==null");
                    else {
                        //这种匿名类是错误的写法
//                        Log.i("appjson", iHermesOtherBean.customMethod(new HermesMethodBean(1, "aaaa", 1D)));
                        //这种private的对象也是错误的
//                        HermesMethodBean hermesMethodBean = new HermesMethodBean(1, "a", 1D);
//                        iHermesOtherBean.customMethod(hermesMethodBean);
                        //必须是非private且不是匿名类的对象
//                        hermesMethodBean = new HermesMethodBean(1, "a", 1D);
                        iHermesOtherBeanNew.setString1("bbbbbb");
                        Log.i("appjson", iHermesOtherBeanNew.customMethod(hermesMethodBean) + "aaa");
                        iHermesOtherBeanNew.customBackMethod(hermesMethodBean, new IHermesOtherListener() {
                            @Override
                            public void getVoid() {
                                Log.i("appjson", "getVoid()");
                            }

                            @Override
                            public int getBackInteger() {
                                Log.i("appjson", "getBackInteger()");
                                return 100;
                            }

                            @Override
                            public void setInteger(int i) {
                                Log.i("appjson", "setInteger(" + i + ")");

                            }

                            @Override
                            public String getBackString() {
                                Log.i("appjson", "getBackString()");
                                return "getBackString-admin";
                            }

                            @Override
                            public void setString(String str) {
                                Log.i("appjson", "setString(" + str + ")");
                            }

                            @Override
                            public HermesMethodBean getHermesMethod() {
                                Log.i("appjson", "getHermesMethod()");
                                return new HermesMethodBean(50, "test", 99D);
                            }

                            @Override
                            public void setHermesMethod(HermesMethodBean hermesMethod) {
                                Log.i("appjson", "setHermesMethod():" + hermesMethod.toString());
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.i("appjson", "error:" + e.getMessage());
                }
            }
        });
        Hermes.connect(this);
    }

    public void send(View view) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //断开Hermes服务
        Hermes.disconnect(this);
    }

    public void userManager(View view) {
        iUerManager = Hermes1.getDefault().getInstance(IUserManager.class);

//        DnUserManager.getInstance()
//                .setFriend(new Friend());
    }

    public void getSend(View view) {
        Toast.makeText(this,  iUerManager.getFriend().toString(), Toast.LENGTH_SHORT).show();
    }
}