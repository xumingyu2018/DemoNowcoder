package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.entities.Comment;
import com.xmy.demonowcoder.entities.DiscussPost;
import com.xmy.demonowcoder.entities.Event;
import com.xmy.demonowcoder.event.EventProducer;
import com.xmy.demonowcoder.service.CommentService;
import com.xmy.demonowcoder.service.DiscussPostService;
import com.xmy.demonowcoder.util.CommunityConstant;
import com.xmy.demonowcoder.util.HostHolder;
import com.xmy.demonowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

/**
 * @author xumingyu
 * @date 2022/5/5
 **/
@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private RedisTemplate redisTemplate;

    // 需要从前端带一个参数
    @RequestMapping(value = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        /**
         * 触发评论事件
         * 评论完后，调用Kafka生产者，发送系统通知
         */
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setEntityId(comment.getEntityId())
                .setEntityType(comment.getEntityType())
                .setUserId(hostHolder.getUser().getId())
                .setData("postId", discussPostId);
        /**
         * event.setEntityUserId要分情况设置被发起事件的用户id
         * 1.评论的是帖子，被发起事件（评论）的用户->该帖子发布人id
         * 2.评论的是用户的评论，被发起事件（评论）的用户->该评论发布人id
         */
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            // 先找评论表对应的帖子id,在根据帖子表id找到发帖人id
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());

        }

        eventProducer.fireMessage(event);

        /**
         * 增加评论时，将帖子异步提交到Elasticsearch服务器
         * 通过Kafka消息队列去提交，修改Elasticsearch中帖子的评论数
         **/
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            event = new Event()
                    .setTopic(TOPIC_PUBILISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireMessage(event);

            /**
             * 计算帖子分数
             * 将评论过的帖子id存入set去重的redis集合
             */
            String redisKey = RedisKeyUtil.getPostScore();
            redisTemplate.opsForSet().add(redisKey, discussPostId);

        }
        return "redirect:/discuss/detail/" + discussPostId;
    }


}
