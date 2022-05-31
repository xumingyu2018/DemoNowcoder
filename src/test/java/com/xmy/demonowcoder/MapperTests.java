package com.xmy.demonowcoder;

import com.xmy.demonowcoder.dao.*;
import com.xmy.demonowcoder.entities.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DemoNowcoderApplication.class)//启用DemoNowcoderApplication类作为配置类
public class MapperTests {//ApplicationContextAware实现spring容器

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private MessageMapper messageMapper;

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
        List<DiscussPost> list = discussPostMapper.selectDiscussPosts(0, 0, 10, 0);
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

    @Test
    public void testSelectComment() {
        List<Comment> comments = commentMapper.selectCommentsByEntity(1, 103, 0, 5);
        System.out.println(comments);

        int count = commentMapper.selectCountByEntity(1, 103);
        System.out.println(count);
    }

    @Test
    public void testInsertComment() {
        Comment comment = new Comment();
        comment.setEntityType(1);
        comment.setEntityId(111);
        comment.setTargetId(0);
        comment.setContent("这是一个插入评论测试");
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentMapper.insertComment(comment);
    }

    @Test
    public void testSelectMessage() {
        List<Message> list = messageMapper.selectConversations(107, 0, 10);
        for (Message message : list) {
            System.out.println(message);
        }

        int count = messageMapper.selectConversationCount(107);
        System.out.println(count);

        list = messageMapper.selectLetters("104_107", 0, 10);
        for (Message message : list) {
            System.out.println(message);
        }

        int count1 = messageMapper.selectLetterCount("104_107");
        System.out.println(count1);

        int unreadCount = messageMapper.selectLetterUnreadCount(104, "104_107");
        System.out.println(unreadCount);
    }

    @Test
    public void testInsertMessage() {
        Message message = new Message();
        message.setFromId(107);
        message.setToId(109);
        message.setConversationId("107_109");
        message.setContent("你好tom,我是xmy");
        message.setCreateTime(new Date());
        messageMapper.insertMessage(message);
    }

    @Test
    public void testUpdateMessageStatus() {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(12);
        ids.add(11);
        messageMapper.updateStatus(ids, 1);
    }

    @Test
    public void testNotices() {
        Message message = messageMapper.selectLatestNotice(101, "comment");
        System.out.println(message);
    }

    @Test
    public void testUpdateDiscussportType() {
        discussPostMapper.updateType(130, 1);
        discussPostMapper.updateStatus(130, 1);
    }


}
