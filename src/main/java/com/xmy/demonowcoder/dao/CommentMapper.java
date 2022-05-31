package com.xmy.demonowcoder.dao;

import com.xmy.demonowcoder.entities.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xumingyu
 * @date 2022/5/4
 **/
@Mapper
public interface CommentMapper {

    /**
     * 根据评论类型(帖子评论和回复评论)和评论Id--分页查询评论
     *
     * @return Comment类型集合
     */
    List<Comment> selectCommentsByEntity(@Param("entityType") int entityType, @Param("entityId") int entityId,
                                         @Param("offset") int offset, @Param("limit") int limit);

    int selectCountByEntity(@Param("entityType") int entityType, @Param("entityId") int entityId);

    int insertComment(Comment comment);

    Comment selectComment(@Param("id") int id);

    List<Comment> selectCommentsByUserId(@Param("userId") int userId, @Param("entityType") int entityType,
                                         @Param("offset") int offset, @Param("limit") int limit);

    int selectCountByUserId(@Param("userId") int userId, @Param("entityType") int entityType);
}
