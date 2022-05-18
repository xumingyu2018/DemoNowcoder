package com.xmy.demonowcoder.service;

import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.util.CommunityConstant;
import com.xmy.demonowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author xumingyu
 * @date 2022/5/10
 **/
@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    /**
     * 关注
     **/
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                // 开启事务
                redisOperations.multi();
                /*
                    System.currentTimeMillis()->用于获取当前系统时间,以毫秒为单位
                    关注时，首先将实体(用户或帖子)id添加用户关注的集合中，再将用户id添加进实体粉丝的集合中
                 */
                redisOperations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                redisOperations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return redisOperations.exec();
            }
        });
    }

    /**
     * 取消关注
     **/
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                // 开启事务
                redisOperations.multi();
                /*关注时，首先将实体(用户或帖子)id移除用户关注的集合中，再将用户id移除进实体粉丝的集合中*/
                redisOperations.opsForZSet().remove(followeeKey, entityId);
                redisOperations.opsForZSet().remove(followerKey, userId);

                return redisOperations.exec();
            }
        });
    }

    /**
     * 查询关注的实体(用户)数量
     **/
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        // opsForZSet().zCard获取有序集合中的数量
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    /**
     * 查询粉丝的实体数量
     **/
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    /* 查询当前用户是否已关注该实体 **/
    // userId->当前登录用户  entityType->用户类型（3） entityId->关注的用户id
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        /* opsForZSet().score 获取有序集合中指定元素权重分数  followee:userId:entityType = entityId的分数（这里是时间）
           若有时间，则表明已关注；
        */
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    /**
     * 查询某用户关注的人
     **/
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        // 按最新时间倒序查询目标用户id封装在set<Integet>中
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }
        // 将user信息map和redis用户关注时间map一起封装到list
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            HashMap<String, Object> map = new HashMap<>();
            // 用户信息map
            User user = userService.findUserById(targetId);
            map.put("user", user);
            // 目标用户关注时间map(将long型拆箱成基本数据类型)
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followeeTime", new Date(score.longValue()));

            list.add(map);
        }
        return list;
    }

    /**
     * 查询某用户的粉丝
     **/
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);

        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        if (targetIds == null) {
            return null;
        }
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            HashMap<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followerTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

}
