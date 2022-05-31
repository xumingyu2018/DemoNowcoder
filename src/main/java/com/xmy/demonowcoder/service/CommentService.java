package com.xmy.demonowcoder.service;

import com.xmy.demonowcoder.dao.CommentMapper;
import com.xmy.demonowcoder.entities.Comment;
import com.xmy.demonowcoder.util.CommunityConstant;
import com.xmy.demonowcoder.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * @author xumingyu
 * @date 2022/5/4
 **/
@Service
public class CommentService implements CommunityConstant {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private DiscussPostService discussPostService;

    /**
     * 分页查询评论
     **/
    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    /**
     * 添加评论(涉及事务)
     * 先添加评论，后修改discuss_post中的评论数（作为一个整体事务，出错需要整体回滚！）
     *
     * @return
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        /**添加评论**/
        //过滤标签
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        //过滤敏感词
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);

        /**
         * 更新帖子评论数量
         * 如果是帖子类型才更改帖子评论数量，并且获取帖子评论的id
         */
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }

        return rows;
    }

    public Comment findCommentById(int id) {
        return commentMapper.selectComment(id);
    }

    /**
     * 查询我的回复
     **/
    public List<Comment> findCommentsByUserId(int userId, int entityType, int offset, int limit) {
        return commentMapper.selectCommentsByUserId(userId, entityType, offset, limit);
    }

    public int findCommentCountByUserId(int userId, int entityType) {
        return commentMapper.selectCountByUserId(userId, entityType);
    }

}
