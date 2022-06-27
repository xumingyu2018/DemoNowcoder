package com.xmy.demonowcoder;

import com.xmy.demonowcoder.config.RabbitConfig;
import com.xmy.demonowcoder.util.CommunityConstant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static com.xmy.demonowcoder.util.CommunityConstant.*;

/**
 * @author xumingyu
 * @date 2022/6/26
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DemoNowcoderApplication.class)
public class RabbitTests implements CommunityConstant {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void RabbitProducer() {
//        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME,"boot.haha","Spring整合RabbitMQ!");
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "comment", "发送评论！");
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "like", "用户点赞了！");
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "follow", "用户关注了！");
    }
}

@Component
class RabbitConsumer {
    /**
     * 监听某个队列的消息
     *
     * @param message 接收到的消息
     */
    @RabbitListener(queues = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void myListener1(String message) {
        System.out.println("消费者接收到的消息为：" + message);
    }

//    @RabbitListener(queues = "publish")
//    public void myListener2(String message){
//        System.out.println("消费者接收到的消息为：" + message);
//    }


}
