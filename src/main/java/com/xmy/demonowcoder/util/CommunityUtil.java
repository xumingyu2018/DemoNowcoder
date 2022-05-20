package com.xmy.demonowcoder.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.UUID;

/**
 * @author xumingyu
 * @date 2022/4/20
 **/
public class CommunityUtil {

    /*生成随机字符串
     * 用于邮件激活码，salt5位随机数加密
     * */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /*MD5加密
     *hello-->abc123def456
     *hello + 3e4a8-->abc123def456abc
     */
    public static String md5(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        //MD5加密方法
        return DigestUtils.md5DigestAsHex(key.getBytes());
        //参数是bytes型
    }

    /**
     * 标准的返回格式(带3个参数)
     * 方便前后端交互
     */

    public static String getJSONString(int code, String msg, Map<String, Object> map) {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", msg);

        if (map != null) {
            //从map里的key集合中取出每一个key
            for (String key : map.keySet()) {
                json.put(key, map.get(key));
            }
        }
        return json.toJSONString();
    }

    public static String getJSONString(int code, String msg) {
        return getJSONString(code, msg, null);
    }

    public static String getJSONString(int code) {
        return getJSONString(code, null, null);
    }

//    public static void main(String[] args) {
//        HashMap<String, Object> map = new HashMap<>();
//        map.put("name","张三");
//        map.put("age","25");
//        System.out.println(getJSONString(404,"ok",map));
//    }
}
