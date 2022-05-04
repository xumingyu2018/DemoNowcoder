package com.xmy.demonowcoder;

import com.xmy.demonowcoder.dao.DiscussPostMapper;
import com.xmy.demonowcoder.dao.LoginTicketMapper;
import com.xmy.demonowcoder.dao.UserMapper;
import com.xmy.demonowcoder.entities.DiscussPost;
import com.xmy.demonowcoder.entities.LoginTicket;
import com.xmy.demonowcoder.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DemoNowcoderApplication.class)//启用DemoNowcoderApplication类作为配置类
class MapperTests {//ApplicationContextAware实现spring容器

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Test
    public void testSelectUser() {
        User user1 = userMapper.selectById(101);
        System.out.println(user1);

        User user2 = userMapper.selectByName("xmy");
        System.out.println(user2);

        User user3 = userMapper.selectByEmail("260602448@qq.com");
        System.out.println(user3);
    }

    @Test
    public void testInsertUser(){
        User user = new User();
        user.setUsername("dgx");
        user.setPassword("123");
        user.setSalt("Abc");
        user.setEmail("test1@qq.com");
        user.setHeaderUrl("http://www.nowcoder.com/101.png");
        user.setCreateTime(new Date());

        int rows=userMapper.insertUser(user);
        System.out.println(rows);
    }

    @Test
    public void updateUser(){
        userMapper.updateStatus(101,1);
        userMapper.updatePassword(102,"11");
        userMapper.updateHeader(101,"http://www.img");

    }

    @Test
    public void testSelectPosts() {
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(0, 0, 10);
        for (DiscussPost post : list) {
            System.out.println(post);
        }

        int rows = discussPostMapper.selectDiscussRows(0);
        System.out.println(rows);
    }

    @Test
    public void testInsertLoginTicket() {
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("abc");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));
        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket() {
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("abc");
        System.out.println(loginTicket);
    }

    @Test
    public void testUpdateLoginTicket() {
        //参数前要加@Param不然会报com.mysql.jdbc.MysqlDataTruncation: Data truncation: Truncated incorrect DOUBLE value:...
        loginTicketMapper.updateStatus("abc", 1);
    }


}
