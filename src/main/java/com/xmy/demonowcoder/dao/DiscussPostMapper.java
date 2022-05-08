package com.xmy.demonowcoder.dao;

import com.xmy.demonowcoder.entities.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xumingyu
 * @date 2022/4/14
 **/
@Mapper
public interface DiscussPostMapper {

    /**
     * 分页查询
     * userId=0为所有帖子，1为我的帖子
     * 每个参数必须加@Param("")
     *
     * @return
     */
    List<DiscussPost> selectDiscussPosts(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 为分页查询服务的查询总条数
     * 给参数起别名，如果只有一个参数并且要在<if>里使用，则必须加别名
     *
     * @return
     */
    int selectDiscussRows(@Param("userId") int userId);

    /**
     * 发布帖子
     */
    int insertDiscussPost(DiscussPost discussPost);

    /**
     * 查看帖子详情
     */
    DiscussPost selectDiscussPostById(int id);

    int updateCommentCount(@Param("id") int id, @Param("commentCount") int commentCount);


}

