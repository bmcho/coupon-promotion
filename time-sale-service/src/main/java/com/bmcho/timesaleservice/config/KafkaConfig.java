package com.bmcho.timesaleservice.config;

import com.bmcho.timesaleservice.dto.PurchaseRequestMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
//    private static final String BOOTSTRAP_SERVERS = "localhost:9091";
//    private static final String GROUP_ID = "time-sale-group";
//
//
//    @Bean
//    public ProducerFactory<String, PurchaseRequestMessage> producerFactory() {
//        Map<String , Object> config = new HashMap<>();
//        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
//        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//
//        //멱등성 보장
//        config.put(ProducerConfig.ACKS_CONFIG, "all");
//        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
//        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
//
//        return new DefaultKafkaProducerFactory<>(config);
//    }
//
//    @Bean
//    public KafkaTemplate<String, PurchaseRequestMessage> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//
//    @Bean
//    public ConsumerFactory<String, PurchaseRequestMessage> consumerFactory() {
//        Map<String , Object> config = new HashMap<>();
//        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
//        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        config.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
//        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
//
//         /*
//            실서비스에서는 대부분 latest
//            로컬 개발/테스트용으로만 earliest를 쓰는 경우가 많음.
//         */
//        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
//
//        JsonDeserializer<PurchaseRequestMessage> jsonDeserializer = new JsonDeserializer<>(PurchaseRequestMessage.class);
//        jsonDeserializer.addTrustedPackages("*");
////        jsonDeserializer.setUseTypeMapperForKey(true); // 타입 매핑 활성화 - StringDeserializer 사용중 무의미
//        jsonDeserializer.setRemoveTypeHeaders(false);  // 헤더 유지
//        return new DefaultKafkaConsumerFactory<>(
//                config,
//                new StringDeserializer(),
//                jsonDeserializer);
//    }
//
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, PurchaseRequestMessage> kafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, PurchaseRequestMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory());
//        return factory;
//    }

}
