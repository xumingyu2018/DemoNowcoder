package com.xmy.demonowcoder.Service;

import com.xmy.demonowcoder.dao.UserMapper;
import com.xmy.demonowcoder.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author xumingyu
 * @date 2022/4/14
 **/
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User findUser(int userId){
        return userMapper.selectById(userId);
    }
}
