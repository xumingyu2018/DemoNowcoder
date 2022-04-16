package com.xmy.demonowcoder.Service;

import com.xmy.demonowcoder.dao.DiscussPostMapper;
import com.xmy.demonowcoder.entities.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author xumingyu
 * @date 2022/4/14
 **/
@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    public List<DiscussPost> findDiscussPosts(int userId,int offset,int limit){
        return discussPostMapper.selectDiscussPosts(userId,offset,limit);
    }

    public int findDiscussPostRows(int userId){
        return discussPostMapper.selectDiscussRows(userId);
    }
}
