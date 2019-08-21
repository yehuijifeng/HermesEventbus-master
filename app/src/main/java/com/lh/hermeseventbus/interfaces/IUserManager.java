package com.lh.hermeseventbus.interfaces;

import com.lh.hermeseventbus.annotation.ClassId;
import com.lh.hermeseventbus.bean.Friend;

/**
 * User: LuHao
 * Date: 2019/8/21 21:29
 * Describe:
 */
@ClassId("com.lh.hermeseventbus")
public interface IUserManager {
    public Friend getFriend();
    public void setFriend(Friend friend);
}
