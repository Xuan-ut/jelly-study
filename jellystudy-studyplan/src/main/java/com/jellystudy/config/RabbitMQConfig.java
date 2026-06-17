package com.jellystudy.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String STUDY_EVENT_EXCHANGE = "study.event.exchange";
    public static final String STUDY_EVENT_QUEUE = "study.event.queue";
    public static final String ACHIEVEMENT_QUEUE = "study.achievement.queue";
    public static final String NOTIFICATION_QUEUE = "study.notification.queue";

    public static final String ACTIVITY_EXCHANGE = "jellystudy.activity.exchange";
    public static final String ACTIVITY_QUEUE = "jellystudy.activity.queue";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange studyEventExchange() {
        return new TopicExchange(STUDY_EVENT_EXCHANGE);
    }

    @Bean
    public TopicExchange activityExchange() {
        return new TopicExchange(ACTIVITY_EXCHANGE);
    }

    @Bean
    public Queue studyEventQueue() {
        return new Queue(STUDY_EVENT_QUEUE, true);
    }

    @Bean
    public Queue achievementQueue() {
        return new Queue(ACHIEVEMENT_QUEUE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue activityQueue() {
        return new Queue(ACTIVITY_QUEUE, true);
    }

    @Bean
    public Binding studyEventBinding(Queue studyEventQueue, TopicExchange studyEventExchange) {
        return BindingBuilder.bind(studyEventQueue).to(studyEventExchange).with("study.#");
    }

    @Bean
    public Binding achievementBinding(Queue achievementQueue, TopicExchange studyEventExchange) {
        return BindingBuilder.bind(achievementQueue).to(studyEventExchange).with("*.completed");
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange studyEventExchange) {
        return BindingBuilder.bind(notificationQueue).to(studyEventExchange).with("plan.#");
    }

    @Bean
    public Binding activityBinding(Queue activityQueue, TopicExchange activityExchange) {
        return BindingBuilder.bind(activityQueue).to(activityExchange).with("activity.#");
    }
}
