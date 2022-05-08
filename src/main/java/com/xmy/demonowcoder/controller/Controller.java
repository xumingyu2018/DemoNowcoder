package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.service.TestService;
import com.xmy.demonowcoder.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo小示例
 *
 * @author xumingyu
 * @date 2022/4/10
 **/
@org.springframework.stereotype.Controller
@RequestMapping("/test")
public class Controller {

    @Autowired
    private TestService testService;

    @RequestMapping("/hello")
    @ResponseBody//响应文本（注意不是页面）
    public String sayHello(){
        return "hello";
    }

    @RequestMapping("/data")
    @ResponseBody
    public String getData(){
        return testService.find();
    }

    //URL参数处理1
    @RequestMapping(value = "/students",method = RequestMethod.GET)
    @ResponseBody
    public String getStudents(@RequestParam(value = "current",required = false,defaultValue = "1") int current,
                              @RequestParam(value = "limit",required = false,defaultValue = "10") int limit){
        System.out.println(current);
        System.out.println(limit);
        return "some students";
    }

    //URL参数处理2
    @RequestMapping(value = "/student/{id}",method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id){
        System.out.println(id);
        return "some students";
    }

    //POST请求
    @RequestMapping(value = "/addstudent",method = RequestMethod.POST)
    @ResponseBody//返回HTML页面不加！
    public String addStudent(String name,int password){
        System.out.println(name);
        System.out.println(password);

        return "success ";
    }

    //响应HTML页面数据方法1
    @RequestMapping(value = "/teacher1",method = RequestMethod.GET)
    public ModelAndView getTeacher1(){
        ModelAndView mv = new ModelAndView();
        mv.addObject("name","张三老师");
        mv.addObject("age","40");
        mv.setViewName("/view");
        return mv;
    }

    //响应HTML页面数据方法2
    @RequestMapping(value = "/teacher2",method = RequestMethod.GET)
    public String getTeacher2(Model model){
        model.addAttribute("name","李四老师");
        model.addAttribute("age","30");
        return "view";
    }

    //响应JSON字符串
    //Java对象-->JSON字符串-->JS对象
    @RequestMapping(value = "/json", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "王五");
        map.put("age", "20");
        return map;
    }

    /**
     * Cookie示例(获取Cookie时@CookieValue有点问题！！)
     */
    @RequestMapping(value = "/cookie/set", method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response) {
        //cookie存的必须是字符串
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        cookie.setPath("/Community/test");
        cookie.setMaxAge(60 * 10);
        response.addCookie(cookie);

        return "set cookie!";
    }

    @RequestMapping(value = "/cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code) {
        System.out.println(code);
        return "get cookie!";
    }

    /**
     * Session示例
     */
    @RequestMapping(value = "/session/set", method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session) {
        session.setAttribute("id", 1);
        session.setAttribute("name", "xmy");
        return "set session!";
    }

    @RequestMapping(value = "/session/get", method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session) {
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session!";
    }

    /**
     * Ajax异步请求示例
     */
    @RequestMapping(value = "/ajax", method = RequestMethod.POST)
    @ResponseBody
    public String testAjax(String name, int age) {
        System.out.println(name);
        System.out.println(age);
        return CommunityUtil.getJSONString(200, "操作成功！");
    }


}
