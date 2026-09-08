package com.qkit.framework.jackson;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局时间格式化：LocalDateTime 输出 yyyy-MM-dd HH:mm:ss，LocalDate 输出 yyyy-MM-dd。
 * application.yml 的 spring.jackson.date-format 仅对 java.util.Date 生效，对 LocalDateTime 无效。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonLocalDateTimeCustomizer() {
        return builder -> builder
                .serializers(
                        new LocalDateTimeSerializer(DATE_TIME_FORMATTER),
                        new LocalDateSerializer(DATE_FORMATTER))
                .deserializers(
                        new LocalDateTimeDeserializer(DATE_TIME_FORMATTER),
                        new LocalDateDeserializer(DATE_FORMATTER));
    }
}