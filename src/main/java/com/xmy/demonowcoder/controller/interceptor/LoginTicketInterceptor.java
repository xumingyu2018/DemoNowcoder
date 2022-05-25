package com.xmy.demonowcoder.controller.interceptor;

import com.xmy.demonowcoder.entities.LoginTicket;
import com.xmy.demonowcoder.entities.User;
import com.xmy.demonowcoder.service.UserService;
import com.xmy.demonowcoder.util.CookieUtil;
import com.xmy.demonowcoder.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * 登录拦截器(显示登录信息)
 * @author xumingyu
 * @date 2022/4/27
 **/
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;

    @Override
    /**在Controller访问所有路径之前获取凭证**/
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /**从浏览器Cookie中获取凭证**/
        String ticket = CookieUtil.getValue(request, "ticket");

        if (ticket != null) {
            //查询凭证
            LoginTicket loginTicket = userService.getLoginTicket(ticket);
            //检查凭证是否有效(after：当前时间之后)
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                //根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                /**在本次请求中持有用户
                 * 类似于存入Map,只是考虑到多线程
                 */
                hostHolder.setUser(user);
                /**
                 * 构建用户认证结果,并存入SecurityContext,以便于Security进行授权
                 */
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    @Override
    /**模板之前处理数据**/
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //释放线程资源
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
