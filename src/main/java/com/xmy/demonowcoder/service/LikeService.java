package com.xmy.demonowcoder.service;

import com.xmy.demonowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

/**
 * 点赞功能
 *
 * @author xumingyu
 * @date 2022/5/9
 **/
@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞 (记录谁点了哪个类型哪个留言/帖子id)
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        /**因为要用到两个redis操作，需使用事务**/
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                //判断like:entity:entityType:entityId 是否有对应的 userId
                Boolean isMember = redisOperations.opsForSet().isMember(entityLikeKey, userId);

                // 先查再开启事务
                redisOperations.multi();
                if (isMember) {
                    // 若已被点赞(即entityLikeKey里面有userId)则取消点赞->将userId从中移除
                    redisOperations.opsForSet().remove(entityLikeKey, userId);
                    // 该帖子的用户收到的点赞-1
                    redisOperations.opsForValue().decrement(userLikeKey);
                } else {
                    redisOperations.opsForSet().add(entityLikeKey, userId);
                    redisOperations.opsForValue().increment(userLikeKey);
                }

                return redisOperations.exec();
            }
        });
    }

    // 查询某实体点赞的数量 --> scard like:entity:1:110
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        // 1：已点赞 , 0：赞
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }

}
