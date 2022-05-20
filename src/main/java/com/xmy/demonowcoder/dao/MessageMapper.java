package com.xmy.demonowcoder.dao;

import com.xmy.demonowcoder.entities.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xumingyu
 * @date 2022/5/6
 **/
@Mapper
public interface MessageMapper {

    /**
     * 查询当前用户的会话列表,针对每个会话只返回一条最新的私信
     **/
    List<Message> selectConversations(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询当前用户的会话数量
     **/
    int selectConversationCount(@Param("userId") int userId);

    /**
     * 查询某个会话所包含的私信列表.
     **/
    List<Message> selectLetters(@Param("conversationId") String conversationId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询某个会话所包含的私信数量
     **/
    int selectLetterCount(@Param("conversationId") String conversationId);

    /**
     * 查询未读的数量
     * 1.带参数conversationId ：私信未读数量
     * 2.不带参数conversationId ：当前登录用户 所有会话未读数量
     **/
    int selectLetterUnreadCount(@Param("userId") int userId, @Param("conversationId") String conversationId);

    /**
     * 插入会话
     **/
    int insertMessage(Message message);

    /**
     * 批量更改每个会话的所有未读消息为已读
     **/
    int updateStatus(@Param("ids") List<Integer> ids, @Param("status") int status);

    /**
     * 查询某个主题最新通知
     */
    Message selectLatestNotice(@Param("userId") int userId, @Param("topic") String topic);

    /**
     * 查询某个主题通知个数
     */
    int selectNoticeCount(@Param("userId") int userId, @Param("topic") String topic);

    /**
     * 查询某个主题未读个数(topic可为null,若为null:查询所有主题未读通知个数)
     */
    int selectNoticeUnreadCount(@Param("userId") int userId, @Param("topic") String topic);

    /**
     * 分页查询某个主题的详情
     */
    List<Message> selectNotices(@Param("userId") int userId, @Param("topic") String topic, @Param("offset") int offset, @Param("limit") int limit);

}
