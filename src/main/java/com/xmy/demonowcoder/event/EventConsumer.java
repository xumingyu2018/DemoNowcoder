package com.xmy.demonowcoder.event;

import com.alibaba.fastjson.JSONObject;
import com.xmy.demonowcoder.entities.DiscussPost;
import com.xmy.demonowcoder.entities.Event;
import com.xmy.demonowcoder.entities.Message;
import com.xmy.demonowcoder.service.DiscussPostService;
import com.xmy.demonowcoder.service.ElasticsearchService;
import com.xmy.demonowcoder.service.MessageService;
import com.xmy.demonowcoder.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    // 执行wk命令行的位置
    @Value("${wk.image.command}")
    private String wkImageCommand;

    // 存储wk图片位置
    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 将record.value字符串格式转化为Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        // 注意：event若data=null,是fastjson依赖版本的问题
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

    /**
     * 消费帖子发布事件，将新增的帖子和添加评论后帖子评论数通过消息队列的方式save进Elastisearch服务器中
     * @param record
     */
    @KafkaListener(topics = {TOPIC_PUBILISH})
    public void handleDiscussPostMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 将record.value字符串格式转化为Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        // 注意：event若data=null,是fastjson依赖版本的问题
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);

    }

    /**
     * 帖子删除事件
     **/
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        // 将record.value字符串格式转化为Event对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        // 注意：event若data=null,是fastjson依赖版本的问题
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }

    /**
     * 消费wkhtmltopdf工具分享事件
     */
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }

        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        // 执行cmd d:/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com d:/wkhtmltopdf/wk-images/2.png命令
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        // 启用定时器,监视该图片,一旦生成了,则上传至七牛云.
//        UploadTask task = new UploadTask(fileName, suffix);
//        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
//        task.setFuture(future);
    }
}
