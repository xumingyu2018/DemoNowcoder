package com.xmy.demonowcoder.event;

import com.alibaba.fastjson.JSONObject;
import com.xmy.demonowcoder.config.RabbitConfig;
import com.xmy.demonowcoder.entities.Event;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ事件生产者（主动调用）相当于一个开关
 *
 * @author xumingyu
 * @date 2022/6/26
 **/
@Component
public class EventProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** 使用RabittMQ处理事件 **/
    public void fireMessage(Event event) {
        // 将事件发布到指定的主题,内容为event对象转化的json格式字符串
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, event.getTopic(), JSONObject.toJSONString(event));
    }
}
