package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.entities.Event;
import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.event.EventProducer;
import com.xmy.demonowcoder.service.LikeService;
import com.xmy.demonowcoder.util.CommunityConstant;
import com.xmy.demonowcoder.util.CommunityUtil;
import com.xmy.demonowcoder.util.HostHolder;
import com.xmy.demonowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xumingyu
 * @date 2022/5/9
 **/
@Controller
public class LikeController implements CommunityConstant {

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();
        // 点赞
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        // 获取对应帖子、留言的点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        // 获取当前登录用户点赞状态（1：已点赞 0：赞）
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        // 封装结果到Map
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        /**
         * 触发点赞事件
         * 只有点赞完后，才会调用Kafka生产者，发送系统通知，取消点赞不会调用事件
         */
        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setEntityId(entityId)
                    .setEntityType(entityType)
                    .setUserId(user.getId())
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            // data里面存postId是因为点击查看后链接到具体帖子的页面
            eventProducer.fireMessage(event);
        }

        /**
         * 计算帖子分数
         * 将点赞过的帖子id存入set去重的redis集合
         */
        if (entityType == ENTITY_TYPE_POST) {
            String redisKey = RedisKeyUtil.getPostScore();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return CommunityUtil.getJSONString(0, null, map);
    }

}
