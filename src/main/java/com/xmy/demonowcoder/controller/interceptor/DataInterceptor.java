package com.xmy.demonowcoder.controller.interceptor;

import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.service.DataService;
import com.xmy.demonowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author xumingyu
 * @date 2022/5/25
 **/
@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dataService;
    @Autowired
    private HostHolder hostHolder;

    // 在所有请求之前存用户访问数和日活跃人数
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求用户的ip地址
        String ip = request.getRemoteHost();
        // 统计UV
        dataService.recordUV(ip);

        // 统计DAU
        User user = hostHolder.getUser();
        if (user != null) {
            dataService.recordDAU(user.getId());
        }
        return true;
    }
}
