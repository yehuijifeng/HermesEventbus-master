package com.lh.hermeseventbus.manager;

import com.lh.hermeseventbus.annotation.ClassId;
import com.lh.hermeseventbus.bean.Friend;

/**
 * 好友管理类
 */
@ClassId("com.lh.hermeseventbus.manager.UserManager")
public class UserManager {
    Friend friend;

    private static UserManager sInstance = null;

    private UserManager() {}

    public static synchronized UserManager getInstance(){
        if(sInstance == null){
            sInstance = new UserManager();
        }
        return sInstance;
    }

    public Friend getFriend() {
        return friend;
    }

    public void setFriend(Friend friend) {
        this.friend = friend;
    }
}
