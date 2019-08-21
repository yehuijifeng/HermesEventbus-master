package com.lh.hermeseventbus.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.lh.hermeseventbus.R;
import com.lh.hermeseventbus.core.Hermes;
import com.lh.hermeseventbus.interfaces.IUserManager;
import com.lh.hermeseventbus.service.HermesService;

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
        Hermes.getDefault().connect(this, HermesService.class);
    }

    public void send(View view) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void userManager(View view) {
        iUerManager = Hermes.getDefault().getInstance(IUserManager.class);

//        DnUserManager.getInstance()
//                .setFriend(new Friend());
    }

    public void getSend(View view) {
        Toast.makeText(this,  iUerManager.getFriend().toString(), Toast.LENGTH_SHORT).show();
    }
}