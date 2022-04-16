package com.xmy.demonowcoder.dao;

import com.xmy.demonowcoder.entities.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author xumingyu
 * @date 2022/4/12
 **/
@Mapper
public interface UserMapper {

    User selectById(int id);

    User selectByName(String name);

    User selectByEmail(String email);

    int insertUser(User user);

    int updateStatus(@Param("id") int id,@Param("status") int status);

    int updateHeader(@Param("id") int id,@Param("headerUrl") String headerUrl);

    int updatePassword(@Param("id") int id,@Param("password")String password);
    //一定要加@Param("")与mapper.xml中的#{ }匹配

}
