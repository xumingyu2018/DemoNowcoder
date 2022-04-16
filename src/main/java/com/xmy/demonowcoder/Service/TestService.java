package com.xmy.demonowcoder.Service;

import com.xmy.demonowcoder.dao.TestDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author xumingyu
 * @date 2022/4/10
 **/
@Service
@Scope("prototype")//多实例，默认是单例，每次getbean时实例化
public class TestService {


    @Autowired//sevice调用dao层
    private TestDao testDao;


    public TestService(){
        System.out.println("实例化TestService");
    }

    @PostConstruct//初始方法在构造器之后调用（通过spring容器调用）
    public void init(){
        System.out.println("初始化TestService这个bean");
    }

    //销毁之前调用
    @PreDestroy
    public void destroy(){
        System.out.println("销毁TestService这个bean");
    }

    //开发常用
    public String find(){
        return testDao.select();
    }
}
