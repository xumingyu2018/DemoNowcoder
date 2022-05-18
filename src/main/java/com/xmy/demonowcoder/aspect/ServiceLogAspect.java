package com.xmy.demonowcoder.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Aop统一记录日志
 *
 * @author xumingyu
 * @date 2022/5/7
 **/
@Component
@Aspect
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    @Pointcut("execution(* com.xmy.demonowcoder.service.*.*(..))")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        // 用户ip[1.2.3.4],在[xxx],访问了[com.nowcoder.community.service.xxx()].
        // 通过RequestContextHolder获取request
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        // 通过request.getRemoteHost获取当前用户ip
        String ip = request.getRemoteHost();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        /**
         * joinPoint.getSignature().getDeclaringTypeName()-->com.nowcoder.community.service
         * joinPoint.getSignature().getName() -->方法名
         **/
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        // String.format()加工字符串
        logger.info(String.format("用户[%s],在[%s],访问了[%s].", ip, time, target));
    }
}
