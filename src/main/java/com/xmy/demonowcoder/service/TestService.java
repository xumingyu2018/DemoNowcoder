package com.xmy.demonowcoder.service;

import com.xmy.demonowcoder.dao.TestDao;
import com.xmy.demonowcoder.dao.UserMapper;
import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

/**
 * @author xumingyu
 * @date 2022/4/10
 **/
@Service
@Scope("prototype")//多实例，默认是单例，每次getbean时实例化
public class TestService {

    @Autowired//sevice调用dao层
    private TestDao testDao;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;


    public TestService() {
        System.out.println("实例化TestService");
    }

    @PostConstruct//初始方法在构造器之后调用（通过spring容器调用）
    public void init() {
        System.out.println("初始化TestService这个bean");
    }

    //销毁之前调用
    @PreDestroy
    public void destroy() {
        System.out.println("销毁TestService这个bean");
    }

    //开发常用
    public String find() {
        return testDao.select();
    }

    /**
     * 事务管理Demo
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1() {
        User user = new User();
        user.setUsername("tom");
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5("123") + user.getSalt());
        user.setType(0);
        user.setHeaderUrl("http://localhost:8080/1.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //设置error,验证事务回滚
        Integer.valueOf("abc");

        return "ok";
    }

    public Object save2() {
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        return transactionTemplate.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                User user = new User();
                user.setUsername("Marry");
                user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
                user.setPassword(CommunityUtil.md5("123123") + user.getSalt());
                user.setType(0);
                user.setHeaderUrl("http://localhost:8080/2.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);

                //设置error,验证事务回滚
                Integer.valueOf("abc");
                return "ok";
            }
        });
    }

    // 服务器启动时就自动执行
    @Async
    public void execute1() {
        System.out.println(Thread.currentThread().getName() + " execute1");

    }

    // @Scheduled(initialDelay = 10000, fixedRate = 1000)
    public void execute2() {
        System.out.println(Thread.currentThread().getName() + " execute2");
    }
}
