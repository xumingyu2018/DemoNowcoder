package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.entities.DiscussPost;
import com.xmy.demonowcoder.entities.Page;
import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.service.DiscussPostService;
import com.xmy.demonowcoder.service.LikeService;
import com.xmy.demonowcoder.service.UserService;
import com.xmy.demonowcoder.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xumingyu
 * @date 2022/4/14
 **/
@Controller
public class HomeController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    /***
     * 主页分页查询功能
     * @return
     */
    @RequestMapping(value = "/index", method = RequestMethod.GET)
    // @RequestParam(name = "orderMode") 这是从前端传参数方法是：/index?xx 与Controller绑定
    public String getIndexPage(Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        //传入model参数是因为要返回值给View
        /*
         * 方法调用前，springMVC自动实例化Model和Page,并将Page注入Model
         * 所以，在thymeleaf中可以直接访问Page对象中的数据
         */
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index?orderMode=" + orderMode);

        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
        //List<DiscussPost> list=discussPostService.findDiscussPosts(0,0,10);//查寻所有的帖子
        //将查询的post帖子和user用户名拼接后放入map中,最后把全部map放入新的List中,因为UserId是外键，需要显示的是对应的名字即可
        List<Map<String, Object>> discussPost = new ArrayList<>();

        if (list!=null){
            for(DiscussPost post:list) {
                HashMap<String, Object> map = new HashMap<>();
                //将查询到的帖子放入map
                map.put("post", post);
                //将发布帖子对应的用户id作为参数
                User user = userService.findUserById(post.getUserId());
                //将发帖子的所有用户放入map
                map.put("user", user);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);

                //将组合的map放入List<>
                discussPost.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPost);
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }

    @RequestMapping(value = "error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }
}
