package com.xmy.demonowcoder.event;

import com.alibaba.fastjson.JSONObject;
import com.xmy.demonowcoder.entities.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka事件生产者（主动调用）相当于一个开关
 *
 * @author xumingyu
 * @date 2022/5/17
 **/
@Component
public class EventProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    // 处理事件
    public void fireMessage(Event event) {
        // 将事件发布到指定的主题,内容为event对象转化的json格式字符串
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
