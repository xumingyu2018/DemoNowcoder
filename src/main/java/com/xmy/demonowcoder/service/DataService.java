package com.xmy.demonowcoder.service;

import com.xmy.demonowcoder.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author xumingyu
 * @date 2022/5/25
 **/
@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 将Date类型转化为String类型
    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    // 将指定ip计入UV---k:当前时间 v:ip
    public void recordUV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    // 统计指定日期范围内的ip访问数UV
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (start.after(end)) {
            throw new IllegalArgumentException("请输入正确的时间段！");
        }
        // 整理该日期范围内的Key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            // 获取该日期范围内的Key
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            // 日期+1(按照日历格式)
            calendar.add(Calendar.DATE, 1);
        }
        // 合并日期范围内相同的ip
        String redisKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        // 获取keyList中的每一列key进行合并
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());

        // 返回统计结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    // 将指定用户计入DAU --k:当前时间 v:userId
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    // 统计指定日期范围内的DAU日活跃用户
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (start.before(end)) {
            throw new IllegalArgumentException("请输入正确的时间段！");
        }
        // 整理该日期范围内的Key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            // 日期+1(按照日历格式)
            calendar.add(Calendar.DATE, 1);
        }

        // 进行OR运算
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));

                connection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });


    }
}
