package com.jellystudy.companion.config;

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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    public static final String EXCHANGE_LEARNING_EVENT = "jellystudy.learning.event";
    public static final String EXCHANGE_STUDY_EVENT = "study.event.exchange";
    public static final String EXCHANGE_ACTIVITY = "jellystudy.activity.exchange";

    public static final String QUEUE_SPIRIT_FEED = "companion.spirit.feed";
    public static final String QUEUE_HIVE_COLLECT = "companion.hive.collect";
    public static final String QUEUE_SPIRIT_NOTIFY = "companion.spirit.notify";

    // ===== Exchanges =====
    @Bean
    public TopicExchange learningEventExchange() {
        return new TopicExchange(EXCHANGE_LEARNING_EVENT);
    }

    @Bean
    public TopicExchange studyEventExchange() {
        return new TopicExchange(EXCHANGE_STUDY_EVENT);
    }

    @Bean
    public TopicExchange activityExchange() {
        return new TopicExchange(EXCHANGE_ACTIVITY);
    }

    // ===== Queues =====
    @Bean
    public Queue spiritFeedQueue() {
        return new Queue(QUEUE_SPIRIT_FEED, true);
    }

    @Bean
    public Queue hiveCollectQueue() {
        return new Queue(QUEUE_HIVE_COLLECT, true);
    }

    @Bean
    public Queue spiritNotifyQueue() {
        return new Queue(QUEUE_SPIRIT_NOTIFY, true);
    }

    // ===== Bindings to jellystudy.learning.event =====
    @Bean
    public Binding spiritFeedBinding(Queue spiritFeedQueue, TopicExchange learningEventExchange) {
        return BindingBuilder.bind(spiritFeedQueue).to(learningEventExchange).with("task.#");
    }

    @Bean
    public Binding stageCompletedBinding(Queue spiritFeedQueue, TopicExchange learningEventExchange) {
        return BindingBuilder.bind(spiritFeedQueue).to(learningEventExchange).with("stage.#");
    }

    @Bean
    public Binding planBinding(Queue spiritFeedQueue, TopicExchange learningEventExchange) {
        return BindingBuilder.bind(spiritFeedQueue).to(learningEventExchange).with("plan.#");
    }

    @Bean
    public Binding hiveCollectBinding(Queue hiveCollectQueue, TopicExchange learningEventExchange) {
        return BindingBuilder.bind(hiveCollectQueue).to(learningEventExchange).with("#");
    }

    @Bean
    public Binding spiritNotifyBinding(Queue spiritNotifyQueue, TopicExchange learningEventExchange) {
        return BindingBuilder.bind(spiritNotifyQueue).to(learningEventExchange).with("spirit.#");
    }

    // ===== Bindings to study.event.exchange (from studyplan service) =====
    @Bean
    public Binding studyPlanFeedBinding(Queue spiritFeedQueue, TopicExchange studyEventExchange) {
        return BindingBuilder.bind(spiritFeedQueue).to(studyEventExchange).with("plan.#");
    }

    @Bean
    public Binding studyStageBinding(Queue spiritFeedQueue, TopicExchange studyEventExchange) {
        return BindingBuilder.bind(spiritFeedQueue).to(studyEventExchange).with("stage.#");
    }

    @Bean
    public Binding studyTaskBinding(Queue spiritFeedQueue, TopicExchange studyEventExchange) {
        return BindingBuilder.bind(spiritFeedQueue).to(studyEventExchange).with("task.#");
    }

    @Bean
    public Binding studyKnowledgeBinding(Queue spiritFeedQueue, TopicExchange studyEventExchange) {
        return BindingBuilder.bind(spiritFeedQueue).to(studyEventExchange).with("knowledge.#");
    }

    @Bean
    public Binding studyHiveBinding(Queue hiveCollectQueue, TopicExchange studyEventExchange) {
        return BindingBuilder.bind(hiveCollectQueue).to(studyEventExchange).with("#");
    }

    // ===== Bindings to jellystudy.activity.exchange =====
    @Bean
    public Binding activityHiveBinding(Queue hiveCollectQueue, TopicExchange activityExchange) {
        return BindingBuilder.bind(hiveCollectQueue).to(activityExchange).with("activity.#");
    }
}
