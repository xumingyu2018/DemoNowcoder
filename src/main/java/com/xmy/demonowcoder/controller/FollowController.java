package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.entities.Event;
import com.xmy.demonowcoder.entities.Page;
import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.event.EventProducer;
import com.xmy.demonowcoder.service.FollowService;
import com.xmy.demonowcoder.service.UserService;
import com.xmy.demonowcoder.util.CommunityConstant;
import com.xmy.demonowcoder.util.CommunityUtil;
import com.xmy.demonowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @author xumingyu
 * @date 2022/5/10
 **/
@Controller
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private EventProducer eventProducer;

    /**
     * 关注
     **/
    @RequestMapping(value = "/follow", method = RequestMethod.POST)
    @ResponseBody // 关注是异步请求
    public String follow(int entityType, int entityId) {
        followService.follow(hostHolder.getUser().getId(), entityType, entityId);

        /**
         * 触发关注事件
         * 关注完后，调用Kafka生产者，发送系统通知
         */
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        // 用户关注实体的id就是被关注的用户id->EntityId=EntityUserId

        eventProducer.fireMessage(event);


        return CommunityUtil.getJSONString(0, "已关注");
    }

    /**
     * 取消关注
     **/
    @RequestMapping(value = "/unfollow", method = RequestMethod.POST)
    @ResponseBody // 关注是异步请求
    public String unfollow(int entityType, int entityId) {
        followService.unfollow(hostHolder.getUser().getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0, "已取消关注");
    }

    /**
     * 查询某用户关注列表
     **/
    @RequestMapping(value = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model) {
        // 当前访问的用户信息
        User user = userService.findUserById(userId);
        // Controller层统一处理异常
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);
        // 设置分页信息
        page.setLimit(3);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList != null) {
            for (Map<String, Object> map : userList) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userList);

        return "/site/followee";
    }

    @RequestMapping(value = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollower(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        page.setLimit(3);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, user.getId()));

        List<Map<String, Object>> userlist = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userlist != null) {
            for (Map<String, Object> map : userlist) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", userlist);

        return "/site/follower";
    }


    /**
     * 判端当前登录用户与关注、粉丝列表的关注关系
     **/
    private Boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }
        // 调用当前用户是否已关注user实体Service
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }
}
