package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.service.LikeService;
import com.xmy.demonowcoder.util.CommunityUtil;
import com.xmy.demonowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
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
public class LikeController {

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private LikeService likeService;

    @RequestMapping(value = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId) {
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

        return CommunityUtil.getJSONString(0, null, map);
    }

}
