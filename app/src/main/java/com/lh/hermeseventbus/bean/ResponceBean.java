package com.lh.hermeseventbus.bean;

/**
 * Created by Administrator on 2018/5/23.
 */

public class ResponceBean {

    private Object data;//UserManager

    public ResponceBean(Object data) {
        this.data = data;
    }

    public Object getData() {
        return data;
    }
}
