package com.xmy.demonowcoder.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

/**
 * AOPDemo实例
 *
 * @author xumingyu
 * @date 2022/5/7
 **/
//@Component
//@Aspect
public class DemoAspect {
    /**
     * 第一个* ：方法的任何返回值
     * com.xmy.demonowcoder.service.*.*(..)) ：service包下的所有类所有方法所有参数(..)
     */
    @Pointcut("execution(* com.xmy.demonowcoder.service.*.*(..))")
    public void pointcut() {
    }

    /**
     * 切点方法之前执行
     **/
    @Before("pointcut()")
    public void before() {
        System.out.println("before");
    }

    @After("pointcut()")
    public void after() {
        System.out.println("after");
    }

    /**
     * 返回值以后执行
     **/
    @AfterReturning("pointcut()")
    public void afterRetuning() {
        System.out.println("afterRetuning");
    }

    /**
     * 抛出异常以后执行
     **/
    @AfterThrowing("pointcut()")
    public void afterThrowing() {
        System.out.println("afterThrowing");
    }

    /**
     * 切点的前和后都可以执行
     **/
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("around before");
        Object obj = joinPoint.proceed();
        System.out.println("around after");
        return obj;
    }
}
