package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.entities.Comment;
import com.xmy.demonowcoder.entities.DiscussPost;
import com.xmy.demonowcoder.entities.Page;
import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.service.CommentService;
import com.xmy.demonowcoder.service.DiscussPostService;
import com.xmy.demonowcoder.service.LikeService;
import com.xmy.demonowcoder.service.UserService;
import com.xmy.demonowcoder.util.CommuityConstant;
import com.xmy.demonowcoder.util.CommunityUtil;
import com.xmy.demonowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

/**
 * @author xumingyu
 * @date 2022/5/1
 **/
@Controller
@RequestMapping(value = "/discuss")
public class DiscussPostController implements CommuityConstant {

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;

    /**
     * 发表评论
     *
     * @param title
     * @param content
     * @return
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    // 异步请求要加@ResponseBody,且不要在Controller层用Model
    public String addDiscussPost(String title, String content) {
        //获取当前登录的用户
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(0, "你还没有登录哦！");
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

    /**
     * 评论详情页
     *
     * @param discussPostId
     * @param model
     * @param page          分页查询评论
     * @return
     */
    @RequestMapping(value = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscusspost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        // 通过前端传来的Id查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);

        // 查询作者的头像及用户名
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);

        // 点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);

        // 点赞状态 (没登录就显示0)
        int likeStatus = hostHolder.getUser() == null ? '0' : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        //设置评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());

        // 评论: 给帖子的评论
        // 回复: 给评论的评论
        // 评论列表集合
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());

        // 评论VO(viewObject)列表 (将comment,user信息封装到每一个Map，每一个Map再封装到一个List中)
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            // 每一条评论及该评论的用户封装进map集合
            for (Comment comment : commentList) {
                // 评论Map-->commentVo
                HashMap<String, Object> commentVo = new HashMap<>();
                // 评论
                commentVo.put("comment", comment);
                // 作者(由comment表中 entity = 1 查user表)
                commentVo.put("user", userService.findUserById(comment.getUserId()));

                // 点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                // 点赞状态 (没登录就显示0)
                likeStatus = hostHolder.getUser() == null ? '0' : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                // 回复列表集合（每一条评论的所有回复,不分页）
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);

                // 回复VO
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        // 回复Map
                        HashMap<String, Object> replyVo = new HashMap<>();
                        // 回复
                        replyVo.put("reply", reply);
                        // 作者 (由comment表中 entity = 2 查user表)
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        // 回复目标 (有2种：1.直接回复 2.追加回复)
                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target);

                        // 点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        // 点赞状态 (没登录就显示0)
                        likeStatus = hostHolder.getUser() == null ? '0' : likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);

                        // 将每一个回复Map放在回复List中
                        replyVoList.add(replyVo);
                    }
                }
                // 将每一个回复List放在评论Map中
                commentVo.put("replys", replyVoList);

                // 回复数量统计
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);

                // 再将每一个评论Map放在评论List中
                commentVoList.add(commentVo);
            }
        }
        // 最后将整个List传给前端model渲染
        model.addAttribute("comments", commentVoList);

        return "/site/discuss-detail";
    }
}
