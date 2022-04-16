package com.xmy.demonowcoder.dao;

import org.springframework.stereotype.Repository;

/**
 * @author xumingyu
 * @date 2022/4/10
 **/
@Repository("Hibernate")//自定义bean的名字
//访问数据库的bean，加了这个注解spring会自动扫描
public class TestDaoHibernateImpl implements TestDao{
    @Override
    public String select() {
        return "Hibernate";
    }
}
