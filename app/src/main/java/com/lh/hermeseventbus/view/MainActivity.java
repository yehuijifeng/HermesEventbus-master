package com.lh.hermeseventbus.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lh.hermeseventbus.R;
import com.lh.hermeseventbus.bean.Friend;
import com.lh.hermeseventbus.core.Hermes;
import com.lh.hermeseventbus.manager.UserManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Hermes.getDefault().init(this);
        Hermes.getDefault().register(UserManager.class);//注册usermanager类
        UserManager.getInstance().setFriend(new Friend("admin", 18));

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void receive(Friend friend) {
        Toast.makeText(this, friend.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
