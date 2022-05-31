package com.xmy.demonowcoder;

import com.xmy.demonowcoder.dao.TestDao;
import com.xmy.demonowcoder.service.TestService;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootTest
@ContextConfiguration(classes = DemoNowcoderApplication.class)//启用DemoNowcoderApplication类作为配置类
class DemoNowcoderApplicationTests implements ApplicationContextAware {//ApplicationContextAware实现spring容器

    private ApplicationContext applicationContext;

    //ApplicationContext继承HierarchicalBeanFactory继承BeanFactory（BeanFactory是spring容器顶层接口）
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext=applicationContext;
    }

    @Test//测试spring容器
    public void testApplicationContext(){
        System.out.println(applicationContext);//打印spring容器

        TestDao bean1 = applicationContext.getBean(TestDao.class);//@Primary优先装配
        System.out.println(bean1.select());//调用dao层的bean

        TestDao bean2 = applicationContext.getBean("Hibernate", TestDao.class);
        System.out.println(bean2.select());
    }

    @Test
    public void testBeanManagement(){//spring管理bean的生命周期
        TestService bean1 = applicationContext.getBean(TestService.class);
        System.out.println(bean1);//被spring管理的bean默认是单例的

        TestService bean2 = applicationContext.getBean(TestService.class);
        System.out.println(bean2);
    }

    @Test
    public void testBeanConfig(){
        SimpleDateFormat bean = applicationContext.getBean(SimpleDateFormat.class);
        System.out.println(bean.format(new Date()));
    }

    //----以上是直接获取bean的方法--------
    //----以下是通过依赖注入方法获取bean(常用)-------


    @Autowired//直接将bean注入到spring容器
    private SimpleDateFormat simpleDateFormat;

    @Autowired
    @Qualifier("Hibernate")//在相同父类中，指定bean注入
    private TestDao testDao;

    @Autowired
    private TestService testService;

    @Test
    public void testDI(){
        System.out.println(testDao.select());
        System.out.println(simpleDateFormat.format(new Date()));
        System.out.println(testService);
    }



}
