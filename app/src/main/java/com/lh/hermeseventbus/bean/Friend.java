package com.lh.hermeseventbus.bean;

/**
 * User: LuHao
 * Date: 2019/8/21 20:28
 * Describe:我的好友
 */
public class Friend {
    private String name;
    private int age;

    public Friend(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
