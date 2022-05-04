package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.Service.DiscussPostService;
import com.xmy.demonowcoder.Service.UserService;
import com.xmy.demonowcoder.entities.DiscussPost;
import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.util.CommunityUtil;
import com.xmy.demonowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

/**
 * @author xumingyu
 * @date 2022/5/1
 **/
@Controller
@RequestMapping(value = "/discuss")
public class DiscussPostController {

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;


    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        //获取当前登录的用户
        User user = hostHolder.getUser();
        if (user == null) {
            //403权限不够
            return CommunityUtil.getJSONString(403, "你还没有登录哦！");
        }
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        //业务处理，将用户给的title，content进行处理并添加进数据库
        discussPostService.addDiscussPost(post);

        //返回Json格式字符串,报错的情况将来统一处理
        return CommunityUtil.getJSONString(0, "发布成功！");
    }

    @RequestMapping(value = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscusspost(@PathVariable("discussPostId") int discussPostId, Model model) {
        //通过前端传来的Id查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);

        //查询发帖人的头像及用户名
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);


        return "/site/discuss-detail";
    }

}
