package com.xmy.demonowcoder.dao;

import com.xmy.demonowcoder.entities.LoginTicket;
import org.apache.ibatis.annotations.*;

/**
 * 登录凭证Mapper
 *
 * @author xumingyu
 * @date 2022/4/25
 **/
@Mapper
@Deprecated
/** 废弃loginTicket表，改用redis优化存储 **/
public interface LoginTicketMapper {

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values (#{userId},#{ticket},#{status},#{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket ",
            "where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);

    /**
     * 一定要加@Param()不然会报错
     *
     * @param ticket
     * @param status
     * @return error:com.mysql.jdbc.MysqlDataTruncation:Data truncation:Truncated incorrect DOUBLE value:...
     */
    @Update({
            "update login_ticket set status=#{status} where ticket=#{ticket} "
    })
    int updateStatus(@Param("ticket") String ticket, @Param("status") int status);
}
