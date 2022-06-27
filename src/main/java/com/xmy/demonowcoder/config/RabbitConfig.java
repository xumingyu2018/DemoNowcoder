package com.xmy.demonowcoder.config;

import com.xmy.demonowcoder.util.CommunityConstant;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xumingyu
 * @date 2022/6/26
 **/
@Configuration
public class RabbitConfig implements CommunityConstant {
    /**
     * 定义交换机名称
     **/
    public static final String EXCHANGE_NAME = "community_exchange";

    /**
     * 交换机
     **/
    @Bean("Exchange")
    public Exchange Exchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    /**
     * 定义队列名称
     **/
    @Bean("comment")
    public Queue commentQueue() {
        return QueueBuilder.durable(TOPIC_COMMENT).build();
    }

    @Bean("like")
    public Queue likeQueue() {
        return QueueBuilder.durable(TOPIC_LIKE).build();
    }

    @Bean("follow")
    public Queue followQueue() {
        return QueueBuilder.durable(TOPIC_FOLLOW).build();
    }

    @Bean("publish")
    public Queue publishQueue() {
        return QueueBuilder.durable(TOPIC_PUBILISH).build();
    }

    @Bean("delete")
    public Queue deleteQueue() {
        return QueueBuilder.durable(TOPIC_DELETE).build();
    }

    @Bean("share")
    public Queue shareQueue() {
        return QueueBuilder.durable(TOPIC_SHARE).build();
    }

    /**
     * 队列和交换机绑定关系
     * routing key
     */
    @Bean
    public Binding getCommentBinding(@Qualifier("comment") Queue queue,
                                     @Qualifier("Exchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(TOPIC_COMMENT).noargs();
    }

    @Bean
    public Binding getLikeBinding(@Qualifier("like") Queue queue,
                                  @Qualifier("Exchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(TOPIC_LIKE).noargs();
    }

    @Bean
    public Binding getFollowBinding(@Qualifier("follow") Queue queue,
                                    @Qualifier("Exchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(TOPIC_FOLLOW).noargs();
    }

    @Bean
    public Binding getPublishBinding(@Qualifier("publish") Queue queue,
                                     @Qualifier("Exchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(TOPIC_PUBILISH).noargs();
    }

    @Bean
    public Binding getDeleteBinding(@Qualifier("delete") Queue queue,
                                    @Qualifier("Exchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(TOPIC_DELETE).noargs();
    }

    @Bean
    public Binding getShareBinding(@Qualifier("share") Queue queue,
                                   @Qualifier("Exchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(TOPIC_SHARE).noargs();
    }
}
