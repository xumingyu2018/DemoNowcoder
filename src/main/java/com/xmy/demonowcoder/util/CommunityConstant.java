package com.xmy.demonowcoder.util;

/**
 * @author xumingyu
 * @date 2022/4/21
 **/
public interface CommunityConstant {
    /**
     * 激活成功
     */
    int ACTIVATION_SUCCESS = 0;
    /**
     * 重复激活
     */
    int ACTIVATION_REPEAT = 1;
    /**
     * 激活失败
     */
    int ACTIVATION_FAILURE = 2;
    /**
     * 默认状态的登录凭证的超时时间
     */
    int DEFAULT_EXPIRED_SECONDS = 3600 * 12;
    /**
     * 记住状态的登录凭证超时时间
     */
    int REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 7;

    /**
     * 实体类型: 帖子
     */
    int ENTITY_TYPE_POST = 1;
    /**
     * 实体类型: 评论
     */
    int ENTITY_TYPE_COMMENT = 2;
    /**
     * 实体类型: 用户
     */
    int ENTITY_TYPE_USER = 3;
    /**
     * Kafka主题: 评论
     */
    String TOPIC_COMMENT = "comment";

    /**
     * Kafka主题: 点赞
     */
    String TOPIC_LIKE = "like";

    /**
     * Kafka主题: 关注
     */
    String TOPIC_FOLLOW = "follow";

    /**
     * Kafka主题: 发布帖子
     */
    String TOPIC_PUBILISH = "publish";

    /**
     * 系统用户ID
     */
    int SYSTEM_USER_ID = 1;
}
