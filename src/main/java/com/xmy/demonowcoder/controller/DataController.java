package com.xmy.demonowcoder.controller;

import com.xmy.demonowcoder.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

/**
 * 统计网站访问人数
 *
 * @author xumingyu
 * @date 2022/5/25
 **/
@Controller
public class DataController {

    @Autowired
    private DataService dataService;

    /**
     * 统计页面
     **/
    @RequestMapping(value = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    /**
     * 统计网站UV(ip访问数量)
     * @DateTimeFormat将时间参数转化为字符串
     */
    @RequestMapping(path = "/data/uv", method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start, @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
        // 转发到 /data请求
        return "forward:/data";
    }

    /**
     * 统计网站DAU(登录用户访问数量)
     **/
    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start, @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long dau = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", dau);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        return "forward:/data";
    }
}
