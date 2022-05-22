package com.xmy.demonowcoder.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka消息队列事件（评论、点赞、关注事件）
 * 发布帖子时存储进Elasticsearch事件
 *
 * @author xumingyu
 * @date 2022/5/17
 **/
public class Event {

    // Kafka必要的主题变量
    private String topic;
    // 发起事件的用户id
    private int userId;
    // 用户发起事件的实体类型（评论、点赞、关注类型）
    private int entityType;
    // 用户发起事件的实体(帖子、评论、用户)id
    private int entityId;
    // 被发起事件的用户id(被评论、被点赞、被关注用户)
    private int entityUserId;
    // 其他可扩充内容对应Comment中的content
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    // 注意这里所有set方法返回Event类型,变成链式编程
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    // 方便外界直接调用key-value,而不用再封装一下传整个Map集合
    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
