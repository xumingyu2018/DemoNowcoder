package com.xmy.demonowcoder.event;

import com.alibaba.fastjson.JSONObject;
import com.xmy.demonowcoder.entities.Event;
import com.xmy.demonowcoder.entities.Message;
import com.xmy.demonowcoder.service.MessageService;
import com.xmy.demonowcoder.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka事件消费者(被动调用)
 * 对Message表扩充：1：系统通知，当生产者调用时，存入消息队列，消费者自动调用将event事件相关信息存入Message表
 *
 * @author xumingyu
 * @date 2022/5/17
 **/
@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 将event字符串格式转化为Object对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        // Message表中ToId设置为被发起事件的用户id
        message.setToId(event.getEntityUserId());
        // ConversationId设置为事件的主题（点赞、评论、关注）
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        // 设置content为可扩展内容，封装在Map集合中,用于显示xxx评论..了你的帖子
        HashMap<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityId", event.getEntityId());
        content.put("entityType", event.getEntityType());

        // 将event.getData里的k-v存到context这个Map中，再封装进message
        // Map.Entry是为了更方便的输出map键值对,Entry可以一次性获得key和value者两个值
        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }
        // 将content(map类型)转化成字符串类型封装进message
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);

    }
}
