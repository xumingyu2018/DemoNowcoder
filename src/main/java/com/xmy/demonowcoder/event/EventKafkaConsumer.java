//package com.xmy.demonowcoder.event;
//
//import com.alibaba.fastjson.JSONObject;
//import com.qiniu.common.QiniuException;
//import com.qiniu.common.Zone;
//import com.qiniu.http.Response;
//import com.qiniu.storage.Configuration;
//import com.qiniu.storage.UploadManager;
//import com.qiniu.util.Auth;
//import com.qiniu.util.StringMap;
//import com.xmy.demonowcoder.entities.DiscussPost;
//import com.xmy.demonowcoder.entities.Event;
//import com.xmy.demonowcoder.entities.Message;
//import com.xmy.demonowcoder.service.DiscussPostService;
//import com.xmy.demonowcoder.service.ElasticsearchService;
//import com.xmy.demonowcoder.service.MessageService;
//import com.xmy.demonowcoder.util.CommunityConstant;
//import com.xmy.demonowcoder.util.CommunityUtil;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.Future;
//
///**
// * Kafka事件消费者(被动调用)
// * 对Message表扩充：1：系统通知，当生产者调用时，存入消息队列，消费者自动调用将event事件相关信息存入Message表
// *
// * @author xumingyu
// * @date 2022/5/17
// **/
//@Component
//public class EventConsumer implements CommunityConstant {
//
//    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
//
//    @Autowired
//    private MessageService messageService;
//
//    @Autowired
//    private DiscussPostService discussPostService;
//
//    @Autowired
//    private ElasticsearchService elasticsearchService;
//
//    /**
//     * 执行wk命令行的位置
//     **/
//    @Value("${wk.image.command}")
//    private String wkImageCommand;
//
//    /**
//     * 存储wk图片位置
//     **/
//    @Value("${wk.image.storage}")
//    private String wkImageStorage;
//
//    /**
//     * 使用云服务器获取长图
//     */
//    @Value("${qiniu.key.access}")
//    private String accessKey;
//
//    @Value("${qiniu.key.secret}")
//    private String secretKey;
//
//    @Value("${qiniu.bucket.share.name}")
//    private String shareBucketName;
//
//    /**
//     * 定时器避免还没生成图片就上传服务器
//     **/
//    @Autowired
//    private ThreadPoolTaskScheduler taskScheduler;
//
//    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
//    public void handleCommentMessage(ConsumerRecord record) {
//        if (record == null || record.value() == null) {
//            logger.error("消息的内容为空!");
//            return;
//        }
//        // 将record.value字符串格式转化为Event对象
//        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
//        // 注意：event若data=null,是fastjson依赖版本的问题
//        if (event == null) {
//            logger.error("消息格式错误!");
//            return;
//        }
//
//        Message message = new Message();
//        message.setFromId(SYSTEM_USER_ID);
//        // Message表中ToId设置为被发起事件的用户id
//        message.setToId(event.getEntityUserId());
//        // ConversationId设置为事件的主题（点赞、评论、关注）
//        message.setConversationId(event.getTopic());
//        message.setCreateTime(new Date());
//
//        // 设置content为可扩展内容，封装在Map集合中,用于显示xxx评论..了你的帖子
//        HashMap<String, Object> content = new HashMap<>();
//        content.put("userId", event.getUserId());
//        content.put("entityId", event.getEntityId());
//        content.put("entityType", event.getEntityType());
//
//        // 将event.getData里的k-v存到context这个Map中，再封装进message
//        // Map.Entry是为了更方便的输出map键值对,Entry可以一次性获得key和value者两个值
//        if (!event.getData().isEmpty()) {
//            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
//                content.put(entry.getKey(), entry.getValue());
//            }
//        }
//        // 将content(map类型)转化成字符串类型封装进message
//        message.setContent(JSONObject.toJSONString(content));
//        messageService.addMessage(message);
//
//    }
//
//    /**
//     * 消费帖子发布事件，将新增的帖子和添加评论后帖子评论数通过消息队列的方式save进Elastisearch服务器中
//     * @param record
//     */
//    @KafkaListener(topics = {TOPIC_PUBILISH})
//    public void handleDiscussPostMessage(ConsumerRecord record) {
//        if (record == null || record.value() == null) {
//            logger.error("消息的内容为空!");
//            return;
//        }
//        // 将record.value字符串格式转化为Event对象
//        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
//        // 注意：event若data=null,是fastjson依赖版本的问题
//        if (event == null) {
//            logger.error("消息格式错误!");
//            return;
//        }
//
//        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
//        elasticsearchService.saveDiscussPost(post);
//
//    }
//
//    /**
//     * 帖子删除事件
//     **/
//    @KafkaListener(topics = {TOPIC_DELETE})
//    public void handleDeleteMessage(ConsumerRecord record) {
//        if (record == null || record.value() == null) {
//            logger.error("消息的内容为空!");
//            return;
//        }
//        // 将record.value字符串格式转化为Event对象
//        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
//        // 注意：event若data=null,是fastjson依赖版本的问题
//        if (event == null) {
//            logger.error("消息格式错误!");
//            return;
//        }
//
//        elasticsearchService.deleteDiscussPost(event.getEntityId());
//    }
//
//    /**
//     * 消费wkhtmltopdf分享事件
//     */
//    @KafkaListener(topics = TOPIC_SHARE)
//    public void handleShareMessage(ConsumerRecord record) {
//        if (record == null || record.value() == null) {
//            logger.error("消息的内容为空!");
//            return;
//        }
//        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
//        if (event == null) {
//            logger.error("消息格式错误!");
//            return;
//        }
//
//        String htmlUrl = (String) event.getData().get("htmlUrl");
//        String fileName = (String) event.getData().get("fileName");
//        String suffix = (String) event.getData().get("suffix");
//
//        // 执行cmd d:/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com d:/wkhtmltopdf/wk-images/2.png命令
//        String cmd = wkImageCommand + " --quality 75 "
//                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
//        try {
//            Runtime.getRuntime().exec(cmd);
//            logger.info("生成长图成功: " + cmd);
//        } catch (IOException e) {
//            logger.error("生成长图失败: " + e.getMessage());
//        }
//
//        // 启用定时器,监视该图片,一旦生成了,则上传至七牛云.
//        UploadTask task = new UploadTask(fileName, suffix);
//        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
//        task.setFuture(future);
//    }
//
//    class UploadTask implements Runnable {
//
//        // 文件名称
//        private String fileName;
//        // 文件后缀
//        private String suffix;
//        // 启动任务的返回值
//        private Future future;
//        // 开始时间
//        private long startTime;
//        // 上传次数
//        private int uploadTimes;
//
//        public UploadTask(String fileName, String suffix) {
//            this.fileName = fileName;
//            this.suffix = suffix;
//            this.startTime = System.currentTimeMillis();
//        }
//
//        public void setFuture(Future future) {
//            this.future = future;
//        }
//
//        @Override
//        public void run() {
//            // 生成失败
//            if (System.currentTimeMillis() - startTime > 30000) {
//                logger.error("执行时间过长,终止任务:" + fileName);
//                future.cancel(true);
//                return;
//            }
//            // 上传失败
//            if (uploadTimes >= 3) {
//                logger.error("上传次数过多,终止任务:" + fileName);
//                future.cancel(true);
//                return;
//            }
//
//            String path = wkImageStorage + "/" + fileName + suffix;
//            File file = new File(path);
//            if (file.exists()) {
//                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
//                // 设置响应信息
//                StringMap policy = new StringMap();
//                policy.put("returnBody", CommunityUtil.getJSONString(0));
//                // 生成上传凭证
//                Auth auth = Auth.create(accessKey, secretKey);
//                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
//                // 指定上传机房
//                UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));
//                try {
//                    // 开始上传图片
//                    Response response = manager.put(
//                            path, fileName, uploadToken, null, "image/" + suffix, false);
//                    // 处理响应结果
//                    JSONObject json = JSONObject.parseObject(response.bodyString());
//                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
//                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
//                    } else {
//                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
//                        future.cancel(true);
//                    }
//                } catch (QiniuException e) {
//                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
//                }
//            } else {
//                logger.info("等待图片生成[" + fileName + "].");
//            }
//        }
//    }
//}
